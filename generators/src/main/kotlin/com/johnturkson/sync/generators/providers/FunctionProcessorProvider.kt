package com.johnturkson.sync.generators.providers

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.johnturkson.sync.generators.processors.FunctionProcessor

class FunctionProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FunctionProcessor(environment.codeGenerator, environment.logger, environment.options)
    }
}
