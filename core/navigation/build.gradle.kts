plugins {
    id("com.roshni.games.android.library")
    id("com.roshni.games.android.library.compose")
}

android {
    namespace = "com.roshni.games.core.navigation"
}

dependencies {
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    implementation(libs.core.ui)
    implementation(libs.core.design.system)
}