plugins {
    kotlin("jvm")
}

group = "com.johnturkson.sync"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.30-1.0.0-beta08")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}
