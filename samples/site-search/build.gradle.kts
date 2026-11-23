@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}
/**
 * The static-site search widget: the same retrieval stack as `samples/web`, but shaped the way a
 * documentation site would embed it — one `<div>` plus one `<script>` tag, and every result is a
 * link to a page and heading anchor of the site itself. The sibling `generator` module renders
 * the demo corpus into those pages, with anchors from the same slug rule the chunker used, so
 * links are correct by construction.
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

// Served next to the page: the bundle is the site's search index, fetched once on first use.
tasks.named<Copy>("wasmJsProcessResources") {
    from(rootProject.file("samples/app/src/commonMain/composeResources/files/docs.lrb"))
}
