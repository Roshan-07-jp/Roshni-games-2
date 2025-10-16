plugins {
    id("com.roshni.games.android.library")
}

android {
    namespace = "com.roshni.games.service.backgroundsync"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.work.runtime)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.timber)
}