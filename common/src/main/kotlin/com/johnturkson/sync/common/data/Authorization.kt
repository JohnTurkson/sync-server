package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.PrimaryPartitionKey
import com.johnturkson.sync.generators.annotations.Resource
import com.johnturkson.sync.generators.annotations.SecondaryPartitionKey
import com.johnturkson.sync.generators.annotations.SecondarySortKey
import kotlinx.serialization.Serializable

@Serializable
@Resource
data class Authorization(
    @PrimaryPartitionKey
    @SecondarySortKey("SyncAuthorizationUserIndex")
    val token: String,
    @SecondaryPartitionKey("SyncAuthorizationUserIndex")
    val user: String,
)
