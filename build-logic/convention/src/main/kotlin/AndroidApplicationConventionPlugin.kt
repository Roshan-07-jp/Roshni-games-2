package com.roshni.games.buildlogic.convention

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android.plugin")
                apply("org.jetbrains.kotlin.plugin.parcelize")
                apply("androidx.navigation.safeargs.kotlin")
                apply("androidx.room")
                apply("com.google.firebase.firebase-perf")
                apply("com.google.firebase.crashlytics")
                apply("com.google.gms.google-services")
                apply("io.sentry.android.gradle")
            }

            extensions.configure<AppExtension> {
                compileSdkVersion(libs.versions.compileSdk.get().toInt())

                defaultConfig {
                    applicationId = "com.roshni.games"
                    minSdk = libs.versions.minSdk.get().toInt()
                    targetSdk = libs.versions.targetSdk.get().toInt()
                    versionCode = libs.versions.versionCode.get().toInt()
                    versionName = libs.versions.versionName.get()

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                    isCoreLibraryDesugaringEnabled = true
                }

                kotlinOptions {
                    jvmTarget = "17"
                }

                buildFeatures {
                    buildConfig = true
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

                flavorDimensions += listOf("environment", "distribution")
                productFlavors {
                    create("dev") {
                        dimension = "environment"
                        applicationIdSuffix = ".dev"
                        versionNameSuffix = "-DEV"
                    }

                    create("staging") {
                        dimension = "environment"
                        applicationIdSuffix = ".staging"
                        versionNameSuffix = "-STAGING"
                    }

                    create("prod") {
                        dimension = "environment"
                    }

                    create("playStore") {
                        dimension = "distribution"
                    }

                    create("amazon") {
                        dimension = "distribution"
                    }

                    create("huawei") {
                        dimension = "distribution"
                    }
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                testOptions {
                    unitTests {
                        isIncludeAndroidResources = true
                    }
                }
            }

            dependencies {
                add("coreLibraryDesugaring", libs.androidx.room)
                add("implementation", libs.kotlinx.coroutines.core)
                add("implementation", libs.kotlinx.coroutines.android)
                add("implementation", libs.kotlinx.serialization.json)
                add("implementation", libs.kotlinx.datetime)
                add("implementation", libs.kotlinx.collections.immutable)

                add("implementation", libs.hilt.android)
                add("ksp", libs.hilt.compiler)

                add("implementation", libs.androidx.compose.ui.test)
                add("debugImplementation", libs.androidx.compose.ui.test.manifest)

                add("implementation", platform(libs.firebase.bom))
                add("implementation", libs.firebase.analytics)
                add("implementation", libs.firebase.crashlytics)
                add("implementation", libs.firebase.performance)

                add("implementation", libs.sentry.android)
                add("implementation", libs.sentry.android.fragment)

                add("testImplementation", libs.junit)
                add("testImplementation", libs.kotlinx.coroutines.test)
                add("testImplementation", libs.androidx.test.core)
                add("testImplementation", libs.mockk)

                add("androidTestImplementation", libs.androidx.test.ext.junit)
                add("androidTestImplementation", libs.espresso.core)
                add("androidTestImplementation", libs.androidx.compose.ui.test.junit4)
            }
        }
    }
}