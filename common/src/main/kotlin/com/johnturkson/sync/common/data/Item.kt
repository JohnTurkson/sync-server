package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.dynamodb.Flatten
import com.johnturkson.sync.generators.annotations.dynamodb.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncItems", tableAlias = "Items")
data class Item(
    @Flatten
    val metadata: ItemMetadata,
    @Flatten
    val data: ItemData,
)
