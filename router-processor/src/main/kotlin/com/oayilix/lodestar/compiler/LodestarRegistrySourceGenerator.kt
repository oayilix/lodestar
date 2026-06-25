package com.oayilix.lodestar.compiler

import java.security.MessageDigest

/**
 * Generates deterministic Java source for one module-local Lodestar registry.
 * 为单个模块生成确定性的 Lodestar 局部路由表 Java 源码。
 */
internal object LodestarRegistrySourceGenerator {

    const val REGISTRY_PREFIX = "LodestarRegistry_"

    fun className(routes: List<RouteEntry>): String = "$REGISTRY_PREFIX${stableHash(routes)}"

    fun generateJava(className: String, routes: List<RouteEntry>): String = buildString {
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

    private fun stableHash(routes: List<RouteEntry>): String {
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

    data class RouteEntry(
        val route: String,
        val className: String
    )

    private const val GENERATED_PACKAGE = "com.oayilix.lodestar.mapping"
}
