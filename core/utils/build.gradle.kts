plugins {
    id("com.roshni.games.android.library")
}

android {
    namespace = "com.roshni.games.core.utils"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.timber)
    implementation(libs.gson)
}