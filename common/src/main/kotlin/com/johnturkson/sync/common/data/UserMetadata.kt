package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.ResourceMetadata
import com.johnturkson.sync.generators.annotations.SecondaryPartitionKey
import kotlinx.serialization.Serializable

@Serializable
@ResourceMetadata
data class UserMetadata(
    @SecondaryPartitionKey("SyncUsersIdIndex")
    val id: String,
)
