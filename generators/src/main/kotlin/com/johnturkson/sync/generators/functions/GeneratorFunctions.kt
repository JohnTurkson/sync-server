package com.johnturkson.sync.generators.functions

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.johnturkson.sync.generators.annotations.Flatten
import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.PrimarySortKey
import com.johnturkson.sync.generators.annotations.Resource
import com.johnturkson.sync.generators.annotations.SecondaryPartitionKey
import com.johnturkson.sync.generators.annotations.SecondarySortKey

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
    val resourceAnnotations = resourceClass.annotations.groupBy { annotation ->
        annotation.annotationType.resolve().declaration.qualifiedName?.asString()
    }
    val resourceProperties = resourceClass.getDeclaredProperties()
    val resourceClassName = resourceClass.simpleName.asString()
    val builderClassName = "${resourceClassName}Builder"
    val generatedPackageName = "com.johnturkson.sync.common.generated"
    val generatedClassName = "${resourceClassName}Object"
    
    val generatedResourceBuilderFile = codeGenerator.createNewFile(
        Dependencies.ALL_FILES,
        generatedPackageName,
        generatedClassName,
        "kt"
    )
    
    val imports = mutableSetOf(
        "import ${resourceClass.qualifiedName?.asString()}",
        "import software.amazon.awssdk.enhanced.dynamodb.TableSchema",
        "import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema"
    )
    
    val tags = mutableSetOf<String>()
    val indices = mutableSetOf<String>()
    
    val schemaProperties = resourceProperties.map { property ->
        val name = property.simpleName.asString()
        val type = property.type.element.toString()
        val annotations = property.annotations.groupBy { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        }
        
        annotations.forEach { (name, annotation) ->
            when (name) {
                PrimaryPartitionKey::class.qualifiedName -> {
                    imports += "import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags"
                    tags += "StaticAttributeTags.primaryPartitionKey()"
                }
                PrimarySortKey::class.qualifiedName -> {
                    imports += "import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags"
                    tags += "StaticAttributeTags.primarySortKey()"
                }
                SecondaryPartitionKey::class.qualifiedName -> {
                    imports += "import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags"
                    val index = annotation.first().arguments.first().value.toString()
                    tags += "StaticAttributeTags.secondaryPartitionKey(\"$index\")"
                    indices += index
                }
                SecondarySortKey::class.qualifiedName -> {
                    imports += "import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags"
                    val index = annotation.first().arguments.first().value.toString()
                    tags += "StaticAttributeTags.secondarySortKey(\"$index\")"
                    indices += index
                }
            }
        }
        
        indices += findIndices(property)
        
        if (Flatten::class.qualifiedName in annotations) {
            ".flatten(${type}Object.SCHEMA, $resourceClassName::$name, $builderClassName::$name)"
        } else {
            buildString {
                appendLine(".addAttribute($type::class.java) { attribute ->")
                appendLine("attribute.name(\"$name\")")
                appendLine(".getter($resourceClassName::$name)")
                appendLine(".setter($builderClassName::$name)")
                if (tags.isNotEmpty()) {
                    appendLine(".tags(")
                    appendLine(tags.sorted().joinToString(separator = ",\n"))
                    appendLine(")")
                }
                append("}")
            }
        }
    }.joinToString(separator = "\n")
    
    val definitions = buildString {
        val schema = """
            |val SCHEMA: StaticImmutableTableSchema<$resourceClassName, $builderClassName> =
            |    TableSchema.builder($resourceClassName::class.java, $builderClassName::class.java)
            |       .newItemBuilder(::$builderClassName, $builderClassName::build)
            |       $schemaProperties
            |       .build()
        """.trimMargin()
        
        append(schema)
        
        if (Resource::class.qualifiedName in resourceAnnotations) {
            val resourceAnnotation = resourceAnnotations[Resource::class.qualifiedName]!!
            val tableName = resourceAnnotation.first().arguments.first().value.toString()
            
            imports += setOf(
                "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient",
                "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient",
                "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable",
                "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable"
            )
            
            val tableFields = """
                |
                |
                |val DynamoDbEnhancedAsyncClient.$tableName: DynamoDbAsyncTable<$resourceClassName>
                |    get() = this.table("$tableName", SCHEMA)
                |
                |val DynamoDbEnhancedClient.$tableName: DynamoDbTable<$resourceClassName>
                |    get() = this.table("$tableName", SCHEMA)
            """.trimMargin()
            
            append(tableFields)
            
            if (indices.isNotEmpty()) {
                imports += setOf(
                    "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex",
                    "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex"
                )
            }
            
            indices.forEach { indexName ->
                val indexFields = """
                    |
                    |
                    |val DynamoDbEnhancedAsyncClient.$indexName: DynamoDbAsyncIndex<$resourceClassName>
                    |    get() = this.$tableName.index("$indexName")
                    |
                    |val DynamoDbEnhancedClient.$indexName: DynamoDbIndex<$resourceClassName>
                    |    get() = this.$tableName.index("$indexName")
                """.trimMargin()
                
                append(indexFields)
            }
        }
    }
    
    val generatedClass = """
        |package $generatedPackageName
        |
        |${imports.sorted().joinToString(separator = "\n")}
        |
        |object $generatedClassName {
        |   $definitions
        |}
        |
    """.trimMargin()
    
    generatedResourceBuilderFile.bufferedWriter().use { writer -> writer.write(generatedClass) }
}

fun findIndices(property: KSPropertyDeclaration): Set<String> {
    val indices = mutableSetOf<String>()
    
    val targetAnnotations = setOf(
        SecondaryPartitionKey::class.qualifiedName,
        SecondarySortKey::class.qualifiedName,
    )
    
    val resourceProperties = property.type
        .resolve()
        .declaration
        .closestClassDeclaration()
        ?.getDeclaredProperties()
    
    resourceProperties?.forEach { resourceProperty ->
        val annotations = resourceProperty.annotations.groupBy { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        }
        
        annotations.forEach { (name, annotation) ->
            if (name in targetAnnotations) {
                val index = annotation.first().arguments.first().value.toString()
                indices += index
            }
        }
    }
    
    return indices
}
