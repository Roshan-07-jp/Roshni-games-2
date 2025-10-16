plugins {
    id("com.roshni.games.android.library")
    id("com.roshni.games.android.library.compose")
}

android {
    namespace = "com.roshni.games.feature.achievements"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
