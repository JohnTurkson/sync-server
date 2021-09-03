package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.security.generateSecureRandomBytes
import com.johnturkson.sync.common.data.Item
import com.johnturkson.sync.common.data.ItemMetadata
import com.johnturkson.sync.common.requests.CreateItemRequest
import com.johnturkson.sync.common.responses.CreateItemResponse
import com.johnturkson.sync.functions.resources.Resources.ItemsTable
import com.johnturkson.sync.functions.resources.Resources.Serializer
import com.johnturkson.sync.functions.utilities.getUserByAuthorization
import com.johnturkson.text.encodeBase64

class CreateItemFunction : HttpLambdaFunction<CreateItemRequest, CreateItemResponse> {
    override val serializer = Serializer
    override val decoder = HttpLambdaRequest.serializer(CreateItemRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(CreateItemResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<CreateItemRequest>,
        context: Context,
    ): HttpLambdaResponse<CreateItemResponse> {
        val user = getUserByAuthorization(request.body.authorization) ?: return HttpLambdaResponse(
            400,
            mapOf("Content-Type" to "application/json"),
            false,
            CreateItemResponse.Failure("Invalid User")
        )
        
        val id = generateSecureRandomBytes(16).encodeBase64()
        val metadata = ItemMetadata(id, user.metadata.id)
        val data = request.body.data
        val item = Item(metadata, data)
        
        ItemsTable.putItem(item).join()
        
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
