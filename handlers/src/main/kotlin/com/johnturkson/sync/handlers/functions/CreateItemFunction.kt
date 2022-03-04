package com.johnturkson.sync.handlers.functions

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.johnturkson.sync.common.data.Item
import com.johnturkson.sync.common.data.ItemMetadata
import com.johnturkson.sync.common.requests.CreateItemRequest
import com.johnturkson.sync.common.responses.CreateItemResponse
import com.johnturkson.sync.common.responses.CreateItemResponse.Failure
import com.johnturkson.sync.common.responses.CreateItemResponse.Success
import com.johnturkson.sync.handlers.operations.createItem
import com.johnturkson.sync.handlers.operations.generateResourceId
import com.johnturkson.sync.handlers.operations.verify
import com.johnturkson.sync.handlers.resources.Resources
import com.johnturkson.sync.handlers.resources.Resources.Serializer
import kotlinx.coroutines.runBlocking

class CreateItemFunction :
    RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>,
    LambdaHandler<CreateItemRequest, CreateItemResponse> {
    override fun handleRequest(input: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val response = runBlocking { processRequest(input.body) }
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(response.statusCode)
            .withHeaders(mapOf("Content-Type" to "application/json"))
            .withBody(encodeResponse(response))
            .build()
    }
    
    override suspend fun processRequest(body: String): CreateItemResponse {
        val request = decodeRequest(body) ?: return Failure("Invalid Request", 400)
        val authorization = request.authorization.verify() ?: return Failure("Invalid Authorization", 401)
        val metadata = ItemMetadata(generateResourceId(), request.authorization.user)
        val data = request.data
        return when (val item = authorization.createItem(Item(metadata, data))) {
            null -> Failure("Insufficient Permissions", 403)
            else -> Success(item, 200)
        }
    }
    
    override fun decodeRequest(body: String): CreateItemRequest? {
        return runCatching { Serializer.decodeFromString(CreateItemRequest.serializer(), body) }.getOrNull()
    }
    
    override fun encodeResponse(response: CreateItemResponse): String {
        return Serializer.encodeToString(CreateItemResponse.serializer(), response)
    }
}
