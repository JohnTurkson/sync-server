package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Item
import kotlinx.serialization.Serializable

@Serializable
sealed class ListItemsResponse {
    @Serializable
    data class Success(val items: List<Item>) : ListItemsResponse()
    
    @Serializable
    data class Failure(val error: String) : ListItemsResponse()
}
