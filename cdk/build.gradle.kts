plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    application
}

group = "com.johnturkson.sync.cdk"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}

dependencies {
    implementation(project(":generators"))
    ksp(project(":generators"))
    implementation(project(":common"))
    implementation(project(":functions"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("software.amazon.awscdk:aws-cdk-lib:2.16.0")
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
