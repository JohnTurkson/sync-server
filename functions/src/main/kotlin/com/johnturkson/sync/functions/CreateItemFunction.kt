package com.johnturkson.sync.functions

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
import com.johnturkson.sync.functions.definitions.LambdaHandler
import com.johnturkson.sync.functions.operations.createItem
import com.johnturkson.sync.functions.operations.generateResourceId
import com.johnturkson.sync.functions.operations.verify
import com.johnturkson.sync.functions.resources.Resources.Serializer
import com.johnturkson.sync.generators.annotations.apigateway.Route
import com.johnturkson.sync.generators.annotations.lambda.Function
import kotlinx.coroutines.runBlocking

@Function
@Route("POST", "https://sync.johnturkson.com/CreateItem")
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
    
    override suspend fun processRequest(body: String?): CreateItemResponse {
        val request = decodeRequest(body) ?: return Failure("Invalid Request", 400)
        val authorization = request.authorization.verify() ?: return Failure("Invalid Authorization", 401)
        val metadata = ItemMetadata(generateResourceId(), request.authorization.user)
        val data = request.data
        return when (val item = authorization.createItem(Item(metadata, data))) {
            null -> Failure("Insufficient Permissions", 403)
            else -> Success(item, 200)
        }
    }
    
    override fun decodeRequest(body: String?): CreateItemRequest? {
        return runCatching {
            body?.let { data -> Serializer.decodeFromString(CreateItemRequest.serializer(), data) }
        }.getOrNull()
    }
    
    override fun encodeResponse(response: CreateItemResponse): String {
        return Serializer.encodeToString(CreateItemResponse.serializer(), response)
    }
}
