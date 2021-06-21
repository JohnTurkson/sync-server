package com.johnturkson.sync.server.functions

import com.johnturkson.aws.dynamodb.client.DynamoDBClient
import com.johnturkson.aws.lambda.client.HttpLambdaFunction
import com.johnturkson.aws.lambda.data.HttpRequest
import com.johnturkson.aws.lambda.data.HttpResponse
import kotlinx.serialization.json.Json

// data class TestRequest()

class TestFunction : HttpLambdaFunction<String, String> {
    override val serializer: Json = TODO("Not yet implemented")
    
    override fun decodeTypedRequest(request: HttpRequest): String {
        val client = DynamoDBClient()
        TODO("Not yet implemented")
    }
    
    override fun encodeTypedResponse(response: String): HttpResponse {
        TODO("Not yet implemented")
    }
    
    override fun onInvalidAuthorization(request: HttpRequest, exception: Throwable): HttpResponse {
        TODO("Not yet implemented")
    }
    
    override fun onInvalidHttpRequest(input: String, exception: Throwable): HttpResponse {
        TODO("Not yet implemented")
    }
    
    override fun onInvalidTypedRequest(request: HttpRequest, exception: Throwable): HttpResponse {
        TODO("Not yet implemented")
    }
    
    override fun processTypedRequest(request: String): String {
        TODO("Not yet implemented")
    }
    
    override fun verifyAuthorization(request: HttpRequest) {
        TODO("Not yet implemented")
    }
    
}
