plugins {
    kotlin("jvm") version "2.1.10"
    id("com.gradleup.shadow") version "9.0.0"
}

group = "dev.rkdgus"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
    compileOnly(files("libs/AutoListen.jar"))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
