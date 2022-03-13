package com.johnturkson.sync.generators.functions

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.johnturkson.sync.generators.annotations.dynamodb.Flatten
import com.johnturkson.sync.generators.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.dynamodb.PrimarySortKey
import com.johnturkson.sync.generators.annotations.dynamodb.Resource
import com.johnturkson.sync.generators.annotations.dynamodb.SecondaryPartitionKey
import com.johnturkson.sync.generators.annotations.dynamodb.SecondarySortKey

fun generateBuilderClass(
    resourceClass: KSClassDeclaration,
    codeGenerator: CodeGenerator,
    options: Map<String, String>,
) {
    val resourceProperties = resourceClass.getDeclaredProperties()
    val resourceClassName = resourceClass.simpleName.asString()
    val generatedPackageName = requireNotNull(options["location"])
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

fun generateDefinitionClass(
    resourceClass: KSClassDeclaration,
    codeGenerator: CodeGenerator,
    options: Map<String, String>,
) {
    val resourceAnnotations = resourceClass.annotations.groupBy { annotation ->
        annotation.annotationType.resolve().declaration.qualifiedName?.asString()
    }
    val resourceProperties = resourceClass.getDeclaredProperties()
    val resourceClassName = resourceClass.simpleName.asString()
    val builderClassName = generateBuilderClassName(resourceClassName)
    val tableClassName = generateDefinitionClassName(resourceClassName)
    val generatedPackageName = requireNotNull(options["location"])
    
    val generatedResourceBuilderFile = codeGenerator.createNewFile(
        Dependencies.ALL_FILES,
        generatedPackageName,
        tableClassName,
        "kt"
    )
    
    val imports = mutableSetOf(
        "import ${resourceClass.qualifiedName?.asString()}",
        "import software.amazon.awssdk.enhanced.dynamodb.TableSchema",
        "import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema"
    )
    
    val tableIndices = mutableSetOf<Pair<String, String>>()
    
    val schemaProperties = resourceProperties.map { property ->
        val name = property.simpleName.asString()
        val type = property.type.element.toString()
        val annotations = property.annotations.groupBy { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        }
        
        val tags = mutableSetOf<String>()
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
                    val (indexName, indexAlias) = annotation.first().arguments.map { argument -> argument.value.toString() }
                    tags += "StaticAttributeTags.secondaryPartitionKey(\"$indexName\")"
                    tableIndices += indexName to indexAlias
                }
                SecondarySortKey::class.qualifiedName -> {
                    imports += "import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags"
                    val (indexName, indexAlias) = annotation.first().arguments.map { argument -> argument.value.toString() }
                    tags += "StaticAttributeTags.secondarySortKey(\"$indexName\")"
                    tableIndices += indexName to indexAlias
                }
            }
        }
        
        val indexAnnotations = setOf(
            SecondaryPartitionKey::class.qualifiedName!!,
            SecondarySortKey::class.qualifiedName!!,
        )
        tableIndices += findTableIndices(property, indexAnnotations)
        
        if (Flatten::class.qualifiedName in annotations) {
            ".flatten(${generateDefinitionClassName(type)}.SCHEMA, $resourceClassName::$name, $builderClassName::$name)"
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
            val (tableName, tableAlias) = resourceAnnotation.first().arguments
                .map { argument -> argument.value.toString() }
            
            imports += setOf(
                "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient",
                "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient",
                "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable",
                "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable"
            )
            
            val tableFields = """
                |
                |
                |val DynamoDbEnhancedAsyncClient.$tableAlias: DynamoDbAsyncTable<$resourceClassName>
                |    get() = this.table("$tableName", SCHEMA)
                |
                |val DynamoDbEnhancedClient.$tableAlias: DynamoDbTable<$resourceClassName>
                |    get() = this.table("$tableName", SCHEMA)
            """.trimMargin()
            
            append(tableFields)
            
            if (tableIndices.isNotEmpty()) {
                imports += setOf(
                    "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex",
                    "import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex"
                )
            }
            
            tableIndices.forEach { (indexName, indexAlias) ->
                val indexFields = """
                    |
                    |
                    |val DynamoDbEnhancedAsyncClient.$indexAlias: DynamoDbAsyncIndex<$resourceClassName>
                    |    get() = this.$tableAlias.index("$indexName")
                    |
                    |val DynamoDbEnhancedClient.$indexAlias: DynamoDbIndex<$resourceClassName>
                    |    get() = this.$tableAlias.index("$indexName")
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
        |object $tableClassName {
        |   $definitions
        |}
        |
    """.trimMargin()
    
    generatedResourceBuilderFile.bufferedWriter().use { writer -> writer.write(generatedClass) }
}

fun findTableIndices(property: KSPropertyDeclaration, targetAnnotations: Set<String>): Set<Pair<String, String>> {
    val indices = mutableSetOf<Pair<String, String>>()
    
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
                val (indexName, indexAlias) = annotation.first().arguments
                    .map { argument -> argument.value.toString() }
                indices += indexName to indexAlias
            }
        }
    }
    
    return indices
}

private fun generateBuilderClassName(resource: String, suffix: String = "Builder"): String {
    return resource + suffix
}

private fun generateDefinitionClassName(resource: String, suffix: String = "Definition"): String {
    return resource + suffix
}
