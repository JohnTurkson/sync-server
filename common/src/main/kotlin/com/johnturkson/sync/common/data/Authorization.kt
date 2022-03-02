package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.Resource
import com.johnturkson.sync.generators.annotations.SecondaryPartitionKey
import com.johnturkson.sync.generators.annotations.SecondarySortKey
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
