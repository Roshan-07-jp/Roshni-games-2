pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Roshni Games"

// Core Modules
include(":app")
include(":core:ui")
include(":core:navigation")
include(":core:database")
include(":core:network")
include(":core:utils")
include(":core:design-system")

// Feature Modules
include(":feature:splash")
include(":feature:home")
include(":feature:game-library")
include(":feature:game-player")
include(":feature:settings")
include(":feature:profile")
include(":feature:search")
include(":feature:achievements")
include(":feature:leaderboard")
include(":feature:social")
include(":feature:parental-controls")
include(":feature:accessibility")

// Service Modules
include(":service:game-loader")
include(":service:background-sync")
include(":service:analytics")

// Game Catalog Module
include(":game-catalog")

// Game Engine Module
include(":game-engine")

// Game Templates Module
include(":game-templates")

// Sample Games Module
include(":sample-games")

// Multiplayer Module
include(":multiplayer")

// Progression Module
include(":progression")

// Offline Gaming Module
include(":offline")

// Metadata & Configuration Module
include(":metadata")

// Game Modules - Dynamic Feature Modules for on-demand downloads
// Template for game modules (uncomment and modify as needed)
// include(":game:puzzle-001")
// include(":game:puzzle-002")
// include(":game:word-001")
// include(":game:arcade-001")
// include(":game:strategy-001")

// Build Logic
includeBuild("build-logic")

// Project-wide properties
extra.set("compileSdk", 34)
extra.set("targetSdk", 34)
extra.set("minSdk", 24)
extra.set("versionCode", 1)
extra.set("versionName", "1.0.0")