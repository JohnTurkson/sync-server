package com.johnturkson.sync.common.requests

import kotlinx.serialization.Serializable

@Serializable
data class ListItemsRequest(val user: String, val authorization: String)
