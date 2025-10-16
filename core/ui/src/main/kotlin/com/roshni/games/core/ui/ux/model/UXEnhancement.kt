package com.roshni.games.core.ui.ux.model

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sealed class representing different types of UX enhancements
 */
sealed class UXEnhancement(
    open val id: String,
    open val type: Type,
    open val priority: Priority = Priority.MEDIUM,
    open val duration: Long? = null,
    open val conditions: List<EnhancementCondition> = emptyList(),
    open val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Types of UX enhancements
     */
    enum class Type {
        VISUAL_FEEDBACK, AUDIO_FEEDBACK, HAPTIC_FEEDBACK, CONTEXTUAL_HELP,
        ADAPTIVE_LAYOUT, PERSONALIZED_CONTENT, ACCESSIBILITY_AID,
        PERFORMANCE_OPTIMIZATION, ANIMATED_TRANSITION, INTERACTIVE_GUIDANCE
    }

    /**
     * Priority levels for enhancements
     */
    enum class Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Conditions that must be met for enhancement to apply
     */
    data class EnhancementCondition(
        val type: ConditionType,
        val value: Any,
        val operator: ComparisonOperator = ComparisonOperator.EQUALS
    )

    /**
     * Types of conditions
     */
    enum class ConditionType {
        USER_PREFERENCE, DEVICE_CAPABILITY, ENVIRONMENTAL_FACTOR,
        GAME_STATE, INTERACTION_HISTORY, ACCESSIBILITY_NEED,
        PERFORMANCE_METRIC, TIME_BASED, LOCATION_BASED
    }

    /**
     * Comparison operators for conditions
     */
    enum class ComparisonOperator {
        EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN,
        GREATER_THAN_OR_EQUALS, LESS_THAN_OR_EQUALS,
        CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH
    }

    /**
     * Visual feedback enhancement
     */
    data class VisualFeedback(
        override val id: String,
        val animationType: AnimationType,
        val color: Color? = null,
        val scale: Float = 1.0f,
        val opacity: Float = 1.0f,
        val blurRadius: Dp = 0.dp,
        val shadowElevation: Dp = 0.dp,
        val animationSpec: AnimationSpec<Float>? = null,
        val triggerDelay: Long = 0,
        val repeatCount: Int = 1,
        override val priority: Priority = Priority.MEDIUM,
        override val duration: Long? = null,
        override val conditions: List<EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : UXEnhancement(id, Type.VISUAL_FEEDBACK, priority, duration, conditions, metadata) {

        /**
         * Types of visual animations
         */
        enum class AnimationType {
            FADE_IN, FADE_OUT, SCALE_UP, SCALE_DOWN, BOUNCE,
            SLIDE_IN, SLIDE_OUT, ROTATE, PULSE, SHAKE,
            COLOR_TRANSITION, SIZE_PULSE, ELASTIC_SCALE,
            BREATHE, WIGGLE, SPIN, FLIP, MORPH
        }

        /**
         * Get default animation spec for animation type
         */
        fun getDefaultAnimationSpec(): AnimationSpec<Float> {
            return when (animationType) {
                AnimationType.FADE_IN, AnimationType.FADE_OUT -> tween(durationMillis = 300)
                AnimationType.SCALE_UP, AnimationType.SCALE_DOWN -> spring(dampingRatio = 0.6f, stiffness = 300f)
                AnimationType.BOUNCE -> spring(dampingRatio = 0.4f, stiffness = 400f)
                AnimationType.SLIDE_IN, AnimationType.SLIDE_OUT -> tween(durationMillis = 250)
                AnimationType.ROTATE -> tween(durationMillis = 400)
                AnimationType.PULSE -> tween(durationMillis = 600)
                AnimationType.SHAKE -> spring(dampingRatio = 0.3f, stiffness = 500f)
                AnimationType.COLOR_TRANSITION -> tween(durationMillis = 500)
                AnimationType.SIZE_PULSE -> tween(durationMillis = 800)
                AnimationType.ELASTIC_SCALE -> spring(dampingRatio = 0.5f, stiffness = 200f)
                AnimationType.BREATHE -> tween(durationMillis = 2000)
                AnimationType.WIGGLE -> spring(dampingRatio = 0.2f, stiffness = 600f)
                AnimationType.SPIN -> tween(durationMillis = 1000)
                AnimationType.FLIP -> tween(durationMillis = 400)
                AnimationType.MORPH -> tween(durationMillis = 600)
            }
        }
    }

    /**
     * Audio feedback enhancement
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
        override val priority: Priority = Priority.MEDIUM,
        override val duration: Long? = null,
        override val conditions: List<EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : UXEnhancement(id, Type.AUDIO_FEEDBACK, priority, duration, conditions, metadata) {

        /**
         * Types of audio feedback sounds
         */
        enum class SoundType {
            CLICK, SUCCESS, ERROR, WARNING, NOTIFICATION,
            ACHIEVEMENT, LEVEL_UP, POWER_UP, GAME_OVER,
            BUTTON_HOVER, SWIPE, TAP, DOUBLE_TAP,
            POSITIVE_ACTION, NEGATIVE_ACTION, NEUTRAL_ACTION,
            COUNTDOWN_TICK, TIMER_EXPIRE, BONUS_COLLECTED,
            ENEMY_DEFEATED, ITEM_COLLECTED, DOOR_OPEN,
            MAGIC_CAST, EXPLOSION, CELEBRATION, AMBIENT
        }

        /**
         * Get resource ID for sound type (platform-specific)
         */
        fun getSoundResourceId(): String {
            return "sound_${soundType.name.lowercase()}"
        }
    }

    /**
     * Haptic feedback enhancement
     */
    data class HapticFeedback(
        override val id: String,
        val pattern: HapticPattern,
        val intensity: Intensity = Intensity.MEDIUM,
        val delay: Long = 0,
        override val priority: Priority = Priority.HIGH,
        override val duration: Long? = null,
        override val conditions: List<EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : UXEnhancement(id, Type.HAPTIC_FEEDBACK, priority, duration, conditions, metadata) {

        /**
         * Haptic feedback patterns
         */
        enum class HapticPattern {
            LIGHT_TICK, MEDIUM_TICK, HEAVY_TICK,
            LIGHT_BUMP, MEDIUM_BUMP, HEAVY_BUMP,
            DOUBLE_TICK, TRIPLE_TICK, CONTINUOUS_HUM,
            SHORT_VIBRATION, LONG_VIBRATION, RHYTHMIC_PATTERN,
            SUCCESS_PATTERN, ERROR_PATTERN, WARNING_PATTERN,
            BUTTON_PRESS, SWIPE_GESTURE, PINCH_GESTURE,
            GAME_IMPACT, POWER_UP_ACTIVATION, ACHIEVEMENT_UNLOCK
        }

        /**
         * Haptic intensity levels
         */
        enum class Intensity {
            LIGHT, MEDIUM, STRONG, MAXIMUM
        }

        /**
         * Get vibration duration for pattern
         */
        fun getVibrationDuration(): Long {
            return when (pattern) {
                HapticPattern.LIGHT_TICK -> 20
                HapticPattern.MEDIUM_TICK -> 30
                HapticPattern.HEAVY_TICK -> 50
                HapticPattern.LIGHT_BUMP -> 15
                HapticPattern.MEDIUM_BUMP -> 25
                HapticPattern.HEAVY_BUMP -> 40
                HapticPattern.DOUBLE_TICK -> 60
                HapticPattern.TRIPLE_TICK -> 90
                HapticPattern.CONTINUOUS_HUM -> 200
                HapticPattern.SHORT_VIBRATION -> 100
                HapticPattern.LONG_VIBRATION -> 300
                HapticPattern.RHYTHMIC_PATTERN -> 400
                HapticPattern.SUCCESS_PATTERN -> 150
                HapticPattern.ERROR_PATTERN -> 200
                HapticPattern.WARNING_PATTERN -> 100
                HapticPattern.BUTTON_PRESS -> 25
                HapticPattern.SWIPE_GESTURE -> 50
                HapticPattern.PINCH_GESTURE -> 75
                HapticPattern.GAME_IMPACT -> 80
                HapticPattern.POWER_UP_ACTIVATION -> 120
                HapticPattern.ACHIEVEMENT_UNLOCK -> 200
            }
        }
    }

    /**
     * Contextual help enhancement
     */
    data class ContextualHelp(
        override val id: String,
        val helpType: HelpType,
        val title: String,
        val message: String,
        val actions: List<HelpAction> = emptyList(),
        val visualAid: VisualAid? = null,
        val triggerCondition: TriggerCondition,
        val dismissCondition: DismissCondition? = null,
        val persistence: Persistence = Persistence.SESSION,
        override val priority: Priority = Priority.HIGH,
        override val duration: Long? = null,
        override val conditions: List<EnhancementCondition> = emptyList(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : UXEnhancement(id, Type.CONTEXTUAL_HELP, priority, duration, conditions, metadata) {

        /**
         * Types of contextual help
         */
        enum class HelpType {
            TOOLTIP, COACH_MARK, ONBOARDING_STEP, ERROR_EXPLANATION,
            FEATURE_DISCOVERY, GESTURE_GUIDANCE, ACCESSIBILITY_HINT,
            PERFORMANCE_TIP, SAFETY_WARNING, PROGRESS_INDICATOR
        }

        /**
         * Actions available in help
         */
        data class HelpAction(
            val id: String,
            val title: String,
            val type: ActionType,
            val data: Map<String, Any> = emptyMap()
        )

        /**
         * Types of help actions
         */
        enum class ActionType {
            DISMISS, LEARN_MORE, TRY_NOW, GOT_IT, NEXT_STEP,
            PREVIOUS_STEP, REPLAY, SHARE, REPORT_ISSUE
        }

        /**
         * Visual aid for help
         */
        data class VisualAid(
            val type: VisualAidType,
            val targetElement: String? = null,
            val highlightColor: Color = Color.Yellow,
            val showRipple: Boolean = false,
            val animationType: AnimationType = AnimationType.FADE_IN
        )

        /**
         * Types of visual aids
         */
        enum class VisualAidType {
            HIGHLIGHT, ARROW_POINTING, SPOTLIGHT, OVERLAY,
            HAND_GESTURE, ANIMATION, SCREENSHOT, VIDEO_CLIP
        }

        /**
         * When to trigger the help
         */
        data class TriggerCondition(
            val event: String,
            val delay: Long = 0,
            val maxTriggers: Int = 1,
            val cooldown: Long = 0
        )

        /**
         * When to dismiss the help
         */
        data class DismissCondition(
            val autoDismissDelay: Long? = null,
            val dismissOnInteraction: Boolean = true,
            val dismissOnNavigation: Boolean = true,
            val dismissEvents: List<String> = emptyList()
        )

        /**
         * How long help should persist
         */
        enum class Persistence {
            ONCE, SESSION, PERSISTENT, CONDITIONAL
        }
    }

    /**
     * Check if enhancement meets all conditions
     */
    fun meetsConditions(context: UXContext): Boolean {
        return conditions.all { condition ->
            evaluateCondition(condition, context)
        }
    }

    /**
     * Evaluate a specific condition
     */
    private fun evaluateCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        return when (condition.type) {
            EnhancementCondition.ConditionType.USER_PREFERENCE -> {
                evaluateUserPreferenceCondition(condition, context)
            }
            EnhancementCondition.ConditionType.DEVICE_CAPABILITY -> {
                evaluateDeviceCapabilityCondition(condition, context)
            }
            EnhancementCondition.ConditionType.ENVIRONMENTAL_FACTOR -> {
                evaluateEnvironmentalFactorCondition(condition, context)
            }
            EnhancementCondition.ConditionType.GAME_STATE -> {
                evaluateGameStateCondition(condition, context)
            }
            EnhancementCondition.ConditionType.INTERACTION_HISTORY -> {
                evaluateInteractionHistoryCondition(condition, context)
            }
            EnhancementCondition.ConditionType.ACCESSIBILITY_NEED -> {
                evaluateAccessibilityNeedCondition(condition, context)
            }
            EnhancementCondition.ConditionType.PERFORMANCE_METRIC -> {
                evaluatePerformanceMetricCondition(condition, context)
            }
            EnhancementCondition.ConditionType.TIME_BASED -> {
                evaluateTimeBasedCondition(condition, context)
            }
            EnhancementCondition.ConditionType.LOCATION_BASED -> {
                evaluateLocationBasedCondition(condition, context)
            }
        }
    }

    /**
     * Evaluate user preference conditions
     */
    private fun evaluateUserPreferenceCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        val preferences = context.userPreferences
        return when (condition.value) {
            is Boolean -> {
                when (condition.operator) {
                    EnhancementCondition.ComparisonOperator.EQUALS -> {
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
                    EnhancementCondition.ComparisonOperator.EQUALS -> {
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

    /**
     * Evaluate device capability conditions
     */
    private fun evaluateDeviceCapabilityCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        val capabilities = context.deviceCapabilities
        return when (condition.value) {
            is Boolean -> {
                when (condition.operator) {
                    EnhancementCondition.ComparisonOperator.EQUALS -> {
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
                    EnhancementCondition.ComparisonOperator.GREATER_THAN -> {
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

    /**
     * Evaluate environmental factor conditions
     */
    private fun evaluateEnvironmentalFactorCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        val factors = context.environmentalFactors
        return when (condition.value) {
            is String -> {
                when (condition.operator) {
                    EnhancementCondition.ComparisonOperator.EQUALS -> {
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
                    EnhancementCondition.ComparisonOperator.LESS_THAN -> {
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
                    EnhancementCondition.ComparisonOperator.EQUALS -> {
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

    /**
     * Evaluate game state conditions
     */
    private fun evaluateGameStateCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        val gameState = context.currentGameState ?: return false
        return when (condition.value) {
            is String -> {
                when (condition.operator) {
                    EnhancementCondition.ComparisonOperator.EQUALS -> {
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
                    EnhancementCondition.ComparisonOperator.GREATER_THAN -> {
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

    /**
     * Evaluate interaction history conditions
     */
    private fun evaluateInteractionHistoryCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        val history = context.interactionHistory
        return when (condition.value) {
            is Number -> {
                when (condition.operator) {
                    EnhancementCondition.ComparisonOperator.GREATER_THAN -> {
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

    /**
     * Evaluate accessibility need conditions
     */
    private fun evaluateAccessibilityNeedCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        val preferences = context.userPreferences
        return when (condition.value) {
            is Boolean -> {
                when (condition.operator) {
                    EnhancementCondition.ComparisonOperator.EQUALS -> {
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

    /**
     * Evaluate performance metric conditions
     */
    private fun evaluatePerformanceMetricCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        // This would integrate with performance monitoring system
        // For now, return true as placeholder
        return true
    }

    /**
     * Evaluate time-based conditions
     */
    private fun evaluateTimeBasedCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        val currentTime = System.currentTimeMillis()
        return when (condition.value) {
            is Number -> {
                when (condition.operator) {
                    EnhancementCondition.ComparisonOperator.GREATER_THAN -> {
                        currentTime > condition.value.toLong()
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    /**
     * Evaluate location-based conditions
     */
    private fun evaluateLocationBasedCondition(condition: EnhancementCondition, context: UXContext): Boolean {
        val factors = context.environmentalFactors
        return when (condition.value) {
            is String -> {
                when (condition.operator) {
                    EnhancementCondition.ComparisonOperator.EQUALS -> {
                        factors.locationContext.name == condition.value
                    }
                    else -> false
                }
            }
            else -> false
        }
    }
}