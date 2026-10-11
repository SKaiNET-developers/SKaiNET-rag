import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("sk.ainet.multiplatform")
    `maven-publish`
}

group = "sk.ainet.rag"
version = "0.1.0"

skainet {
    // SKaiNET 0.57.0 publishes Java 21 bytecode, so this module cannot offer a lower JVM variant.
    jvmTarget = JvmTarget.JVM_21
    explicitApi = false
}

kotlin {
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.skainet.bom))
            implementation(libs.skainet.lang.core)
            implementation(libs.skainet.backend.cpu)
        }
    }
}
