package com.roshni.games.core.utils.rules

/**
 * Sealed class representing various gameplay actions that can be triggered
 * by rule evaluation. Each action type defines specific behavior to modify
 * gameplay, UI, or system state.
 */
sealed class GameplayAction {

    /**
     * Unique identifier for this action
     */
    abstract val id: String

    /**
     * Human-readable description of what this action does
     */
    abstract val description: String

    /**
     * Priority level for action execution (higher values = higher priority)
     */
    abstract val priority: Int

    /**
     * Whether this action should be executed immediately or can be deferred
     */
    abstract val immediate: Boolean

    /**
     * Execute this action with the provided context
     *
     * @param context The rule context containing all necessary information
     * @return true if the action was executed successfully, false otherwise
     */
    abstract suspend fun execute(context: RuleContext): Boolean

    /**
     * Check if this action can be executed in the current context
     *
     * @param context The rule context containing all necessary information
     * @return true if the action can be executed, false otherwise
     */
    abstract suspend fun canExecute(context: RuleContext): Boolean

    /**
     * Modify gameplay mechanics or state
     */
    data class ModifyGameplay(
        override val id: String,
        val modifications: Map<String, Any>,
        val duration: Long? = null, // Duration in milliseconds, null = permanent
        val reason: String = "Rule-triggered gameplay modification",
        override val description: String = "Modify gameplay mechanics",
        override val priority: Int = 3,
        override val immediate: Boolean = true
    ) : GameplayAction() {

        override suspend fun execute(context: RuleContext): Boolean {
            // Implementation would modify game state through game engine
            // This is a placeholder for the actual game state modification logic
            return try {
                // TODO: Integrate with actual game engine to apply modifications
                println("Executing gameplay modification: $modifications")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            // Check if game is in a state where modifications are allowed
            return context.gameState.currentLevel != null
        }
    }

    /**
     * Show a message or notification to the user
     */
    data class ShowMessage(
        override val id: String,
        val message: String,
        val messageType: MessageType = MessageType.INFO,
        val title: String? = null,
        val duration: Long = 5000, // Duration in milliseconds
        val actions: List<UserAction> = emptyList(),
        override val description: String = "Show message to user",
        override val priority: Int = 1,
        override val immediate: Boolean = true
    ) : GameplayAction() {

        enum class MessageType {
            INFO, SUCCESS, WARNING, ERROR, ACHIEVEMENT
        }

        data class UserAction(
            val text: String,
            val action: () -> Unit
        )

        override suspend fun execute(context: RuleContext): Boolean {
            return try {
                // TODO: Integrate with UI system to show message
                println("Showing message: $message (Type: $messageType)")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            // Messages can always be shown
            return true
        }
    }

    /**
     * Unlock content or features
     */
    data class UnlockContent(
        override val id: String,
        val contentIds: List<String>,
        val reason: String = "Rule evaluation unlocked content",
        val notifyUser: Boolean = true,
        override val description: String = "Unlock game content",
        override val priority: Int = 2,
        override val immediate: Boolean = false
    ) : GameplayAction() {

        override suspend fun execute(context: RuleContext): Boolean {
            return try {
                // TODO: Integrate with content management system
                println("Unlocking content: $contentIds")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            // Can unlock content if user has necessary permissions
            return !context.userProfile.parentalControlsEnabled ||
                   context.userProfile.age?.let { it >= 13 } == true
        }
    }

    /**
     * Apply visual or audio effects
     */
    data class ApplyEffect(
        override val id: String,
        val effectType: EffectType,
        val intensity: Float = 1.0f,
        val duration: Long = 3000, // Duration in milliseconds
        val targetArea: String? = null,
        override val description: String = "Apply visual/audio effect",
        override val priority: Int = 1,
        override val immediate: Boolean = true
    ) : GameplayAction() {

        enum class EffectType {
            VISUAL_FEEDBACK,
            AUDIO_CUE,
            HAPTIC_FEEDBACK,
            SCREEN_FLASH,
            PARTICLE_EFFECT,
            SOUND_EFFECT
        }

        override suspend fun execute(context: RuleContext): Boolean {
            return try {
                // TODO: Integrate with audio/visual systems
                println("Applying effect: $effectType (Intensity: $intensity)")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            // Check if device supports the effect type
            return when (effectType) {
                EffectType.HAPTIC_FEEDBACK -> true // Assume all devices support haptic
                EffectType.VISUAL_FEEDBACK -> true // Assume all devices support visual
                EffectType.AUDIO_CUE -> true // Assume all devices support audio
                else -> true
            }
        }
    }

    /**
     * Modify player progression or stats
     */
    data class ModifyProgression(
        override val id: String,
        val statChanges: Map<String, Any>,
        val reason: String = "Rule-triggered progression change",
        val notifyUser: Boolean = false,
        override val description: String = "Modify player progression",
        override val priority: Int = 2,
        override val immediate: Boolean = true
    ) : GameplayAction() {

        override suspend fun execute(context: RuleContext): Boolean {
            return try {
                // TODO: Integrate with player progression system
                println("Modifying progression: $statChanges")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            // Can modify progression if user is logged in and has a profile
            return context.userId.isNotBlank()
        }
    }

    /**
     * Trigger analytics or logging events
     */
    data class LogEvent(
        override val id: String,
        val eventName: String,
        val eventData: Map<String, Any>,
        val logLevel: LogLevel = LogLevel.INFO,
        override val description: String = "Log analytics event",
        override val priority: Int = 0,
        override val immediate: Boolean = false
    ) : GameplayAction() {

        enum class LogLevel {
            DEBUG, INFO, WARN, ERROR
        }

        override suspend fun execute(context: RuleContext): Boolean {
            return try {
                // TODO: Integrate with analytics system
                println("Logging event: $eventName with data: $eventData")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            // Logging can always be performed
            return true
        }
    }

    /**
     * Control feature availability
     */
    data class ToggleFeature(
        override val id: String,
        val featureId: String,
        val enabled: Boolean,
        val reason: String = "Rule-triggered feature toggle",
        val duration: Long? = null, // Duration in milliseconds, null = permanent
        override val description: String = "Toggle feature availability",
        override val priority: Int = 2,
        override val immediate: Boolean = true
    ) : GameplayAction() {

        override suspend fun execute(context: RuleContext): Boolean {
            return try {
                // TODO: Integrate with feature flag system
                println("Toggling feature $featureId to $enabled")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            // Feature toggling is always allowed
            return true
        }
    }

    /**
     * Save game state or progress
     */
    data class SaveState(
        override val id: String,
        val saveReason: String = "Rule-triggered save",
        val saveData: Map<String, Any>? = null,
        override val description: String = "Save game state",
        override val priority: Int = 1,
        override val immediate: Boolean = false
    ) : GameplayAction() {

        override suspend fun execute(context: RuleContext): Boolean {
            return try {
                // TODO: Integrate with save state system
                println("Saving game state: $saveReason")
                true
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            // Can save if there's an active game session
            return context.sessionId != null
        }
    }

    /**
     * Custom action with user-defined execution logic
     */
    data class CustomAction(
        override val id: String,
        val executor: suspend (RuleContext) -> Boolean,
        val canExecuteChecker: suspend (RuleContext) -> Boolean = { true },
        override val description: String = "Custom gameplay action",
        override val priority: Int = 1,
        override val immediate: Boolean = true
    ) : GameplayAction() {

        override suspend fun execute(context: RuleContext): Boolean {
            return try {
                executor(context)
            } catch (e: Exception) {
                false
            }
        }

        override suspend fun canExecute(context: RuleContext): Boolean {
            return try {
                canExecuteChecker(context)
            } catch (e: Exception) {
                false
            }
        }
    }
}