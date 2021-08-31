package com.johnturkson.sync.data

import kotlinx.serialization.Serializable
import kotlin.properties.Delegates

@Serializable
// @DynamoDbImmutable(builder = Credentials.Builder::class)
data class Credentials(
    // @get:DynamoDbFlatten
    override val metadata: CredentialsMetadata,
    // @get:DynamoDbFlatten
    override val data: CredentialsData,
) : Resource<Credentials> {
    class Builder {
        private var metadata by Delegates.notNull<CredentialsMetadata>()
        private var data by Delegates.notNull<CredentialsData>()
        
        fun metadata(metadata: CredentialsMetadata): Builder {
            this.metadata = metadata
            return this
        }
        
        fun data(data: CredentialsData): Builder {
            this.data = data
            return this
        }
        
        fun build(): Credentials {
            return Credentials(metadata, data)
        }
    }
}
