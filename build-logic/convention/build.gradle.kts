plugins {
    `kotlin-dsl`
}

group = "com.roshni.games.buildlogic.convention"

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.firebase.gradle.plugin)
    compileOnly(libs.gms.gradle.plugin)
    compileOnly(libs.sentry.gradle.plugin)
}

gradlePluginPortal {
    repositories {
        gradlePluginPortal()
    }
}