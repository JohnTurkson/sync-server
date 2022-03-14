package com.johnturkson.sync.cdk

import software.amazon.awscdk.App

fun main() {
    val app = App()
    SyncStack(app, "SyncStack")
    app.synth()
}
