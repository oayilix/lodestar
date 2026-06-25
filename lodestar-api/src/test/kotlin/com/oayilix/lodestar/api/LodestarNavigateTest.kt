package com.oayilix.lodestar.api

import android.content.ContextWrapper
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LodestarNavigateTest {

    private lateinit var context: RecordingContext

    @Before
    fun resetLodestar() {
        Lodestar.resetForTesting()
        context = RecordingContext()
    }

    @Test
    fun returnsNotInitializedBeforeRouteTableIsLoaded() {
        val result = Lodestar.navigate(context, "lodestar://example.com/app/test")

        assertEquals(NavigateResult.NotInitialized, result)
        assertEquals(null, context.startedIntent)
    }

    @Test
    fun rejectsInvalidRouteAfterInitialization() {
        Lodestar.init()

        val result = Lodestar.navigate(context, " lodestar://example.com/app/test")

        assertEquals(NavigateResult.InvalidRoute(" lodestar://example.com/app/test"), result)
        assertEquals(null, context.startedIntent)
    }

    @Test
    fun returnsRouteNotFoundForMissingDestination() {
        Lodestar.init()

        val result = Lodestar.navigate(context, "lodestar://example.com/app/missing?id=42")

        assertEquals(NavigateResult.RouteNotFound("lodestar://example.com/app/missing"), result)
        assertEquals(null, context.startedIntent)
    }

    @Test
    fun returnsNotInitializedFromNavigate() {
        val result = Lodestar.navigate(context, "lodestar://example.com/app/test")

        assertEquals(NavigateResult.NotInitialized, result)
        assertEquals(null, context.startedIntent)
    }

    @Test
    fun contextNavigateToDelegatesToLodestar() {
        Lodestar.init()

        val result = context.navigateTo(" lodestar://example.com/app/test")

        assertEquals(NavigateResult.InvalidRoute(" lodestar://example.com/app/test"), result)
        assertEquals(null, context.startedIntent)
    }

    @Test
    fun plansOriginalRouteDataAndNewTaskFlagForNonActivityContext() {
        val route = "lodestar://example.com/app/test?id=42#section"

        val options = Lodestar.navigateIntentOptions(isActivityContext = false, route = route)

        assertEquals(route, options.dataUri)
        assertTrue(options.addNewTaskFlag)
    }

    @Test
    fun doesNotPlanNewTaskFlagForActivityContext() {
        val options = Lodestar.navigateIntentOptions(
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
