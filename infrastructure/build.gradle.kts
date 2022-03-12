plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    application
}

group = "com.johnturkson.sync.infrastructure"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}

dependencies {
    implementation(project(":generators"))
    ksp(project(":generators"))
    implementation(project(":handlers"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("software.amazon.awscdk:aws-cdk-lib:2.15.0")
}

application {
    mainClass.set("com.johnturkson.sync.infrastructure.SyncAppKt")
}
