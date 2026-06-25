package com.oayilix.lodestar.api

import android.content.ContextWrapper
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LodestarNavigationTest {

    private lateinit var context: RecordingContext

    @Before
    fun resetLodestar() {
        Lodestar.resetForTesting()
        context = RecordingContext()
    }

    @Test
    fun returnsNotInitializedBeforeRouteTableIsLoaded() {
        val result = Lodestar.navigation(context, "lodestar://example.com/app/test")

        assertEquals(NavigationResult.NotInitialized, result)
        assertEquals(null, context.startedIntent)
    }

    @Test
    fun rejectsInvalidRouteAfterInitialization() {
        Lodestar.init()

        val result = Lodestar.navigation(context, " lodestar://example.com/app/test")

        assertEquals(NavigationResult.InvalidRoute(" lodestar://example.com/app/test"), result)
        assertEquals(null, context.startedIntent)
    }

    @Test
    fun returnsRouteNotFoundForMissingDestination() {
        Lodestar.init()

        val result = Lodestar.navigation(context, "lodestar://example.com/app/missing?id=42")

        assertEquals(NavigationResult.RouteNotFound("lodestar://example.com/app/missing"), result)
        assertEquals(null, context.startedIntent)
    }

    @Test
    fun plansOriginalRouteDataAndNewTaskFlagForNonActivityContext() {
        val route = "lodestar://example.com/app/test?id=42#section"

        val options = Lodestar.navigationIntentOptions(isActivityContext = false, route = route)

        assertEquals(route, options.dataUri)
        assertTrue(options.addNewTaskFlag)
    }

    @Test
    fun doesNotPlanNewTaskFlagForActivityContext() {
        val options = Lodestar.navigationIntentOptions(
            isActivityContext = true,
            route = "lodestar://example.com/app/test"
        )

        assertEquals("lodestar://example.com/app/test", options.dataUri)
        assertEquals(false, options.addNewTaskFlag)
    }

    private class RecordingContext : ContextWrapper(null) {
        var startedIntent: Intent? = null

        override fun getPackageName(): String = "com.oayilix.lodestar.test"

        override fun startActivity(intent: Intent) {
            startedIntent = intent
        }
    }
}
