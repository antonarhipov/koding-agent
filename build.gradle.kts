plugins {
    kotlin("jvm") version "2.2.0-RC2"
    kotlin("plugin.serialization") version "2.2.0-RC2"
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("ai.koog:koog-agents:0.2.1")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.example.kagent.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.example.kagent.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlin {
    jvmToolchain(21)
}

