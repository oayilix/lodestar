package com.oayilix.lodestar.compiler

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationProcessingPolicyTest {

    @Test
    fun defersRegistryGenerationWhenAnySymbolIsNotReady() {
        assertTrue(DestinationProcessingPolicy.shouldDeferRegistryGeneration(deferredCount = 1))
        assertFalse(DestinationProcessingPolicy.shouldDeferRegistryGeneration(deferredCount = 0))
    }

    @Test
    fun stopsBeforeGenerationForSemanticErrors() {
        assertTrue(DestinationProcessingPolicy.shouldStopForInvalidTargets(invalidTargetCount = 1))
        assertTrue(DestinationProcessingPolicy.shouldStopForInvalidRoutes(parsedRouteCount = 1, classCount = 2))
        assertTrue(DestinationProcessingPolicy.shouldStopForDuplicates(duplicateCount = 1))
    }

    @Test
    fun skipsLaterRoundsAfterRegistryWasGenerated() {
        assertTrue(DestinationProcessingPolicy.shouldSkipGeneratedRegistry(alreadyGenerated = true))
        assertFalse(DestinationProcessingPolicy.shouldSkipGeneratedRegistry(alreadyGenerated = false))
    }
}
