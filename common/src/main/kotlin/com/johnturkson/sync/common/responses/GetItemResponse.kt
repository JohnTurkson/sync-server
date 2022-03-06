package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Item
import kotlinx.serialization.Serializable

@Serializable
sealed class GetItemResponse {
    abstract val statusCode: Int
    
    @Serializable
    data class Success(val item: Item, override val statusCode: Int) : GetItemResponse()
    
    @Serializable
    data class Failure(val error: String, override val statusCode: Int) : GetItemResponse()
}
