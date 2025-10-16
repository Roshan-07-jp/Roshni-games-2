plugins {
    id("com.roshni.games.android.library")
}

android {
    namespace = "com.roshni.games.offline"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:utils"))
    implementation(project(":game-engine"))

    // Coroutines for async operations
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Dependency injection
    implementation(libs.hilt.android)

    // JSON serialization for AI data
    implementation(libs.kotlinx.serialization.json)

    // AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Room for offline data persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Bluetooth/WiFi for local multiplayer
    implementation(libs.androidx.core.core)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}