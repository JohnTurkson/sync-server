plugins {
    kotlin("jvm")
    application
}

group = "com.johnturkson.sync.cdk"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":functions"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("software.amazon.awscdk:aws-cdk-lib:2.17.0")
    implementation("software.amazon.awscdk:apigatewayv2-alpha:2.17.0-alpha.0")
    implementation("software.amazon.awscdk:apigatewayv2-integrations-alpha:2.17.0-alpha.0")
}

application {
    mainClass.set("com.johnturkson.sync.cdk.SyncAppKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}
