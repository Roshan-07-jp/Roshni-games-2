package com.roshni.games.core.utils.rules

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Context information for rule evaluation
 *
 * @property userId The ID of the user the rule is being evaluated for
 * @property gameId The ID of the game context (if applicable)
 * @property sessionId The current game session ID (if applicable)
 * @property timestamp The timestamp when the rule is being evaluated
 * @property deviceInfo Device-specific information
 * @property userProfile User profile data relevant for rule evaluation
 * @property gameState Current game state information
 * @property dispatcher Coroutine dispatcher for async operations (defaults to IO)
 * @property metadata Additional context-specific metadata
 */
data class RuleContext(
    val userId: String,
    val gameId: String? = null,
    val sessionId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val userProfile: UserProfile = UserProfile(),
    val gameState: GameState = GameState(),
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Device-specific information for rule evaluation
     */
    data class DeviceInfo(
        val platform: String = "android",
        val osVersion: String = android.os.Build.VERSION.RELEASE,
        val deviceModel: String = android.os.Build.MODEL,
        val screenDensity: Int = android.content.res.Resources.getSystem().displayMetrics.densityDpi,
        val isTablet: Boolean = false,
        val locale: String = java.util.Locale.getDefault().toString(),
        val timezone: String = java.util.TimeZone.getDefault().id,
        val networkType: String = "unknown",
        val batteryLevel: Float = -1f,
        val isCharging: Boolean = false
    )

    /**
     * User profile information relevant for rule evaluation
     */
    data class UserProfile(
        val age: Int? = null,
        val subscriptionTier: String? = null,
        val isPremium: Boolean = false,
        val parentalControlsEnabled: Boolean = false,
        val accessibilitySettings: AccessibilitySettings = AccessibilitySettings(),
        val preferences: Map<String, Any> = emptyMap(),
        val achievements: List<String> = emptyList(),
        val playTime: PlayTimeInfo = PlayTimeInfo()
    ) {

        /**
         * Accessibility settings for the user
         */
        data class AccessibilitySettings(
            val highContrast: Boolean = false,
            val largeText: Boolean = false,
            val screenReader: Boolean = false,
            val reducedMotion: Boolean = false,
            val colorBlindMode: String? = null
        )

        /**
         * Play time information for the user
         */
        data class PlayTimeInfo(
            val totalPlayTime: Long = 0L,
            val weeklyPlayTime: Long = 0L,
            val dailyPlayTime: Long = 0L,
            val lastPlayDate: Long? = null,
            val streakDays: Int = 0
        )
    }

    /**
     * Current game state information
     */
    data class GameState(
        val currentLevel: Int? = null,
        val score: Long = 0L,
        val lives: Int = 0,
        val powerUps: Map<String, Int> = emptyMap(),
        val achievements: List<String> = emptyList(),
        val progress: Float = 0f,
        val difficulty: String? = null,
        val gameMode: String? = null,
        val customState: Map<String, Any> = emptyMap()
    )
}