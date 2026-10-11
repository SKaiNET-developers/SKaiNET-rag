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

    // SKaiNET picks SIMD kernels where the JVM offers them — generation is minutes vs. seconds.
    tasks.withType<JavaExec>().configureEach {
        jvmArgs("--add-modules", "jdk.incubator.vector")
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
            implementation(project(":rag-generator-smollm2"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
