plugins {
    id("com.roshni.games.android.library")
    id("com.roshni.games.android.library.compose")
    id("kotlin-kapt")
    id("dagger.hilt.plugin")
}

android {
    namespace = "com.roshni.games.feature.profile"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Compose
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // DateTime
    implementation(libs.kotlinx.datetime)

    // Room (for DAOs)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Coil (existing)
    implementation(libs.coil)
}
