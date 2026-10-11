import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("sk.ainet.multiplatform")
    `maven-publish`
}

group = "sk.ainet.rag"
version = "0.1.0"

skainet {
    jvmTarget = JvmTarget.JVM_21
    explicitApi = false
}

kotlin {
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            api(project(":rag-core"))
            implementation(project(":rag-embedder-skainet"))
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
