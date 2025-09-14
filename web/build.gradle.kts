plugins {
    kotlin("jvm")
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
}

application {
    mainClass.set("dev.samicpp.web.MainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}
