package com.johnturkson.sync.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class ResourceProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val resourceFiles = resolver.getAllFiles().filterIsInstance<KSClassDeclaration>()
        resourceFiles.forEach { resource -> processResource(resource) }
        return emptyList()
    }
    
    private fun processResource(resource: KSClassDeclaration) {
        
    }
}
