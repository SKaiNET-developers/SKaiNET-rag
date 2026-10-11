import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The SmolLM2 generator binding: the generator-smollm2-skainet cartridge (consumed from ~/.m2,
// never as a nested source clone) behind rag-core's Generator seam. Not on the
// sk.ainet.multiplatform convention plugin for the same reason as rag-embedder-leaf: this
// module's targets must track the cartridge's published klibs, nothing more.
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
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":rag-core"))
            implementation(libs.cartridge.generator.smollm2)
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
    // ~260 MB of bf16 weights widen to f32 on load; Gradle's 512 MB default heap cannot hold them.
    maxHeapSize = "4g"
}
