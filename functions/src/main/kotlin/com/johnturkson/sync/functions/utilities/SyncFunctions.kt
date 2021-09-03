package com.johnturkson.sync.functions.utilities

import com.johnturkson.sync.common.data.User
import com.johnturkson.sync.functions.resources.Resources
import org.springframework.security.crypto.bcrypt.BCrypt
import software.amazon.awssdk.enhanced.dynamodb.Key
import java.security.MessageDigest

fun getUserByAuthorization(token: String): User? {
    val authorizationTablePartitionKey = Key.builder().partitionValue(token).build()
    val authorization = Resources.AuthorizationTable.getItem(authorizationTablePartitionKey).join() ?: return null
    val userTablePartitionKey = Key.builder().partitionValue(authorization.user).build()
    return Resources.UsersTable.getItem(userTablePartitionKey).join() ?: return null
}

fun hashPassword(password: String): String {
    val algorithm = "SHA3-512"
    val hash = MessageDigest.getInstance(algorithm).digest(password.toByteArray())
    return BCrypt.hashpw(hash, BCrypt.gensalt())
}
