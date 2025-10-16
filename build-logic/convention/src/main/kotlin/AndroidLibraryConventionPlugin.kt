package com.roshni.games.buildlogic.convention

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android.plugin")
                apply("org.jetbrains.kotlin.plugin.parcelize")
                apply("androidx.navigation.safeargs.kotlin")
                apply("androidx.room")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = libs.versions.compileSdk.get().toInt()

                defaultConfig {
                    minSdk = libs.versions.minSdk.get().toInt()
                    targetSdk = libs.versions.targetSdk.get().toInt()
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
                    compose = true
                }

                composeOptions {
                    kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
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