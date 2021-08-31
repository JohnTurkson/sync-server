package com.johnturkson.sync.resources

import com.johnturkson.sync.data.Credentials
import com.johnturkson.sync.data.CredentialsData
import com.johnturkson.sync.data.CredentialsMetadata
import kotlinx.serialization.json.Json
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.core.SdkSystemSetting
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags
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
    
    val CredentialsMetadataSchema = TableSchema.builder(CredentialsMetadata::class.java, CredentialsMetadata.Builder::class.java)
        .newItemBuilder(CredentialsMetadata::Builder, CredentialsMetadata.Builder::build)
        .addAttribute(String::class.java) { attribute ->
            attribute.name("id")
                .getter(CredentialsMetadata::id)
                .setter(CredentialsMetadata.Builder::id)
                .tags(StaticAttributeTags.primaryPartitionKey())
        }
        .build()
    
    val CredentialsDataSchema = TableSchema.builder(CredentialsData::class.java, CredentialsData.Builder::class.java)
        .newItemBuilder(CredentialsData::Builder, CredentialsData.Builder::build)
        .addAttribute(String::class.java) { attribute ->
            attribute.name("service")
                .getter(CredentialsData::service)
                .setter(CredentialsData.Builder::service)
        }
        .addAttribute(String::class.java) { attribute ->
            attribute.name("login")
                .getter(CredentialsData::login)
                .setter(CredentialsData.Builder::login)
        }
        .addAttribute(String::class.java) { attribute ->
            attribute.name("password")
                .getter(CredentialsData::password)
                .setter(CredentialsData.Builder::password)
        }
        .build()
    
    val CredentialsTableSchema = TableSchema.builder(Credentials::class.java, Credentials.Builder::class.java)
        .newItemBuilder(Credentials::Builder, Credentials.Builder::build)
        .flatten(CredentialsMetadataSchema, Credentials::metadata, Credentials.Builder::metadata)
        .flatten(CredentialsDataSchema, Credentials::data, Credentials.Builder::data)
        .build()
    
    val CredentialsTable = DynamoDbClient.table("Credentials", CredentialsTableSchema)
}
