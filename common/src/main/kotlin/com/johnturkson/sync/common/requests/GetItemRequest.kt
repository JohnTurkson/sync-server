package com.johnturkson.sync.common.requests

import kotlinx.serialization.Serializable

@Serializable
data class GetItemRequest(val id: String, val authorization: String)
