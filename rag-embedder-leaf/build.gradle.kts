import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The LEAF embedder binding: the mdbr-leaf-ir cartridge (consumed from ~/.m2, never as a nested
// source clone) behind rag-core's Embedder seam. Not on the sk.ainet.multiplatform convention
// plugin because the cartridge publishes jvm + macosArm64 only — upstream has no iOS klibs for
// skainet-transformers-inference-bert yet, and this module's targets must track the cartridge's.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "sk.ainet.rag"
version = "0.1.0"

kotlin {
    jvmToolchain(21)

    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":rag-core"))
            // api: rag-bundler reaches LeafEmbeddingCartridge through this module for
            // build-time corpus embedding — one dependency edge to the cartridge, not two.
            api(libs.cartridge.embedding.leaf)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest>().configureEach {
    // SKaiNET picks SIMD kernels where the JVM offers them.
    jvmArgs("--add-modules", "jdk.incubator.vector")
}
