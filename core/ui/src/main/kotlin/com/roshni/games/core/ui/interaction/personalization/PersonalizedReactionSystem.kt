package com.roshni.games.core.ui.interaction.personalization

import com.roshni.games.core.ui.interaction.InteractionResponse
import com.roshni.games.core.ui.interaction.PersonalizedReaction
import com.roshni.games.core.ui.interaction.Reaction
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Interface for generating personalized reactions based on user behavior and preferences
 */
interface PersonalizedReactionSystem {

    /**
     * Current status of the personalization system
     */
    val status: StateFlow<PersonalizationStatus>

    /**
     * Whether the system is initialized and ready
     */
    val isReady: Boolean

    /**
     * Initialize the personalization system
     */
    suspend fun initialize(context: UXContext): Boolean

    /**
     * Generate personalized reactions for an interaction
     */
    suspend fun getPersonalizedReactions(
        interaction: UserInteraction?,
        context: UXContext,
        limit: Int = 10
    ): List<PersonalizedReaction>

    /**
     * Learn from user interaction and response to improve personalization
     */
    suspend fun learnFromInteraction(
        interaction: UserInteraction,
        response: InteractionResponse,
        context: UXContext
    )

    /**
     * Update user behavior model based on recent interactions
     */
    suspend fun updateUserBehaviorModel(context: UXContext)

    /**
     * Get personalization statistics
     */
    suspend fun getPersonalizationStatistics(): PersonalizationStatistics

    /**
     * Register a custom personalization rule
     */
    suspend fun registerPersonalizationRule(rule: PersonalizationRule): Boolean

    /**
     * Unregister a personalization rule
     */
    suspend fun unregisterPersonalizationRule(ruleId: String): Boolean

    /**
     * Get user preference profile
     */
    suspend fun getUserPreferenceProfile(userId: String): UserPreferenceProfile?

    /**
     * Update user preferences
     */
    suspend fun updateUserPreferences(
        userId: String,
        preferences: Map<String, Any>
    ): Boolean

    /**
     * Reset personalization data for a user
     */
    suspend fun resetUserProfile(userId: String)

    /**
     * Clear all personalization data
     */
    suspend fun reset()

    /**
     * Observe personalization updates
     */
    fun observePersonalizationUpdates(): Flow<PersonalizationUpdate>

    /**
     * Observe user profile changes
     */
    fun observeUserProfileChanges(): Flow<UserProfileChange>
}

/**
 * Status of the personalization system
 */
data class PersonalizationStatus(
    val isInitialized: Boolean = false,
    val totalUsersProfiled: Int = 0,
    val totalPersonalizedReactions: Long = 0,
    val averagePersonalizationScore: Double = 0.0,
    val lastActivityTime: Long? = null,
    val ruleCount: Int = 0,
    val memoryUsageKb: Long = 0
)

/**
 * Statistics about personalization effectiveness
 */
data class PersonalizationStatistics(
    val totalPersonalizedReactions: Long = 0,
    val averageConfidence: Double = 0.0,
    val userSatisfactionScore: Double = 0.0,
    val personalizationAccuracy: Double = 0.0,
    val mostEffectiveRuleTypes: List<String> = emptyList(),
    val userEngagementImprovement: Double = 0.0,
    val reactionDiversity: Double = 0.0,
    val adaptationRate: Double = 0.0
)

/**
 * Notification of personalization system updates
 */
data class PersonalizationUpdate(
    val type: UpdateType,
    val userId: String?,
    val ruleId: String?,
    val confidence: Double,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Types of personalization updates
 */
enum class UpdateType {
    RULE_TRIGGERED, PROFILE_UPDATED, PREFERENCE_CHANGED,
    BEHAVIOR_LEARNED, REACTION_GENERATED, MODEL_UPDATED
}

/**
 * Notification of user profile changes
 */
data class UserProfileChange(
    val userId: String,
    val changeType: ProfileChangeType,
    val oldProfile: UserPreferenceProfile?,
    val newProfile: UserPreferenceProfile,
    val timestamp: Long
)

/**
 * Types of profile changes
 */
enum class ProfileChangeType {
    PREFERENCES_UPDATED, BEHAVIOR_LEARNED, CONTEXT_CHANGED,
    INTERACTION_PATTERN_UPDATED, PERSONALIZATION_IMPROVED
}

/**
 * Default implementation of the Personalized Reaction System
 */
class PersonalizedReactionSystemImpl : PersonalizedReactionSystem {

    private val mutex = Mutex()

    private val _status = MutableStateFlow(PersonalizationStatus())
    override val status: StateFlow<PersonalizationStatus> = _status.asStateFlow()

    private val personalizationUpdates = MutableStateFlow<List<PersonalizationUpdate>>(emptyList())
    private val userProfileChanges = MutableStateFlow<List<UserProfileChange>>(emptyList())

    // Core data storage
    private val userProfiles = mutableMapOf<String, UserPreferenceProfile>()
    private val personalizationRules = MutableStateFlow<List<PersonalizationRule>>(emptyList())
    private val reactionHistory = mutableMapOf<String, MutableList<ReactionHistoryEntry>>()

    // Learning components
    private val behaviorAnalyzer = UserBehaviorAnalyzer()
    private val preferenceLearner = PreferenceLearner()
    private val contextAnalyzer = ContextAnalyzer()

    override val isReady: Boolean
        get() = _status.value.isInitialized

    override suspend fun initialize(context: UXContext): Boolean = mutex.withLock {
        try {
            Timber.d("Initializing Personalized Reaction System")

            _status.value = _status.value.copy(isInitialized = false)

            // Initialize with default personalization rules
            initializeDefaultRules()

            // Create initial user profile if user ID is available
            context.userId?.let { userId ->
                createInitialUserProfile(userId, context)
            }

            _status.value = _status.value.copy(
                isInitialized = true,
                lastActivityTime = System.currentTimeMillis(),
                ruleCount = personalizationRules.value.size
            )

            Timber.d("Personalized Reaction System initialized with ${personalizationRules.value.size} rules")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Personalized Reaction System")
            false
        }
    }

    override suspend fun getPersonalizedReactions(
        interaction: UserInteraction?,
        context: UXContext,
        limit: Int
    ): List<PersonalizedReaction> = mutex.withLock {
        val reactions = mutableListOf<PersonalizedReaction>()

        try {
            val userId = context.userId ?: "anonymous"

            // Get or create user profile
            val profile = getOrCreateUserProfile(userId, context)

            // Apply personalization rules
            for (rule in personalizationRules.value) {
                if (rule.shouldApply(interaction, context, profile)) {
                    val personalizedReaction = rule.generateReaction(interaction, context, profile)
                    if (personalizedReaction != null) {
                        reactions.add(personalizedReaction)

                        // Record the personalization update
                        val update = PersonalizationUpdate(
                            type = UpdateType.RULE_TRIGGERED,
                            userId = userId,
                            ruleId = rule.id,
                            confidence = personalizedReaction.confidence,
                            timestamp = System.currentTimeMillis()
                        )

                        personalizationUpdates.value = (personalizationUpdates.value + update).takeLast(100)

                        if (reactions.size >= limit) break
                    }
                }
            }

            // Sort by priority and confidence
            val sortedReactions = reactions.sortedWith(
                compareByDescending<PersonalizedReaction> { it.priority.value }
                    .thenByDescending { it.confidence }
            ).take(limit)

            // Update statistics
            if (sortedReactions.isNotEmpty) {
                _status.value = _status.value.copy(
                    totalPersonalizedReactions = _status.value.totalPersonalizedReactions + sortedReactions.size,
                    averagePersonalizationScore = sortedReactions.map { it.confidence }.average()
                )
            }

            Timber.d("Generated ${sortedReactions.size} personalized reactions for user $userId")

            sortedReactions

        } catch (e: Exception) {
            Timber.e(e, "Failed to generate personalized reactions")
            emptyList()
        }
    }

    override suspend fun learnFromInteraction(
        interaction: UserInteraction,
        response: InteractionResponse,
        context: UXContext
    ) {
        mutex.withLock {
            try {
                val userId = context.userId ?: "anonymous"

                // Get or create user profile
                val profile = getOrCreateUserProfile(userId, context)

                // Analyze behavior patterns
                val behaviorInsights = behaviorAnalyzer.analyzeInteraction(interaction, response, context)

                // Learn preferences from the interaction
                val preferenceUpdates = preferenceLearner.learnFromInteraction(interaction, response, context)

                // Analyze contextual factors
                val contextInsights = contextAnalyzer.analyzeContext(interaction, context)

                // Update user profile with new insights
                val updatedProfile = profile.updateWithInsights(
                    behaviorInsights = behaviorInsights,
                    preferenceUpdates = preferenceUpdates,
                    contextInsights = contextInsights
                )

                userProfiles[userId] = updatedProfile

                // Record reaction history
                val historyEntry = ReactionHistoryEntry(
                    interaction = interaction,
                    response = response,
                    context = context,
                    profile = updatedProfile,
                    timestamp = System.currentTimeMillis()
                )

                reactionHistory.getOrPut(userId) { mutableListOf() }.add(historyEntry)

                // Keep only recent history (last 500 entries per user)
                if (reactionHistory[userId]?.size ?: 0 > 500) {
                    reactionHistory[userId]?.removeAt(0)
                }

                // Notify profile change
                val profileChange = UserProfileChange(
                    userId = userId,
                    changeType = ProfileChangeType.BEHAVIOR_LEARNED,
                    oldProfile = profile,
                    newProfile = updatedProfile,
                    timestamp = System.currentTimeMillis()
                )

                userProfileChanges.value = (userProfileChanges.value + profileChange).takeLast(100)

                Timber.d("Learned from interaction for user $userId")

            } catch (e: Exception) {
                Timber.e(e, "Failed to learn from interaction")
            }
        }
    }

    override suspend fun updateUserBehaviorModel(context: UXContext) {
        mutex.withLock {
            try {
                val userId = context.userId ?: "anonymous"
                val profile = getOrCreateUserProfile(userId, context)

                // Update behavior model based on recent history
                val recentHistory = reactionHistory[userId]?.takeLast(50) ?: emptyList()

                if (recentHistory.isNotEmpty()) {
                    val updatedProfile = behaviorAnalyzer.updateBehaviorModel(profile, recentHistory)
                    userProfiles[userId] = updatedProfile

                    val profileChange = UserProfileChange(
                        userId = userId,
                        changeType = ProfileChangeType.MODEL_UPDATED,
                        oldProfile = profile,
                        newProfile = updatedProfile,
                        timestamp = System.currentTimeMillis()
                    )

                    userProfileChanges.value = (userProfileChanges.value + profileChange).takeLast(100)
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to update user behavior model")
            }
        }
    }

    override suspend fun getPersonalizationStatistics(): PersonalizationStatistics {
        val allReactions = reactionHistory.values.flatten()
        val allRules = personalizationRules.value

        val ruleTypeStats = allRules.groupBy { it.type }
            .mapValues { it.value.size }

        val mostEffectiveTypes = ruleTypeStats.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key.name }

        return PersonalizationStatistics(
            totalPersonalizedReactions = allReactions.size.toLong(),
            averageConfidence = if (allReactions.isNotEmpty()) {
                allReactions.mapNotNull { it.response.personalizedReactions.firstOrNull()?.confidence }.average()
            } else 0.0,
            userSatisfactionScore = calculateUserSatisfactionScore(allReactions),
            personalizationAccuracy = calculatePersonalizationAccuracy(allReactions),
            mostEffectiveRuleTypes = mostEffectiveTypes,
            userEngagementImprovement = calculateEngagementImprovement(allReactions),
            reactionDiversity = calculateReactionDiversity(allReactions),
            adaptationRate = calculateAdaptationRate(allReactions)
        )
    }

    override suspend fun registerPersonalizationRule(rule: PersonalizationRule): Boolean = mutex.withLock {
        try {
            // Check if rule already exists
            if (personalizationRules.value.any { it.id == rule.id }) {
                return false
            }

            // Validate rule
            if (!isValidRule(rule)) {
                return false
            }

            personalizationRules.value = personalizationRules.value + rule

            _status.value = _status.value.copy(ruleCount = personalizationRules.value.size)

            Timber.d("Registered personalization rule ${rule.id}")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to register personalization rule ${rule.id}")
            false
        }
    }

    override suspend fun unregisterPersonalizationRule(ruleId: String): Boolean = mutex.withLock {
        try {
            val initialSize = personalizationRules.value.size
            personalizationRules.value = personalizationRules.value.filter { it.id != ruleId }

            val success = personalizationRules.value.size < initialSize
            if (success) {
                _status.value = _status.value.copy(ruleCount = personalizationRules.value.size)
                Timber.d("Unregistered personalization rule $ruleId")
            }

            success

        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister personalization rule $ruleId")
            false
        }
    }

    override suspend fun getUserPreferenceProfile(userId: String): UserPreferenceProfile? {
        return userProfiles[userId]
    }

    override suspend fun updateUserPreferences(
        userId: String,
        preferences: Map<String, Any>
    ): Boolean = mutex.withLock {
        try {
            val profile = userProfiles[userId] ?: return false
            val updatedProfile = profile.updatePreferences(preferences)
            userProfiles[userId] = updatedProfile

            val profileChange = UserProfileChange(
                userId = userId,
                changeType = ProfileChangeType.PREFERENCES_UPDATED,
                oldProfile = profile,
                newProfile = updatedProfile,
                timestamp = System.currentTimeMillis()
            )

            userProfileChanges.value = (userProfileChanges.value + profileChange).takeLast(100)

            Timber.d("Updated preferences for user $userId")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to update preferences for user $userId")
            false
        }
    }

    override suspend fun resetUserProfile(userId: String) {
        userProfiles.remove(userId)
        reactionHistory.remove(userId)

        Timber.d("Reset profile for user $userId")
    }

    override suspend fun reset() {
        userProfiles.clear()
        reactionHistory.clear()
        personalizationRules.value = emptyList()
        personalizationUpdates.value = emptyList()
        userProfileChanges.value = emptyList()

        _status.value = PersonalizationStatus()
    }

    override fun observePersonalizationUpdates(): Flow<PersonalizationUpdate> = personalizationUpdates

    override fun observeUserProfileChanges(): Flow<UserProfileChange> = userProfileChanges

    /**
     * Initialize with default personalization rules
     */
    private suspend fun initializeDefaultRules() {
        val defaultRules = listOf(
            // Time-based personalization
            PersonalizationRule(
                id = "time_based_greeting",
                name = "Time-based Greeting",
                description = "Show different greetings based on time of day",
                type = PersonalizationRuleType.TEMPORAL,
                conditions = listOf(
                    RuleCondition.ContextCondition(
                        contextFactors = mapOf("timeOfDay" to "any")
                    )
                ),
                priority = 2,
                confidence = 0.8
            ),

            // Behavior-based content adaptation
            PersonalizationRule(
                id = "behavior_based_content",
                name = "Behavior-based Content Adaptation",
                description = "Adapt content based on user interaction patterns",
                type = PersonalizationRuleType.BEHAVIORAL,
                conditions = listOf(
                    RuleCondition.BehaviorCondition(
                        requiredPatterns = listOf("completionist_behavior", "rapid_tapping")
                    )
                ),
                priority = 3,
                confidence = 0.7
            ),

            // Preference-based UI adaptation
            PersonalizationRule(
                id = "preference_based_ui",
                name = "Preference-based UI Adaptation",
                description = "Adapt UI elements based on user preferences",
                type = PersonalizationRuleType.PREFERENCE_BASED,
                conditions = listOf(
                    RuleCondition.PreferenceCondition(
                        preferenceFactors = mapOf("theme" to "any", "animationSpeed" to "any")
                    )
                ),
                priority = 2,
                confidence = 0.9
            ),

            // Contextual help personalization
            PersonalizationRule(
                id = "contextual_help",
                name = "Contextual Help Personalization",
                description = "Show contextual help based on user behavior",
                type = PersonalizationRuleType.CONTEXTUAL,
                conditions = listOf(
                    RuleCondition.ContextCondition(
                        contextFactors = mapOf("gameState" to "new_player", "interactionHistory" to "limited")
                    )
                ),
                priority = 3,
                confidence = 0.75
            ),

            // Engagement-based reaction timing
            PersonalizationRule(
                id = "engagement_timing",
                name = "Engagement-based Reaction Timing",
                description = "Time reactions based on user engagement level",
                type = PersonalizationRuleType.ENGAGEMENT_BASED,
                conditions = listOf(
                    RuleCondition.EngagementCondition(
                        minEngagementScore = 0.3,
                        maxEngagementScore = 0.9
                    )
                ),
                priority = 1,
                confidence = 0.6
            )
        )

        defaultRules.forEach { registerPersonalizationRule(it) }
    }

    /**
     * Get or create user profile
     */
    private fun getOrCreateUserProfile(userId: String, context: UXContext): UserPreferenceProfile {
        return userProfiles.getOrPut(userId) {
            createInitialUserProfile(userId, context)
        }
    }

    /**
     * Create initial user profile
     */
    private fun createInitialUserProfile(userId: String, context: UXContext): UserPreferenceProfile {
        return UserPreferenceProfile(
            userId = userId,
            interactionPreferences = context.userPreferences.let { prefs ->
                mapOf(
                    "theme" to prefs.theme.name,
                    "animationSpeed" to prefs.animationSpeed.name,
                    "soundEnabled" to prefs.soundEnabled,
                    "hapticFeedbackEnabled" to prefs.hapticFeedbackEnabled,
                    "reducedMotion" to prefs.reducedMotion,
                    "highContrast" to prefs.highContrast,
                    "largeText" to prefs.largeText
                )
            },
            behavioralTraits = emptyMap(),
            contextualPreferences = mapOf(
                "timeOfDay" to context.environmentalFactors.timeOfDay.name,
                "lightingCondition" to context.environmentalFactors.lightingCondition.name,
                "locationContext" to context.environmentalFactors.locationContext.name
            ),
            engagementLevel = 0.5, // Default moderate engagement
            personalizationScore = 0.0,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Validate that a personalization rule is properly configured
     */
    private fun isValidRule(rule: PersonalizationRule): Boolean {
        return rule.id.isNotBlank() &&
                rule.name.isNotBlank() &&
                rule.conditions.isNotEmpty()
    }

    /**
     * Calculate user satisfaction score based on reaction history
     */
    private fun calculateUserSatisfactionScore(reactions: List<ReactionHistoryEntry>): Double {
        if (reactions.isEmpty()) return 0.0

        // This would be calculated based on user feedback, engagement metrics, etc.
        // For now, return a placeholder based on reaction diversity and frequency
        val reactionTypes = reactions.flatMap { it.response.getAllReactions() }.map { it.type }
        val uniqueTypes = reactionTypes.distinct().size
        val totalReactions = reactionTypes.size

        return (uniqueTypes.toDouble() / totalReactions.coerceAtLeast(1)).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate personalization accuracy
     */
    private fun calculatePersonalizationAccuracy(reactions: List<ReactionHistoryEntry>): Double {
        if (reactions.isEmpty()) return 0.0

        // This would compare predicted reactions with actual user preferences
        // For now, return a placeholder based on confidence scores
        return reactions.mapNotNull { entry ->
            entry.response.personalizedReactions.firstOrNull()?.confidence
        }.average().coerceIn(0.0, 1.0)
    }

    /**
     * Calculate engagement improvement from personalized reactions
     */
    private fun calculateEngagementImprovement(reactions: List<ReactionHistoryEntry>): Double {
        if (reactions.isEmpty()) return 0.0

        // This would measure how personalized reactions improve user engagement
        // For now, return a placeholder based on reaction frequency
        val personalizedCount = reactions.sumOf { it.response.personalizedReactions.size }
        val totalReactions = reactions.sumOf { it.response.getAllReactions().size }

        return (personalizedCount.toDouble() / totalReactions.coerceAtLeast(1)).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate reaction diversity
     */
    private fun calculateReactionDiversity(reactions: List<ReactionHistoryEntry>): Double {
        if (reactions.isEmpty()) return 0.0

        val allTypes = reactions.flatMap { entry ->
            entry.response.getAllReactions().map { it.type }
        }

        val uniqueTypes = allTypes.distinct().size
        val totalTypes = Reaction.Type.values().size

        return uniqueTypes.toDouble() / totalTypes
    }

    /**
     * Calculate adaptation rate (how quickly the system learns and adapts)
     */
    private fun calculateAdaptationRate(reactions: List<ReactionHistoryEntry>): Double {
        if (reactions.size < 2) return 0.0

        // Measure how personalization scores improve over time
        val firstHalf = reactions.take(reactions.size / 2)
        val secondHalf = reactions.takeLast(reactions.size / 2)

        val firstHalfAvg = firstHalf.mapNotNull { entry ->
            entry.response.personalizedReactions.firstOrNull()?.confidence
        }.average()

        val secondHalfAvg = secondHalf.mapNotNull { entry ->
            entry.response.personalizedReactions.firstOrNull()?.confidence
        }.average()

        return ((secondHalfAvg - firstHalfAvg) / firstHalfAvg.coerceAtLeast(0.1)).coerceIn(-1.0, 1.0)
    }
}

/**
 * Data class to store reaction history for learning
 */
private data class ReactionHistoryEntry(
    val interaction: UserInteraction,
    val response: InteractionResponse,
    val context: UXContext,
    val profile: UserPreferenceProfile,
    val timestamp: Long
)

/**
 * Factory for creating Personalized Reaction System instances
 */
object PersonalizedReactionSystemFactory {

    fun create(): PersonalizedReactionSystem {
        return PersonalizedReactionSystemImpl()
    }
}