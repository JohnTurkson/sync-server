package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.dynamodb.ResourceMetadata
import kotlinx.serialization.Serializable

@Serializable
@ResourceMetadata
data class UserMetadata(
    @PrimaryPartitionKey
    val id: String,
    val email: String
)
