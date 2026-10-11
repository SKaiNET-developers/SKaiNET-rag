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
    }
}

rootProject.name = "skainet-localrag"

include(":rag-core")
include(":rag-embedder-skainet")
include(":rag-embedder-leaf")
include(":rag-runtime")
include(":rag-bundler")
include(":samples:cli")
include(":samples:corpus-java")
include(":samples:app")
include(":samples:app-android")
