package com.johnturkson.sync.handlers.definitions

interface LambdaHandler<T, R> {
    suspend fun processRequest(body: String?): R
    
    fun decodeRequest(body: String?): T?
    
    fun encodeResponse(response: R): String
}
