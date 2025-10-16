plugins {
    id("com.roshni.games.android.library")
}

android {
    namespace = "com.roshni.games.gamecatalog"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:utils"))

    // Serialization for game data
    implementation(libs.kotlinx.serialization.json)

    // Coroutines for async operations
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}