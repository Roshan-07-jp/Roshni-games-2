package com.roshni.games.core.ui.ux.model

import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a user interaction within the gaming platform
 */
data class UserInteraction(
    val id: String,
    val type: InteractionType,
    val timestamp: Long,
    val duration: Long? = null,
    val context: InteractionContext,
    val metadata: Map<String, Any> = emptyMap(),
    val result: InteractionResult? = null
) {

    /**
     * Types of user interactions
     */
    enum class InteractionType {
        TAP, SWIPE, LONG_PRESS, SCROLL, PINCH, VOICE_COMMAND,
        GESTURE, BUTTON_CLICK, NAVIGATION, GAME_ACTION,
        MENU_INTERACTION, DIALOG_INTERACTION, SEARCH,
        SHARE, BOOKMARK, RATING, PURCHASE
    }

    /**
     * Context information for the interaction
     */
    data class InteractionContext(
        val screenName: String,
        val componentId: String? = null,
        val userId: String? = null,
        val sessionId: String? = null,
        val gameId: String? = null,
        val position: InteractionPosition? = null,
        val deviceInfo: DeviceInfo = DeviceInfo(),
        val accessibilityInfo: AccessibilityInfo? = null
    )

    /**
     * Position information for the interaction
     */
    data class InteractionPosition(
        val x: Float,
        val y: Float,
        val width: Float? = null,
        val height: Float? = null,
        val screenWidth: Int,
        val screenHeight: Int
    )

    /**
     * Device information for the interaction
     */
    data class DeviceInfo(
        val screenWidth: Int = 0,
        val screenHeight: Int = 0,
        val density: Float = 0f,
        val orientation: ScreenOrientation = ScreenOrientation.PORTRAIT,
        val deviceType: DeviceType = DeviceType.PHONE,
        val osVersion: String = "",
        val appVersion: String = ""
    )

    /**
     * Accessibility information for the interaction
     */
    data class AccessibilityInfo(
        val isAccessibilityEnabled: Boolean = false,
        val talkBackEnabled: Boolean = false,
        val largeTextEnabled: Boolean = false,
        val highContrastEnabled: Boolean = false,
        val fontScale: Float = 1.0f,
        val screenReaderActive: Boolean = false
    )

    /**
     * Result of the interaction
     */
    data class InteractionResult(
        val success: Boolean,
        val feedback: List<String> = emptyList(),
        val errors: List<String> = emptyList(),
        val data: Map<String, Any> = emptyMap()
    )

    /**
     * Screen orientation
     */
    enum class ScreenOrientation {
        PORTRAIT, LANDSCAPE, UNKNOWN
    }

    /**
     * Device type
     */
    enum class DeviceType {
        PHONE, TABLET, DESKTOP, TV, WEARABLE, UNKNOWN
    }
}

/**
 * Context for UX enhancement operations
 */
data class UXContext(
    val userId: String? = null,
    val sessionId: String? = null,
    val gameId: String? = null,
    val screenName: String,
    val componentId: String? = null,
    val userPreferences: UserPreferences = UserPreferences(),
    val deviceCapabilities: DeviceCapabilities = DeviceCapabilities(),
    val currentGameState: GameState? = null,
    val interactionHistory: List<UserInteraction> = emptyList(),
    val environmentalFactors: EnvironmentalFactors = EnvironmentalFactors(),
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * User preferences for UX customization
     */
    data class UserPreferences(
        val theme: ThemePreference = ThemePreference.AUTO,
        val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
        val soundEnabled: Boolean = true,
        val hapticFeedbackEnabled: Boolean = true,
        val reducedMotion: Boolean = false,
        val highContrast: Boolean = false,
        val largeText: Boolean = false,
        val language: String = "en",
        val region: String = "US",
        val accessibilityProfile: AccessibilityProfile = AccessibilityProfile.STANDARD
    )

    /**
     * Device capabilities for UX adaptation
     */
    data class DeviceCapabilities(
        val hasVibrator: Boolean = false,
        val hasSpeaker: Boolean = true,
        val hasCamera: Boolean = false,
        val hasMicrophone: Boolean = false,
        val hasGyroscope: Boolean = false,
        val hasAccelerometer: Boolean = false,
        val hasProximitySensor: Boolean = false,
        val hasLightSensor: Boolean = false,
        val screenRefreshRate: Int = 60,
        val maxTextureSize: Int = 2048,
        val supportsHDR: Boolean = false,
        val supportsWideColorGamut: Boolean = false
    )

    /**
     * Current game state information
     */
    data class GameState(
        val gameId: String,
        val level: Int? = null,
        val score: Long? = null,
        val lives: Int? = null,
        val powerUps: List<String> = emptyList(),
        val achievements: List<String> = emptyList(),
        val difficulty: Difficulty = Difficulty.NORMAL,
        val gameMode: String? = null,
        val timeRemaining: Long? = null
    )

    /**
     * Environmental factors affecting UX
     */
    data class EnvironmentalFactors(
        val timeOfDay: TimeOfDay = TimeOfDay.UNKNOWN,
        val lightingCondition: LightingCondition = LightingCondition.NORMAL,
        val noiseLevel: NoiseLevel = NoiseLevel.NORMAL,
        val batteryLevel: Int = 100,
        val networkQuality: NetworkQuality = NetworkQuality.GOOD,
        val isInMotion: Boolean = false,
        val locationContext: LocationContext = LocationContext.UNKNOWN
    )

    /**
     * Theme preference
     */
    enum class ThemePreference {
        LIGHT, DARK, AUTO, HIGH_CONTRAST
    }

    /**
     * Animation speed preference
     */
    enum class AnimationSpeed {
        SLOW, NORMAL, FAST, DISABLED
    }

    /**
     * Accessibility profile
     */
    enum class AccessibilityProfile {
        STANDARD, VISUAL_IMPAIRMENT, MOTOR_IMPAIRMENT, COGNITIVE_IMPAIRMENT, HEARING_IMPAIRMENT
    }

    /**
     * Game difficulty
     */
    enum class Difficulty {
        EASY, NORMAL, HARD, EXPERT, CUSTOM
    }

    /**
     * Time of day
     */
    enum class TimeOfDay {
        MORNING, AFTERNOON, EVENING, NIGHT, UNKNOWN
    }

    /**
     * Lighting condition
     */
    enum class LightingCondition {
        BRIGHT, NORMAL, DIM, DARK
    }

    /**
     * Noise level
     */
    enum class NoiseLevel {
        QUIET, NORMAL, LOUD, VERY_LOUD
    }

    /**
     * Network quality
     */
    enum class NetworkQuality {
        EXCELLENT, GOOD, FAIR, POOR, OFFLINE
    }

    /**
     * Location context
     */
    enum class LocationContext {
        HOME, WORK, SCHOOL, PUBLIC_TRANSPORT, OUTDOORS, INDOORS, UNKNOWN
    }
}

/**
 * Enhanced interaction that includes UX improvements
 */
data class EnhancedInteraction(
    val originalInteraction: UserInteraction,
    val enhancements: List<UXEnhancement>,
    val appliedRules: List<UXEnhancementRule>,
    val personalizationScore: Double,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * Check if this enhanced interaction was successful
     */
    fun isSuccessful(): Boolean {
        return enhancements.isNotEmpty() && confidence > 0.5
    }

    /**
     * Get the primary enhancement type
     */
    fun getPrimaryEnhancementType(): UXEnhancement.Type? {
        return enhancements.firstOrNull()?.type
    }

    /**
     * Check if interaction has specific enhancement type
     */
    fun hasEnhancementType(type: UXEnhancement.Type): Boolean {
        return enhancements.any { it.type == type }
    }
}