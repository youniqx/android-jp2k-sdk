// Top-level build file where you can add configuration options common to all subprojects/modules.

plugins {
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
