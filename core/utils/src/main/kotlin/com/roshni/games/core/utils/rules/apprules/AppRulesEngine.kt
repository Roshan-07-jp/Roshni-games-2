package com.roshni.games.core.utils.rules.apprules

import com.roshni.games.core.utils.rules.RuleContext
import com.roshni.games.core.utils.rules.RuleEngine
import kotlinx.coroutines.flow.Flow

/**
 * Core interface for the App Rules Engine that manages business rule validation,
 * enforcement, and execution across the gaming platform. This engine handles
 * high-level application rules including gameplay, monetization, and social features.
 */
interface AppRulesEngine {

    /**
     * Register a new business rule with the engine
     *
     * @param rule The business rule to register
     * @return true if registration was successful, false otherwise
     */
    suspend fun registerBusinessRule(rule: BusinessRule): Boolean

    /**
     * Unregister a business rule from the engine
     *
     * @param ruleId The ID of the rule to unregister
     * @return true if unregistration was successful, false otherwise
     */
    suspend fun unregisterBusinessRule(ruleId: String): Boolean

    /**
     * Get a registered business rule by its ID
     *
     * @param ruleId The ID of the rule to retrieve
     * @return The business rule if found, null otherwise
     */
    suspend fun getBusinessRule(ruleId: String): BusinessRule?

    /**
     * Get all registered business rules
     *
     * @return List of all registered business rules
     */
    suspend fun getAllBusinessRules(): List<BusinessRule>

    /**
     * Get business rules by category
     *
     * @param category The category to filter by
     * @return List of business rules in the specified category
     */
    suspend fun getBusinessRulesByCategory(category: RuleCategory): List<BusinessRule>

    /**
     * Get business rules by type
     *
     * @param ruleType The rule type to filter by
     * @return List of business rules of the specified type
     */
    suspend fun getBusinessRulesByType(ruleType: BusinessRuleType): List<BusinessRule>

    /**
     * Validate a business operation against all applicable rules
     *
     * @param operation The business operation to validate
     * @param context The validation context
     * @return Validation result containing all rule evaluation outcomes
     */
    suspend fun validateOperation(
        operation: BusinessOperation,
        context: ValidationContext
    ): ValidationResult

    /**
     * Enforce business rules for a specific operation
     *
     * @param operation The business operation to enforce rules for
     * @param context The enforcement context
     * @return Enforcement result containing the outcome of rule enforcement
     */
    suspend fun enforceRules(
        operation: BusinessOperation,
        context: EnforcementContext
    ): EnforcementResult

    /**
     * Evaluate business rules for a specific scenario
     *
     * @param scenario The business scenario to evaluate
     * @param context The rule context
     * @return List of rule evaluation results
     */
    suspend fun evaluateScenario(
        scenario: BusinessScenario,
        context: RuleContext
    ): List<BusinessRuleResult>

    /**
     * Execute business rule actions based on evaluation results
     *
     * @param results The business rule evaluation results
     * @param context The execution context
     * @return true if all actions were executed successfully, false otherwise
     */
    suspend fun executeBusinessActions(
        results: List<BusinessRuleResult>,
        context: ExecutionContext
    ): Boolean

    /**
     * Get business rules that apply to a specific operation type
     *
     * @param operationType The type of operation to get rules for
     * @return List of applicable business rules
     */
    suspend fun getRulesForOperation(operationType: BusinessOperationType): List<BusinessRule>

    /**
     * Check if an operation is allowed based on business rules
     *
     * @param operation The operation to check
     * @param context The validation context
     * @return true if the operation is allowed, false otherwise
     */
    suspend fun isOperationAllowed(
        operation: BusinessOperation,
        context: ValidationContext
    ): Boolean

    /**
     * Get the reason why an operation was denied (if applicable)
     *
     * @param operation The operation that was denied
     * @param context The validation context
     * @return Human-readable reason for denial, or null if operation is allowed
     */
    suspend fun getDenialReason(
        operation: BusinessOperation,
        context: ValidationContext
    ): String?

    /**
     * Start continuous validation of business rules
     *
     * @param contextProvider Function that provides the current context for validation
     * @param validationInterval Interval between validations in milliseconds
     * @return Flow of validation results
     */
    fun startContinuousValidation(
        contextProvider: suspend () -> ValidationContext,
        validationInterval: Long = 5000L
    ): Flow<ValidationResult>

    /**
     * Stop continuous validation
     */
    fun stopContinuousValidation()

    /**
     * Check if continuous validation is currently running
     */
    fun isContinuousValidationRunning(): Boolean

    /**
     * Get validation statistics for business rules
     *
     * @param ruleId Optional rule ID to get statistics for (null for all rules)
     * @return Business rule validation statistics
     */
    suspend fun getValidationStatistics(ruleId: String? = null): BusinessRuleStatistics

    /**
     * Clear all validation statistics
     */
    suspend fun clearStatistics()

    /**
     * Validate all registered business rules
     *
     * @return Validation result containing any errors or warnings
     */
    suspend fun validateAllBusinessRules(): BusinessRuleValidationResult

    /**
     * Export business rules configuration for backup or migration
     *
     * @return Exported configuration as a map
     */
    suspend fun exportConfiguration(): Map<String, Any>

    /**
     * Import business rules configuration
     *
     * @param configuration The configuration to import
     * @return true if import was successful, false otherwise
     */
    suspend fun importConfiguration(configuration: Map<String, Any>): Boolean

    /**
     * Get the current engine status
     */
    suspend fun getEngineStatus(): AppRulesEngineStatus

    /**
     * Shutdown the app rules engine and clean up resources
     */
    suspend fun shutdown()
}

/**
 * Statistics about business rule validation performance and results
 */
data class BusinessRuleStatistics(
    val totalValidations: Long = 0,
    val successfulValidations: Long = 0,
    val failedValidations: Long = 0,
    val averageValidationTimeMs: Double = 0.0,
    val totalExecutionTimeMs: Long = 0,
    val lastValidationTime: Long? = null,
    val rulesValidated: Map<String, Long> = emptyMap(), // ruleId -> validation count
    val categoryStatistics: Map<RuleCategory, CategoryStats> = emptyMap(),
    val operationTypeStatistics: Map<BusinessOperationType, OperationStats> = emptyMap()
) {

    data class CategoryStats(
        val validationCount: Long = 0,
        val averageValidationTimeMs: Double = 0.0,
        val successRate: Double = 0.0
    )

    data class OperationStats(
        val validationCount: Long = 0,
        val allowedCount: Long = 0,
        val deniedCount: Long = 0,
        val averageValidationTimeMs: Double = 0.0
    )
}

/**
 * Result of business rule validation
 */
data class BusinessRuleValidationResult(
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
 * Current status of the app rules engine
 */
data class AppRulesEngineStatus(
    val isRunning: Boolean,
    val registeredRuleCount: Int,
    val activeRuleCount: Int,
    val continuousValidationRunning: Boolean,
    val lastActivityTime: Long?,
    val memoryUsage: Long = 0,
    val errorCount: Long = 0,
    val uptimeMs: Long = 0,
    val validationSuccessRate: Double = 0.0
)