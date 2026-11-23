@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}
/**
 * L1 of the site-search engine: the retrieval stack compiled to Kotlin/Wasm behind a three-call
 * `@JsExport` boundary (`coreInit`/`coreSearch`/`coreRelease`), JSON strings in and out so the
 * ABI never depends on Kotlin's JS mapping of data classes. Nobody scripts against this module
 * directly — `glue/skainet-search.js` (L2, plain handwritten ES module) is the public API, and
 * `packDist` lays both out together as the deployable engine folder.
 */
kotlin {
    wasmJs {
        outputModuleName.set("skainet-search-core")
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":rag-runtime"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.browser)
        }
    }
}

// The engine distribution is the compiler's own ES-module output plus the glue — no bundler
// pass: the .mjs/.wasm pair the Kotlin toolchain emits is already what a <script type="module">
// or a Node `import()` consumes.
val packDist by tasks.registering(Sync::class) {
    dependsOn("compileProductionExecutableKotlinWasmJsOptimize")
    from(layout.buildDirectory.dir("compileSync/wasmJs/main/productionExecutable/optimized")) {
        exclude("*.wasm.map")
    }
    from("glue")
    into(layout.buildDirectory.dir("dist/engine"))
}

// The same engine folder, laid into the Antora extension so the npm package is self-contained.
val packEngineNpm by tasks.registering(Sync::class) {
    dependsOn(packDist)
    from(layout.buildDirectory.dir("dist/engine"))
    into(rootProject.layout.projectDirectory.dir("site-search/antora-extension/dist/engine"))
}
