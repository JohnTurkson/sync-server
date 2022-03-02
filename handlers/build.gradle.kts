plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.graalvm.buildtools.native")
}

group = "com.johnturkson.sync.handlers"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.springframework.security:spring-security-crypto:5.5.2")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.0")
    implementation("com.amazonaws:aws-lambda-java-runtime-interface-client:2.1.0")
    implementation(platform("software.amazon.awssdk:bom:2.16.104"))
    implementation("software.amazon.awssdk:netty-nio-client")
    implementation("software.amazon.awssdk:dynamodb-enhanced") {
        exclude("software.amazon.awssdk", "netty-nio-client")
        exclude("software.amazon.awssdk", "apache-client")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

nativeBuild {
    mainClass.set("com.amazonaws.services.lambda.runtime.api.client.AWSLambda")
    buildArgs.add("--verbose")
    buildArgs.add("--no-fallback")
    buildArgs.add("--enable-url-protocols=http")
    buildArgs.add("--initialize-at-build-time=org.slf4j")
}

tasks.register<Zip>("buildLambdaImage") {
    dependsOn("nativeBuild")
    dependsOn("buildLambdaBootstrap")
    archiveFileName.set("${project.name}.zip")
    destinationDirectory.set(file("$buildDir/lambda/image"))
    from("$buildDir/native/nativeBuild")
    from("$buildDir/lambda/bootstrap")
}

tasks.register("buildLambdaBootstrap") {
    mkdir("$buildDir/lambda/bootstrap")
    File("$buildDir/lambda/bootstrap", "bootstrap").bufferedWriter().use { writer ->
        writer.write(
            """
            #!/usr/bin/env bash
            
            ./${project.name} ${"$"}_HANDLER
            """.trimIndent()
        )
    }
}
