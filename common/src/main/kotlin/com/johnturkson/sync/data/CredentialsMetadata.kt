package com.johnturkson.sync.data

import com.johnturkson.sync.generator.annotations.Resource
import kotlinx.serialization.Serializable
import kotlin.properties.Delegates

@Serializable
@Resource
data class CredentialsMetadata(
    val id: String,
) {
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
