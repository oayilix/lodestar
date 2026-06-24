package com.oayilix.lodestar.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteNormalizerTest {

    @Test
    fun normalizesSchemeHostAndPath() {
        assertEquals(
            "router://example.com/app/first",
            RouteNormalizer.normalize("ROUTER://Example.COM/app/./first")
        )
    }

    @Test
    fun ignoresQueryAndFragmentForRouteLookup() {
        assertEquals(
            "router://example.com/app/first",
            RouteNormalizer.normalize("router://example.com/app/first?id=42#section")
        )
    }

    @Test
    fun rejectsMalformedOrRelativeRoutes() {
        assertNull(RouteNormalizer.normalize(""))
        assertNull(RouteNormalizer.normalize("/app/first"))
        assertNull(RouteNormalizer.normalize(" router://example.com/app/first"))
        assertNull(RouteNormalizer.normalize("router:///app/first"))
    }
}
