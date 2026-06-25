package com.oayilix.lodestar.compiler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LodestarRegistrySourceGeneratorTest {

    @Test
    fun generatesStableRegistryClassNameWithExpectedPrefix() {
        val routes = listOf(
            LodestarRegistrySourceGenerator.RouteEntry(
                route = "lodestar://example.com/app/first",
                className = "com.example.FirstActivity"
            )
        )

        val first = LodestarRegistrySourceGenerator.className(routes)
        val second = LodestarRegistrySourceGenerator.className(routes)

        assertEquals(first, second)
        assertTrue(first.startsWith(LodestarRegistrySourceGenerator.REGISTRY_PREFIX))
    }

    @Test
    fun generatedSourceContainsDirectActivityClassLiteralsAndEscapedRoutes() {
        val routes = listOf(
            LodestarRegistrySourceGenerator.RouteEntry(
                route = "lodestar://example.com/app/quote\"page",
                className = "com.example.QuotedActivity"
            )
        )
        val className = LodestarRegistrySourceGenerator.className(routes)

        val source = LodestarRegistrySourceGenerator.generateJava(className, routes)

        assertTrue(source.contains("public final class $className"))
        assertTrue(source.contains("routes.put(\"lodestar://example.com/app/quote\\\"page\", com.example.QuotedActivity.class);"))
        assertTrue(source.contains("return Collections.unmodifiableMap(routes);"))
    }
}
