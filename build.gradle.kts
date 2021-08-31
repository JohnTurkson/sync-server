plugins {
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
    id("com.google.devtools.ksp") version "1.5.30-1.0.0-beta08"
}

group = "com.johnturkson.sync"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}
