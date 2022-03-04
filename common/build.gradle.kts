plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

group = "com.johnturkson.sync.common"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":generators"))
    ksp(project(":generators"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation(platform("software.amazon.awssdk:bom:2.16.104"))
    implementation("software.amazon.awssdk:dynamodb-enhanced") {
        exclude(group = "software.amazon.awssdk", module = "apache-client")
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }
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
