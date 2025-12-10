plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "com.example.antigravity"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
intellij {
    version.set("2024.1")
    type.set("RD") // Target Rider
    plugins.set(listOf()) // Add dependencies if needed
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
}

tasks {
    // Set the JVM compatibility versions
    // Toolchain handles JVM target

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    runIde {
        // Pass credentials from gradle.properties to the IDE environment
        val clientId = project.findProperty("google.client.id") as? String ?: ""
        val clientSecret = project.findProperty("google.client.secret") as? String ?: ""
        environment("GOOGLE_CLIENT_ID", clientId)
        environment("GOOGLE_CLIENT_SECRET", clientSecret)
    }
}
