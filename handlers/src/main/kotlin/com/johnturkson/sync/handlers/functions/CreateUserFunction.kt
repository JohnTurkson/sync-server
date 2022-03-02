package com.johnturkson.sync.handlers.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.sync.common.data.Authorization
import com.johnturkson.sync.common.data.User
import com.johnturkson.sync.common.data.UserCredentials
import com.johnturkson.sync.common.data.UserMetadata
import com.johnturkson.sync.common.generated.AuthorizationObject.Authorization
import com.johnturkson.sync.common.generated.UserCredentialsObject.UserCredentials
import com.johnturkson.sync.common.generated.UserObject.Users
import com.johnturkson.sync.common.requests.CreateUserRequest
import com.johnturkson.sync.common.responses.CreateUserResponse
import com.johnturkson.sync.handlers.operations.generateAuthorizationToken
import com.johnturkson.sync.handlers.operations.generateResourceId
import com.johnturkson.sync.handlers.operations.hashPassword
import com.johnturkson.sync.handlers.resources.Resources
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest

class CreateUserFunction : HttpLambdaFunction<CreateUserRequest, CreateUserResponse> {
    override val serializer = Resources.Serializer
    override val decoder = HttpLambdaRequest.serializer(CreateUserRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(CreateUserResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<CreateUserRequest>,
        context: Context,
    ): HttpLambdaResponse<CreateUserResponse> {
        val password = request.body.password
        val id = generateResourceId()
        val userMetadata = UserMetadata(id)
        val user = User(userMetadata)
        val userCredentials = UserCredentials(id, password.hashPassword())
        val token = generateAuthorizationToken()
        val authorization = Authorization(token, user.metadata.id)
        
        runBlocking {
            Resources.DynamoDbClient.transactWriteItems { transaction ->
                val userExistsCondition = Expression.builder()
                    .expression("attribute_not_exists(#email)")
                    .expressionNames(mapOf("#email" to "email"))
                    .build()
                
                transaction.addPutItem(
                    Resources.DynamoDbClient.Users,
                    TransactPutItemEnhancedRequest.builder(User::class.java)
                        .item(user)
                        .conditionExpression(userExistsCondition)
                        .build()
                )
                
                transaction.addPutItem(
                    Resources.DynamoDbClient.UserCredentials,
                    TransactPutItemEnhancedRequest.builder(UserCredentials::class.java)
                        .item(userCredentials)
                        .conditionExpression(userExistsCondition)
                        .build()
                )
                
                transaction.addPutItem(Resources.DynamoDbClient.Authorization, authorization)
            }.await()
        }
        
        return HttpLambdaResponse(
            200,
            mapOf("Content-Type" to "application/json"),
            false,
            CreateUserResponse.Success(user, authorization)
        )
    }
    
    override fun onFailure(exception: Throwable, context: Context): HttpLambdaResponse<CreateUserResponse> {
        return HttpLambdaResponse(
            400,
            mapOf("Content-Type" to "application/json"),
            false,
            CreateUserResponse.Failure(exception.stackTraceToString())
        )
    }
}
