plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "sk.ainet.rag"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("sk.ainet.rag.corpus.java.MainKt")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}
