package com.johnturkson.sync.data

import com.johnturkson.sync.generator.annotations.Resource
import kotlinx.serialization.Serializable
import kotlin.properties.Delegates

@Serializable
@Resource
data class Credentials(
     val metadata: CredentialsMetadata,
     val data: CredentialsData,
) {
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
