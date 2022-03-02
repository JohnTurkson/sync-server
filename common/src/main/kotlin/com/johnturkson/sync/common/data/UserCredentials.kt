package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncUserCredentials", tableAlias = "UserCredentials")
data class UserCredentials(
    @PrimaryPartitionKey
    val user: String,
    val password: String,
)
