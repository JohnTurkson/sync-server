plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("com.google.devtools.ksp") version "1.6.10-1.0.4"
}

group = "com.johnturkson.sync"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}
