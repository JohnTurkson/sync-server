package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Authorization
import kotlinx.serialization.Serializable

@Serializable
sealed class LoginUserResponse {
    @Serializable
    data class Success(val authorization: Authorization) : LoginUserResponse()
    
    @Serializable
    data class Failure(val error: String) : LoginUserResponse()
}
