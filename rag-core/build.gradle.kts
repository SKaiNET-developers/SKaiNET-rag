import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("sk.ainet.multiplatform")
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

group = "sk.ainet.rag"
version = "0.1.0"

skainet {
    // SKaiNET 0.57.0 ships Java 21 bytecode; the whole skeleton standardizes on it.
    jvmTarget = JvmTarget.JVM_21
    // The core began as a port; explicit API mode is a follow-up, not a day-one gate.
    explicitApi = false
}

kotlin {
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
