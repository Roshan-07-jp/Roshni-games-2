plugins {
    id("com.roshni.games.android.application")
    id("com.roshni.games.android.application.compose")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.roshni.games"

    defaultConfig {
        applicationId = "com.roshni.games"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))
    implementation(project(":core:design-system"))

    implementation(project(":feature:splash"))
    implementation(project(":feature:home"))
    implementation(project(":feature:game-library"))
    implementation(project(":feature:game-player"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:search"))
    implementation(project(":feature:achievements"))
    implementation(project(":feature:leaderboard"))
    implementation(project(":feature:social"))
    implementation(project(":feature:parental-controls"))
    implementation(project(":feature:accessibility"))

    implementation(project(":service:game-loader"))
    implementation(project(":service:background-sync"))
    implementation(project(":service:analytics"))

    // Compose
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Network
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Image Loading
    implementation(libs.coil)

    // Utility
    implementation(libs.timber)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.performance)

    // Google Play Services
    implementation(libs.play.services.auth)
    implementation(libs.play.services.games)

    // Analytics & Monitoring
    implementation(libs.sentry.android)
    implementation(libs.sentry.android.fragment)
    implementation(libs.mixpanel.android)

    // In-App Purchase
    implementation(libs.billing)

    // Ads
    implementation(libs.admob)

    // Social
    implementation(libs.facebook.login)

    // Other
    implementation(libs.leak.canary)
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker)
    debugImplementation(libs.stetho)
}