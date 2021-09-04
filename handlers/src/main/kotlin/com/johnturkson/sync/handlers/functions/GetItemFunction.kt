package com.johnturkson.sync.handlers.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.sync.common.requests.GetItemRequest
import com.johnturkson.sync.common.responses.GetItemResponse
import com.johnturkson.sync.handlers.resources.Resources.Serializer
import com.johnturkson.sync.handlers.utilities.getItem
import com.johnturkson.sync.handlers.utilities.verify

class GetItemFunction : HttpLambdaFunction<GetItemRequest, GetItemResponse> {
    override val serializer = Serializer
    override val decoder = HttpLambdaRequest.serializer(GetItemRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(GetItemResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<GetItemRequest>,
        context: Context,
    ): HttpLambdaResponse<GetItemResponse> {
        val authorization = request.body.authorization
        val id = request.body.id
        
        val item = authorization.verify()?.getItem(id)
            ?: return HttpLambdaResponse(
                400,
                mapOf("Content-Type" to "application/json"),
                false,
                GetItemResponse.Failure("Invalid Authorization")
            )
        
        return HttpLambdaResponse(
            200,
            mapOf("Content-Type" to "application/json"),
            false,
            GetItemResponse.Success(item)
        )
    }
    
    override fun onFailure(exception: Throwable, context: Context): HttpLambdaResponse<GetItemResponse> {
        return HttpLambdaResponse(
            400,
            mapOf("Content-Type" to "application/json"),
            false,
            GetItemResponse.Failure(exception.stackTraceToString())
        )
    }
}
