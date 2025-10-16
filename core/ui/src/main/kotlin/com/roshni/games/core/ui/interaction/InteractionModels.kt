package com.roshni.games.core.ui.interaction

import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Response to a user interaction containing all generated reactions and actions
 */
data class InteractionResponse(
    val interactionId: String,
    val immediateReactions: List<ImmediateReaction>,
    val personalizedReactions: List<PersonalizedReaction>,
    val uxEnhancements: List<UXEnhancement>,
    val navigationActions: List<NavigationAction>,
    val context: UXContext,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Get all reactions (immediate + personalized)
     */
    fun getAllReactions(): List<Reaction> {
        return immediateReactions + personalizedReactions
    }

    /**
     * Get reactions by type
     */
    fun getReactionsByType(type: Reaction.Type): List<Reaction> {
        return getAllReactions().filter { it.type == type }
    }

    /**
     * Check if response contains navigation actions
     */
    fun hasNavigationActions(): Boolean {
        return navigationActions.isNotEmpty()
    }

    /**
     * Get primary navigation action
     */
    fun getPrimaryNavigationAction(): NavigationAction? {
        return navigationActions.maxByOrNull { it.priority.value }
    }

    /**
     * Calculate total reaction priority score
     */
    fun calculatePriorityScore(): Double {
        val immediateScore = immediateReactions.sumOf { it.priority.value * 1.0 }
        val personalizedScore = personalizedReactions.sumOf { it.priority.value * 1.5 } // Personalized reactions weighted higher
        return (immediateScore + personalizedScore) / (immediateReactions.size + personalizedReactions.size).coerceAtLeast(1)
    }
}

/**
 * Base interface for all types of reactions
 */
sealed interface Reaction {
    val id: String
    val type: Type
    val priority: Priority
    val conditions: List<UXEnhancement.EnhancementCondition>
    val metadata: Map<String, Any>

    /**
     * Check if reaction meets all conditions for given context
     */
    fun meetsConditions(context: UXContext): Boolean {
        return conditions.all { condition ->
            evaluateCondition(condition, context)
        }
    }

    /**
     * Get estimated duration of the reaction
     */
    fun getEstimatedDuration(): Long

    /**
     * Types of reactions
     */
    enum class Type {
        VISUAL_FEEDBACK, AUDIO_FEEDBACK, HAPTIC_FEEDBACK,
        PERSONALIZED_CONTENT, ADAPTIVE_LAYOUT, CONTEXTUAL_HELP,
        NAVIGATION_SUGGESTION, BEHAVIORAL_RESPONSE
    }

    /**
     * Priority levels for reactions
     */
    enum class Priority(val value: Int) {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4)
    }

    private fun evaluateCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        return when (condition.type) {
            UXEnhancement.EnhancementCondition.ConditionType.USER_PREFERENCE -> {
                evaluateUserPreferenceCondition(condition, context)
            }
            UXEnhancement.EnhancementCondition.ConditionType.DEVICE_CAPABILITY -> {
                evaluateDeviceCapabilityCondition(condition, context)
            }
            UXEnhancement.EnhancementCondition.ConditionType.ENVIRONMENTAL_FACTOR -> {
                evaluateEnvironmentalFactorCondition(condition, context)
            }
            UXEnhancement.EnhancementCondition.ConditionType.GAME_STATE -> {
                evaluateGameStateCondition(condition, context)
            }
            UXEnhancement.EnhancementCondition.ConditionType.INTERACTION_HISTORY -> {
                evaluateInteractionHistoryCondition(condition, context)
            }
            UXEnhancement.EnhancementCondition.ConditionType.ACCESSIBILITY_NEED -> {
                evaluateAccessibilityNeedCondition(condition, context)
            }
            UXEnhancement.EnhancementCondition.ConditionType.PERFORMANCE_METRIC -> {
                evaluatePerformanceMetricCondition(condition, context)
            }
            UXEnhancement.EnhancementCondition.ConditionType.TIME_BASED -> {
                evaluateTimeBasedCondition(condition, context)
            }
            UXEnhancement.EnhancementCondition.ConditionType.LOCATION_BASED -> {
                evaluateLocationBasedCondition(condition, context)
            }
        }
    }

    private fun evaluateUserPreferenceCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        val preferences = context.userPreferences
        return when (condition.value) {
            is Boolean -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS -> {
                        when (condition.value) {
                            "soundEnabled" -> preferences.soundEnabled == condition.value
                            "hapticFeedbackEnabled" -> preferences.hapticFeedbackEnabled == condition.value
                            "reducedMotion" -> preferences.reducedMotion == condition.value
                            "highContrast" -> preferences.highContrast == condition.value
                            "largeText" -> preferences.largeText == condition.value
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            is String -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS -> {
                        when (condition.value) {
                            "theme" -> preferences.theme.name == condition.value
                            "animationSpeed" -> preferences.animationSpeed.name == condition.value
                            "language" -> preferences.language == condition.value
                            "accessibilityProfile" -> preferences.accessibilityProfile.name == condition.value
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun evaluateDeviceCapabilityCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        val capabilities = context.deviceCapabilities
        return when (condition.value) {
            is Boolean -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS -> {
                        when (condition.value) {
                            "hasVibrator" -> capabilities.hasVibrator == condition.value
                            "hasSpeaker" -> capabilities.hasSpeaker == condition.value
                            "hasCamera" -> capabilities.hasCamera == condition.value
                            "supportsHDR" -> capabilities.supportsHDR == condition.value
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            is Number -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.GREATER_THAN -> {
                        when (condition.value) {
                            "screenRefreshRate" -> capabilities.screenRefreshRate > condition.value.toInt()
                            "maxTextureSize" -> capabilities.maxTextureSize > condition.value.toInt()
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun evaluateEnvironmentalFactorCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        val factors = context.environmentalFactors
        return when (condition.value) {
            is String -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS -> {
                        when (condition.value) {
                            "timeOfDay" -> factors.timeOfDay.name == condition.value
                            "lightingCondition" -> factors.lightingCondition.name == condition.value
                            "noiseLevel" -> factors.noiseLevel.name == condition.value
                            "networkQuality" -> factors.networkQuality.name == condition.value
                            "locationContext" -> factors.locationContext.name == condition.value
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            is Number -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.LESS_THAN -> {
                        when (condition.value) {
                            "batteryLevel" -> factors.batteryLevel < condition.value.toInt()
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            is Boolean -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS -> {
                        when (condition.value) {
                            "isInMotion" -> factors.isInMotion == condition.value
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun evaluateGameStateCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        val gameState = context.currentGameState ?: return false
        return when (condition.value) {
            is String -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS -> {
                        when (condition.value) {
                            "difficulty" -> gameState.difficulty.name == condition.value
                            "gameMode" -> gameState.gameMode == condition.value
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            is Number -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.GREATER_THAN -> {
                        when (condition.value) {
                            "level" -> (gameState.level ?: 0) > condition.value.toInt()
                            "score" -> (gameState.score ?: 0) > condition.value.toLong()
                            "lives" -> (gameState.lives ?: 0) > condition.value.toInt()
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun evaluateInteractionHistoryCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        val history = context.interactionHistory
        return when (condition.value) {
            is Number -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.GREATER_THAN -> {
                        when (condition.value) {
                            "interactionCount" -> history.size > condition.value.toInt()
                            "recentInteractionCount" -> {
                                val recentThreshold = System.currentTimeMillis() - 300000 // 5 minutes
                                history.count { it.timestamp > recentThreshold } > condition.value.toInt()
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun evaluateAccessibilityNeedCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        val preferences = context.userPreferences
        return when (condition.value) {
            is Boolean -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS -> {
                        when (condition.value) {
                            "visualImpairment" -> preferences.accessibilityProfile == UXContext.AccessibilityProfile.VISUAL_IMPAIRMENT
                            "motorImpairment" -> preferences.accessibilityProfile == UXContext.AccessibilityProfile.MOTOR_IMPAIRMENT
                            "cognitiveImpairment" -> preferences.accessibilityProfile == UXContext.AccessibilityProfile.COGNITIVE_IMPAIRMENT
                            "hearingImpairment" -> preferences.accessibilityProfile == UXContext.AccessibilityProfile.HEARING_IMPAIRMENT
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun evaluatePerformanceMetricCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        // This would integrate with performance monitoring system
        // For now, return true as placeholder
        return true
    }

    private fun evaluateTimeBasedCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        val currentTime = System.currentTimeMillis()
        return when (condition.value) {
            is Number -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.GREATER_THAN -> {
                        currentTime > condition.value.toLong()
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun evaluateLocationBasedCondition(condition: UXEnhancement.EnhancementCondition, context: UXContext): Boolean {
        val factors = context.environmentalFactors
        return when (condition.value) {
            is String -> {
                when (condition.operator) {
                    UXEnhancement.EnhancementCondition.ComparisonOperator.EQUALS -> {
                        factors.locationContext.name == condition.value
                    }
                    else -> false
                }
            }
            else -> false
        }
    }
}

/**
 * Immediate reactions that happen instantly in response to user interactions
 */
sealed class ImmediateReaction(
    override val id: String,
    override val type: Reaction.Type,
    override val priority: Reaction.Priority,
    override val conditions: List<UXEnhancement.EnhancementCondition> = emptyList(),
    override val metadata: Map<String, Any> = emptyMap()
) : Reaction {

    /**
     * Visual feedback reaction
     */
    data class VisualFeedback(
        override val id: String,
        val animationType: AnimationType,
        val color: Color? = null,
        val scale: Float = 1.0f,
        val opacity: Float = 1.0f,
        val blurRadius: Dp = 0.dp,
        val shadowElevation: Dp = 0.dp,
        val triggerDelay: Long = 0,
        val repeatCount: Int = 1,
        override val priority: Reaction.Priority = Reaction.Priority.MEDIUM,
        override val conditions: List<UXEnhancement.EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : ImmediateReaction(id, Reaction.Type.VISUAL_FEEDBACK, priority, conditions, metadata) {

        enum class AnimationType {
            FADE_IN, FADE_OUT, SCALE_UP, SCALE_DOWN, BOUNCE,
            SLIDE_IN, SLIDE_OUT, ROTATE, PULSE, SHAKE,
            COLOR_TRANSITION, SIZE_PULSE, ELASTIC_SCALE,
            BREATHE, WIGGLE, SPIN, FLIP, MORPH
        }

        override fun getEstimatedDuration(): Long {
            return when (animationType) {
                AnimationType.FADE_IN, AnimationType.FADE_OUT -> 300L
                AnimationType.SCALE_UP, AnimationType.SCALE_DOWN -> 250L
                AnimationType.BOUNCE -> 400L
                AnimationType.SLIDE_IN, AnimationType.SLIDE_OUT -> 250L
                AnimationType.ROTATE -> 400L
                AnimationType.PULSE -> 600L
                AnimationType.SHAKE -> 300L
                AnimationType.COLOR_TRANSITION -> 500L
                AnimationType.SIZE_PULSE -> 800L
                AnimationType.ELASTIC_SCALE -> 350L
                AnimationType.BREATHE -> 2000L
                AnimationType.WIGGLE -> 400L
                AnimationType.SPIN -> 1000L
                AnimationType.FLIP -> 400L
                AnimationType.MORPH -> 600L
            }
        }
    }

    /**
     * Audio feedback reaction
     */
    data class AudioFeedback(
        override val id: String,
        val soundType: SoundType,
        val volume: Float = 0.7f,
        val pitch: Float = 1.0f,
        val pan: Float = 0.0f,
        val loopCount: Int = 1,
        val fadeInDuration: Long = 0,
        val fadeOutDuration: Long = 0,
        val delay: Long = 0,
        override val priority: Reaction.Priority = Reaction.Priority.MEDIUM,
        override val conditions: List<UXEnhancement.EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : ImmediateReaction(id, Reaction.Type.AUDIO_FEEDBACK, priority, conditions, metadata) {

        enum class SoundType {
            CLICK, SUCCESS, ERROR, WARNING, NOTIFICATION,
            ACHIEVEMENT, LEVEL_UP, POWER_UP, GAME_OVER,
            BUTTON_HOVER, SWIPE, TAP, DOUBLE_TAP,
            POSITIVE_ACTION, NEGATIVE_ACTION, NEUTRAL_ACTION,
            COUNTDOWN_TICK, TIMER_EXPIRE, BONUS_COLLECTED,
            ENEMY_DEFEATED, ITEM_COLLECTED, DOOR_OPEN,
            MAGIC_CAST, EXPLOSION, CELEBRATION, AMBIENT
        }

        override fun getEstimatedDuration(): Long {
            return when (soundType) {
                SoundType.CLICK -> 100L
                SoundType.SUCCESS -> 200L
                SoundType.ERROR -> 300L
                SoundType.WARNING -> 250L
                SoundType.NOTIFICATION -> 150L
                SoundType.ACHIEVEMENT -> 500L
                SoundType.LEVEL_UP -> 400L
                SoundType.POWER_UP -> 300L
                SoundType.GAME_OVER -> 1000L
                SoundType.BUTTON_HOVER -> 50L
                SoundType.SWIPE -> 80L
                SoundType.TAP -> 60L
                SoundType.DOUBLE_TAP -> 120L
                SoundType.POSITIVE_ACTION -> 200L
                SoundType.NEGATIVE_ACTION -> 250L
                SoundType.NEUTRAL_ACTION -> 150L
                SoundType.COUNTDOWN_TICK -> 100L
                SoundType.TIMER_EXPIRE -> 400L
                SoundType.BONUS_COLLECTED -> 300L
                SoundType.ENEMY_DEFEATED -> 350L
                SoundType.ITEM_COLLECTED -> 250L
                SoundType.DOOR_OPEN -> 200L
                SoundType.MAGIC_CAST -> 400L
                SoundType.EXPLOSION -> 600L
                SoundType.CELEBRATION -> 800L
                SoundType.AMBIENT -> 2000L
            }
        }

        fun getSoundResourceId(): String {
            return "sound_${soundType.name.lowercase()}"
        }
    }

    /**
     * Haptic feedback reaction
     */
    data class HapticFeedback(
        override val id: String,
        val pattern: HapticPattern,
        val intensity: Intensity = Intensity.MEDIUM,
        val delay: Long = 0,
        override val priority: Reaction.Priority = Reaction.Priority.HIGH,
        override val conditions: List<UXEnhancement.EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : ImmediateReaction(id, Reaction.Type.HAPTIC_FEEDBACK, priority, conditions, metadata) {

        enum class HapticPattern {
            LIGHT_TICK, MEDIUM_TICK, HEAVY_TICK,
            LIGHT_BUMP, MEDIUM_BUMP, HEAVY_BUMP,
            DOUBLE_TICK, TRIPLE_TICK, CONTINUOUS_HUM,
            SHORT_VIBRATION, LONG_VIBRATION, RHYTHMIC_PATTERN,
            SUCCESS_PATTERN, ERROR_PATTERN, WARNING_PATTERN,
            BUTTON_PRESS, SWIPE_GESTURE, PINCH_GESTURE,
            GAME_IMPACT, POWER_UP_ACTIVATION, ACHIEVEMENT_UNLOCK
        }

        enum class Intensity {
            LIGHT, MEDIUM, STRONG, MAXIMUM
        }

        override fun getEstimatedDuration(): Long {
            return when (pattern) {
                HapticPattern.LIGHT_TICK -> 20L
                HapticPattern.MEDIUM_TICK -> 30L
                HapticPattern.HEAVY_TICK -> 50L
                HapticPattern.LIGHT_BUMP -> 15L
                HapticPattern.MEDIUM_BUMP -> 25L
                HapticPattern.HEAVY_BUMP -> 40L
                HapticPattern.DOUBLE_TICK -> 60L
                HapticPattern.TRIPLE_TICK -> 90L
                HapticPattern.CONTINUOUS_HUM -> 200L
                HapticPattern.SHORT_VIBRATION -> 100L
                HapticPattern.LONG_VIBRATION -> 300L
                HapticPattern.RHYTHMIC_PATTERN -> 400L
                HapticPattern.SUCCESS_PATTERN -> 150L
                HapticPattern.ERROR_PATTERN -> 200L
                HapticPattern.WARNING_PATTERN -> 100L
                HapticPattern.BUTTON_PRESS -> 25L
                HapticPattern.SWIPE_GESTURE -> 50L
                HapticPattern.PINCH_GESTURE -> 75L
                HapticPattern.GAME_IMPACT -> 80L
                HapticPattern.POWER_UP_ACTIVATION -> 120L
                HapticPattern.ACHIEVEMENT_UNLOCK -> 200L
            }
        }
    }
}

/**
 * Personalized reactions based on user behavior patterns and preferences
 */
data class PersonalizedReaction(
    override val id: String,
    override val type: Reaction.Type,
    override val priority: Reaction.Priority,
    val title: String,
    val description: String,
    val confidence: Double,
    val userContext: Map<String, Any>,
    val adaptiveContent: AdaptiveContent,
    val triggerCondition: TriggerCondition,
    override val conditions: List<UXEnhancement.EnhancementCondition> = emptyList(),
    override val metadata: Map<String, Any> = emptyMap()
) : Reaction {

    override fun getEstimatedDuration(): Long {
        return adaptiveContent.estimatedDuration
    }

    data class AdaptiveContent(
        val contentType: ContentType,
        val contentData: Map<String, Any>,
        val personalizationFactors: List<String>,
        val estimatedDuration: Long
    )

    data class TriggerCondition(
        val event: String,
        val delay: Long = 0,
        val maxTriggers: Int = 1,
        val cooldown: Long = 0
    )

    enum class ContentType {
        TEXT, IMAGE, VIDEO, INTERACTIVE, MIXED
    }
}

/**
 * Navigation actions that can be triggered by interactions
 */
sealed class NavigationAction(
    override val id: String,
    override val type: Reaction.Type,
    override val priority: Priority,
    override val conditions: List<UXEnhancement.EnhancementCondition> = emptyList(),
    override val metadata: Map<String, Any> = emptyMap()
) : Reaction {

    enum class Priority(val value: Int) {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4)
    }

    /**
     * Immediate navigation to a specific destination
     */
    data class ImmediateNavigation(
        override val id: String,
        val destination: String,
        val arguments: Map<String, Any> = emptyMap(),
        val animationType: AnimationType? = null,
        override val priority: Priority = Priority.HIGH,
        override val conditions: List<UXEnhancement.EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : NavigationAction(id, Reaction.Type.NAVIGATION_SUGGESTION, priority, conditions, metadata) {

        enum class AnimationType {
            FADE, SLIDE, SCALE, NONE
        }

        override fun getEstimatedDuration(): Long = 300L // Standard navigation duration
    }

    /**
     * Contextual navigation based on user behavior and context
     */
    data class ContextualNavigation(
        override val id: String,
        val destination: String,
        val triggerCondition: TriggerCondition,
        val suggestions: List<String> = emptyList(),
        val confidence: Double = 0.0,
        override val priority: Priority = Priority.MEDIUM,
        override val conditions: List<UXEnhancement.EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : NavigationAction(id, Reaction.Type.NAVIGATION_SUGGESTION, priority, conditions, metadata) {

        data class TriggerCondition(
            val event: String,
            val delay: Long = 0,
            val maxTriggers: Int = 1,
            val cooldown: Long = 0
        )

        override fun getEstimatedDuration(): Long = 500L // Contextual navigation takes longer
    }
}