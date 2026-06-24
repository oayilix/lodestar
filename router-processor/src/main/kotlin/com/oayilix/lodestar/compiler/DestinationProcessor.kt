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
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

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
        if (generated) return emptyList()

        val symbols = resolver.getSymbolsWithAnnotation(DESTINATION_ANNOTATION).toList()
        val deferred = symbols.filterNot(KSAnnotated::validate)
        val classes = symbols.filter(KSAnnotated::validate).filterIsInstance<KSClassDeclaration>()
        if (classes.isEmpty()) return deferred

        val routes = classes.mapNotNull(::parseRoute)
        if (routes.size != classes.size) return deferred

        // Fail in the declaring module before bytecode aggregation to provide source-level errors.
        // 在字节码聚合前于声明模块中失败，以提供源码级错误位置。
        val duplicates = routes.groupBy(RouteDefinition::route).filterValues { it.size > 1 }
        duplicates.forEach { (route, definitions) ->
            val targets = definitions.joinToString { it.className }
            logger.error("Duplicate Lodestar route '$route' in targets: $targets", definitions.first().symbol)
        }
        if (duplicates.isNotEmpty()) return deferred

        // Stable ordering and content hashing make generated names reproducible across clean builds.
        // 稳定排序与内容哈希确保多次干净构建生成相同名称。
        val sortedRoutes = routes.sortedBy(RouteDefinition::route)
        val generatedClassName = "LodestarRegistry_${stableHash(sortedRoutes)}"
        val sourceFiles = sortedRoutes.mapNotNull { it.symbol.containingFile }.distinct()
        val content = generateJava(generatedClassName, sortedRoutes)

        codeGenerator.createNewFile(
            Dependencies(aggregating = true, *sourceFiles.toTypedArray()),
            GENERATED_PACKAGE,
            generatedClassName,
            "java"
        ).bufferedWriter(Charsets.UTF_8).use { it.write(content) }

        generated = true
        logger.info("Generated $GENERATED_PACKAGE.$generatedClassName with ${sortedRoutes.size} routes")
        return deferred
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
        val route = rawRoute?.let(::normalizeDeclaredRoute)
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

    private fun normalizeDeclaredRoute(value: String): String? = runCatching {
        if (value.isBlank() || value != value.trim()) return null
        val uri = URI(value)
        if (!uri.isAbsolute || uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null
        if (uri.rawQuery != null || uri.rawFragment != null) return null
        val path = uri.normalize().rawPath?.takeIf(String::isNotEmpty) ?: "/"
        "${uri.scheme.lowercase(Locale.ROOT)}://${uri.host.lowercase(Locale.ROOT)}$path"
    }.getOrNull()

    private fun generateJava(className: String, routes: List<RouteDefinition>): String = buildString {
        appendLine("package $GENERATED_PACKAGE;")
        appendLine()
        appendLine("import android.app.Activity;")
        appendLine("import java.util.Collections;")
        appendLine("import java.util.LinkedHashMap;")
        appendLine("import java.util.Map;")
        appendLine()
        appendLine("public final class $className {")
        appendLine("    private $className() {}")
        appendLine()
        appendLine("    public static Map<String, Class<? extends Activity>> get() {")
        appendLine("        Map<String, Class<? extends Activity>> routes = new LinkedHashMap<>();")
        routes.forEach { route ->
            appendLine("        routes.put(\"${escapeJava(route.route)}\", ${route.className}.class);")
        }
        appendLine("        return Collections.unmodifiableMap(routes);")
        appendLine("    }")
        appendLine("}")
    }

    private fun stableHash(routes: List<RouteDefinition>): String {
        val input = routes.joinToString("\n") { "${it.route}\u0000${it.className}" }
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    private fun escapeJava(value: String): String = buildString {
        // Annotation values are source data and must never be interpolated without escaping.
        // 注解值属于源码输入，插入生成代码前必须进行转义。
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) append("\\u%04x".format(char.code)) else append(char)
            }
        }
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
