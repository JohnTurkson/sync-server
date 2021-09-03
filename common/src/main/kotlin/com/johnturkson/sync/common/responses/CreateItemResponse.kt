package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Item
import kotlinx.serialization.Serializable

@Serializable
sealed class CreateItemResponse {
    @Serializable
    data class Success(val item: Item) : CreateItemResponse()
    
    @Serializable
    data class Failure(val error: String) : CreateItemResponse()
}
