package com.johnturkson.sync.requests

import kotlinx.serialization.Serializable

@Serializable
data class GetCredentialsRequest(val id: String, val authorization: String)
