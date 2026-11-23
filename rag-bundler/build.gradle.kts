plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    `maven-publish`
}

group = "sk.ainet.rag"
version = "0.1.0"

// The leaf cartridge exists only in the local Maven repository (published from the blueprints
// repo), so an environment without it — CI indexing with the weight-free HRP embedder — builds
// with -Plocalrag.noLeaf=true: the dependency is dropped and `--embedder leaf` becomes a clear
// runtime refusal (src/main-noleaf) instead of a compile failure. The default build is unchanged.
val noLeaf = providers.gradleProperty("localrag.noLeaf").orNull == "true"

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir(if (noLeaf) "src/main-noleaf/kotlin" else "src/main-leaf/kotlin")
    }
}

application {
    mainClass.set("sk.ainet.rag.bundler.MainKt")
    // SKaiNET picks SIMD kernels where the JVM offers them.
    applicationDefaultJvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}

dependencies {
    implementation(project(":rag-core"))
    implementation(project(":rag-embedder-skainet"))
    if (!noLeaf) {
        // Exposes the embedding-leaf-skainet cartridge (from ~/.m2) for --embedder leaf.
        implementation(project(":rag-embedder-leaf"))
    }
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}
