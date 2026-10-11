plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)

    jvm {
        mainRun {
            mainClass.set("sk.ainet.rag.sample.cli.MainJvmKt")
        }
    }
    macosArm64 {
        binaries {
            executable {
                entryPoint = "sk.ainet.rag.sample.cli.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":rag-runtime"))
            implementation(project(":rag-embedder-leaf"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
