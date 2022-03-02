package com.johnturkson.sync.common.data

import com.johnturkson.sync.generators.annotations.Flatten
import com.johnturkson.sync.generators.annotations.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncUsers", tableAlias = "Users")
data class User(
    @Flatten
    val metadata: UserMetadata,
)
