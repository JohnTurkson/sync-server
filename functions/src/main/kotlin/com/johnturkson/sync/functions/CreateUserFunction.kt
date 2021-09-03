package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.security.generateSecureRandomBytes
import com.johnturkson.sync.common.data.*
import com.johnturkson.sync.common.requests.CreateUserRequest
import com.johnturkson.sync.common.responses.CreateUserResponse
import com.johnturkson.sync.functions.resources.Resources
import com.johnturkson.sync.functions.utilities.hashPassword
import com.johnturkson.text.encodeBase64
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest

class CreateUserFunction : HttpLambdaFunction<CreateUserRequest, CreateUserResponse> {
    override val serializer = Resources.Serializer
    override val decoder = HttpLambdaRequest.serializer(CreateUserRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(CreateUserResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<CreateUserRequest>,
        context: Context,
    ): HttpLambdaResponse<CreateUserResponse> {
        val (email, password) = request.body.data
        val id = generateSecureRandomBytes(16).encodeBase64()
        val userMetadata = UserMetadata(id)
        val userData = UserData(email)
        val user = User(userMetadata, userData)
        val userCredentials = UserCredentials(email, hashPassword(password))
        val token = generateSecureRandomBytes(16).encodeBase64()
        val authorization = Authorization(token, user.metadata.id)
        
        Resources.DynamoDbClient.transactWriteItems { transaction ->
            val userExistsCondition = Expression.builder()
                .expression("attribute_not_exists(#email)")
                .expressionNames(mapOf("#email" to "email"))
                .build()
            
            transaction.addPutItem(
                Resources.UsersTable,
                PutItemEnhancedRequest.builder(User::class.java)
                    .item(user)
                    .conditionExpression(userExistsCondition)
                    .build()
            )
            
            transaction.addPutItem(
                Resources.UserCredentialsTable,
                PutItemEnhancedRequest.builder(UserCredentials::class.java)
                    .item(userCredentials)
                    .conditionExpression(userExistsCondition)
                    .build()
            )
            
            transaction.addPutItem(Resources.AuthorizationTable, authorization)
        }.join()
        
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
