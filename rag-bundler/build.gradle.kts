plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    `maven-publish`
}

group = "sk.ainet.rag"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("sk.ainet.rag.bundler.MainKt")
    // SKaiNET picks SIMD kernels where the JVM offers them.
    applicationDefaultJvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}

dependencies {
    implementation(project(":rag-core"))
    implementation(project(":rag-embedder-skainet"))
    // Exposes the embedding-leaf-skainet cartridge (from ~/.m2) for --embedder leaf.
    implementation(project(":rag-embedder-leaf"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    testImplementation(kotlin("test"))
}
