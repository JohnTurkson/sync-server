package com.johnturkson.sync.common.requests

import com.johnturkson.sync.common.data.UserCredentials
import kotlinx.serialization.Serializable

@Serializable
data class LoginUserRequest(val credentials: UserCredentials)
