package com.oayilix.lodestar.compiler

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import java.security.MessageDigest

/**
 * Generates deterministic Kotlin source for one module-local Lodestar registry with KotlinPoet.
 * 使用 KotlinPoet 为单个模块生成确定性的 Lodestar 局部路由表 Kotlin 源码。
 *
 * KotlinPoet keeps imports, type names, string escaping, and generic signatures structured instead
 * of relying on manually concatenated source text.
 * KotlinPoet 会结构化处理 import、类型名、字符串转义和泛型签名，避免依赖手动拼接源码文本。
 */
internal object LodestarRegistrySourceGenerator {

    const val REGISTRY_PREFIX = "LodestarRegistry_"

    fun className(routes: List<RouteEntry>): String = "$REGISTRY_PREFIX${stableHash(routes)}"

    fun generateKotlin(className: String, routes: List<RouteEntry>): String =
        // FileSpec represents one generated Kotlin file and owns the package/import section.
        // FileSpec 表示一个生成的 Kotlin 文件，并负责 package/import 区域。
        FileSpec.builder(GENERATED_PACKAGE, className)
            .addType(
                // The registry is emitted as an object so Kotlin still produces one stable class.
                // 路由表以 object 输出，使 Kotlin 仍然生成一个稳定的 class。
                TypeSpec.objectBuilder(className)
                    .addFunction(buildGetFunction(routes))
                    .build()
            )
            .build()
            .toString()

    private fun buildGetFunction(routes: List<RouteEntry>): FunSpec {
        // Map<String, Class<out Activity>> matches the runtime and Gradle aggregation contract.
        // Map<String, Class<out Activity>> 与运行时和 Gradle 聚合约定保持一致。
        val routeMapType = MAP.parameterizedBy(STRING, ACTIVITY_CLASS_TYPE)

        return FunSpec.builder("get")
            // @JvmStatic is required because the Gradle aggregation bytecode invokes get() as a
            // static method on each generated registry class.
            // 必须添加 @JvmStatic，因为 Gradle 聚合字节码会以静态方法形式调用每个生成路由表的 get()。
            .addAnnotation(AnnotationSpec.builder(JVM_STATIC).build())
            .addModifiers(KModifier.PUBLIC)
            .returns(routeMapType)
            .addStatement("val routes = %T()", LINKED_HASH_MAP.parameterizedBy(STRING, ACTIVITY_CLASS_TYPE))
            .apply {
                routes.forEach { route ->
                    // %S escapes route literals safely, while %T imports or qualifies Activity types.
                    // %S 会安全转义路由字符串，%T 会自动导入或限定 Activity 类型。
                    addStatement(
                        "routes[%S] = %T::class.java",
                        route.route,
                        ClassName.bestGuess(route.className)
                    )
                }
            }
            .addStatement("return %T.unmodifiableMap(routes)", COLLECTIONS)
            .build()
    }

    private fun stableHash(routes: List<RouteEntry>): String {
        // The generated class name is content-addressed so clean builds are reproducible.
        // 生成类名由内容哈希决定，因此干净构建结果可复现。
        val input = routes.joinToString("\n") { "${it.route}\u0000${it.className}" }
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    data class RouteEntry(
        val route: String,
        val className: String
    )

    private const val GENERATED_PACKAGE = "com.oayilix.lodestar.mapping"

    // KotlinPoet type tokens used by the generator. Keeping them centralized avoids spelling the
    // same JVM/Kotlin type names across statements.
    // KotlinPoet 类型令牌集中放置，避免在多个 statement 中重复书写 JVM/Kotlin 类型名。
    private val ACTIVITY = ClassName("android.app", "Activity")
    private val ACTIVITY_CLASS_TYPE = ClassName("java.lang", "Class")
        .parameterizedBy(WildcardTypeName.producerOf(ACTIVITY))
    private val COLLECTIONS = ClassName("java.util", "Collections")
    private val JVM_STATIC = ClassName("kotlin.jvm", "JvmStatic")
    private val LINKED_HASH_MAP = ClassName("java.util", "LinkedHashMap")
}
