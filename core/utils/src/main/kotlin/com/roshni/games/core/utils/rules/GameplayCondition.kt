package com.roshni.games.core.utils.rules

/**
 * Sealed class representing various gameplay conditions that can be evaluated
 * in the rule engine. Each condition type encapsulates specific logic for
 * determining whether a particular gameplay state or requirement is met.
 */
sealed class GameplayCondition {

    /**
     * Unique identifier for this condition
     */
    abstract val id: String

    /**
     * Human-readable description of what this condition checks
     */
    abstract val description: String

    /**
     * Priority level for condition evaluation (higher values = higher priority)
     */
    abstract val priority: Int

    /**
     * Check if this condition is met given the provided context
     *
     * @param context The rule context containing all necessary information
     * @return true if the condition is met, false otherwise
     */
    abstract suspend fun evaluate(context: RuleContext): Boolean

    /**
     * Get a confidence score for this condition evaluation (0.0 to 1.0)
     *
     * @param context The rule context containing all necessary information
     * @return confidence score between 0.0 and 1.0
     */
    abstract suspend fun getConfidence(context: RuleContext): Float

    /**
     * Condition based on player's current level
     */
    data class LevelCondition(
        override val id: String,
        val minLevel: Int? = null,
        val maxLevel: Int? = null,
        val exactLevel: Int? = null,
        override val description: String = "Player level condition",
        override val priority: Int = 1
    ) : GameplayCondition() {

        override suspend fun evaluate(context: RuleContext): Boolean {
            val currentLevel = context.gameState.currentLevel ?: return false

            return when {
                exactLevel != null -> currentLevel == exactLevel
                minLevel != null && maxLevel != null -> currentLevel in minLevel..maxLevel
                minLevel != null -> currentLevel >= minLevel
                maxLevel != null -> currentLevel <= maxLevel
                else -> true
            }
        }

        override suspend fun getConfidence(context: RuleContext): Float {
            return if (evaluate(context)) 1.0f else 0.0f
        }
    }

    /**
     * Condition based on player's score
     */
    data class ScoreCondition(
        override val id: String,
        val minScore: Long? = null,
        val maxScore: Long? = null,
        val targetScore: Long? = null,
        val percentageOfTarget: Float? = null,
        override val description: String = "Player score condition",
        override val priority: Int = 1
    ) : GameplayCondition() {

        override suspend fun evaluate(context: RuleContext): Boolean {
            val currentScore = context.gameState.score

            return when {
                targetScore != null && percentageOfTarget != null -> {
                    val requiredScore = (targetScore * percentageOfTarget / 100).toLong()
                    currentScore >= requiredScore
                }
                minScore != null && maxScore != null -> currentScore in minScore..maxScore
                minScore != null -> currentScore >= minScore
                maxScore != null -> currentScore <= maxScore
                else -> true
            }
        }

        override suspend fun getConfidence(context: RuleContext): Float {
            return if (evaluate(context)) 1.0f else 0.0f
        }
    }

    /**
     * Condition based on time-based requirements
     */
    data class TimeCondition(
        override val id: String,
        val minPlayTime: Long? = null, // in minutes
        val maxPlayTime: Long? = null,
        val dailyPlayTimeLimit: Long? = null,
        val weeklyPlayTimeLimit: Long? = null,
        val requireStreak: Boolean = false,
        val minStreakDays: Int? = null,
        override val description: String = "Time-based condition",
        override val priority: Int = 2
    ) : GameplayCondition() {

        override suspend fun evaluate(context: RuleContext): Boolean {
            val playTime = context.userProfile.playTime

            // Check daily play time limit
            if (dailyPlayTimeLimit != null && playTime.dailyPlayTime >= dailyPlayTimeLimit) {
                return false
            }

            // Check weekly play time limit
            if (weeklyPlayTimeLimit != null && playTime.weeklyPlayTime >= weeklyPlayTimeLimit) {
                return false
            }

            // Check play time range
            if (minPlayTime != null && playTime.totalPlayTime < minPlayTime) {
                return false
            }

            if (maxPlayTime != null && playTime.totalPlayTime > maxPlayTime) {
                return false
            }

            // Check streak requirements
            if (requireStreak && minStreakDays != null) {
                return playTime.streakDays >= minStreakDays
            }

            return true
        }

        override suspend fun getConfidence(context: RuleContext): Float {
            return if (evaluate(context)) 1.0f else 0.0f
        }
    }

    /**
     * Condition based on device capabilities and state
     */
    data class DeviceCondition(
        override val id: String,
        val requireCharging: Boolean? = null,
        val minBatteryLevel: Float? = null,
        val maxBatteryLevel: Float? = null,
        val allowedPlatforms: List<String>? = null,
        val minScreenDensity: Int? = null,
        val requireNetwork: Boolean? = null,
        val allowedNetworkTypes: List<String>? = null,
        override val description: String = "Device capability condition",
        override val priority: Int = 3
    ) : GameplayCondition() {

        override suspend fun evaluate(context: RuleContext): Boolean {
            val device = context.deviceInfo

            // Check charging requirement
            if (requireCharging == true && !device.isCharging) {
                return false
            }

            // Check battery level
            if (minBatteryLevel != null && device.batteryLevel < minBatteryLevel) {
                return false
            }

            if (maxBatteryLevel != null && device.batteryLevel > maxBatteryLevel) {
                return false
            }

            // Check platform
            if (allowedPlatforms != null && device.platform !in allowedPlatforms) {
                return false
            }

            // Check screen density
            if (minScreenDensity != null && device.screenDensity < minScreenDensity) {
                return false
            }

            // Check network requirements
            if (requireNetwork == true && device.networkType == "none") {
                return false
            }

            if (allowedNetworkTypes != null && device.networkType !in allowedNetworkTypes) {
                return false
            }

            return true
        }

        override suspend fun getConfidence(context: RuleContext): Float {
            return if (evaluate(context)) 1.0f else 0.0f
        }
    }

    /**
     * Condition based on user profile and preferences
     */
    data class UserCondition(
        override val id: String,
        val minAge: Int? = null,
        val maxAge: Int? = null,
        val subscriptionTiers: List<String>? = null,
        val requirePremium: Boolean? = null,
        val parentalControlsEnabled: Boolean? = null,
        val accessibilityRequirements: List<String>? = null,
        val preferences: Map<String, Any>? = null,
        override val description: String = "User profile condition",
        override val priority: Int = 2
    ) : GameplayCondition() {

        override suspend fun evaluate(context: RuleContext): Boolean {
            val profile = context.userProfile

            // Check age requirements
            if (minAge != null && (profile.age ?: 0) < minAge) {
                return false
            }

            if (maxAge != null && (profile.age ?: Int.MAX_VALUE) > maxAge) {
                return false
            }

            // Check subscription requirements
            if (subscriptionTiers != null && profile.subscriptionTier !in subscriptionTiers) {
                return false
            }

            if (requirePremium == true && !profile.isPremium) {
                return false
            }

            // Check parental controls
            if (parentalControlsEnabled != null &&
                profile.parentalControlsEnabled != parentalControlsEnabled) {
                return false
            }

            // Check accessibility requirements
            if (accessibilityRequirements != null) {
                val userAccessibility = profile.accessibilitySettings
                val hasRequiredAccessibility = accessibilityRequirements.any { requirement ->
                    when (requirement) {
                        "high_contrast" -> userAccessibility.highContrast
                        "large_text" -> userAccessibility.largeText
                        "screen_reader" -> userAccessibility.screenReader
                        "reduced_motion" -> userAccessibility.reducedMotion
                        else -> false
                    }
                }
                if (!hasRequiredAccessibility) return false
            }

            // Check preferences
            if (preferences != null) {
                preferences.forEach { (key, expectedValue) ->
                    val actualValue = profile.preferences[key]
                    if (actualValue != expectedValue) {
                        return false
                    }
                }
            }

            return true
        }

        override suspend fun getConfidence(context: RuleContext): Float {
            return if (evaluate(context)) 1.0f else 0.0f
        }
    }

    /**
     * Condition based on achievements and progress
     */
    data class AchievementCondition(
        override val id: String,
        val requiredAchievements: List<String>? = null,
        val forbiddenAchievements: List<String>? = null,
        val minProgress: Float? = null,
        val maxProgress: Float? = null,
        val achievementCategories: List<String>? = null,
        override val description: String = "Achievement-based condition",
        override val priority: Int = 1
    ) : GameplayCondition() {

        override suspend fun evaluate(context: RuleContext): Boolean {
            val achievements = context.gameState.achievements
            val progress = context.gameState.progress

            // Check required achievements
            if (requiredAchievements != null) {
                val hasAllRequired = requiredAchievements.all { it in achievements }
                if (!hasAllRequired) return false
            }

            // Check forbidden achievements
            if (forbiddenAchievements != null) {
                val hasForbidden = forbiddenAchievements.any { it in achievements }
                if (hasForbidden) return false
            }

            // Check progress range
            if (minProgress != null && progress < minProgress) {
                return false
            }

            if (maxProgress != null && progress > maxProgress) {
                return false
            }

            // Check achievement categories (simplified - would need category mapping)
            if (achievementCategories != null) {
                // This would require additional logic to map achievements to categories
                // For now, we'll assume this passes if any achievements exist
                if (achievementCategories.isNotEmpty() && achievements.isEmpty()) {
                    return false
                }
            }

            return true
        }

        override suspend fun getConfidence(context: RuleContext): Float {
            return if (evaluate(context)) 1.0f else 0.0f
        }
    }

    /**
     * Custom condition with user-defined evaluation logic
     */
    data class CustomCondition(
        override val id: String,
        val evaluator: suspend (RuleContext) -> Boolean,
        val confidenceEvaluator: suspend (RuleContext) -> Float = { 1.0f },
        override val description: String = "Custom condition",
        override val priority: Int = 1
    ) : GameplayCondition() {

        override suspend fun evaluate(context: RuleContext): Boolean {
            return try {
                evaluator(context)
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun getConfidence(context: RuleContext): Float {
            return try {
                confidenceEvaluator(context).coerceIn(0.0f, 1.0f)
            } catch (e: Exception) {
                0.0f
            }
        }
    }
}