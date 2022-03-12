package com.johnturkson.sync.infrastructure

import software.amazon.awscdk.App

fun main() {
    val app = App()
    SyncStack(app, "SyncStack")
    app.synth()
}
