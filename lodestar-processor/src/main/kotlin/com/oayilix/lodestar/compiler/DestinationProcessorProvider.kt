package com.oayilix.lodestar.compiler

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/** KSP entry point discovered through `META-INF/services`. 通过 `META-INF/services` 发现的 KSP 入口。 */
class DestinationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return DestinationProcessor(environment.codeGenerator, environment.logger)
    }
}
