package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.ResourceMetadata
import com.johnturkson.sync.generators.annotations.SecondaryPartitionKey
import com.johnturkson.sync.generators.annotations.SecondarySortKey
import kotlinx.serialization.Serializable

@Serializable
@ResourceMetadata
data class ItemMetadata(
    @PrimaryPartitionKey
    @SecondarySortKey("SyncItemsUserIndex")
    val id: String,
    @SecondaryPartitionKey("SyncItemsUserIndex")
    val user: String
)
