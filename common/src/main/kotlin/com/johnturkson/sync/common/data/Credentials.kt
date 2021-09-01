package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.Flatten
import com.johnturkson.sync.generators.annotations.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource
data class Credentials(
    @Flatten
    val metadata: CredentialsMetadata,
    @Flatten
    val data: CredentialsData,
)
