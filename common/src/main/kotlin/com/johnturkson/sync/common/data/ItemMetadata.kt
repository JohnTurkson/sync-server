package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.dynamodb.ResourceMetadata
import com.johnturkson.sync.generators.annotations.dynamodb.SecondaryPartitionKey
import com.johnturkson.sync.generators.annotations.dynamodb.SecondarySortKey
import kotlinx.serialization.Serializable

@Serializable
@ResourceMetadata
data class ItemMetadata(
    @PrimaryPartitionKey
    @SecondarySortKey(indexName = "SyncItemsUserIndex", indexAlias = "ItemsUserIndex")
    val id: String,
    @SecondaryPartitionKey(indexName = "SyncItemsUserIndex", indexAlias = "ItemsUserIndex")
    val user: String
)
