package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.ResourceMetadata
import kotlinx.serialization.Serializable

@Serializable
@ResourceMetadata
data class CredentialsMetadata(
    @PrimaryPartitionKey
    val id: String,
)
