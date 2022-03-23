package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.dynamodb.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncUserCredentials", tableAlias = "UserCredentials")
data class UserCredentials(
    @PrimaryPartitionKey
    val email: String,
    val password: String,
)
