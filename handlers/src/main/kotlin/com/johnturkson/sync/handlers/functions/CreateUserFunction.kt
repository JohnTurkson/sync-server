package com.johnturkson.sync.handlers.functions

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.User
import com.johnturkson.sync.common.data.UserCredentials
import com.johnturkson.sync.common.data.UserMetadata
import com.johnturkson.sync.common.generated.AuthorizationObject.Authorization
import com.johnturkson.sync.common.generated.UserCredentialsObject.UserCredentials
import com.johnturkson.sync.common.generated.UserObject.Users
import com.johnturkson.sync.common.requests.CreateUserRequest
import com.johnturkson.sync.common.responses.CreateUserResponse
import com.johnturkson.sync.common.responses.CreateUserResponse.Failure
import com.johnturkson.sync.common.responses.CreateUserResponse.Success
import com.johnturkson.sync.handlers.operations.generateAuthorizationToken
import com.johnturkson.sync.handlers.operations.generateResourceId
import com.johnturkson.sync.handlers.operations.hashPassword
import com.johnturkson.sync.handlers.resources.Resources
import com.johnturkson.sync.handlers.resources.Resources.Serializer
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest

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
    
    override suspend fun processRequest(body: String): CreateUserResponse {
        val request = decodeRequest(body) ?: return Failure("Invalid Request", 400)
        val user = User(UserMetadata(generateResourceId()))
        val userCredentials = UserCredentials(user.metadata.id, request.password.hashPassword())
        val authorization = Authorization(generateAuthorizationToken(), user.metadata.id)
        Resources.DynamoDbClient.transactWriteItems { transaction ->
            val userExistsCondition = Expression.builder()
                .expression("attribute_not_exists(#email)")
                .expressionNames(mapOf("#email" to "email"))
                .build()
            
            transaction.addPutItem(
                Resources.DynamoDbClient.Users,
                PutItemEnhancedRequest.builder(User::class.java)
                    .item(user)
                    .conditionExpression(userExistsCondition)
                    .build()
            )
            
            transaction.addPutItem(
                Resources.DynamoDbClient.UserCredentials,
                PutItemEnhancedRequest.builder(UserCredentials::class.java)
                    .item(userCredentials)
                    .conditionExpression(userExistsCondition)
                    .build()
            )
            
            transaction.addPutItem(Resources.DynamoDbClient.Authorization, authorization)
        }.await()
        return Success(user, authorization, 200)
    }
    
    override fun decodeRequest(body: String): CreateUserRequest? {
        return runCatching { Serializer.decodeFromString(CreateUserRequest.serializer(), body) }.getOrNull()
    }
    
    override fun encodeResponse(response: CreateUserResponse): String {
        return Serializer.encodeToString(CreateUserResponse.serializer(), response)
    }
}
