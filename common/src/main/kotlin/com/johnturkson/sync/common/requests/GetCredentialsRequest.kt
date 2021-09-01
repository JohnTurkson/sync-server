package com.johnturkson.sync.common.requests

import kotlinx.serialization.Serializable

@Serializable
data class GetCredentialsRequest(val id: String, val authorization: String)
