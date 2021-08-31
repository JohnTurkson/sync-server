package com.johnturkson.sync.data

import kotlinx.serialization.Serializable
import kotlin.properties.Delegates

@Serializable
// @DynamoDbImmutable(builder = CredentialsMetadata.Builder::class)
data class CredentialsMetadata(
    // @get:DynamoDbPartitionKey
    override val id: String,
) : Metadata<Credentials> {
    class Builder {
        private var id by Delegates.notNull<String>()
        
        fun id(id: String): Builder {
            this.id = id
            return this
        }
        
        fun build(): CredentialsMetadata {
            return CredentialsMetadata(id)
        }
    }
}
