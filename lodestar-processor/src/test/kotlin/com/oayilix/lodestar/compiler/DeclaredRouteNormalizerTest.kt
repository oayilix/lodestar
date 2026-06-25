package com.oayilix.lodestar.compiler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeclaredRouteNormalizerTest {

    @Test
    fun normalizesCompileTimeRouteKey() {
        assertEquals(
            "lodestar://example.com/app/first",
            DeclaredRouteNormalizer.normalize("LODESTAR://Example.COM/app/./first")
        )
    }

    @Test
    fun rejectsQueryAndFragmentInRouteDeclaration() {
        assertNull(DeclaredRouteNormalizer.normalize("lodestar://example.com/app/first?id=42"))
        assertNull(DeclaredRouteNormalizer.normalize("lodestar://example.com/app/first#section"))
    }

    @Test
    fun rejectsMalformedOrRelativeDeclarations() {
        assertNull(DeclaredRouteNormalizer.normalize(""))
        assertNull(DeclaredRouteNormalizer.normalize("/app/first"))
        assertNull(DeclaredRouteNormalizer.normalize(" lodestar://example.com/app/first"))
        assertNull(DeclaredRouteNormalizer.normalize("lodestar:///app/first"))
    }
}
