package com.johnturkson.sync.common.responses

import com.johnturkson.sync.common.data.Credentials
import kotlinx.serialization.Serializable

@Serializable
sealed class GetCredentialsResponse {
    @Serializable
    data class Success(val credentials: Credentials) : GetCredentialsResponse()
    
    @Serializable
    data class Failure(val error: String) : GetCredentialsResponse()
}
