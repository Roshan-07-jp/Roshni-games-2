plugins {
    id("com.roshni.games.android.library")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.roshni.games.core.network"
}

dependencies {
    // Retrofit and HTTP
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // DateTime for API models
    implementation(libs.kotlinx.datetime)

    // JSON parsing
    implementation(libs.gson)

    // Timber for logging
    implementation(libs.timber)

    implementation(libs.core.utils)
}