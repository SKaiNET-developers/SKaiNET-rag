// The thin Android application shell: AGP 9 forbids com.android.application on a KMP module, so
// the whole app — UI, MainActivity, resources — lives in :samples:app (a KMP Android library)
// and this module contributes nothing but the launcher manifest and the packaging.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "sk.ainet.rag.sample.app.shell"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        applicationId = "sk.ainet.rag.sample.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":samples:app"))
}
