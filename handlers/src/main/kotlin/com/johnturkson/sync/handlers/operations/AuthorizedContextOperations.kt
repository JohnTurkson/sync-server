package com.johnturkson.sync.handlers.operations

import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.Item
import com.johnturkson.sync.common.data.User
import com.johnturkson.sync.common.data.UserCredentials
import com.johnturkson.sync.common.generated.AuthorizationObject.Authorization
import com.johnturkson.sync.common.generated.ItemObject.Items
import com.johnturkson.sync.common.generated.ItemObject.ItemsUserIndex
import com.johnturkson.sync.common.generated.UserCredentialsObject.UserCredentials
import com.johnturkson.sync.common.generated.UserObject.Users
import com.johnturkson.sync.handlers.contexts.AuthorizedContext
import com.johnturkson.sync.handlers.resources.Resources.DynamoDbClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

suspend fun Authorization.verify(): AuthorizedContext? {
    val authorization = DynamoDbClient.Authorization.getItem(this).await() ?: return null
    return if (authorization == this) AuthorizedContext(authorization) else null
}

suspend fun UserCredentials.verify(): AuthorizedContext? {
    val credentials = DynamoDbClient.UserCredentials.getItem(this).await() ?: return null
    val passwordMatches = this.password.comparePasswordToHash(credentials.password)
    if (!passwordMatches) return null
    val token = generateAuthorizationToken()
    val authorization = Authorization(token, credentials.user)
    DynamoDbClient.Authorization.putItem(authorization).await()
    return AuthorizedContext(authorization)
}

suspend fun AuthorizedContext.getUser(id: String): User? {
    val key = Key.builder().partitionValue(id).build()
    val user = DynamoDbClient.Users.getItem(key).await() ?: return null
    return if (user.metadata.id == this.authorization.user) user else null
}

suspend fun AuthorizedContext.getItem(id: String): Item? {
    val key = Key.builder().partitionValue(id).build()
    val item = DynamoDbClient.Items.getItem(key).await() ?: return null
    return if (item.metadata.user == this.authorization.user) item else null
}

fun AuthorizedContext.listItems(user: String): Flow<Item>? {
    if (user != this.authorization.user) return null
    val key = Key.builder().partitionValue(user).build()
    return flow {
        DynamoDbClient.ItemsUserIndex.query(QueryConditional.keyEqualTo(key))
            .subscribe { page -> runBlocking { page.items().forEach { emit(it) } } }
            .await()
    }
}

suspend fun AuthorizedContext.createItem(item: Item): Item? {
    if (item.metadata.user != this.authorization.user) return null
    DynamoDbClient.Items.putItem(item).await()
    return item
}
