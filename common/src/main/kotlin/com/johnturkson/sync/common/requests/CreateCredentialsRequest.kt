package com.johnturkson.sync.common.requests

import com.johnturkson.sync.common.data.CredentialsData
import kotlinx.serialization.Serializable

@Serializable
data class CreateCredentialsRequest(val data: CredentialsData, val authorization: String)
