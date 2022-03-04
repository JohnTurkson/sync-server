package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Authorization
import kotlinx.serialization.Serializable

@Serializable
sealed class LoginUserResponse {
    abstract val statusCode: Int
    
    @Serializable
    data class Success(val authorization: Authorization, override val statusCode: Int) : LoginUserResponse()
    
    @Serializable
    data class Failure(val error: String, override val statusCode: Int) : LoginUserResponse()
}
