package com.johnturkson.sync.handlers.utilities

import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.Item
import com.johnturkson.sync.common.data.User
import com.johnturkson.sync.common.data.UserCredentials
import com.johnturkson.sync.handlers.contexts.AuthorizedContext
import com.johnturkson.sync.handlers.resources.Resources
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

fun Authorization.verify(): AuthorizedContext? {
    val authorization = Resources.AuthorizationTable.getItem(this).join() ?: return null
    return if (authorization == this) AuthorizedContext(authorization) else null
}

fun UserCredentials.verify(): AuthorizedContext? {
    val credentials = Resources.UserCredentialsTable
        .getItem(this)
        .join() ?: return null
    val passwordMatches = this.password.comparePasswordToHash(credentials.password)
    if (!passwordMatches) return null
    val token = generateAuthorizationToken()
    val authorization = Authorization(token, credentials.user)
    Resources.AuthorizationTable.putItem(authorization).join()
    return AuthorizedContext(authorization)
}

fun AuthorizedContext.getUser(id: String): User? {
    val key = Key.builder().partitionValue(id).build()
    val user = Resources.UsersTable.getItem(key).join() ?: return null
    return if (user.metadata.id == this.authorization.user) user else null
}

fun AuthorizedContext.getItem(id: String): Item? {
    val key = Key.builder().partitionValue(id).build()
    val item = Resources.ItemsTable.getItem(key).join() ?: return null
    return if (item.metadata.user == this.authorization.user) item else null
}

fun AuthorizedContext.listItems(user: String): List<Item>? {
    if (user != this.authorization.user) return null
    val key = Key.builder().partitionValue(user).build()
    val items = mutableListOf<Item>()
    Resources.ItemsUserIndex.query(QueryConditional.keyEqualTo(key))
        .subscribe { page -> items += page.items() }
        .join()
    return items
}

fun AuthorizedContext.createItem(item: Item): Item? {
    if (item.metadata.user != this.authorization.user) return null
    Resources.ItemsTable.putItem(item).join()
    return item
}
