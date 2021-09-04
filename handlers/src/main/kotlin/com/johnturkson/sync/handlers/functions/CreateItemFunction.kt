package com.johnturkson.sync.handlers.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.sync.common.data.Item
import com.johnturkson.sync.common.data.ItemMetadata
import com.johnturkson.sync.common.requests.CreateItemRequest
import com.johnturkson.sync.common.responses.CreateItemResponse
import com.johnturkson.sync.handlers.resources.Resources.Serializer
import com.johnturkson.sync.handlers.utilities.createItem
import com.johnturkson.sync.handlers.utilities.generateResourceId
import com.johnturkson.sync.handlers.utilities.verify

class CreateItemFunction : HttpLambdaFunction<CreateItemRequest, CreateItemResponse> {
    override val serializer = Serializer
    override val decoder = HttpLambdaRequest.serializer(CreateItemRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(CreateItemResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<CreateItemRequest>,
        context: Context,
    ): HttpLambdaResponse<CreateItemResponse> {
        val authorization = request.body.authorization
        
        val id = generateResourceId()
        val metadata = ItemMetadata(id, authorization.user)
        val data = request.body.data
        val item = Item(metadata, data)
        
        authorization.verify()?.createItem(item)
            ?: return HttpLambdaResponse(
                400,
                mapOf("Content-Type" to "application/json"),
                false,
                CreateItemResponse.Failure("Invalid Authorization")
            )
        
        return HttpLambdaResponse(
            200,
            mapOf("Content-Type" to "application/json"),
            false,
            CreateItemResponse.Success(item)
        )
    }
    
    override fun onFailure(exception: Throwable, context: Context): HttpLambdaResponse<CreateItemResponse> {
        return HttpLambdaResponse(
            400,
            mapOf("Content-Type" to "application/json"),
            false,
            CreateItemResponse.Failure(exception.stackTraceToString())
        )
    }
}
