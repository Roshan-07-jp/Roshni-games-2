package com.roshni.games.core.ui.interaction.personalization

import com.roshni.games.core.ui.interaction.PersonalizedReaction
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UserInteraction

/**
 * User preference profile for personalization
 */
data class UserPreferenceProfile(
    val userId: String,
    val interactionPreferences: Map<String, Any> = emptyMap(),
    val behavioralTraits: Map<String, Double> = emptyMap(),
    val contextualPreferences: Map<String, String> = emptyMap(),
    val engagementLevel: Double = 0.5,
    val personalizationScore: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val learningProgress: Double = 0.0,
    val preferenceStrengths: Map<String, Double> = emptyMap()
) {

    /**
     * Update profile with new insights
     */
    fun updateWithInsights(
        behaviorInsights: Map<String, Double> = emptyMap(),
        preferenceUpdates: Map<String, Any> = emptyMap(),
        contextInsights: Map<String, String> = emptyMap()
    ): UserPreferenceProfile {
        val updatedBehavioralTraits = behavioralTraits + behaviorInsights
        val updatedInteractionPreferences = interactionPreferences + preferenceUpdates
        val updatedContextualPreferences = contextualPreferences + contextInsights

        // Recalculate engagement level based on insights
        val newEngagementLevel = calculateEngagementLevel(
            updatedBehavioralTraits,
            updatedInteractionPreferences
        )

        // Update personalization score based on learning progress
        val newPersonalizationScore = calculatePersonalizationScore(
            updatedBehavioralTraits,
            newEngagementLevel
        )

        return copy(
            behavioralTraits = updatedBehavioralTraits,
            interactionPreferences = updatedInteractionPreferences,
            contextualPreferences = updatedContextualPreferences,
            engagementLevel = newEngagementLevel,
            personalizationScore = newPersonalizationScore,
            lastUpdated = System.currentTimeMillis(),
            learningProgress = (learningProgress + 0.01).coerceAtMost(1.0)
        )
    }

    /**
     * Update preferences with new values
     */
    fun updatePreferences(newPreferences: Map<String, Any>): UserPreferenceProfile {
        return copy(
            interactionPreferences = interactionPreferences + newPreferences,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Get preference value with default
     */
    fun getPreference(key: String, defaultValue: Any): Any {
        return interactionPreferences[key] ?: defaultValue
    }

    /**
     * Get behavioral trait with default
     */
    fun getBehavioralTrait(key: String, defaultValue: Double = 0.0): Double {
        return behavioralTraits[key] ?: defaultValue
    }

    /**
     * Calculate engagement level based on behavioral traits and preferences
     */
    private fun calculateEngagementLevel(
        traits: Map<String, Double>,
        preferences: Map<String, Any>
    ): Double {
        var engagementScore = 0.5 // Base engagement

        // Factor in interaction frequency
        val interactionFrequency = traits["interaction_frequency"] ?: 0.0
        engagementScore += interactionFrequency * 0.2

        // Factor in preference consistency
        val preferenceConsistency = traits["preference_consistency"] ?: 0.0
        engagementScore += preferenceConsistency * 0.15

        // Factor in contextual adaptability
        val contextualAdaptability = traits["contextual_adaptability"] ?: 0.0
        engagementScore += contextualAdaptability * 0.15

        return engagementScore.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate personalization score based on learning progress
     */
    private fun calculatePersonalizationScore(
        traits: Map<String, Double>,
        engagement: Double
    ): Double {
        val learningProgress = traits["learning_progress"] ?: 0.0
        val behaviorPredictionAccuracy = traits["behavior_prediction_accuracy"] ?: 0.0

        return (learningProgress * 0.4) + (behaviorPredictionAccuracy * 0.4) + (engagement * 0.2)
    }
}

/**
 * Personalization rule for generating adaptive reactions
 */
data class PersonalizationRule(
    val id: String,
    val name: String,
    val description: String,
    val type: PersonalizationRuleType,
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
    val priority: Int = 1,
    val confidence: Double = 0.7,
    val isActive: Boolean = true,
    val cooldownMs: Long = 0,
    val maxExecutions: Int? = null,
    val metadata: Map<String, Any> = emptyMap()
) {

    private var executionCount = 0
    private var lastExecutionTime = 0L

    /**
     * Check if this rule should apply to the given interaction and context
     */
    fun shouldApply(
        interaction: UserInteraction?,
        context: UXContext,
        profile: UserPreferenceProfile
    ): Boolean {
        if (!isActive) return false

        // Check cooldown
        val currentTime = System.currentTimeMillis()
        if (cooldownMs > 0 && currentTime - lastExecutionTime < cooldownMs) {
            return false
        }

        // Check execution limits
        if (maxExecutions != null && executionCount >= maxExecutions) {
            return false
        }

        // Evaluate all conditions
        return conditions.all { condition ->
            condition.evaluate(interaction, context, profile)
        }
    }

    /**
     * Generate a personalized reaction based on this rule
     */
    fun generateReaction(
        interaction: UserInteraction?,
        context: UXContext,
        profile: UserPreferenceProfile
    ): PersonalizedReaction? {
        if (!shouldApply(interaction, context, profile)) {
            return null
        }

        try {
            // Execute actions to generate reaction
            val reactions = actions.mapNotNull { action ->
                action.execute(interaction, context, profile)
            }.flatten()

            if (reactions.isEmpty()) return null

            // Use the first reaction (could be enhanced to select best reaction)
            val reaction = reactions.first()

            // Update execution tracking
            executionCount++
            lastExecutionTime = System.currentTimeMillis()

            return reaction

        } catch (e: Exception) {
            // Log error and return null
            return null
        }
    }

    /**
     * Get rule statistics
     */
    fun getStatistics(): RuleStatistics {
        return RuleStatistics(
            executionCount = executionCount,
            lastExecutionTime = lastExecutionTime,
            successRate = if (executionCount > 0) 1.0 else 0.0 // Simplified
        )
    }

    /**
     * Reset execution tracking
     */
    fun reset() {
        executionCount = 0
        lastExecutionTime = 0L
    }
}

/**
 * Types of personalization rules
 */
enum class PersonalizationRuleType {
    PREFERENCE_BASED, BEHAVIORAL, TEMPORAL, CONTEXTUAL, ENGAGEMENT_BASED, ADAPTIVE
}

/**
 * Conditions that must be met for a personalization rule to apply
 */
sealed class RuleCondition {

    abstract fun evaluate(
        interaction: UserInteraction?,
        context: UXContext,
        profile: UserPreferenceProfile
    ): Boolean

    /**
     * Condition based on user preferences
     */
    data class PreferenceCondition(
        val preferenceFactors: Map<String, String>
    ) : RuleCondition() {
        override fun evaluate(
            interaction: UserInteraction?,
            context: UXContext,
            profile: UserPreferenceProfile
        ): Boolean {
            return preferenceFactors.all { (key, expectedValue) ->
                val actualValue = profile.interactionPreferences[key]?.toString()
                actualValue == expectedValue || expectedValue == "any"
            }
        }
    }

    /**
     * Condition based on user behavior patterns
     */
    data class BehaviorCondition(
        val requiredPatterns: List<String>,
        val minConfidence: Double = 0.5
    ) : RuleCondition() {
        override fun evaluate(
            interaction: UserInteraction?,
            context: UXContext,
            profile: UserPreferenceProfile
        ): Boolean {
            return requiredPatterns.all { pattern ->
                val patternConfidence = profile.behavioralTraits["pattern_${pattern}_confidence"] ?: 0.0
                patternConfidence >= minConfidence
            }
        }
    }

    /**
     * Condition based on contextual factors
     */
    data class ContextCondition(
        val contextFactors: Map<String, String>
    ) : RuleCondition() {
        override fun evaluate(
            interaction: UserInteraction?,
            context: UXContext,
            profile: UserPreferenceProfile
        ): Boolean {
            return contextFactors.all { (key, expectedValue) ->
                val actualValue = when (key) {
                    "timeOfDay" -> context.environmentalFactors.timeOfDay.name
                    "lightingCondition" -> context.environmentalFactors.lightingCondition.name
                    "locationContext" -> context.environmentalFactors.locationContext.name
                    "gameState" -> context.currentGameState?.let { "active" } ?: "inactive"
                    "networkQuality" -> context.environmentalFactors.networkQuality.name
                    else -> profile.contextualPreferences[key]
                }
                actualValue == expectedValue || expectedValue == "any"
            }
        }
    }

    /**
     * Condition based on user engagement level
     */
    data class EngagementCondition(
        val minEngagementScore: Double,
        val maxEngagementScore: Double? = null
    ) : RuleCondition() {
        override fun evaluate(
            interaction: UserInteraction?,
            context: UXContext,
            profile: UserPreferenceProfile
        ): Boolean {
            val engagement = profile.engagementLevel
            return engagement >= minEngagementScore &&
                   (maxEngagementScore == null || engagement <= maxEngagementScore)
        }
    }
}

/**
 * Actions to perform when a personalization rule triggers
 */
sealed class RuleAction {

    abstract fun execute(
        interaction: UserInteraction?,
        context: UXContext,
        profile: UserPreferenceProfile
    ): List<PersonalizedReaction>

    /**
     * Generate adaptive content based on user profile
     */
    data class GenerateAdaptiveContent(
        val contentType: PersonalizedReaction.ContentType,
        val baseContent: Map<String, Any>,
        val personalizationFactors: List<String>
    ) : RuleAction() {
        override fun execute(
            interaction: UserInteraction?,
            context: UXContext,
            profile: UserPreferenceProfile
        ): List<PersonalizedReaction> {
            val personalizedContent = personalizeContent(baseContent, profile, personalizationFactors)

            return listOf(
                PersonalizedReaction(
                    id = "adaptive_${System.currentTimeMillis()}",
                    type = Reaction.Type.PERSONALIZED_CONTENT,
                    priority = Reaction.Priority.MEDIUM,
                    title = personalizedContent["title"] as? String ?: "Personalized Content",
                    description = personalizedContent["description"] as? String ?: "",
                    confidence = calculateContentConfidence(personalizedContent, profile),
                    userContext = profile.interactionPreferences,
                    adaptiveContent = PersonalizedReaction.AdaptiveContent(
                        contentType = contentType,
                        contentData = personalizedContent,
                        personalizationFactors = personalizationFactors,
                        estimatedDuration = personalizedContent["duration"] as? Long ?: 3000L
                    ),
                    triggerCondition = PersonalizedReaction.TriggerCondition(
                        event = interaction?.type?.name ?: "unknown",
                        delay = 0
                    )
                )
            )
        }

        private fun personalizeContent(
            baseContent: Map<String, Any>,
            profile: UserPreferenceProfile,
            factors: List<String>
        ): Map<String, Any> {
            val personalized = baseContent.toMutableMap()

            for (factor in factors) {
                when (factor) {
                    "theme" -> {
                        val userTheme = profile.interactionPreferences["theme"] as? String ?: "auto"
                        personalized["theme"] = userTheme
                    }
                    "animationSpeed" -> {
                        val animationSpeed = profile.interactionPreferences["animationSpeed"] as? String ?: "normal"
                        personalized["animationSpeed"] = animationSpeed
                    }
                    "language" -> {
                        val language = profile.interactionPreferences["language"] as? String ?: "en"
                        personalized["language"] = language
                    }
                    "engagementLevel" -> {
                        personalized["engagementLevel"] = profile.engagementLevel
                    }
                }
            }

            return personalized
        }

        private fun calculateContentConfidence(
            content: Map<String, Any>,
            profile: UserPreferenceProfile
        ): Double {
            var confidence = 0.5 // Base confidence

            // Increase confidence based on personalization factors used
            val personalizationFactors = content["personalizationFactors"] as? List<String> ?: emptyList()
            confidence += personalizationFactors.size * 0.1

            // Factor in user engagement level
            confidence += profile.engagementLevel * 0.2

            return confidence.coerceIn(0.0, 1.0)
        }
    }

    /**
     * Modify existing reaction based on user preferences
     */
    data class ModifyReaction(
        val reactionId: String,
        val modifications: Map<String, Any>
    ) : RuleAction() {
        override fun execute(
            interaction: UserInteraction?,
            context: UXContext,
            profile: UserPreferenceProfile
        ): List<PersonalizedReaction> {
            // This would modify existing reactions - placeholder for now
            return emptyList()
        }
    }
}

/**
 * Statistics for personalization rule execution
 */
data class RuleStatistics(
    val executionCount: Int,
    val lastExecutionTime: Long,
    val successRate: Double,
    val averageExecutionTimeMs: Double = 0.0,
    val errorCount: Int = 0
)

/**
 * Behavior analysis insights
 */
data class BehaviorInsights(
    val interactionPatterns: Map<String, Double> = emptyMap(),
    val temporalPreferences: Map<Int, Double> = emptyMap(),
    val engagementMetrics: Map<String, Double> = emptyMap(),
    val adaptationScore: Double = 0.0,
    val confidence: Double = 0.0
)

/**
 * Context analysis insights
 */
data class ContextInsights(
    val environmentalFactors: Map<String, Double> = emptyMap(),
    val situationalAwareness: Map<String, String> = emptyMap(),
    val contextualRelevance: Double = 0.0,
    val timingOptimization: Long = 0
)

/**
 * Personalization configuration
 */
data class PersonalizationConfig(
    val enableAdaptiveLearning: Boolean = true,
    val learningRate: Double = 0.1,
    val minConfidenceThreshold: Double = 0.5,
    val maxRulesPerUser: Int = 50,
    val enableContextualAdaptation: Boolean = true,
    val enableBehavioralPrediction: Boolean = true,
    val enablePreferenceEvolution: Boolean = true,
    val adaptationSensitivity: Double = 0.7
)

/**
 * Personalization result
 */
data class PersonalizationResult(
    val personalizedReactions: List<PersonalizedReaction>,
    val appliedRules: List<String>,
    val personalizationScore: Double,
    val adaptationLevel: Double,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)