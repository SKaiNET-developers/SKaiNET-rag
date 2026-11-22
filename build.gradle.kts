@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

// The Kotlin plugins are declared once here so every subproject shares one plugin classloader;
// applying them only in subprojects loads KGP per-project and its build services break with
// classloader cast errors (the SwiftPM aggregation service among them).
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    // Keeps the npm dependencies the wasmJs toolchain pulls in pinned through yarn.lock;
    // required by sk.ainet.multiplatform as soon as any module builds a js/wasmJs target.
    id("sk.ainet.npm-pins")
}

// The settings file declares FAIL_ON_PROJECT_REPOS, so the Node.js/Yarn/Binaryen distribution
// repositories live there with every other repository. A null download base URL stops the
// Kotlin plugin registering its own copies, which that mode would reject. The toolchain
// plugins attach to the root project (compile) and to each wasm subproject (tests), so the
// override reacts to them everywhere.
allprojects {
    fun nullDownloadUrl(spec: org.jetbrains.kotlin.gradle.targets.js.EnvSpec<*>) =
        spec.downloadBaseUrl.set(null as String?)

    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin> {
        nullDownloadUrl(the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>())
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
        nullDownloadUrl(the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>())
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
        nullDownloadUrl(the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec>())
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
        nullDownloadUrl(the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>())
    }
}
