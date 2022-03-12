package com.johnturkson.sync.infrastructure

import software.amazon.awscdk.App
import software.amazon.awscdk.DefaultStackSynthesizer
import software.amazon.awscdk.StackProps

fun main() {
    val app = App()
    val synthesizer = DefaultStackSynthesizer.Builder.create()
        .fileAssetsBucketName("cdk.johnturkson.com")
        .build()
    val props = StackProps.builder()
        .stackName("cdk")
        .description("cdk")
        .tags(mapOf("cdk" to "cdk"))
        .synthesizer(synthesizer)
        .build()
    
    SyncStack(app, "SyncStack", props)
    
    app.synth()
}
