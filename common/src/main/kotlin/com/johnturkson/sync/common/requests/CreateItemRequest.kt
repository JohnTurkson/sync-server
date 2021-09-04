package com.johnturkson.sync.common.requests

import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.ItemData
import kotlinx.serialization.Serializable

@Serializable
data class CreateItemRequest(val data: ItemData, val authorization: Authorization)
