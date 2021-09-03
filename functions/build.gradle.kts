plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.johnturkson.sync"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}

dependencies {
    implementation(project(":common"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("com.johnturkson.aws:aws-lambda-handler:0.0.6")
    implementation("com.johnturkson.security:security-tools:0.0.4")
    implementation("com.johnturkson.text:text-tools:0.0.2")
    implementation("org.springframework.security:spring-security-crypto:5.5.2")
    implementation(platform("software.amazon.awssdk:bom:2.17.31"))
    implementation("software.amazon.awssdk:aws-crt-client:2.17.31-PREVIEW")
    implementation("software.amazon.awssdk:dynamodb-enhanced") {
        exclude(group = "software.amazon.awssdk", module = "apache-client")
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }
}

tasks.named<DefaultTask>("build") {
    dependsOn("buildLambdaLayer")
    dependsOn("buildLambdaFunctions")
}

tasks.register<Zip>("buildLambdaLayer") {
    archiveFileName.set("SyncKotlinLambdaLayer.zip")
    destinationDirectory.set(file("$buildDir/lambda/layers"))
    into("java/lib") {
        from(configurations.runtimeClasspath)
    }
}

tasks.register<Zip>("buildLambdaFunctions") {
    archiveFileName.set("SyncKotlinLambdaFunctions.zip")
    destinationDirectory.set(file("$buildDir/lambda/functions"))
    from(tasks.named("compileKotlin"))
    from(tasks.named("processResources"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}
