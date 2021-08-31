package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.sync.requests.GetCredentialsRequest
import com.johnturkson.sync.resources.Resources.CredentialsTable
import com.johnturkson.sync.resources.Resources.Serializer
import com.johnturkson.sync.responses.GetCredentialsResponse
import software.amazon.awssdk.enhanced.dynamodb.Key

class GetCredentialsFunction : HttpLambdaFunction<GetCredentialsRequest, GetCredentialsResponse> {
    override val serializer = Serializer
    override val decoder = HttpLambdaRequest.serializer(GetCredentialsRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(GetCredentialsResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<GetCredentialsRequest>,
        context: Context,
    ): HttpLambdaResponse<GetCredentialsResponse> {
        val key = Key.builder().partitionValue(request.body.id).build()
        val data = CredentialsTable.getItem(key).join()
        return HttpLambdaResponse(
            200,
            mapOf("Content-Type" to "application/json"),
            false,
            GetCredentialsResponse.Success(data)
        )
    }
    
    override fun onFailure(exception: Throwable, context: Context): HttpLambdaResponse<GetCredentialsResponse> {
        return HttpLambdaResponse(
            400,
            mapOf("Content-Type" to "application/json"),
            false,
            GetCredentialsResponse.Failure("")
        )
    }
}
