package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.sync.common.requests.GetItemRequest
import com.johnturkson.sync.common.responses.GetItemResponse
import com.johnturkson.sync.functions.resources.Resources.ItemsTable
import com.johnturkson.sync.functions.resources.Resources.Serializer
import software.amazon.awssdk.enhanced.dynamodb.Key

class GetItemFunction : HttpLambdaFunction<GetItemRequest, GetItemResponse> {
    override val serializer = Serializer
    override val decoder = HttpLambdaRequest.serializer(GetItemRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(GetItemResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<GetItemRequest>,
        context: Context,
    ): HttpLambdaResponse<GetItemResponse> {
        val key = Key.builder().partitionValue(request.body.id).build()
        val data = ItemsTable.getItem(key).join()
        return HttpLambdaResponse(
            200,
            mapOf("Content-Type" to "application/json"),
            false,
            GetItemResponse.Success(data)
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
