package com.johnturkson.sync.handlers.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.sync.common.requests.ListItemsRequest
import com.johnturkson.sync.common.responses.ListItemsResponse
import com.johnturkson.sync.handlers.operations.listItems
import com.johnturkson.sync.handlers.operations.verify
import com.johnturkson.sync.handlers.resources.Resources
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class ListItemsFunction : HttpLambdaFunction<ListItemsRequest, ListItemsResponse> {
    override val serializer = Resources.Serializer
    override val decoder = HttpLambdaRequest.serializer(ListItemsRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(ListItemsResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<ListItemsRequest>,
        context: Context,
    ): HttpLambdaResponse<ListItemsResponse> {
        val authorization = request.body.authorization
        val user = request.body.user
        val items = runBlocking { authorization.verify()?.listItems(user)?.toList() }
            ?: return HttpLambdaResponse(
                400,
                mapOf("Content-Type" to "application/json"),
                false,
                ListItemsResponse.Failure("Invalid Authorization")
            )
        
        return HttpLambdaResponse(
            200,
            mapOf("Content-Type" to "application/json"),
            false,
            ListItemsResponse.Success(items)
        )
    }
    
    override fun onFailure(exception: Throwable, context: Context): HttpLambdaResponse<ListItemsResponse> {
        return HttpLambdaResponse(
            400,
            mapOf("Content-Type" to "application/json"),
            false,
            ListItemsResponse.Failure(exception.stackTraceToString())
        )
    }
}
