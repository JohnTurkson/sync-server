package com.johnturkson.sync.functions.resources

import kotlinx.serialization.json.Json
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.core.SdkSystemSetting
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
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
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                .build()
        )
        .build()
}
