package com.oayilix.lodestar.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteNormalizerTest {

    @Test
    fun normalizesSchemeHostAndPath() {
        assertEquals(
            "lodestar://example.com/app/first",
            RouteNormalizer.normalize("LODESTAR://Example.COM/app/./first")
        )
    }

    @Test
    fun ignoresQueryAndFragmentForRouteLookup() {
        assertEquals(
            "lodestar://example.com/app/first",
            RouteNormalizer.normalize("lodestar://example.com/app/first?id=42#section")
        )
    }

    @Test
    fun rejectsMalformedOrRelativeRoutes() {
        assertNull(RouteNormalizer.normalize(""))
        assertNull(RouteNormalizer.normalize("/app/first"))
        assertNull(RouteNormalizer.normalize(" lodestar://example.com/app/first"))
        assertNull(RouteNormalizer.normalize("lodestar:///app/first"))
    }
}
