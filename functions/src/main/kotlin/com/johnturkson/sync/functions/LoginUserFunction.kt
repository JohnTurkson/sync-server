package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.johnturkson.sync.common.requests.LoginUserRequest
import com.johnturkson.sync.common.responses.LoginUserResponse
import com.johnturkson.sync.common.responses.LoginUserResponse.Failure
import com.johnturkson.sync.generators.annotations.lambda.Function
import com.johnturkson.sync.functions.definitions.LambdaHandler
import com.johnturkson.sync.functions.operations.verify
import com.johnturkson.sync.functions.resources.Resources.Serializer
import com.johnturkson.sync.generators.annotations.apigateway.Route
import kotlinx.coroutines.runBlocking

@Function
@Route("POST", "/LoginUser")
class LoginUserFunction :
    RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>,
    LambdaHandler<LoginUserRequest, LoginUserResponse> {
    override fun handleRequest(input: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val response = runBlocking { processRequest(input.body) }
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(response.statusCode)
            .withHeaders(mapOf("Content-Type" to "application/json"))
            .withBody(encodeResponse(response))
            .build()
    }
    
    override suspend fun processRequest(body: String?): LoginUserResponse {
        val request = decodeRequest(body) ?: return Failure("Invalid Request", 400)
        val authorization = request.credentials.verify()?.authorization ?: return Failure("Invalid Authorization", 401)
        return LoginUserResponse.Success(authorization, 200)
    }
    
    override fun decodeRequest(body: String?): LoginUserRequest? {
        return runCatching {
            body?.let { data -> Serializer.decodeFromString(LoginUserRequest.serializer(), data) }
        }.getOrNull()
    }
    
    override fun encodeResponse(response: LoginUserResponse): String {
        return Serializer.encodeToString(LoginUserResponse.serializer(), response)
    }
    
}
