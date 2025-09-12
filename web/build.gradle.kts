plugins {
    kotlin("jvm")
    application
}

group = "dev.samicpp"
version = "1.0"

dependencies {
    implementation(project(":http"))
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("dev.samicpp.web.MainKt")
}
