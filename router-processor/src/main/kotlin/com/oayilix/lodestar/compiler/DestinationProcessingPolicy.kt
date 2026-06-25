package com.oayilix.lodestar.compiler

/**
 * Centralizes KSP round decisions so deferred-symbol behavior stays testable.
 * 集中管理 KSP 轮次决策，使延迟符号处理行为可被测试覆盖。
 */
internal object DestinationProcessingPolicy {

    fun shouldSkipGeneratedRegistry(alreadyGenerated: Boolean): Boolean = alreadyGenerated

    fun shouldDeferRegistryGeneration(deferredCount: Int): Boolean = deferredCount > 0

    fun shouldStopForInvalidTargets(invalidTargetCount: Int): Boolean = invalidTargetCount > 0

    fun shouldStopForInvalidRoutes(parsedRouteCount: Int, classCount: Int): Boolean =
        parsedRouteCount != classCount

    fun shouldStopForDuplicates(duplicateCount: Int): Boolean = duplicateCount > 0
}
