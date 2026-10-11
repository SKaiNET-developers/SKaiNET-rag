// The Kotlin plugins are declared once here so every subproject shares one plugin classloader;
// applying them only in subprojects loads KGP per-project and its build services break with
// classloader cast errors (the SwiftPM aggregation service among them).
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
}
