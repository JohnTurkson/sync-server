package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.ResourceData
import kotlinx.serialization.Serializable

@Serializable
@ResourceData
data class CredentialsData(
    val service: String,
    val login: String,
    val password: String,
)
