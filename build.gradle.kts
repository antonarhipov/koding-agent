plugins {
    kotlin("jvm") version "2.2.0-RC"
    kotlin("plugin.serialization") version "2.2.0-RC"
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("ai.koog:koog-agents:0.1.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.example.kagent.MainKt")
}

kotlin {
    jvmToolchain(21)
}

