plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.20"

    id("com.gradleup.shadow")
    application
}

group = "dev.samicpp"
version = "1.0"

dependencies {
    implementation(project(":http"))
    implementation(kotlin("stdlib"))

    implementation("org.graalvm.sdk:graal-sdk:24.0.2") 
    implementation("org.graalvm.polyglot:polyglot:24.2.2")
    implementation("org.graalvm.js:js:24.2.2")
    implementation("org.graalvm.python:python:24.2.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

application {
    mainClass.set("dev.samicpp.web.MainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("web-java")
    archiveVersion.set("1.0.0")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("jar-builds"))
    manifest {
        attributes["Main-Class"] = "dev.samicpp.web.MainKt"
    }
}
