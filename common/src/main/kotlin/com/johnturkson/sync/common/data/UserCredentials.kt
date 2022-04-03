package com.johnturkson.sync.common.data

import com.johnturkson.cdk.generator.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.cdk.generator.annotations.dynamodb.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncUserCredentials", tableAlias = "UserCredentials")
data class UserCredentials(
    @PrimaryPartitionKey
    val email: String,
    val password: String,
)
