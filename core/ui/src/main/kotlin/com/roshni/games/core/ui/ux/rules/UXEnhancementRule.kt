package com.roshni.games.core.ui.ux.rules

import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Rule for determining when and how to apply UX enhancements
 */
data class UXEnhancementRule(
    val id: String,
    val name: String,
    val description: String,
    val category: RuleCategory,
    val priority: Int = 0,
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
    val isEnabled: Boolean = true,
    val validFrom: Long? = null,
    val validUntil: Long? = null,
    val maxExecutions: Int? = null,
    val cooldownMs: Long = 0,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
) {

    private var executionCount = 0
    private var lastExecutionTime = 0L

    /**
     * Categories of UX enhancement rules
     */
    enum class RuleCategory {
        ACCESSIBILITY, PERFORMANCE, PERSONALIZATION,
        CONTEXTUAL, BEHAVIORAL, ENVIRONMENTAL,
        GAME_SPECIFIC, USER_PREFERENCE, DEVICE_SPECIFIC,
        SAFETY, ONBOARDING, ENGAGEMENT
    }

    /**
     * Conditions that must be met for rule to apply
     */
    sealed class RuleCondition {
        abstract fun evaluate(context: UXContext, interaction: UserInteraction?): Boolean

        data class UserPreferenceCondition(
            val preference: String,
            val operator: ComparisonOperator,
            val value: Any
        ) : RuleCondition() {
            override fun evaluate(context: UXContext, interaction: UserInteraction?): Boolean {
                val preferences = context.userPreferences
                return when (preference) {
                    "theme" -> compare(preferences.theme.name, operator, value)
                    "animationSpeed" -> compare(preferences.animationSpeed.name, operator, value)
                    "soundEnabled" -> compare(preferences.soundEnabled, operator, value)
                    "hapticFeedbackEnabled" -> compare(preferences.hapticFeedbackEnabled, operator, value)
                    "reducedMotion" -> compare(preferences.reducedMotion, operator, value)
                    "highContrast" -> compare(preferences.highContrast, operator, value)
                    "largeText" -> compare(preferences.largeText, operator, value)
                    "language" -> compare(preferences.language, operator, value)
                    "accessibilityProfile" -> compare(preferences.accessibilityProfile.name, operator, value)
                    else -> false
                }
            }
        }

        data class DeviceCapabilityCondition(
            val capability: String,
            val operator: ComparisonOperator,
            val value: Any
        ) : RuleCondition() {
            override fun evaluate(context: UXContext, interaction: UserInteraction?): Boolean {
                val capabilities = context.deviceCapabilities
                return when (capability) {
                    "hasVibrator" -> compare(capabilities.hasVibrator, operator, value)
                    "hasSpeaker" -> compare(capabilities.hasSpeaker, operator, value)
                    "hasCamera" -> compare(capabilities.hasCamera, operator, value)
                    "screenRefreshRate" -> compare(capabilities.screenRefreshRate, operator, value)
                    "maxTextureSize" -> compare(capabilities.maxTextureSize, operator, value)
                    "supportsHDR" -> compare(capabilities.supportsHDR, operator, value)
                    else -> false
                }
            }
        }

        data class EnvironmentalCondition(
            val factor: String,
            val operator: ComparisonOperator,
            val value: Any
        ) : RuleCondition() {
            override fun evaluate(context: UXContext, interaction: UserInteraction?): Boolean {
                val env = context.environmentalFactors
                return when (factor) {
                    "timeOfDay" -> compare(env.timeOfDay.name, operator, value)
                    "lightingCondition" -> compare(env.lightingCondition.name, operator, value)
                    "noiseLevel" -> compare(env.noiseLevel.name, operator, value)
                    "batteryLevel" -> compare(env.batteryLevel, operator, value)
                    "networkQuality" -> compare(env.networkQuality.name, operator, value)
                    "isInMotion" -> compare(env.isInMotion, operator, value)
                    "locationContext" -> compare(env.locationContext.name, operator, value)
                    else -> false
                }
            }
        }

        data class GameStateCondition(
            val state: String,
            val operator: ComparisonOperator,
            val value: Any
        ) : RuleCondition() {
            override fun evaluate(context: UXContext, interaction: UserInteraction?): Boolean {
                val gameState = context.currentGameState ?: return false
                return when (state) {
                    "level" -> compare(gameState.level ?: 0, operator, value)
                    "score" -> compare(gameState.score ?: 0L, operator, value)
                    "lives" -> compare(gameState.lives ?: 0, operator, value)
                    "difficulty" -> compare(gameState.difficulty.name, operator, value)
                    "gameMode" -> compare(gameState.gameMode, operator, value)
                    "timeRemaining" -> compare(gameState.timeRemaining ?: 0L, operator, value)
                    else -> false
                }
            }
        }

        data class InteractionCondition(
            val property: String,
            val operator: ComparisonOperator,
            val value: Any
        ) : RuleCondition() {
            override fun evaluate(context: UXContext, interaction: UserInteraction?): Boolean {
                if (interaction == null) return false
                return when (property) {
                    "type" -> compare(interaction.type.name, operator, value)
                    "duration" -> compare(interaction.duration ?: 0L, operator, value)
                    "screenName" -> compare(interaction.context.screenName, operator, value)
                    "componentId" -> compare(interaction.context.componentId, operator, value)
                    "timestamp" -> compare(interaction.timestamp, operator, value)
                    else -> false
                }
            }
        }

        data class TimeBasedCondition(
            val timeRange: TimeRange
        ) : RuleCondition() {
            override fun evaluate(context: UXContext, interaction: UserInteraction?): Boolean {
                val currentTime = System.currentTimeMillis()
                return currentTime in timeRange.startTime..timeRange.endTime
            }
        }

        data class FrequencyCondition(
            val maxExecutionsPerPeriod: Int,
            val periodMs: Long
        ) : RuleCondition() {
            override fun evaluate(context: UXContext, interaction: UserInteraction?): Boolean {
                // This would track execution frequency - simplified for now
                return true
            }
        }
    }

    /**
     * Actions to perform when rule conditions are met
     */
    sealed class RuleAction {
        abstract fun execute(context: UXContext, interaction: UserInteraction?): List<UXEnhancement>

        data class ApplyEnhancement(
            val enhancement: UXEnhancement,
            val delay: Long = 0
        ) : RuleAction() {
            override fun execute(context: UXContext, interaction: UserInteraction?): List<UXEnhancement> {
                return if (enhancement.meetsConditions(context)) {
                    listOf(enhancement)
                } else {
                    emptyList()
                }
            }
        }

        data class ModifyEnhancement(
            val enhancementId: String,
            val modifications: Map<String, Any>
        ) : RuleAction() {
            override fun execute(context: UXContext, interaction: UserInteraction?): List<UXEnhancement> {
                // This would modify existing enhancements - placeholder for now
                return emptyList()
            }
        }

        data class ConditionalEnhancement(
            val condition: RuleCondition,
            val ifTrue: List<UXEnhancement>,
            val ifFalse: List<UXEnhancement> = emptyList()
        ) : RuleAction() {
            override fun execute(context: UXContext, interaction: UserInteraction?): List<UXEnhancement> {
                val meetsCondition = condition.evaluate(context, interaction)
                val enhancements = if (meetsCondition) ifTrue else ifFalse
                return enhancements.filter { it.meetsConditions(context) }
            }
        }

        data class SequentialEnhancements(
            val enhancements: List<UXEnhancement>,
            val delays: List<Long>
        ) : RuleAction() {
            override fun execute(context: UXContext, interaction: UserInteraction?): List<UXEnhancement> {
                return enhancements.filter { it.meetsConditions(context) }
            }
        }
    }

    /**
     * Time range for time-based conditions
     */
    data class TimeRange(
        val startTime: Long,
        val endTime: Long
    )

    /**
     * Comparison operators for rule conditions
     */
    enum class ComparisonOperator {
        EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN,
        GREATER_THAN_OR_EQUALS, LESS_THAN_OR_EQUALS,
        CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH,
        IN_RANGE, NOT_IN_RANGE
    }

    /**
     * Evaluate if this rule should be applied
     */
    fun shouldApply(context: UXContext, interaction: UserInteraction?): Boolean {
        // Check if rule is enabled
        if (!isEnabled) return false

        // Check validity period
        val currentTime = System.currentTimeMillis()
        if (validFrom != null && currentTime < validFrom) return false
        if (validUntil != null && currentTime > validUntil) return false

        // Check execution limits
        if (maxExecutions != null && executionCount >= maxExecutions) return false

        // Check cooldown
        if (cooldownMs > 0 && currentTime - lastExecutionTime < cooldownMs) return false

        // Evaluate all conditions
        return conditions.all { it.evaluate(context, interaction) }
    }

    /**
     * Execute the rule and return applicable enhancements
     */
    fun execute(context: UXContext, interaction: UserInteraction?): List<UXEnhancement> {
        if (!shouldApply(context, interaction)) return emptyList()

        return try {
            val enhancements = actions.flatMap { it.execute(context, interaction) }

            // Update execution tracking
            executionCount++
            lastExecutionTime = System.currentTimeMillis()

            enhancements
        } catch (e: Exception) {
            // Log error and return empty list
            emptyList()
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

    /**
     * Create a copy with modified properties
     */
    fun copyWith(
        isEnabled: Boolean? = null,
        priority: Int? = null,
        maxExecutions: Int? = null
    ): UXEnhancementRule {
        return copy(
            isEnabled = isEnabled ?: this.isEnabled,
            priority = priority ?: this.priority,
            maxExecutions = maxExecutions ?: this.maxExecutions
        )
    }
}

/**
 * Statistics for rule execution
 */
data class RuleStatistics(
    val executionCount: Int,
    val lastExecutionTime: Long,
    val successRate: Double,
    val averageExecutionTimeMs: Double = 0.0,
    val errorCount: Int = 0
)

/**
 * Helper function to compare values using different operators
 */
@Suppress("UNCHECKED_CAST")
private fun <T> compare(left: T, operator: UXEnhancementRule.ComparisonOperator, right: Any): Boolean {
    return when (operator) {
        UXEnhancementRule.ComparisonOperator.EQUALS -> left == right
        UXEnhancementRule.ComparisonOperator.NOT_EQUALS -> left != right
        UXEnhancementRule.ComparisonOperator.GREATER_THAN -> compareNumbers(left, right) { l, r -> l > r }
        UXEnhancementRule.ComparisonOperator.LESS_THAN -> compareNumbers(left, right) { l, r -> l < r }
        UXEnhancementRule.ComparisonOperator.GREATER_THAN_OR_EQUALS -> compareNumbers(left, right) { l, r -> l >= r }
        UXEnhancementRule.ComparisonOperator.LESS_THAN_OR_EQUALS -> compareNumbers(left, right) { l, r -> l <= r }
        UXEnhancementRule.ComparisonOperator.CONTAINS -> {
            when (left) {
                is String -> left.contains(right as String)
                is Collection<*> -> left.contains(right)
                else -> false
            }
        }
        UXEnhancementRule.ComparisonOperator.NOT_CONTAINS -> {
            when (left) {
                is String -> !left.contains(right as String)
                is Collection<*> -> !left.contains(right)
                else -> false
            }
        }
        UXEnhancementRule.ComparisonOperator.STARTS_WITH -> {
            left is String && right is String && left.startsWith(right)
        }
        UXEnhancementRule.ComparisonOperator.ENDS_WITH -> {
            left is String && right is String && left.endsWith(right)
        }
        UXEnhancementRule.ComparisonOperator.IN_RANGE -> {
            // Simplified - would need range implementation
            false
        }
        UXEnhancementRule.ComparisonOperator.NOT_IN_RANGE -> {
            // Simplified - would need range implementation
            false
        }
    }
}

/**
 * Helper function to compare numeric values
 */
private inline fun compareNumbers(left: Any?, right: Any?, comparison: (Number, Number) -> Boolean): Boolean {
    return try {
        val leftNum = left as? Number ?: return false
        val rightNum = right as? Number ?: return false
        comparison(leftNum.toDouble(), rightNum.toDouble())
    } catch (e: Exception) {
        false
    }
}

/**
 * Rule engine for managing and executing UX enhancement rules
 */
interface UXEnhancementRuleEngine {

    /**
     * Current rules in the engine
     */
    val rules: StateFlow<List<UXEnhancementRule>>

    /**
     * Add a rule to the engine
     */
    suspend fun addRule(rule: UXEnhancementRule): Boolean

    /**
     * Remove a rule from the engine
     */
    suspend fun removeRule(ruleId: String): Boolean

    /**
     * Update an existing rule
     */
    suspend fun updateRule(rule: UXEnhancementRule): Boolean

    /**
     * Get a rule by ID
     */
    suspend fun getRule(ruleId: String): UXEnhancementRule?

    /**
     * Get rules by category
     */
    suspend fun getRulesByCategory(category: UXEnhancementRule.RuleCategory): List<UXEnhancementRule>

    /**
     * Get rules by tags
     */
    suspend fun getRulesByTags(tags: List<String>): List<UXEnhancementRule>

    /**
     * Evaluate rules for given context and interaction
     */
    suspend fun evaluateRules(
        context: UXContext,
        interaction: UserInteraction?
    ): List<UXEnhancement>

    /**
     * Enable or disable a rule
     */
    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Boolean

    /**
     * Get statistics for all rules
     */
    suspend fun getAllRuleStatistics(): Map<String, RuleStatistics>

    /**
     * Get statistics for a specific rule
     */
    suspend fun getRuleStatistics(ruleId: String): RuleStatistics?

    /**
     * Reset all rule statistics
     */
    suspend fun resetAllStatistics()

    /**
     * Reset statistics for a specific rule
     */
    suspend fun resetRuleStatistics(ruleId: String)

    /**
     * Observe rule changes
     */
    fun observeRules(): Flow<List<UXEnhancementRule>>

    /**
     * Observe rule execution results
     */
    fun observeRuleExecutions(): Flow<RuleExecutionResult>
}

/**
 * Result of rule execution
 */
data class RuleExecutionResult(
    val ruleId: String,
    val success: Boolean,
    val enhancements: List<UXEnhancement>,
    val executionTimeMs: Long,
    val timestamp: Long,
    val error: String? = null
)

/**
 * Default implementation of the UX enhancement rule engine
 */
class UXEnhancementRuleEngineImpl : UXEnhancementRuleEngine {

    private val _rules = MutableStateFlow<List<UXEnhancementRule>>(emptyList())
    override val rules: StateFlow<List<UXEnhancementRule>> = _rules.asStateFlow()

    private val ruleExecutions = MutableStateFlow<List<RuleExecutionResult>>(emptyList())

    override suspend fun addRule(rule: UXEnhancementRule): Boolean {
        return try {
            // Check if rule already exists
            if (_rules.value.any { it.id == rule.id }) {
                return false
            }

            // Validate rule
            if (!isValidRule(rule)) {
                return false
            }

            _rules.value = _rules.value + rule
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeRule(ruleId: String): Boolean {
        return try {
            val initialSize = _rules.value.size
            _rules.value = _rules.value.filter { it.id != ruleId }
            _rules.value.size < initialSize
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateRule(rule: UXEnhancementRule): Boolean {
        return try {
            val index = _rules.value.indexOfFirst { it.id == rule.id }
            if (index == -1) return false

            if (!isValidRule(rule)) return false

            val updatedRules = _rules.value.toMutableList()
            updatedRules[index] = rule
            _rules.value = updatedRules

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getRule(ruleId: String): UXEnhancementRule? {
        return _rules.value.find { it.id == ruleId }
    }

    override suspend fun getRulesByCategory(category: UXEnhancementRule.RuleCategory): List<UXEnhancementRule> {
        return _rules.value.filter { it.category == category }
    }

    override suspend fun getRulesByTags(tags: List<String>): List<UXEnhancementRule> {
        return _rules.value.filter { rule ->
            tags.any { tag -> rule.tags.contains(tag) }
        }
    }

    override suspend fun evaluateRules(
        context: UXContext,
        interaction: UserInteraction?
    ): List<UXEnhancement> {
        val startTime = System.currentTimeMillis()

        val applicableRules = _rules.value
            .filter { it.isEnabled && it.shouldApply(context, interaction) }
            .sortedByDescending { it.priority }

        val allEnhancements = mutableListOf<UXEnhancement>()

        for (rule in applicableRules) {
            try {
                val enhancements = rule.execute(context, interaction)
                allEnhancements.addAll(enhancements)

                // Record execution result
                val executionTime = System.currentTimeMillis() - startTime
                val result = RuleExecutionResult(
                    ruleId = rule.id,
                    success = enhancements.isNotEmpty(),
                    enhancements = enhancements,
                    executionTimeMs = executionTime,
                    timestamp = System.currentTimeMillis()
                )

                ruleExecutions.value = (ruleExecutions.value + result).takeLast(100) // Keep last 100

            } catch (e: Exception) {
                val result = RuleExecutionResult(
                    ruleId = rule.id,
                    success = false,
                    enhancements = emptyList(),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    timestamp = System.currentTimeMillis(),
                    error = e.message
                )

                ruleExecutions.value = (ruleExecutions.value + result).takeLast(100)
            }
        }

        return allEnhancements.distinctBy { it.id } // Remove duplicates
    }

    override suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Boolean {
        return updateRule(_rules.value.find { it.id == ruleId }?.copy(isEnabled = enabled) ?: return false)
    }

    override suspend fun getAllRuleStatistics(): Map<String, RuleStatistics> {
        return _rules.value.associate { it.id to it.getStatistics() }
    }

    override suspend fun getRuleStatistics(ruleId: String): RuleStatistics? {
        return _rules.value.find { it.id == ruleId }?.getStatistics()
    }

    override suspend fun resetAllStatistics() {
        _rules.value.forEach { it.reset() }
    }

    override suspend fun resetRuleStatistics(ruleId: String) {
        _rules.value.find { it.id == ruleId }?.reset()
    }

    override fun observeRules(): Flow<List<UXEnhancementRule>> = rules

    override fun observeRuleExecutions(): Flow<RuleExecutionResult> = ruleExecutions

    /**
     * Validate that a rule is properly configured
     */
    private fun isValidRule(rule: UXEnhancementRule): Boolean {
        return rule.id.isNotBlank() &&
               rule.name.isNotBlank() &&
               rule.conditions.isNotEmpty() &&
               rule.actions.isNotEmpty()
    }
}