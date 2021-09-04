package com.johnturkson.sync.common.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(val password: String)
