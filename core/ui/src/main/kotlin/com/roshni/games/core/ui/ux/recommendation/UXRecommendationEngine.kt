package com.roshni.games.core.ui.ux.recommendation

import com.roshni.games.core.ui.ux.engine.EnhancementFeedback
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Interface for UX recommendation engine
 */
interface UXRecommendationEngine {

    /**
     * Get personalized UX enhancement recommendations
     */
    suspend fun getRecommendations(
        context: UXContext,
        interaction: UserInteraction?,
        limit: Int = 10
    ): List<UXEnhancement>

    /**
     * Update user context for better personalization
     */
    suspend fun updateUserContext(context: UXContext)

    /**
     * Record user feedback for an enhancement to improve future recommendations
     */
    suspend fun recordFeedback(enhancementId: String, feedback: EnhancementFeedback)

    /**
     * Get recommendation statistics
     */
    suspend fun getRecommendationStatistics(): Map<String, Any>

    /**
     * Train the recommendation model with historical data
     */
    suspend fun trainModel()

    /**
     * Clear all user data and reset the model
     */
    suspend fun clearUserData()

    /**
     * Observe recommendation updates
     */
    fun observeRecommendations(): Flow<List<UXEnhancement>>
}

/**
 * User profile for personalization
 */
data class UserProfile(
    val userId: String,
    val preferences: MutableMap<String, Double> = mutableMapOf(),
    val behaviorPatterns: MutableMap<String, Double> = mutableMapOf(),
    val enhancementRatings: MutableMap<String, Double> = mutableMapOf(),
    val interactionHistory: MutableList<UserInteraction> = mutableListOf(),
    val feedbackHistory: MutableList<EnhancementFeedback> = mutableListOf(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Enhancement scoring factors
 */
data class EnhancementScore(
    val enhancementId: String,
    val baseScore: Double,
    val personalizationScore: Double,
    val contextScore: Double,
    val performanceScore: Double,
    val finalScore: Double,
    val factors: Map<String, Double> = emptyMap()
)

/**
 * Default implementation of UX recommendation engine using collaborative filtering and content-based approaches
 */
class UXRecommendationEngineImpl(
    private val enhancementCatalog: List<UXEnhancement> = emptyList()
) : UXRecommendationEngine {

    private val mutex = Mutex()
    private val userProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    private val recommendations = MutableStateFlow<List<UXEnhancement>>(emptyList())

    private val enhancementPerformance = MutableStateFlow<Map<String, EnhancementMetrics>>(emptyMap())

    // Model parameters
    private var learningRate = 0.01
    private var explorationRate = 0.1
    private var decayFactor = 0.95

    override suspend fun getRecommendations(
        context: UXContext,
        interaction: UserInteraction?,
        limit: Int
    ): List<UXEnhancement> = mutex.withLock {
        try {
            val userId = context.userId ?: return emptyList()

            // Get or create user profile
            val userProfile = getOrCreateUserProfile(userId)

            // Score all available enhancements
            val scoredEnhancements = enhancementCatalog.mapNotNull { enhancement ->
                scoreEnhancement(enhancement, context, interaction, userProfile)
                    ?.let { score -> enhancement to score }
            }

            // Sort by final score and return top recommendations
            val topEnhancements = scoredEnhancements
                .sortedByDescending { it.second.finalScore }
                .take(limit)
                .map { it.first }

            recommendations.value = topEnhancements

            Timber.d("Generated ${topEnhancements.size} recommendations for user $userId")
            topEnhancements

        } catch (e: Exception) {
            Timber.e(e, "Failed to generate recommendations")
            emptyList()
        }
    }

    override suspend fun updateUserContext(context: UXContext) = mutex.withLock {
        try {
            val userId = context.userId ?: return
            val userProfile = getOrCreateUserProfile(userId)

            // Update user preferences based on context
            updateUserPreferences(userProfile, context)

            // Update behavior patterns
            updateBehaviorPatterns(userProfile, context)

            // Update profile timestamp
            userProfile.lastUpdated = System.currentTimeMillis()

            userProfiles.value = userProfiles.value + (userId to userProfile)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update user context")
        }
    }

    override suspend fun recordFeedback(enhancementId: String, feedback: EnhancementFeedback) = mutex.withLock {
        try {
            // Update enhancement performance metrics
            val currentMetrics = enhancementPerformance.value[enhancementId] ?: EnhancementMetrics()
            val updatedMetrics = currentMetrics.copy(
                totalRatings = currentMetrics.totalRatings + 1,
                averageRating = ((currentMetrics.averageRating * currentMetrics.totalRatings) + feedback.rating) /
                              (currentMetrics.totalRatings + 1),
                helpfulCount = currentMetrics.helpfulCount + if (feedback.helpful) 1 else 0,
                lastFeedbackTime = System.currentTimeMillis()
            )
            enhancementPerformance.value = enhancementPerformance.value + (enhancementId to updatedMetrics)

            // Update user profile if user ID is available in feedback context
            feedback.context["userId"]?.let { userId ->
                val userProfile = getOrCreateUserProfile(userId.toString())
                userProfile.enhancementRatings[enhancementId] = feedback.rating.toDouble()
                userProfile.feedbackHistory.add(feedback)
                userProfiles.value = userProfiles.value + (userId.toString() to userProfile)
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to record feedback for enhancement $enhancementId")
        }
    }

    override suspend fun getRecommendationStatistics(): Map<String, Any> {
        val profiles = userProfiles.value
        val performance = enhancementPerformance.value

        return mapOf(
            "totalUsers" to profiles.size,
            "totalEnhancements" to enhancementCatalog.size,
            "averageRatings" to performance.values.map { it.averageRating }.average(),
            "totalFeedbackCount" to performance.values.sumOf { it.totalRatings },
            "mostRatedEnhancement" to performance.maxByOrNull { it.value.totalRatings }?.key,
            "bestRatedEnhancement" to performance.maxByOrNull { it.value.averageRating }?.key,
            "modelParameters" to mapOf(
                "learningRate" to learningRate,
                "explorationRate" to explorationRate,
                "decayFactor" to decayFactor
            )
        )
    }

    override suspend fun trainModel() = mutex.withLock {
        try {
            // Simple model training - in a real implementation this would use ML algorithms
            val profiles = userProfiles.value.values

            if (profiles.isEmpty()) return

            // Update learning parameters based on performance
            val avgRating = enhancementPerformance.value.values.map { it.averageRating }.average()
            if (avgRating > 4.0) {
                learningRate = min(learningRate * 1.1, 0.1)
            } else if (avgRating < 3.0) {
                learningRate = max(learningRate * 0.9, 0.001)
            }

            // Decay exploration rate
            explorationRate *= decayFactor

            Timber.d("Model training completed. Learning rate: $learningRate, Exploration rate: $explorationRate")

        } catch (e: Exception) {
            Timber.e(e, "Failed to train recommendation model")
        }
    }

    override suspend fun clearUserData() = mutex.withLock {
        userProfiles.value = emptyMap()
        enhancementPerformance.value = emptyMap()
        recommendations.value = emptyList()
    }

    override fun observeRecommendations(): Flow<List<UXEnhancement>> = recommendations.asStateFlow()

    /**
     * Get or create user profile
     */
    private fun getOrCreateUserProfile(userId: String): UserProfile {
        return userProfiles.value[userId] ?: UserProfile(userId)
    }

    /**
     * Score an enhancement for a user in a given context
     */
    private fun scoreEnhancement(
        enhancement: UXEnhancement,
        context: UXContext,
        interaction: UserInteraction?,
        userProfile: UserProfile
    ): EnhancementScore? {
        return try {
            val baseScore = calculateBaseScore(enhancement, context)
            val personalizationScore = calculatePersonalizationScore(enhancement, userProfile, context)
            val contextScore = calculateContextScore(enhancement, context, interaction)
            val performanceScore = calculatePerformanceScore(enhancement)

            // Weighted combination
            val finalScore = (
                baseScore * 0.2 +
                personalizationScore * 0.4 +
                contextScore * 0.3 +
                performanceScore * 0.1
            )

            // Apply exploration factor for diversity
            val explorationFactor = if (Math.random() < explorationRate) {
                Math.random() * 0.3 - 0.15 // -0.15 to +0.15
            } else 0.0

            EnhancementScore(
                enhancementId = enhancement.id,
                baseScore = baseScore,
                personalizationScore = personalizationScore,
                contextScore = contextScore,
                performanceScore = performanceScore,
                finalScore = finalScore + explorationFactor,
                factors = mapOf(
                    "baseScore" to baseScore,
                    "personalizationScore" to personalizationScore,
                    "contextScore" to contextScore,
                    "performanceScore" to performanceScore,
                    "explorationFactor" to explorationFactor
                )
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to score enhancement ${enhancement.id}")
            null
        }
    }

    /**
     * Calculate base score for an enhancement
     */
    private fun calculateBaseScore(enhancement: UXEnhancement, context: UXContext): Double {
        var score = 0.5 // Base score

        // Priority bonus
        score += when (enhancement.priority) {
            UXEnhancement.Priority.LOW -> 0.0
            UXEnhancement.Priority.MEDIUM -> 0.1
            UXEnhancement.Priority.HIGH -> 0.2
            UXEnhancement.Priority.CRITICAL -> 0.3
        }

        // Type preference based on context
        when (enhancement.type) {
            UXEnhancement.Type.ACCESSIBILITY_AID -> {
                if (context.userPreferences.accessibilityProfile != UXContext.AccessibilityProfile.STANDARD) {
                    score += 0.2
                }
            }
            UXEnhancement.Type.AUDIO_FEEDBACK -> {
                if (context.userPreferences.soundEnabled) score += 0.1
            }
            UXEnhancement.Type.HAPTIC_FEEDBACK -> {
                if (context.userPreferences.hapticFeedbackEnabled) score += 0.1
            }
            UXEnhancement.Type.VISUAL_FEEDBACK -> {
                if (!context.userPreferences.reducedMotion) score += 0.1
            }
            else -> {}
        }

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate personalization score based on user profile
     */
    private fun calculatePersonalizationScore(
        enhancement: UXEnhancement,
        userProfile: UserProfile,
        context: UXContext
    ): Double {
        var score = 0.5

        // Direct rating from user history
        userProfile.enhancementRatings[enhancement.id]?.let { rating ->
            score = rating / 5.0
        }

        // Preference similarity
        val preferenceMatches = countPreferenceMatches(enhancement, context.userPreferences)
        score += (preferenceMatches.toDouble() / 10.0) * 0.2

        // Behavior pattern alignment
        val behaviorAlignment = calculateBehaviorAlignment(enhancement, userProfile)
        score += behaviorAlignment * 0.3

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate context relevance score
     */
    private fun calculateContextScore(
        enhancement: UXEnhancement,
        context: UXContext,
        interaction: UserInteraction?
    ): Double {
        var score = 0.5

        // Check if enhancement conditions are met by context
        val conditionsMet = enhancement.conditions.count { it.evaluateCondition(context) }
        val totalConditions = enhancement.conditions.size
        if (totalConditions > 0) {
            score += (conditionsMet.toDouble() / totalConditions) * 0.4
        }

        // Interaction type relevance
        interaction?.let { intr ->
            val typeRelevance = when (enhancement.type) {
                UXEnhancement.Type.VISUAL_FEEDBACK -> when (intr.type) {
                    UserInteraction.InteractionType.TAP, UserInteraction.InteractionType.SWIPE -> 0.8
                    else -> 0.4
                }
                UXEnhancement.Type.AUDIO_FEEDBACK -> when (intr.type) {
                    UserInteraction.InteractionType.VOICE_COMMAND -> 0.9
                    UserInteraction.InteractionType.BUTTON_CLICK -> 0.7
                    else -> 0.3
                }
                UXEnhancement.Type.HAPTIC_FEEDBACK -> when (intr.type) {
                    UserInteraction.InteractionType.TAP, UserInteraction.InteractionType.BUTTON_CLICK -> 0.8
                    else -> 0.4
                }
                UXEnhancement.Type.CONTEXTUAL_HELP -> when (intr.type) {
                    UserInteraction.InteractionType.SEARCH, UserInteraction.InteractionType.NAVIGATION -> 0.8
                    else -> 0.5
                }
                else -> 0.5
            }
            score += typeRelevance * 0.3
        }

        // Environmental factor relevance
        val envScore = calculateEnvironmentalRelevance(enhancement, context)
        score += envScore * 0.3

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate performance-based score
     */
    private fun calculatePerformanceScore(enhancement: UXEnhancement): Double {
        val metrics = enhancementPerformance.value[enhancement.id] ?: return 0.5

        var score = 0.5

        // Average rating factor
        score += (metrics.averageRating / 5.0) * 0.4

        // Usage frequency factor
        score += min(metrics.totalRatings / 100.0, 1.0) * 0.3

        // Helpfulness factor
        if (metrics.totalRatings > 0) {
            val helpfulness = metrics.helpfulCount.toDouble() / metrics.totalRatings
            score += helpfulness * 0.3
        }

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Count how many user preferences match enhancement requirements
     */
    private fun countPreferenceMatches(
        enhancement: UXEnhancement,
        preferences: UXContext.UserPreferences
    ): Int {
        var matches = 0

        // This would analyze enhancement conditions against user preferences
        // Simplified implementation
        if (enhancement.type == UXEnhancement.Type.AUDIO_FEEDBACK && preferences.soundEnabled) {
            matches++
        }
        if (enhancement.type == UXEnhancement.Type.HAPTIC_FEEDBACK && preferences.hapticFeedbackEnabled) {
            matches++
        }
        if (enhancement.type == UXEnhancement.Type.VISUAL_FEEDBACK && !preferences.reducedMotion) {
            matches++
        }

        return matches
    }

    /**
     * Calculate how well enhancement aligns with user behavior patterns
     */
    private fun calculateBehaviorAlignment(
        enhancement: UXEnhancement,
        userProfile: UserProfile
    ): Double {
        // Simplified behavior alignment calculation
        var alignment = 0.5

        // Check if user has positively rated similar enhancements
        val similarEnhancements = userProfile.enhancementRatings.keys.filter { id ->
            val rating = userProfile.enhancementRatings[id] ?: 0.0
            rating > 3.0 && id.contains(enhancement.type.name)
        }

        if (similarEnhancements.isNotEmpty()) {
            val avgRating = similarEnhancements.map { userProfile.enhancementRatings[it] ?: 0.0 }.average()
            alignment += (avgRating / 5.0) * 0.3
        }

        return alignment.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate environmental relevance
     */
    private fun calculateEnvironmentalRelevance(
        enhancement: UXEnhancement,
        context: UXContext
    ): Double {
        val env = context.environmentalFactors
        var relevance = 0.5

        // Adjust based on environmental factors
        when (enhancement.type) {
            UXEnhancement.Type.AUDIO_FEEDBACK -> {
                if (env.noiseLevel == UXContext.NoiseLevel.LOUD) relevance -= 0.2
                if (env.timeOfDay == UXContext.TimeOfDay.NIGHT) relevance -= 0.1
            }
            UXEnhancement.Type.HAPTIC_FEEDBACK -> {
                if (env.isInMotion) relevance += 0.2
            }
            UXEnhancement.Type.VISUAL_FEEDBACK -> {
                if (env.lightingCondition == UXContext.LightingCondition.DARK) relevance += 0.1
            }
            else -> {}
        }

        return relevance.coerceIn(0.0, 1.0)
    }

    /**
     * Update user preferences based on context
     */
    private fun updateUserPreferences(userProfile: UserProfile, context: UXContext) {
        val preferences = context.userPreferences

        // Update preference weights based on context
        userProfile.preferences["theme_${preferences.theme.name}"] =
            (userProfile.preferences["theme_${preferences.theme.name}"] ?: 0.0) + 0.1

        userProfile.preferences["animationSpeed_${preferences.animationSpeed.name}"] =
            (userProfile.preferences["animationSpeed_${preferences.animationSpeed.name}"] ?: 0.0) + 0.1

        if (preferences.soundEnabled) {
            userProfile.preferences["soundEnabled"] =
                (userProfile.preferences["soundEnabled"] ?: 0.0) + 0.1
        }

        if (preferences.hapticFeedbackEnabled) {
            userProfile.preferences["hapticFeedbackEnabled"] =
                (userProfile.preferences["hapticFeedbackEnabled"] ?: 0.0) + 0.1
        }
    }

    /**
     * Update behavior patterns based on context
     */
    private fun updateBehaviorPatterns(userProfile: UserProfile, context: UXContext) {
        val gameState = context.currentGameState
        val env = context.environmentalFactors

        // Update game-related behavior patterns
        gameState?.let { state ->
            userProfile.behaviorPatterns["difficulty_${state.difficulty.name}"] =
                (userProfile.behaviorPatterns["difficulty_${state.difficulty.name}"] ?: 0.0) + 0.1

            userProfile.behaviorPatterns["gameMode_${state.gameMode}"] =
                (userProfile.behaviorPatterns["gameMode_${state.gameMode}"] ?: 0.0) + 0.1
        }

        // Update environmental behavior patterns
        userProfile.behaviorPatterns["timeOfDay_${env.timeOfDay.name}"] =
            (userProfile.behaviorPatterns["timeOfDay_${env.timeOfDay.name}"] ?: 0.0) + 0.05

        userProfile.behaviorPatterns["location_${env.locationContext.name}"] =
            (userProfile.behaviorPatterns["location_${env.locationContext.name}"] ?: 0.0) + 0.05
    }
}

/**
 * Enhancement performance metrics
 */
data class EnhancementMetrics(
    val totalRatings: Int = 0,
    val averageRating: Double = 0.0,
    val helpfulCount: Int = 0,
    val usageCount: Int = 0,
    val lastFeedbackTime: Long = 0L,
    val successRate: Double = 0.0
)

/**
 * Factory for creating recommendation engines
 */
object UXRecommendationEngineFactory {

    fun create(
        enhancementCatalog: List<UXEnhancement> = emptyList()
    ): UXRecommendationEngine {
        return UXRecommendationEngineImpl(enhancementCatalog)
    }

    fun createWithDefaultCatalog(): UXRecommendationEngine {
        // Create default enhancement catalog
        val defaultEnhancements = listOf(
            // Visual feedback enhancements
            UXEnhancement.VisualFeedback(
                id = "visual_button_press",
                animationType = UXEnhancement.VisualFeedback.AnimationType.SCALE_UP,
                priority = UXEnhancement.Priority.MEDIUM
            ),
            UXEnhancement.VisualFeedback(
                id = "visual_success_animation",
                animationType = UXEnhancement.VisualFeedback.AnimationType.BOUNCE,
                priority = UXEnhancement.Priority.HIGH
            ),

            // Audio feedback enhancements
            UXEnhancement.AudioFeedback(
                id = "audio_button_click",
                soundType = UXEnhancement.AudioFeedback.SoundType.CLICK,
                priority = UXEnhancement.Priority.LOW
            ),
            UXEnhancement.AudioFeedback(
                id = "audio_success_sound",
                soundType = UXEnhancement.AudioFeedback.SoundType.SUCCESS,
                priority = UXEnhancement.Priority.MEDIUM
            ),

            // Haptic feedback enhancements
            UXEnhancement.HapticFeedback(
                id = "haptic_button_press",
                pattern = UXEnhancement.HapticFeedback.HapticPattern.LIGHT_TICK,
                priority = UXEnhancement.Priority.MEDIUM
            ),
            UXEnhancement.HapticFeedback(
                id = "haptic_error_feedback",
                pattern = UXEnhancement.HapticFeedback.HapticPattern.ERROR_PATTERN,
                priority = UXEnhancement.Priority.HIGH
            ),

            // Contextual help enhancements
            UXEnhancement.ContextualHelp(
                id = "contextual_first_time_help",
                helpType = UXEnhancement.ContextualHelp.HelpType.ONBOARDING_STEP,
                title = "Getting Started",
                message = "Welcome to the game! Here's how to play...",
                triggerCondition = UXEnhancement.ContextualHelp.TriggerCondition("FIRST_LAUNCH"),
                priority = UXEnhancement.Priority.HIGH
            )
        )

        return UXRecommendationEngineImpl(defaultEnhancements)
    }
}

// Extension function to evaluate conditions (placeholder for the actual implementation)
private fun UXEnhancement.EnhancementCondition.evaluateCondition(context: UXContext): Boolean {
    // This would contain the actual condition evaluation logic
    // For now, return true as a placeholder
    return true
}