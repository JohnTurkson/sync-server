package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Item
import kotlinx.serialization.Serializable

@Serializable
sealed class ListItemsResponse {
    abstract val statusCode: Int
    
    @Serializable
    data class Success(val items: List<Item>, override val statusCode: Int) : ListItemsResponse()
    
    @Serializable
    data class Failure(val error: String, override val statusCode: Int) : ListItemsResponse()
}
