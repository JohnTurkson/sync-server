package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Credentials
import kotlinx.serialization.Serializable

@Serializable
sealed class CreateCredentialsResponse {
    @Serializable
    data class Success(val credentials: Credentials) : CreateCredentialsResponse()
    
    @Serializable
    data class Failure(val error: String) : CreateCredentialsResponse()
}
