package com.johnturkson.sync.common.data

import com.johnturkson.cdk.generator.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.cdk.generator.annotations.dynamodb.ResourceMetadata
import kotlinx.serialization.Serializable

@Serializable
@ResourceMetadata
data class UserMetadata(
    @PrimaryPartitionKey
    val id: String,
    val email: String
)
