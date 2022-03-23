package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.johnturkson.sync.common.requests.GetItemRequest
import com.johnturkson.sync.common.responses.GetItemResponse
import com.johnturkson.sync.common.responses.GetItemResponse.Failure
import com.johnturkson.sync.common.responses.GetItemResponse.Success
import com.johnturkson.sync.generators.annotations.lambda.Function
import com.johnturkson.sync.functions.definitions.LambdaHandler
import com.johnturkson.sync.functions.operations.getItem
import com.johnturkson.sync.functions.operations.verify
import com.johnturkson.sync.functions.resources.Resources.Serializer
import com.johnturkson.sync.generators.annotations.apigateway.Route
import kotlinx.coroutines.runBlocking

@Function
@Route("POST", "/GetItem")
class GetItemFunction :
    RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>,
    LambdaHandler<GetItemRequest, GetItemResponse> {
    override fun handleRequest(input: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val response = runBlocking { processRequest(input.body) }
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(response.statusCode)
            .withHeaders(mapOf("Content-Type" to "application/json"))
            .withBody(encodeResponse(response))
            .build()
    }
    
    override suspend fun processRequest(body: String?): GetItemResponse {
        val request = decodeRequest(body) ?: return Failure("Invalid Request", 400)
        val authorization = request.authorization.verify() ?: return Failure("Invalid Authorization", 401)
        return when (val item = authorization.getItem(request.id)) {
            null -> Failure("Not Found", 404)
            else -> Success(item, 200)
        }
    }
    
    override fun decodeRequest(body: String?): GetItemRequest? {
        return runCatching {
            body?.let { data -> Serializer.decodeFromString(GetItemRequest.serializer(), data) }
        }.getOrNull()
    }
    
    override fun encodeResponse(response: GetItemResponse): String {
        return Serializer.encodeToString(GetItemResponse.serializer(), response)
    }
}
