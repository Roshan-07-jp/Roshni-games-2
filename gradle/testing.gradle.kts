/**
 * Testing configuration for all modules
 */

plugins {
    id("kotlin-android")
}

// Test dependencies for all modules
dependencies {
    // JUnit 5
    testImplementation(libs.junit)

    // Android Testing
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)

    // MockK for mocking
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk)

    // Turbine for testing coroutines
    testImplementation(libs.turbine)
    androidTestImplementation(libs.turbine)

    // Robolectric for unit testing Android components
    testImplementation(libs.robolectric)

    // Compose Testing
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.manifest)

    // Debug Implementation for testing
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Test configuration
tasks.withType<Test> {
    useJUnitPlatform()

    // Enable test logging
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    // Set test JVM arguments
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED"
    )
}

// Android test configuration
tasks.withType<com.android.build.gradle.internal.tasks.AndroidTestTask> {
    useJUnitPlatform()
}

// Robolectric configuration
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Coverage configuration (if using JaCoCo or Kover)
plugins.withId("org.jetbrains.kotlinx.kover") {
    extensions.configure<org.jetbrains.kotlinx.kover.api.KoverTaskExtension> {
        excludes = listOf(
            "*.BuildConfig",
            "*_Factory*",
            "*_Injector*",
            "*.databinding.*",
            "*.dagger.*",
            "*.Application",
            "*.Activity",
            "*.Fragment",
            "*.View",
            "*.Adapter",
            "*.Holder",
            "*.Test",
            "*.test.*",
            "*Test"
        )
    }
}