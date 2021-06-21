plugins {
    kotlin("jvm") version "1.5.10"
    kotlin("plugin.serialization") version "1.5.10"
}

group = "com.johnturkson.sync"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://packages.johnturkson.com/maven")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
    implementation("com.johnturkson.aws:aws-dynamodb-client:0.0.3")
    implementation("com.johnturkson.aws:aws-lambda-client:0.0.3")
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
