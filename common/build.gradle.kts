plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

group = "com.johnturkson.sync.common"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}

dependencies {
    implementation("com.johnturkson.cdk:cdk-generator:0.0.1")
    ksp("com.johnturkson.cdk:cdk-generator:0.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("software.amazon.awscdk:aws-cdk-lib:2.17.0")
    implementation("software.amazon.awscdk:apigatewayv2-alpha:2.17.0-alpha.0")
    implementation("software.amazon.awscdk:apigatewayv2-integrations-alpha:2.17.0-alpha.0")
    implementation(platform("software.amazon.awssdk:bom:2.16.104"))
    implementation("software.amazon.awssdk:dynamodb-enhanced") {
        exclude("software.amazon.awssdk", "apache-client")
        exclude("software.amazon.awssdk", "netty-nio-client")
    }
}

ksp {
    arg("location", "$group.generated")
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
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}
