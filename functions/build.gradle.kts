plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.graalvm.buildtools.native")
}

group = "com.johnturkson.sync.functions"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}

dependencies {
    implementation(project(":generators"))
    ksp(project(":generators"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("com.johnturkson.security:security-tools:0.0.5")
    implementation("com.johnturkson.text:text-tools:0.0.2")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.springframework.security:spring-security-crypto:5.6.2")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.0")
    implementation("com.amazonaws:aws-lambda-java-runtime-interface-client:2.1.0")
    implementation("software.amazon.awscdk:aws-cdk-lib:2.17.0")
    implementation("software.amazon.awscdk:apigatewayv2-alpha:2.17.0-alpha.0")
    implementation("software.amazon.awscdk:apigatewayv2-integrations-alpha:2.17.0-alpha.0")
    implementation(platform("software.amazon.awssdk:bom:2.16.104"))
    implementation("software.amazon.awssdk:netty-nio-client")
    implementation("software.amazon.awssdk:dynamodb-enhanced") {
        exclude("software.amazon.awssdk", "netty-nio-client")
        exclude("software.amazon.awssdk", "apache-client")
    }
}

ksp {
    arg("location", "$group.generated")
    arg("hostedZone", "johnturkson.com")
    arg("routeSelectionExpression", "\$request.body.type")
    arg("HANDLER_LOCATION", "../functions/build/lambda/image/functions.zip")
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("build/generated/ksp/main/kotlin")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.GRAAL_VM)
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
