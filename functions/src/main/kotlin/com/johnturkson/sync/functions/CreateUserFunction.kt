package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.User
import com.johnturkson.sync.common.data.UserCredentials
import com.johnturkson.sync.common.data.UserEmail
import com.johnturkson.sync.common.data.UserMetadata
import com.johnturkson.sync.common.generated.AuthorizationDefinition.Authorization
import com.johnturkson.sync.common.generated.UserCredentialsDefinition.UserCredentials
import com.johnturkson.sync.common.generated.UserDefinition.Users
import com.johnturkson.sync.common.generated.UserEmailDefinition.UserEmails
import com.johnturkson.sync.common.requests.CreateUserRequest
import com.johnturkson.sync.common.responses.CreateUserResponse
import com.johnturkson.sync.common.responses.CreateUserResponse.Failure
import com.johnturkson.sync.common.responses.CreateUserResponse.Success
import com.johnturkson.sync.functions.definitions.LambdaHandler
import com.johnturkson.sync.functions.operations.generateAuthorizationToken
import com.johnturkson.sync.functions.operations.generateResourceId
import com.johnturkson.sync.functions.operations.hashPassword
import com.johnturkson.sync.functions.resources.Resources.DynamoDbClient
import com.johnturkson.sync.functions.resources.Resources.Serializer
import com.johnturkson.sync.generators.annotations.apigateway.HttpApiRoute
import com.johnturkson.sync.generators.annotations.lambda.Function
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest

@Function
@HttpApiRoute("POST", "sync.johnturkson.com", "/CreateUser")
class CreateUserFunction :
    RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>,
    LambdaHandler<CreateUserRequest, CreateUserResponse> {
    override fun handleRequest(input: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val response = runBlocking { processRequest(input.body) }
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(response.statusCode)
            .withHeaders(mapOf("Content-Type" to "application/json"))
            .withBody(encodeResponse(response))
            .build()
    }
    
    override suspend fun processRequest(body: String?): CreateUserResponse {
        val request = decodeRequest(body) ?: return Failure("Invalid Request", 400)
        
        val user = runCatching {
            createUser(request)
        }.onFailure { exception ->
            exception.printStackTrace()
        }.getOrElse {
            return Failure("User Already Exists", 409)
        }
        
        val authorization = runCatching {
            createUserAuthorization(user)
        }.onFailure { exception ->
            exception.printStackTrace()
        }.getOrElse {
            return Failure("Failed to Create User Token", 500)
        }
        
        return Success(user, authorization, 200)
    }
    
    override fun decodeRequest(body: String?): CreateUserRequest? {
        return runCatching {
            body?.let { data -> Serializer.decodeFromString(CreateUserRequest.serializer(), data) }
        }.onFailure { exception ->
            exception.printStackTrace()
        }.getOrNull()
    }
    
    override fun encodeResponse(response: CreateUserResponse): String {
        return Serializer.encodeToString(CreateUserResponse.serializer(), response)
    }
    
    private suspend fun createUser(request: CreateUserRequest): User {
        val user = User(UserMetadata(generateResourceId(), request.email))
        val userEmail = UserEmail(user.metadata.email, user.metadata.id)
        val userCredentials = UserCredentials(user.metadata.email, request.password.hashPassword())
        
        DynamoDbClient.transactWriteItems { transaction ->
            val userEmailExistsCondition = Expression.builder()
                .expression("attribute_not_exists(#email)")
                .expressionNames(mapOf("#email" to "email"))
                .build()
            
            transaction.addPutItem(
                DynamoDbClient.UserEmails,
                PutItemEnhancedRequest.builder(UserEmail::class.java)
                    .item(userEmail)
                    .conditionExpression(userEmailExistsCondition)
                    .build()
            )
            transaction.addPutItem(DynamoDbClient.UserCredentials, userCredentials)
            transaction.addPutItem(DynamoDbClient.Users, user)
        }.await()
        
        return user
    }
    
    private suspend fun createUserAuthorization(user: User): Authorization {
        val authorization = Authorization(generateAuthorizationToken(), user.metadata.id)
        DynamoDbClient.Authorization.putItem(authorization).await()
        return authorization
    }
}
