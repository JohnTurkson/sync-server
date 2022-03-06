package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.User
import kotlinx.serialization.Serializable

@Serializable
sealed class CreateUserResponse {
    abstract val statusCode: Int
    
    @Serializable
    data class Success(
        val user: User,
        val authorization: Authorization,
        override val statusCode: Int,
    ) : CreateUserResponse()
    
    @Serializable
    data class Failure(val error: String, override val statusCode: Int) : CreateUserResponse()
}
