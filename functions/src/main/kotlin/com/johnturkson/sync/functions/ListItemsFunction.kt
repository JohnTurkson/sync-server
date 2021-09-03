package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.sync.common.data.Item
import com.johnturkson.sync.common.requests.ListItemsRequest
import com.johnturkson.sync.common.responses.ListItemsResponse
import com.johnturkson.sync.functions.resources.Resources
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo

class ListItemsFunction : HttpLambdaFunction<ListItemsRequest, ListItemsResponse> {
    override val serializer = Resources.Serializer
    override val decoder = HttpLambdaRequest.serializer(ListItemsRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(ListItemsResponse.serializer())
    
    override fun process(request: HttpLambdaRequest<ListItemsRequest>, context: Context): HttpLambdaResponse<ListItemsResponse> {
        val user = request.body.user
        val itemsIndexKey = Key.builder().partitionValue(user).build()
        
        val items = mutableListOf<Item>()
        Resources.ItemsUserIndex.query(keyEqualTo(itemsIndexKey))
            .subscribe { page -> items += page.items() }
            .join()
        
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
