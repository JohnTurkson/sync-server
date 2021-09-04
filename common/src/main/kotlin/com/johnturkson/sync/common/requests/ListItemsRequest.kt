package com.johnturkson.sync.common.requests

import com.johnturkson.sync.common.data.Authorization
import kotlinx.serialization.Serializable

@Serializable
data class ListItemsRequest(val user: String, val authorization: Authorization)
