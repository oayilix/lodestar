package com.oayilix.lodestar.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LodestarInitializationTest {

    @Before
    fun resetLodestar() {
        Lodestar.resetForTesting()
    }

    @Test
    fun loadsGeneratedRouteTableOnce() {
        val first = Lodestar.init()
        val second = Lodestar.init()

        assertTrue(first is InitializationResult.Success)
        assertEquals(1, first.routeCount)
        assertTrue(second is InitializationResult.AlreadyInitialized)
        assertEquals(1, second.routeCount)
    }

    @Test
    fun initOrThrowReturnsLoadedRouteCount() {
        val routeCount = Lodestar.initOrThrow()

        assertEquals(1, routeCount)
        assertEquals(1, Lodestar.initOrThrow())
    }
}
