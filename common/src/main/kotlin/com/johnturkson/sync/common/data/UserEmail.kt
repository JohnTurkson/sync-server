package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncUserEmails", tableAlias = "UserEmails")
data class UserEmail(
    @PrimaryPartitionKey
    val email: String,
    val user: String,
)
