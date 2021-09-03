package com.johnturkson.sync.functions.resources

import com.johnturkson.sync.common.generated.AuthorizationObject
import com.johnturkson.sync.common.generated.ItemObject
import com.johnturkson.sync.common.generated.UserCredentialsObject
import com.johnturkson.sync.common.generated.UserObject
import kotlinx.serialization.json.Json
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.core.SdkSystemSetting
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

object Resources {
    val Serializer = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    val DynamoDbClient = DynamoDbEnhancedAsyncClient.builder()
        .dynamoDbClient(
            DynamoDbAsyncClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
                .build()
        )
        .build()
    
    val ItemsTable = DynamoDbClient.table("SyncItems", ItemObject.SCHEMA)
    val ItemsUserIndex = ItemsTable.index("SyncItemsUserIndex")
    
    val AuthorizationTable = DynamoDbClient.table("SyncAuthorization", AuthorizationObject.SCHEMA)
    val AuthorizationUserIndex = AuthorizationTable.index("SyncAuthorizationUserIndex")
    
    val UserCredentialsTable = DynamoDbClient.table("SyncUserCredentials", UserCredentialsObject.SCHEMA)
    
    val UsersTable = DynamoDbClient.table("SyncUsers", UserObject.SCHEMA)
    val UsersIdIndex = UsersTable.index("SyncUsersIdIndex")
}
