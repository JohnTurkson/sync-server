package com.johnturkson.sync.functions.operations

import com.johnturkson.security.hash
import org.springframework.security.crypto.bcrypt.BCrypt

fun String.prehashPassword(): ByteArray {
    val algorithm = "SHA3-512"
    return this.hash(algorithm)
}

fun String.hashPassword(): String {
    val prehash = this.prehashPassword()
    val salt = BCrypt.gensalt()
    return BCrypt.hashpw(prehash, salt)
}

fun String.comparePasswordToHash(hash: String): Boolean {
    val prehash = this.prehashPassword()
    return BCrypt.checkpw(prehash, hash)
}
