package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Item
import kotlinx.serialization.Serializable

@Serializable
sealed class GetItemResponse {
    @Serializable
    data class Success(val item: Item) : GetItemResponse()
    
    @Serializable
    data class Failure(val error: String) : GetItemResponse()
}
