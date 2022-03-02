package com.johnturkson.sync.handlers.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.sync.common.requests.LoginUserRequest
import com.johnturkson.sync.common.responses.LoginUserResponse
import com.johnturkson.sync.handlers.resources.Resources
import com.johnturkson.sync.handlers.operations.verify
import kotlinx.coroutines.runBlocking

class LoginUserFunction : HttpLambdaFunction<LoginUserRequest, LoginUserResponse> {
    override val serializer = Resources.Serializer
    override val decoder = HttpLambdaRequest.serializer(LoginUserRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(LoginUserResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<LoginUserRequest>,
        context: Context,
    ): HttpLambdaResponse<LoginUserResponse> {
        val credentials = request.body.credentials
        val authorization = runBlocking { credentials.verify()?.authorization }
            ?: return HttpLambdaResponse(
                400,
                mapOf("Content-Type" to "application/json"),
                false,
                LoginUserResponse.Failure("Invalid User")
            )
        
        return HttpLambdaResponse(
            200,
            mapOf("Content-Type" to "application/json"),
            false,
            LoginUserResponse.Success(authorization)
        )
    }
    
    override fun onFailure(exception: Throwable, context: Context): HttpLambdaResponse<LoginUserResponse> {
        return HttpLambdaResponse(
            400,
            mapOf("Content-Type" to "application/json"),
            false,
            LoginUserResponse.Failure(exception.stackTraceToString())
        )
    }
}
