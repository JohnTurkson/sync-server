package com.johnturkson.sync.requests

import com.johnturkson.sync.data.CredentialsData
import kotlinx.serialization.Serializable

@Serializable
data class CreateCredentialsRequest(val data: CredentialsData, val authorization: String)
