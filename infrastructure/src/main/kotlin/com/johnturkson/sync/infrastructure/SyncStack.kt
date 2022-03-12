package com.johnturkson.sync.infrastructure

import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.constructs.Construct

class SyncStack(parent: Construct, name: String, props: StackProps? = null) : Stack(parent, name, props) {
    init {
        Function.Builder.create(this, "TestFunction2")
            .description("test")
            .code(Code.fromAsset("../handlers/build/lambda/image/handlers.zip"))
            .handler("com.johnturkson.sync.handlers.functions.CreateUserFunction")
            .timeout(Duration.seconds(5))
            .memorySize(1024)
            .runtime(Runtime.PROVIDED_AL2)
            .build()
    }
}
