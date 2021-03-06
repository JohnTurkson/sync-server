package com.johnturkson.sync.common.data

import com.johnturkson.cdk.generator.annotations.dynamodb.ResourceData
import kotlinx.serialization.Serializable

@Serializable
@ResourceData
data class ItemData(
    val service: String,
    val login: String,
    val password: String,
)
