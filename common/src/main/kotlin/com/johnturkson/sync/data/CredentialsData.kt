package com.johnturkson.sync.data

import com.johnturkson.sync.generator.annotations.Resource
import kotlinx.serialization.Serializable
import kotlin.properties.Delegates

@Serializable
@Resource
data class CredentialsData(
    val service: String,
    val login: String,
    val password: String,
) {
    class Builder {
        private var service by Delegates.notNull<String>()
        private var login by Delegates.notNull<String>()
        private var password by Delegates.notNull<String>()
        
        fun service(service: String): Builder {
            this.service = service
            return this
        }
        
        fun login(login: String): Builder {
            this.login = login
            return this
        }
        
        fun password(password: String): Builder {
            this.password = password
            return this
        }
        
        fun build(): CredentialsData {
            return CredentialsData(service, login, password)
        }
    }
}
