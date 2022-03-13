package com.johnturkson.sync.handlers.operations

import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.Item
import com.johnturkson.sync.common.data.UserCredentials
import com.johnturkson.sync.common.generated.AuthorizationTable.Authorization
import com.johnturkson.sync.common.generated.ItemTable.Items
import com.johnturkson.sync.common.generated.ItemTable.ItemsUserIndex
import com.johnturkson.sync.common.generated.UserCredentialsTable.UserCredentials
import com.johnturkson.sync.common.generated.UserEmailTable.UserEmails
import com.johnturkson.sync.handlers.contexts.AuthorizedContext
import com.johnturkson.sync.handlers.resources.Resources.DynamoDbClient
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.future.await
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

suspend fun Authorization.verify(): AuthorizedContext? {
    val authorization = DynamoDbClient.Authorization.getItem(this).await() ?: return null
    val authorizedContext = AuthorizedContext(authorization)
    return if (authorization == this) authorizedContext else null
}

suspend fun UserCredentials.verify(): AuthorizedContext? {
    val credentials = DynamoDbClient.UserCredentials.getItem(this).await() ?: return null
    val passwordMatches = this.password.comparePasswordToHash(credentials.password)
    if (!passwordMatches) return null
    val key = Key.builder().partitionValue(credentials.email).build()
    val userEmail = DynamoDbClient.UserEmails.getItem(key).await() ?: return null
    val authorization = Authorization(generateAuthorizationToken(), userEmail.user)
    DynamoDbClient.Authorization.putItem(authorization).await()
    return AuthorizedContext(authorization)
}

suspend fun AuthorizedContext.getItem(id: String): Item? {
    val key = Key.builder().partitionValue(id).build()
    val item = DynamoDbClient.Items.getItem(key).await() ?: return null
    return if (item.metadata.user == this.authorization.user) item else null
}

fun AuthorizedContext.listItems(user: String): Flow<Item>? {
    if (user != this.authorization.user) return null
    val key = Key.builder().partitionValue(user).build()
    return channelFlow {
        DynamoDbClient.ItemsUserIndex.query(QueryConditional.keyEqualTo(key))
            .subscribe { page -> page.items().forEach { item -> trySendBlocking(item) } }
            .await()
    }
}

suspend fun AuthorizedContext.createItem(item: Item): Item? {
    if (item.metadata.user != this.authorization.user) return null
    DynamoDbClient.Items.putItem(item).await()
    return item
}
