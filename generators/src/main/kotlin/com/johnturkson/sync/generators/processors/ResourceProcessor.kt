package com.johnturkson.sync.generators.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.johnturkson.sync.generators.annotations.Resource
import com.johnturkson.sync.generators.utilities.generateBuilderClass
import com.johnturkson.sync.generators.utilities.generateSchemaObject

class ResourceProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val resourceAnnotation = requireNotNull(Resource::class.qualifiedName)
        val resourceClasses = resolver.getSymbolsWithAnnotation(resourceAnnotation)
            .filterIsInstance<KSClassDeclaration>()
        
        resourceClasses.forEach { resourceClass ->
            generateBuilderClass(resourceClass, codeGenerator)
            generateSchemaObject(resourceClass, codeGenerator)
        }
        
        return emptyList()
    }
}
