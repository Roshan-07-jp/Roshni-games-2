package com.roshni.games.core.utils.feature.di

import com.roshni.games.core.utils.feature.FeatureManager
import com.roshni.games.core.utils.feature.FeatureManagerImpl
import com.roshni.games.core.utils.feature.features.AccessibilityFeature
import com.roshni.games.core.utils.feature.features.GameLibraryFeature
import com.roshni.games.core.utils.feature.features.ParentalControlsFeature
import com.roshni.games.core.utils.rules.RuleEngine
import com.roshni.games.core.utils.workflow.WorkflowEngine
import org.koin.dsl.module

/**
 * Dependency injection module for feature management components
 */
val featureModule = module {
    // Provide singleton instance of FeatureManager
    single<FeatureManager> {
        FeatureManagerImpl(
            ruleEngine = get(),
            workflowEngine = get()
        )
    }

    // Provide core feature instances
    single<GameLibraryFeature> {
        GameLibraryFeature()
    }

    single<ParentalControlsFeature> {
        ParentalControlsFeature()
    }

    single<AccessibilityFeature> {
        AccessibilityFeature()
    }

    // Provide feature factories for dynamic feature creation
    factory<com.roshni.games.core.utils.feature.Feature> { (featureId: String) ->
        when (featureId) {
            "game_library" -> get<GameLibraryFeature>()
            "parental_controls" -> get<ParentalControlsFeature>()
            "accessibility" -> get<AccessibilityFeature>()
            else -> throw IllegalArgumentException("Unknown feature: $featureId")
        }
    }

    // Provide feature qualifiers for specific feature access
    single(com.roshni.games.core.utils.feature.di.FeatureQualifiers.GAME_LIBRARY_FEATURE) {
        get<GameLibraryFeature>()
    }

    single(com.roshni.games.core.utils.feature.di.FeatureQualifiers.PARENTAL_CONTROLS_FEATURE) {
        get<ParentalControlsFeature>()
    }

    single(com.roshni.games.core.utils.feature.di.FeatureQualifiers.ACCESSIBILITY_FEATURE) {
        get<AccessibilityFeature>()
    }
}

/**
 * Feature-related qualifiers for dependency injection
 */
object FeatureQualifiers {
    const val GAME_LIBRARY_FEATURE = "game_library_feature"
    const val PARENTAL_CONTROLS_FEATURE = "parental_controls_feature"
    const val ACCESSIBILITY_FEATURE = "accessibility_feature"
}

/**
 * Feature management extension functions for easy access
 */
fun com.roshni.games.core.utils.feature.FeatureManager.getGameLibraryFeature(): GameLibraryFeature {
    return getFeature("game_library") as? GameLibraryFeature
        ?: throw IllegalStateException("GameLibraryFeature not registered")
}

fun com.roshni.games.core.utils.feature.FeatureManager.getParentalControlsFeature(): ParentalControlsFeature {
    return getFeature("parental_controls") as? ParentalControlsFeature
        ?: throw IllegalStateException("ParentalControlsFeature not registered")
}

fun com.roshni.games.core.utils.feature.FeatureManager.getAccessibilityFeature(): AccessibilityFeature {
    return getFeature("accessibility") as? AccessibilityFeature
        ?: throw IllegalStateException("AccessibilityFeature not registered")
}