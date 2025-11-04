// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Aplicando plugins usando os aliases do libs.versions.toml
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.protobuf) apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}