package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.User
import kotlinx.serialization.Serializable

@Serializable
sealed class CreateUserResponse {
    @Serializable
    data class Success(val user: User, val authorization: Authorization) : CreateUserResponse()
    
    @Serializable
    data class Failure(val error: String) : CreateUserResponse()
}
