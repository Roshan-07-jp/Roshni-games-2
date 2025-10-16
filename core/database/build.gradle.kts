plugins {
    id("com.roshni.games.android.library")
    id("dagger.hilt.android.plugin")
    id("androidx.room")
}

android {
    namespace = "com.roshni.games.core.database"
}

dependencies {
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // SQLCipher for encryption
    implementation(libs.sqlcipher)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // DateTime for LocalDateTime support
    implementation(libs.kotlinx.datetime)

    implementation(libs.core.utils)
}