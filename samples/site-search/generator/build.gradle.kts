plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "sk.ainet.rag"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("sk.ainet.rag.sample.sitesearch.generator.MainKt")
}

dependencies {
    // FrontMatterParser and Slugs: the pages must carry the exact anchors the chunker indexed.
    implementation(project(":rag-core"))
}
