package com.roshni.games.core.utils.rules

import kotlinx.coroutines.flow.Flow

/**
 * Core interface for the rule engine that manages rule registration,
 * evaluation, and execution across the gaming platform.
 */
interface RuleEngine {

    /**
     * Register a new rule with the engine
     *
     * @param rule The rule to register
     * @return true if registration was successful, false otherwise
     */
    suspend fun registerRule(rule: Rule): Boolean

    /**
     * Unregister a rule from the engine
     *
     * @param ruleId The ID of the rule to unregister
     * @return true if unregistration was successful, false otherwise
     */
    suspend fun unregisterRule(ruleId: String): Boolean

    /**
     * Get a registered rule by its ID
     *
     * @param ruleId The ID of the rule to retrieve
     * @return The rule if found, null otherwise
     */
    suspend fun getRule(ruleId: String): Rule?

    /**
     * Get all registered rules
     *
     * @return List of all registered rules
     */
    suspend fun getAllRules(): List<Rule>

    /**
     * Get rules by category
     *
     * @param category The category to filter by
     * @return List of rules in the specified category
     */
    suspend fun getRulesByCategory(category: String): List<Rule>

    /**
     * Get rules by tags
     *
     * @param tags The tags to filter by
     * @return List of rules that have any of the specified tags
     */
    suspend fun getRulesByTags(tags: List<String>): List<Rule>

    /**
     * Evaluate a single rule against the provided context
     *
     * @param ruleId The ID of the rule to evaluate
     * @param context The context to evaluate against
     * @return The result of the rule evaluation
     */
    suspend fun evaluateRule(ruleId: String, context: RuleContext): RuleResult

    /**
     * Evaluate multiple rules against the provided context
     *
     * @param ruleIds The IDs of the rules to evaluate
     * @param context The context to evaluate against
     * @return List of rule evaluation results
     */
    suspend fun evaluateRules(ruleIds: List<String>, context: RuleContext): List<RuleResult>

    /**
     * Evaluate all registered rules against the provided context
     *
     * @param context The context to evaluate against
     * @return List of rule evaluation results
     */
    suspend fun evaluateAllRules(context: RuleContext): List<RuleResult>

    /**
     * Evaluate rules by category
     *
     * @param category The category of rules to evaluate
     * @param context The context to evaluate against
     * @return List of rule evaluation results
     */
    suspend fun evaluateRulesByCategory(category: String, context: RuleContext): List<RuleResult>

    /**
     * Execute actions from rule evaluation results
     *
     * @param results The rule evaluation results containing actions to execute
     * @param context The context for action execution
     * @return true if all actions were executed successfully, false otherwise
     */
    suspend fun executeActions(results: List<RuleResult>, context: RuleContext): Boolean

    /**
     * Execute actions from a single rule evaluation result
     *
     * @param result The rule evaluation result containing actions to execute
     * @param context The context for action execution
     * @return true if all actions were executed successfully, false otherwise
     */
    suspend fun executeActions(result: RuleResult, context: RuleContext): Boolean

    /**
     * Enable or disable a rule
     *
     * @param ruleId The ID of the rule to modify
     * @param enabled Whether the rule should be enabled
     * @return true if the operation was successful, false otherwise
     */
    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Boolean

    /**
     * Check if a rule is currently enabled
     *
     * @param ruleId The ID of the rule to check
     * @return true if the rule is enabled, false otherwise
     */
    suspend fun isRuleEnabled(ruleId: String): Boolean

    /**
     * Get rule evaluation statistics
     *
     * @param ruleId The ID of the rule to get statistics for (null for all rules)
     * @return Rule evaluation statistics
     */
    suspend fun getRuleStatistics(ruleId: String? = null): RuleStatistics

    /**
     * Clear all rule evaluation statistics
     */
    suspend fun clearStatistics()

    /**
     * Start continuous evaluation of rules (for real-time rule processing)
     *
     * @param contextProvider Function that provides the current context for evaluation
     * @param evaluationInterval Interval between evaluations in milliseconds
     * @return Flow of rule evaluation results
     */
    fun startContinuousEvaluation(
        contextProvider: suspend () -> RuleContext,
        evaluationInterval: Long = 1000L
    ): Flow<List<RuleResult>>

    /**
     * Stop continuous evaluation
     */
    fun stopContinuousEvaluation()

    /**
     * Check if continuous evaluation is currently running
     */
    fun isContinuousEvaluationRunning(): Boolean

    /**
     * Validate all registered rules
     *
     * @return Validation result containing any errors or warnings
     */
    suspend fun validateAllRules(): ValidationResult

    /**
     * Export rules configuration for backup or migration
     *
     * @return Exported rules configuration as a map
     */
    suspend fun exportRules(): Map<String, Any>

    /**
     * Import rules configuration
     *
     * @param configuration The configuration to import
     * @return true if import was successful, false otherwise
     */
    suspend fun importRules(configuration: Map<String, Any>): Boolean

    /**
     * Get the current engine status
     */
    suspend fun getEngineStatus(): EngineStatus

    /**
     * Shutdown the rule engine and clean up resources
     */
    suspend fun shutdown()
}

/**
 * Statistics about rule evaluation performance and results
 */
data class RuleStatistics(
    val totalEvaluations: Long = 0,
    val successfulEvaluations: Long = 0,
    val failedEvaluations: Long = 0,
    val averageEvaluationTimeMs: Double = 0.0,
    val totalExecutionTimeMs: Long = 0,
    val lastEvaluationTime: Long? = null,
    val rulesEvaluated: Map<String, Long> = emptyMap(), // ruleId -> evaluation count
    val categoryStatistics: Map<String, CategoryStats> = emptyMap()
) {

    data class CategoryStats(
        val evaluationCount: Long = 0,
        val averageEvaluationTimeMs: Double = 0.0,
        val successRate: Double = 0.0
    )
}

/**
 * Result of rule validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val ruleValidationResults: Map<String, RuleValidationResult> = emptyMap()
) {

    data class RuleValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )
}

/**
 * Current status of the rule engine
 */
data class EngineStatus(
    val isRunning: Boolean,
    val registeredRuleCount: Int,
    val activeRuleCount: Int,
    val continuousEvaluationRunning: Boolean,
    val lastActivityTime: Long?,
    val memoryUsage: Long = 0,
    val errorCount: Long = 0,
    val uptimeMs: Long = 0
)