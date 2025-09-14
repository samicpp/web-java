plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("com.gradleup.shadow") version "8.3.0"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

// tasks.withType<Jar> {
//     manifest {
//         attributes["Main-Class"] = "dev.samicpp.web.MainKt"
//     }
//     archiveBaseName.set("web-java")
//     archiveVersion.set("1.0.0")
//     destinationDirectory.set(layout.buildDirectory.dir("jar-builds"))
// }
subprojects {
    tasks.withType<Jar> {
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("jar-builds"))
        manifest {
            attributes["Main-Class"] = "dev.samicpp.web.MainKt"
        }
        archiveBaseName.set("web-java")
    }
    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        archiveBaseName.set("web-java")
        archiveVersion.set("1.0.0")
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("jar-builds"))
        manifest {
            attributes["Main-Class"] = "dev.samicpp.web.MainKt"
        }
    }

}
