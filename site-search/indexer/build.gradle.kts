@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}
/**
 * The build-time half of the site-search engine: neutral page/section records in (JSON a site
 * generator's extension extracts from its rendered HTML), one `.lrb` bundle out — embeddings
 * computed in-process through the same compiled embedder the browser engine queries with, so
 * index/query parity holds by construction.
 *
 * Two targets, one codebase: `jvm` for Gradle-side and test use, `wasmJs(nodejs)` because
 * Antora builds run in Node-only Docker images (Node >= 22 for WasmGC) — `packNpm` lays the
 * linked module into the Antora extension's `dist/`.
 */
kotlin {
    jvmToolchain(21)
    jvm()
    wasmJs {
        outputModuleName.set("skainet-search-indexer")
        nodejs()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":rag-core"))
            implementation(project(":rag-embedder-skainet"))
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val packNpm by tasks.registering(Sync::class) {
    dependsOn("compileProductionExecutableKotlinWasmJsOptimize")
    from(layout.buildDirectory.dir("compileSync/wasmJs/main/productionExecutable/optimized")) {
        exclude("*.wasm.map")
    }
    into(rootProject.layout.projectDirectory.dir("site-search/antora-extension/dist/indexer"))
}
