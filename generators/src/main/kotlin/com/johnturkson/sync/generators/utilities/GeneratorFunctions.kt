package com.johnturkson.sync.generators.utilities

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.johnturkson.sync.generators.annotations.Flatten
import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey

fun generateBuilderClass(resourceClass: KSClassDeclaration, codeGenerator: CodeGenerator) {
    val resourceProperties = resourceClass.getDeclaredProperties()
    val resourceClassName = resourceClass.simpleName.asString()
    val generatedPackageName = "com.johnturkson.sync.common.generated"
    val generatedClassName = "${resourceClassName}Builder"
    
    val generatedResourceBuilderFile = codeGenerator.createNewFile(
        Dependencies.ALL_FILES,
        generatedPackageName,
        generatedClassName,
        "kt"
    )
    
    val imports = mutableSetOf(
        "import ${resourceClass.qualifiedName?.asString()}",
        "import kotlin.properties.Delegates"
    )
    
    resourceProperties.map { property ->
        property.type.resolve().declaration.qualifiedName?.asString()
    }.filter { packageName ->
        packageName?.startsWith("kotlin.") == false
    }.map { packageName ->
        "import $packageName"
    }.forEach { import ->
        imports += import
    }
    
    val fields = resourceProperties.map { property ->
        val name = property.simpleName.asString()
        val type = property.type.element.toString()
        
        "private var $name by Delegates.notNull<$type>()"
    }.joinToString(separator = "\n")
    
    val methods = resourceProperties.map { property ->
        val name = property.simpleName.asString()
        val type = property.type.element.toString()
        
        """
                |
                |fun $name($name: $type): $generatedClassName {
                |    this.$name = $name
                |    return this
                |}
            """.trimMargin()
    }.joinToString(separator = "\n")
    
    val buildMethod = """
            |
            |fun build(): ${resourceClass.simpleName.asString()} {
            |    return ${resourceClass.simpleName.asString()}(${resourceProperties.joinToString(separator = ", ")})
            |}
        """.trimMargin()
    
    val generatedClass = """
            |package $generatedPackageName
            |
            |${imports.sorted().joinToString(separator = "\n")}
            |
            |class $generatedClassName {
            |    $fields
            |    $methods
            |    $buildMethod
            |}
            |
        """.trimMargin()
    
    generatedResourceBuilderFile.bufferedWriter().use { writer -> writer.write(generatedClass) }
}

fun generateSchemaObject(resourceClass: KSClassDeclaration, codeGenerator: CodeGenerator) {
    val resourceProperties = resourceClass.getDeclaredProperties()
    val resourceClassName = resourceClass.simpleName.asString()
    val builderClassName = "${resourceClassName}Builder"
    val generatedPackageName = "com.johnturkson.sync.common.generated"
    val generatedClassName = "${resourceClassName}Item"
    
    val generatedResourceBuilderFile = codeGenerator.createNewFile(
        Dependencies.ALL_FILES,
        generatedPackageName,
        generatedClassName,
        "kt"
    )
    
    val imports = mutableSetOf(
        "import ${resourceClass.qualifiedName?.asString()}",
        "import software.amazon.awssdk.enhanced.dynamodb.TableSchema"
    )
    
    val schemaProperties = resourceProperties.map { property ->
        val name = property.simpleName.asString()
        val type = property.type.element.toString()
        val annotations = property.annotations
            .map { annotation -> annotation.annotationType.resolve().declaration.qualifiedName?.asString() }
            .filterNotNull()
        
        if (Flatten::class.qualifiedName in annotations) {
            ".flatten(${type}Item.SCHEMA, $resourceClassName::$name, $builderClassName::$name)"
        } else if (PrimaryPartitionKey::class.qualifiedName in annotations) {
            imports += "import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags"
            
            """
                |.addAttribute($type::class.java) { attribute ->
                |    attribute.name("$name")
                |        .getter($resourceClassName::$name)
                |        .setter($builderClassName::$name)
                |        .tags(StaticAttributeTags.primaryPartitionKey())
                |}
            """.trimMargin()
        } else {
            """
                |.addAttribute($type::class.java) { attribute ->
                |    attribute.name("$name")
                |        .getter($resourceClassName::$name)
                |        .setter($builderClassName::$name)
                |}
            """.trimMargin()
        }
    }.joinToString(separator = "\n")
    
    val schema = """
        |val SCHEMA = TableSchema.builder($resourceClassName::class.java, $builderClassName::class.java)
        |    .newItemBuilder(::$builderClassName, $builderClassName::build)
        |    $schemaProperties
        |    .build()
    """.trimMargin()
    
    val generatedClass = """
        |package $generatedPackageName
        |
        |${imports.sorted().joinToString(separator = "\n")}
        |
        |object $generatedClassName {
        |   $schema
        |}
        |
    """.trimMargin()
    
    generatedResourceBuilderFile.bufferedWriter().use { writer -> writer.write(generatedClass) }
}
