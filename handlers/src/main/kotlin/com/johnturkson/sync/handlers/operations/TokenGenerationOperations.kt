package com.johnturkson.sync.handlers.operations

import com.johnturkson.security.generateSecureRandomBytes
import com.johnturkson.text.encodeBase64

fun generateResourceId(): String {
    val length = 16
    return generateSecureRandomBytes(length).encodeBase64()
}

fun generateAuthorizationToken(): String {
    val length = 16
    return generateSecureRandomBytes(length).encodeBase64()
}
