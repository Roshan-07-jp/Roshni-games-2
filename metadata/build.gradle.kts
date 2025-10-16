plugins {
    id("com.roshni.games.android.library")
}

android {
    namespace = "com.roshni.games.metadata"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:utils"))
    implementation(project(":game-catalog"))

    // Coroutines for async operations
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Dependency injection
    implementation(libs.hilt.android)

    // JSON serialization for metadata
    implementation(libs.kotlinx.serialization.json)

    // AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Room for metadata persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}