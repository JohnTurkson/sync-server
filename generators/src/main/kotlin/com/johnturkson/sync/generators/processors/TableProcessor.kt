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
        
        resourceClasses.mapNotNull { resourceClass ->
            val resource = resourceClass.getAnnotationsByType(Resource::class).first()
            val partialTable = PartialTable(tableName = resource.tableName, tableAlias = resource.tableAlias)
            process(resourceClass, partialTable).asTable()
        }.forEach { table ->
            generateFile(table)
        }
        
        return emptyList()
    }
    
    private fun process(
        resourceClass: KSClassDeclaration,
        partialTable: PartialTable = PartialTable(),
    ): PartialTable {
        return resourceClass.getDeclaredProperties().map { property ->
            val propertyName = property.simpleName.asString()
            val propertyType = property.type.element.toString()
            
            val tableAttributeKey = Key(propertyName, "STRING")
            
            val primaryPartitionKey = property.getAnnotationsByType(PrimaryPartitionKey::class).firstOrNull()
            val primarySortKey = property.getAnnotationsByType(PrimarySortKey::class).firstOrNull()
            val secondaryPartitionKey = property.getAnnotationsByType(SecondaryPartitionKey::class).firstOrNull()
            val secondarySortKey = property.getAnnotationsByType(SecondarySortKey::class).firstOrNull()
            val flattenKey = property.getAnnotationsByType(Flatten::class).firstOrNull()
            
            // TODO multiple can be selected on a single property
            when {
                primaryPartitionKey != null -> PartialTable(primaryPartitionKey = tableAttributeKey)
                primarySortKey != null -> PartialTable(primarySortKey = tableAttributeKey)
                secondaryPartitionKey != null -> PartialTable(secondaryIndices = listOf(
                    PartialSecondaryIndex(
                        secondaryPartitionKey.indexName,
                        secondaryPartitionKey.indexAlias,
                        tableAttributeKey
                    )
                ))
                secondarySortKey != null -> PartialTable(secondaryIndices = listOf(
                    PartialSecondaryIndex(
                        secondarySortKey.indexName,
                        secondarySortKey.indexAlias,
                        tableAttributeKey
                    )
                ))
                flattenKey != null -> process(property.type.resolve().declaration.closestClassDeclaration()!!,
                    partialTable)
                else -> PartialTable()
            }
        }.fold(partialTable) { merged, next -> merged merge next }
    }
    
    private fun generateFile(table: Table) {
        val generatedPackageName = requireNotNull(options["location"])
        val generatedClassName = generateTableClassName(table)
        
        val generatedTableFile = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            generatedPackageName,
            generatedClassName,
            "kt",
        )
        
        val generatedClass = """
            |package $generatedPackageName
            |
            |object $generatedClassName
            |val table =
            |   ${table.toString().split("\n")}
            |
        """.trimMargin()
        
        generatedTableFile.bufferedWriter().use { writer -> writer.write(generatedClass) }
    }
    
    private fun generateTableClassName(table: Table): String {
        return "${table.tableAlias}Table"
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
        val primaryPartitionKey: Key,
        val primarySortKey: Key?,
        val secondaryIndices: List<SecondaryIndex>,
    )
    
    data class PartialTable(
        val tableName: String? = null,
        val tableAlias: String? = null,
        val primaryPartitionKey: Key? = null,
        val primarySortKey: Key? = null,
        val secondaryIndices: List<PartialSecondaryIndex> = emptyList(),
    )
    
    data class SecondaryIndex(
        val indexName: String,
        val indexAlias: String,
        val secondaryPartitionKey: Key,
        val secondarySortKey: Key?,
    )
    
    data class PartialSecondaryIndex(
        val indexName: String? = null,
        val indexAlias: String? = null,
        val secondaryPartitionKey: Key? = null,
        val secondarySortKey: Key? = null,
    )
    
    data class Key(val attributeName: String, val attributeType: String)
    
    // data class PrimaryPartitionKey(val attributeName: String, val attributeType: String)
    //
    // data class PrimarySortKey(val attributeName: String, val attributeType: String)
    //
    // data class SecondaryPartitionKey(val attributeName: String, val attributeType: String)
    //
    // data class SecondarySortKey(val attributeName: String, val attributeType: String)
}
