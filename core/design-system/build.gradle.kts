plugins {
    id("com.roshni.games.android.library")
    id("com.roshni.games.android.library.compose")
}

android {
    namespace = "com.roshni.games.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material3.window.size)

    // Google Fonts for custom typography
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")

    implementation(libs.core.utils)
}