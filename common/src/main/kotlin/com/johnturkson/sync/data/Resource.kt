package com.johnturkson.sync.data

sealed interface Resource<T> {
    val metadata: Metadata<T>
    val data: Data<T>
}
