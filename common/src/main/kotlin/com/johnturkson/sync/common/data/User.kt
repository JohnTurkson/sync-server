package com.johnturkson.sync.common.data

import com.johnturkson.cdk.generator.annotations.dynamodb.Flatten
import com.johnturkson.cdk.generator.annotations.dynamodb.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource(tableName = "SyncUsers", tableAlias = "Users")
data class User(
    @Flatten
    val metadata: UserMetadata,
)
