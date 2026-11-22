@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}
/**
 * The browser demo: the same retrieval stack the CLI and the apps use, compiled to Wasm and
 * served as a static page. `wasmJsBrowserDistribution` produces a self-contained folder
 * (build/dist/wasmJs/productionExecutable) that any static host — GitHub Pages included —
 * can serve as-is. After the one bundle fetch, nothing leaves the tab.
 */
kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("app")
        browser {
            commonWebpackConfig {
                outputFileName = "app.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":rag-runtime"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.browser)
        }
    }
}

// The demo serves the exact bytes the Compose sample ships: one checked-in bundle, every
// target — Android, desktop, iOS, and this page — loading the identical corpus.
tasks.named<Copy>("wasmJsProcessResources") {
    from(rootProject.file("samples/app/src/commonMain/composeResources/files/docs.lrb"))
}
