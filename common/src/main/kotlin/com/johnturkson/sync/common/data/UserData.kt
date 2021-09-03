package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.ResourceData
import kotlinx.serialization.Serializable

@Serializable
@ResourceData
data class UserData(
    @PrimaryPartitionKey
    val email: String,
)
