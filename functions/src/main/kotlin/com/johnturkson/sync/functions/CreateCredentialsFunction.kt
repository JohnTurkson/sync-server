package com.johnturkson.sync.functions

import com.amazonaws.services.lambda.runtime.Context
import com.johnturkson.aws.lambda.handler.HttpLambdaFunction
import com.johnturkson.aws.lambda.handler.events.HttpLambdaRequest
import com.johnturkson.aws.lambda.handler.events.HttpLambdaResponse
import com.johnturkson.security.generateSecureRandomBytes
import com.johnturkson.sync.common.data.Credentials
import com.johnturkson.sync.common.data.CredentialsMetadata
import com.johnturkson.sync.common.requests.CreateCredentialsRequest
import com.johnturkson.sync.common.responses.CreateCredentialsResponse
import com.johnturkson.sync.functions.resources.Resources.CredentialsTable
import com.johnturkson.sync.functions.resources.Resources.Serializer
import com.johnturkson.text.encodeBase64

class CreateCredentialsFunction : HttpLambdaFunction<CreateCredentialsRequest, CreateCredentialsResponse> {
    override val serializer = Serializer
    override val decoder = HttpLambdaRequest.serializer(CreateCredentialsRequest.serializer())
    override val encoder = HttpLambdaResponse.serializer(CreateCredentialsResponse.serializer())
    
    override fun process(
        request: HttpLambdaRequest<CreateCredentialsRequest>,
        context: Context,
    ): HttpLambdaResponse<CreateCredentialsResponse> {
        val id = generateSecureRandomBytes(16).encodeBase64()
        val metadata = CredentialsMetadata(id)
        val data = request.body.data
        val item = Credentials(metadata, data)
        CredentialsTable.putItem(item).join()
        return HttpLambdaResponse(
            200,
            mapOf("Content-Type" to "application/json"),
            false,
            CreateCredentialsResponse.Success(item)
        )
    }
    
    override fun onFailure(exception: Throwable, context: Context): HttpLambdaResponse<CreateCredentialsResponse> {
        return HttpLambdaResponse(
            400,
            mapOf("Content-Type" to "application/json"),
            false,
            CreateCredentialsResponse.Failure("")
        )
    }
}
