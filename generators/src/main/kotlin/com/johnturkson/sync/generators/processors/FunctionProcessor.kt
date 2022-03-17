package com.johnturkson.sync.generators.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.johnturkson.sync.generators.annotations.lambda.Function

class FunctionProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val functionAnnotation = requireNotNull(Function::class.qualifiedName)
        val functionClasses = resolver.getSymbolsWithAnnotation(functionAnnotation)
            .filterIsInstance<KSClassDeclaration>()
        
        functionClasses.forEach { resourceClass ->
            generateFunctionClass(resourceClass, codeGenerator)
        }
        if (functionClasses.toList().isNotEmpty()) generateFunctionsClass(functionClasses)
        
        return emptyList()
    }
    
    private fun generateFunctionClass(resourceClass: KSClassDeclaration, codeGenerator: CodeGenerator) {
        val resourceClassName = resourceClass.simpleName.asString()
        val generatedPackageName = requireNotNull(options["location"])
        val handlerName = resourceClass.qualifiedName?.asString()
        val handlerLocation = requireNotNull(options["HANDLER_LOCATION"])
        
        val functionAnnotation = resourceClass.annotations.first { annotation ->
            val annotationName = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
            annotationName == Function::class.qualifiedName
        }
        
        val (duration, memory) = functionAnnotation.arguments.map { argument -> argument.value.toString() }
        
        val imports = """
            import software.amazon.awscdk.Duration
            import software.amazon.awscdk.services.lambda.Architecture
            import software.amazon.awscdk.services.lambda.Code
            import software.amazon.awscdk.services.lambda.Function
            import software.amazon.awscdk.services.lambda.Runtime
            import software.constructs.Construct
        """.trimIndent()
        
        val builder = """
            |return Function.Builder.create(construct, "$resourceClassName")
            |    .functionName("$resourceClassName")
            |    .handler("$handlerName")
            |    .code(Code.fromAsset("$handlerLocation"))
            |    .timeout(Duration.seconds($duration))
            |    .memorySize($memory)
            |    .runtime(Runtime.PROVIDED_AL2)
            |    .architecture(Architecture.X86_64)
        """.trimMargin()
        
        val generatedClass = """
            |package $generatedPackageName
            |
            |$imports
            |
            |object $resourceClassName {
            |    fun builder(construct: Construct): Function.Builder {
            |        $builder
            |    }
            |
            |    fun build(construct: Construct): Function {
            |        return builder(construct).build()
            |    }
            |}
        """.trimMargin()
        
        val generatedResourceBuilderFile = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            generatedPackageName,
            resourceClassName,
            "kt"
        )
        
        generatedResourceBuilderFile.bufferedWriter().use { writer -> writer.write(generatedClass) }
    }
    
    private fun generateFunctionsClass(resourceClasses: Sequence<KSClassDeclaration>) {
        val generatedPackageName = requireNotNull(options["location"])
        val generatedClassName = "Functions"
        
        val imports = """
            import software.amazon.awscdk.services.lambda.Function
            import software.constructs.Construct
        """.trimIndent()
        
        val builders = resourceClasses.joinToString(separator = "\n") { resourceClass ->
            "add(${resourceClass.simpleName.asString()}.builder(construct))"
        }
        
        val generatedClass = """
            |package $generatedPackageName
            |
            |$imports
            |
            |object Functions {
            |    fun builders(construct: Construct): List<Function.Builder> {
            |        return buildList {
            |            $builders
            |        }
            |    }
            |
            |    fun build(construct: Construct): List<Function> {
            |        return builders(construct).map { builder -> builder.build() }
            |    }
            |}
        """.trimMargin()
        
        val generatedResourceBuilderFile = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            generatedPackageName,
            generatedClassName,
            "kt"
        )
        
        generatedResourceBuilderFile.bufferedWriter().use { writer -> writer.write(generatedClass) }
    }
}
