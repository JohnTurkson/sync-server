package com.johnturkson.sync.common.data

import com.johnturkson.cdk.generator.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.cdk.generator.annotations.dynamodb.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncUserEmails", tableAlias = "UserEmails")
data class UserEmail(
    @PrimaryPartitionKey
    val email: String,
    val user: String,
)
