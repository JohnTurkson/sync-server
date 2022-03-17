package com.johnturkson.sync.generators.processors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.johnturkson.sync.generators.annotations.dynamodb.Flatten
import com.johnturkson.sync.generators.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.dynamodb.PrimarySortKey
import com.johnturkson.sync.generators.annotations.dynamodb.Resource
import com.johnturkson.sync.generators.annotations.dynamodb.SecondaryPartitionKey
import com.johnturkson.sync.generators.annotations.dynamodb.SecondarySortKey

@KspExperimental
class TableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val resourceAnnotation = requireNotNull(Resource::class.qualifiedName)
        val resourceClasses = resolver.getSymbolsWithAnnotation(resourceAnnotation)
            .filterIsInstance<KSClassDeclaration>()
        
        val tables = resourceClasses.mapNotNull { resourceClass ->
            val resource = resourceClass.getAnnotationsByType(Resource::class).first()
            val partialTable = PartialTable(tableName = resource.tableName, tableAlias = resource.tableAlias)
            process(resourceClass, partialTable).asTable()
        }
        
        tables.forEach { table -> generateTableFile(table) }
        if (tables.toList().isNotEmpty()) generateTablesFile(tables)
        
        return emptyList()
    }
    
    private fun process(
        resourceClass: KSClassDeclaration,
        partialTable: PartialTable = PartialTable(),
    ): PartialTable {
        return resourceClass.getDeclaredProperties().map { property ->
            val propertyName = property.simpleName.asString()
            val propertyType = property.type.element.toString()
            
            val tableAttributeKey = TableAttributeKey(propertyName, "STRING")
            
            val primaryPartitionKey = property.getAnnotationsByType(PrimaryPartitionKey::class).firstOrNull()
            val primarySortKey = property.getAnnotationsByType(PrimarySortKey::class).firstOrNull()
            val secondaryPartitionKey = property.getAnnotationsByType(SecondaryPartitionKey::class).firstOrNull()
            val secondarySortKey = property.getAnnotationsByType(SecondarySortKey::class).firstOrNull()
            val flattenKey = property.getAnnotationsByType(Flatten::class).firstOrNull()
            
            var data = PartialTable()
            
            if (primaryPartitionKey != null) data = data merge PartialTable(primaryPartitionKey = tableAttributeKey)
            if (primarySortKey != null) data = data merge PartialTable(primarySortKey = tableAttributeKey)
            if (secondaryPartitionKey != null) data = data merge PartialTable(
                secondaryIndices = listOf(
                    PartialSecondaryIndex(
                        secondaryPartitionKey.indexName,
                        secondaryPartitionKey.indexAlias,
                        tableAttributeKey
                    )
                )
            )
            if (secondarySortKey != null) data = data merge PartialTable(
                secondaryIndices = listOf(
                    PartialSecondaryIndex(
                        secondarySortKey.indexName,
                        secondarySortKey.indexAlias,
                        tableAttributeKey
                    )
                )
            )
            if (flattenKey != null) data = data merge process(
                property.type.resolve().declaration.closestClassDeclaration()!!,
                partialTable
            )
            
            data
        }.fold(partialTable) { merged, next -> merged merge next }
    }
    
    private fun generateTableFile(table: Table) {
        val generatedPackageName = requireNotNull(options["location"])
        val generatedClassName = table.tableAlias
        
        val generatedTableFile = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            generatedPackageName,
            generatedClassName,
            "kt",
        )
        
        val imports = buildSet {
            add("import software.amazon.awscdk.services.dynamodb.Attribute")
            add("import software.amazon.awscdk.services.dynamodb.AttributeType")
            add("import software.amazon.awscdk.services.dynamodb.BillingMode")
            add("import software.amazon.awscdk.services.dynamodb.Table")
            add("import software.constructs.Construct")
            add("import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps")
            if (table.secondaryIndices.isNotEmpty()) {
                add("import software.amazon.awscdk.services.dynamodb.ProjectionType")
            }
        }
        
        val builder = buildString {
            append("""
                Table.Builder.create(construct, "${table.tableAlias}")
                    .tableName("${table.tableAlias}")
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .partitionKey(
                        Attribute.builder()
                            .name("${table.primaryPartitionKey.attributeName}")
                            .type(AttributeType.${table.primaryPartitionKey.attributeType})
                            .build()
                    )
            """.trimIndent())
            
            if (table.primarySortKey != null) {
                append("""
                    .sortKey(
                        Attribute.builder().name("${table.primarySortKey.attributeName}").type(AttributeType.${table.primarySortKey.attributeType}).build()
                    )
                """.trimIndent())
            }
        }
        
        val indexBuilders = table.secondaryIndices.joinToString(
            separator = ",\n",
            prefix = "listOf(\n",
            postfix = "\n)",
        ) { index -> "${index.indexAlias}.builder()" }
        
        val indices = table.secondaryIndices.map { index ->
            val indexBuilder = buildString {
                append("""
                    GlobalSecondaryIndexProps.builder()
                        .indexName("${index.indexName}")
                        .projectionType(ProjectionType.ALL)
                        .partitionKey(
                            Attribute.builder()
                                .name("${index.secondaryPartitionKey.attributeName}")
                                .type(AttributeType.${index.secondaryPartitionKey.attributeType})
                                .build()
                        )
                """.trimIndent())
                
                if (index.secondarySortKey != null) {
                    append("""
                        .sortKey(
                            Attribute.builder()
                            .name("${index.secondarySortKey.attributeName}")
                            .type(AttributeType.${index.secondarySortKey.attributeType})
                            .build()
                        )
                    """.trimIndent())
                }
            }
            
            val generatedIndexClass = """
                object ${index.indexAlias} {
                    fun builder(): GlobalSecondaryIndexProps.Builder {
                        return $indexBuilder
                    }
                    
                    fun build(): GlobalSecondaryIndexProps {
                        return builder().build()
                    }
                }
            """.trimMargin()
            
            generatedIndexClass
        }
        
        val generatedTableClass = """
            package $generatedPackageName
            
            ${imports.sorted().joinToString(separator = "\n")}
            
            object $generatedClassName {
                fun indexBuilders(): List<GlobalSecondaryIndexProps.Builder> {
                    return $indexBuilders
                }
            
                fun indices(): List<GlobalSecondaryIndexProps> {
                    return indexBuilders().map { builder -> builder.build() }
                }
            
                fun builder(construct: Construct): Table.Builder {
                    return $builder
                }
                
                fun build(construct: Construct): Table {
                    val table = builder(construct).build()
                    indices().forEach { index -> table.addGlobalSecondaryIndex(index) }
                    return table
                }
                
                ${indices.joinToString(separator = "\n")}
            }
        """.trimIndent()
        
        generatedTableFile.bufferedWriter().use { writer -> writer.write(generatedTableClass) }
    }
    
    private fun generateTablesFile(tables: Sequence<Table>) {
        val generatedPackageName = requireNotNull(options["location"])
        val generatedClassName = "Tables"
        
        val generatedTablesFile = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            generatedPackageName,
            generatedClassName,
            "kt",
        )
        
        val imports = buildSet {
            add("import software.amazon.awscdk.services.dynamodb.Table")
            add("import software.constructs.Construct")
        }
        
        val tableBuilders = tables.map { table -> "${table.tableAlias}.builder(construct)" }
            .joinToString(separator = ",\n", prefix = "listOf(\n", postfix = "\n)")
        
        val builtTables = tables.map { table -> "${table.tableAlias}.build(construct)" }
            .joinToString(separator = ",\n", prefix = "listOf(\n", postfix = "\n)")
        
        val generatedClass = """
            package $generatedPackageName
            
            ${imports.sorted().joinToString(separator = "\n")}
            
            object $generatedClassName {
                fun builders(construct: Construct): List<Table.Builder> {
                    return $tableBuilders
                }
                
                fun build(construct: Construct): List<Table> {
                    return $builtTables
                }
            }
        """.trimIndent()
        
        generatedTablesFile.bufferedWriter().use { writer -> writer.write(generatedClass) }
    }
    
    private fun PartialTable.asTable(): Table? {
        return runCatching {
            Table(
                tableName!!,
                tableAlias!!,
                primaryPartitionKey!!,
                primarySortKey,
                secondaryIndices.map { partialIndex -> partialIndex.asSecondaryIndex()!! }
            )
        }.getOrNull()
    }
    
    private fun PartialSecondaryIndex.asSecondaryIndex(): SecondaryIndex? {
        return runCatching {
            SecondaryIndex(
                indexName!!,
                indexAlias!!,
                secondaryPartitionKey!!,
                secondarySortKey
            )
        }.getOrNull()
    }
    
    private infix fun PartialTable.merge(other: PartialTable): PartialTable {
        val tableName = listOfNotNull(this.tableName, other.tableName).firstOrNull()
        val tableAlias = listOfNotNull(this.tableAlias, other.tableAlias).firstOrNull()
        val primaryPartitionKey = listOfNotNull(this.primaryPartitionKey, other.primaryPartitionKey).firstOrNull()
        val primarySortKey = listOfNotNull(this.primarySortKey, other.primarySortKey).firstOrNull()
        val secondaryIndices = this.secondaryIndices merge other.secondaryIndices
        return PartialTable(tableName, tableAlias, primaryPartitionKey, primarySortKey, secondaryIndices)
    }
    
    private infix fun List<PartialSecondaryIndex>.merge(other: List<PartialSecondaryIndex>): List<PartialSecondaryIndex> {
        val combined = this + other
        val aggregated = combined.groupBy { secondaryIndex -> secondaryIndex.indexName }
        return aggregated.map { (_, secondaryIndices) ->
            secondaryIndices.fold(PartialSecondaryIndex()) { merged, next -> merged merge next }
        }
    }
    
    private infix fun PartialSecondaryIndex.merge(other: PartialSecondaryIndex): PartialSecondaryIndex {
        val indexName = listOfNotNull(this.indexName, other.indexName).firstOrNull()
        val indexAlias = listOfNotNull(this.indexAlias, other.indexAlias).firstOrNull()
        val secondaryPartitionKey = listOfNotNull(this.secondaryPartitionKey, other.secondaryPartitionKey).firstOrNull()
        val secondarySortKey = listOfNotNull(this.secondarySortKey, other.secondarySortKey).firstOrNull()
        return PartialSecondaryIndex(indexName, indexAlias, secondaryPartitionKey, secondarySortKey)
    }
    
    data class Table(
        val tableName: String,
        val tableAlias: String,
        val primaryPartitionKey: TableAttributeKey,
        val primarySortKey: TableAttributeKey?,
        val secondaryIndices: List<SecondaryIndex>,
    )
    
    data class PartialTable(
        val tableName: String? = null,
        val tableAlias: String? = null,
        val primaryPartitionKey: TableAttributeKey? = null,
        val primarySortKey: TableAttributeKey? = null,
        val secondaryIndices: List<PartialSecondaryIndex> = emptyList(),
    )
    
    data class SecondaryIndex(
        val indexName: String,
        val indexAlias: String,
        val secondaryPartitionKey: TableAttributeKey,
        val secondarySortKey: TableAttributeKey?,
    )
    
    data class PartialSecondaryIndex(
        val indexName: String? = null,
        val indexAlias: String? = null,
        val secondaryPartitionKey: TableAttributeKey? = null,
        val secondarySortKey: TableAttributeKey? = null,
    )
    
    data class TableAttributeKey(val attributeName: String, val attributeType: String)
}
