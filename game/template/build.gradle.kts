plugins {
    id("com.roshni.games.android.dynamic.feature")
}

android {
    namespace = "com.roshni.games.game.GAME_ID" // Replace GAME_ID with actual game identifier

    defaultConfig {
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:design-system"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))
    implementation(project(":service:game-loader"))

    // Game-specific dependencies
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Game engine dependencies (add as needed)
    // implementation(libs.libgdx)
    // implementation(libs.unity.ads)
    // implementation(libs.cocos2d)

    // Game-specific assets and resources
    // implementation(files("libs/game-engine.jar"))
}