package com.oayilix.lodestar.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate

/**
 * Validates `@Destination` declarations and generates one deterministic registry per module.
 * 校验 `@Destination` 声明，并为每个模块生成一个确定性的路由表。
 *
 * The generated registry stores direct Activity class literals so shrinkers can preserve and
 * rewrite references safely.
 * 生成的路由表保存 Activity 直接类引用，使代码压缩器能够安全地保留并重写引用。
 */
class DestinationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // KSP may invoke a processor in multiple rounds; a module registry must be emitted once.
        // KSP 可能执行多个处理轮次；每个模块的路由表只能生成一次。
        if (DestinationProcessingPolicy.shouldSkipGeneratedRegistry(generated)) return emptyList()

        val symbols = resolver.getSymbolsWithAnnotation(DESTINATION_ANNOTATION).toList()
        val deferred = symbols.filterNot(KSAnnotated::validate)
        if (DestinationProcessingPolicy.shouldDeferRegistryGeneration(deferred.size)) {
            // Wait for later KSP rounds before writing the single registry; generating now could
            // publish an incomplete route table when another processor creates referenced types.
            // 等待后续 KSP 轮次再写入唯一路由表；提前生成可能在其他处理器生成依赖类型前发布不完整路由表。
            logger.info("Deferring Lodestar registry generation until ${deferred.size} symbols are valid.")
            return deferred
        }

        val invalidTargets = symbols.filterNot { it is KSClassDeclaration }
        invalidTargets.forEach {
            logger.error("@Destination can only annotate a concrete Activity class.", it)
        }
        if (DestinationProcessingPolicy.shouldStopForInvalidTargets(invalidTargets.size)) return emptyList()

        val classes = symbols.filterIsInstance<KSClassDeclaration>()
        if (classes.isEmpty()) return emptyList()

        val routes = classes.mapNotNull(::parseRoute)
        if (DestinationProcessingPolicy.shouldStopForInvalidRoutes(routes.size, classes.size)) return emptyList()

        // Fail in the declaring module before bytecode aggregation to provide source-level errors.
        // 在字节码聚合前于声明模块中失败，以提供源码级错误位置。
        val duplicates = routes.groupBy(RouteDefinition::route).filterValues { it.size > 1 }
        duplicates.forEach { (route, definitions) ->
            val targets = definitions.joinToString { it.className }
            logger.error("Duplicate Lodestar route '$route' in targets: $targets", definitions.first().symbol)
        }
        if (DestinationProcessingPolicy.shouldStopForDuplicates(duplicates.size)) return emptyList()

        // Stable ordering and content hashing make generated names reproducible across clean builds.
        // 稳定排序与内容哈希确保多次干净构建生成相同名称。
        val sortedRoutes = routes.sortedBy(RouteDefinition::route)
        val routeEntries = sortedRoutes.map {
            LodestarRegistrySourceGenerator.RouteEntry(it.route, it.className)
        }
        val generatedClassName = LodestarRegistrySourceGenerator.className(routeEntries)
        val sourceFiles = sortedRoutes.mapNotNull { it.symbol.containingFile }.distinct()
        val content = LodestarRegistrySourceGenerator.generateKotlin(generatedClassName, routeEntries)

        codeGenerator.createNewFile(
            Dependencies(aggregating = true, *sourceFiles.toTypedArray()),
            GENERATED_PACKAGE,
            generatedClassName,
            // KotlinPoet emits Kotlin source, so KSP must place the file under generated .kt sources.
            // KotlinPoet 输出 Kotlin 源码，因此 KSP 必须将文件写入生成的 .kt 源码目录。
            "kt"
        ).bufferedWriter(Charsets.UTF_8).use { it.write(content) }

        generated = true
        logger.info("Generated $GENERATED_PACKAGE.$generatedClassName with ${sortedRoutes.size} routes")
        return emptyList()
    }

    private fun parseRoute(symbol: KSClassDeclaration): RouteDefinition? {
        if (symbol.classKind != ClassKind.CLASS || Modifier.ABSTRACT in symbol.modifiers) {
            logger.error("@Destination can only annotate a concrete class.", symbol)
            return null
        }
        if (symbol.modifiers.any { it == Modifier.PRIVATE || it == Modifier.PROTECTED || it == Modifier.INTERNAL }) {
            logger.error("@Destination class must be public.", symbol)
            return null
        }
        if (!symbol.isSubclassOf(ANDROID_ACTIVITY)) {
            logger.error("@Destination class must extend android.app.Activity.", symbol)
            return null
        }

        val annotation = symbol.annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == DESTINATION_ANNOTATION
        } ?: return null
        val rawRoute = annotation.arguments
            .firstOrNull { it.name?.asString() == "url" }
            ?.value as? String
        val route = rawRoute?.let(DeclaredRouteNormalizer::normalize)
        if (route == null) {
            logger.error(
                "Invalid Lodestar route '$rawRoute'. Expected an absolute URI with scheme, host, and path; " +
                    "query and fragment are not allowed in @Destination.",
                symbol
            )
            return null
        }

        val className = symbol.qualifiedName?.asString()
        if (className == null) {
            logger.error("@Destination cannot annotate a local or anonymous class.", symbol)
            return null
        }
        return RouteDefinition(route, className, symbol)
    }

    private fun KSClassDeclaration.isSubclassOf(target: String): Boolean {
        // Track visited declarations to tolerate malformed or cyclic type graphs from incomplete code.
        // 记录已访问声明，以容忍未完成代码产生的异常或循环类型图。
        val visited = mutableSetOf<String>()
        fun visit(declaration: KSClassDeclaration): Boolean {
            val name = declaration.qualifiedName?.asString() ?: return false
            if (name == target) return true
            if (!visited.add(name)) return false
            return declaration.superTypes.any { type ->
                val parent = type.resolve().declaration as? KSClassDeclaration
                parent != null && visit(parent)
            }
        }
        return visit(this)
    }

    private data class RouteDefinition(
        val route: String,
        val className: String,
        val symbol: KSClassDeclaration
    )

    private companion object {
        const val DESTINATION_ANNOTATION = "com.oayilix.lodestar.annotations.Destination"
        const val ANDROID_ACTIVITY = "android.app.Activity"
        const val GENERATED_PACKAGE = "com.oayilix.lodestar.mapping"
    }
}
