package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.johnturkson.sync.common.requests.ListItemsRequest
import com.johnturkson.sync.common.responses.ListItemsResponse
import com.johnturkson.sync.common.responses.ListItemsResponse.Failure
import com.johnturkson.sync.common.responses.ListItemsResponse.Success
import com.johnturkson.sync.generators.annotations.lambda.Function
import com.johnturkson.sync.functions.definitions.LambdaHandler
import com.johnturkson.sync.functions.operations.listItems
import com.johnturkson.sync.functions.operations.verify
import com.johnturkson.sync.functions.resources.Resources.Serializer
import com.johnturkson.sync.generators.annotations.apigateway.Route
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

@Function
@Route("POST", "/ListItems")
class ListItemsFunction :
    RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>,
    LambdaHandler<ListItemsRequest, ListItemsResponse> {
    override fun handleRequest(input: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val response = runBlocking { processRequest(input.body) }
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(response.statusCode)
            .withHeaders(mapOf("Content-Type" to "application/json"))
            .withBody(encodeResponse(response))
            .build()
    }
    
    override suspend fun processRequest(body: String?): ListItemsResponse {
        val request = decodeRequest(body) ?: return Failure("Invalid Request", 400)
        val authorization = request.authorization.verify() ?: return Failure("Invalid Authorization", 401)
        val items = authorization.listItems(request.user)?.toList().orEmpty()
        return Success(items, 200)
    }
    
    override fun decodeRequest(body: String?): ListItemsRequest? {
        return runCatching {
            body?.let { data -> Serializer.decodeFromString(ListItemsRequest.serializer(), data) }
        }.getOrNull()
    }
    
    override fun encodeResponse(response: ListItemsResponse): String {
        return Serializer.encodeToString(ListItemsResponse.serializer(), response)
    }
}
