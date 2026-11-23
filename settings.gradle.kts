pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
    // Resolved once here for the whole build (the SKaiNET org convention): declaring a version
    // at individual application sites fails with "plugin is already on the classpath with an
    // unknown version" once more than one plugin ID from the same jar is used.
    plugins {
        id("sk.ainet.multiplatform") version "1.1.0"
        id("sk.ainet.npm-pins") version "1.1.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Cartridges are consumed from the local Maven repository (publishToMavenLocal in the
        // blueprints repo), never as nested source clones. The filter keeps every other
        // dependency on Maven Central.
        mavenLocal {
            content {
                includeGroupByRegex("sk\\.ainet\\.cartridge.*")
            }
        }
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // The wasmJs toolchain downloads (Node.js, Yarn, Binaryen) declared here, where every
        // other repository lives; the Kotlin plugin's own per-project registration is switched
        // off in the root build script, keeping FAIL_ON_PROJECT_REPOS in force.
        ivy("https://nodejs.org/dist") {
            name = "nodeDistributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "yarnDistributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            name = "binaryenDistributions"
            patternLayout { artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

rootProject.name = "skainet-localrag"

include(":rag-core")
include(":rag-embedder-skainet")
include(":rag-embedder-leaf")
include(":rag-generator-smollm2")
include(":rag-runtime")
include(":rag-bundler")
include(":samples:cli")
include(":samples:corpus-java")
include(":samples:app")
include(":samples:app-android")
include(":samples:web")
include(":samples:site-search")
include(":samples:site-search:generator")
