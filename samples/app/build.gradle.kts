plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    // Bare id, no version: the AGP jar is already on the build classpath via the root's
    // android-application declaration, and a second versioned id from the same jar fails with
    // "already on the classpath with an unknown version".
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "sk.ainet.rag.sample.app"
        compileSdk = 37
        minSdk = 26
        // The AGP 9 KMP-library plugin disables Android resource/asset processing by default.
        // Compose resources reach the APK as Android assets, so without this the bundle is
        // silently absent at runtime (Res.readBytes throws, the app shows its error state).
        androidResources {
            enable = true
        }
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":rag-runtime"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

compose.resources {
    packageOfResClass = "sk.ainet.rag.sample.app.resources"
}

compose.desktop {
    application {
        mainClass = "sk.ainet.rag.sample.app.MainKt"
    }
}
