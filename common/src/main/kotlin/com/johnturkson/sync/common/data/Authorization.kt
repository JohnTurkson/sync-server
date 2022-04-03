package com.johnturkson.sync.common.data

import com.johnturkson.cdk.generator.annotations.dynamodb.PrimaryPartitionKey
import com.johnturkson.cdk.generator.annotations.dynamodb.Resource
import com.johnturkson.cdk.generator.annotations.dynamodb.SecondaryPartitionKey
import com.johnturkson.cdk.generator.annotations.dynamodb.SecondarySortKey
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncAuthorization", tableAlias = "Authorization")
data class Authorization(
    @PrimaryPartitionKey
    @SecondarySortKey(indexName = "SyncAuthorizationUserIndex", indexAlias = "AuthorizationUserIndex")
    val token: String,
    @SecondaryPartitionKey(indexName = "SyncAuthorizationUserIndex", indexAlias = "AuthorizationUserIndex")
    val user: String,
)
