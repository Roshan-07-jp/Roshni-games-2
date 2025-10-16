package com.roshni.games.core.utils.rules.apprules

import com.roshni.games.core.utils.rules.RuleContext
import com.roshni.games.core.utils.rules.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of the AppRulesEngine interface
 * Integrates with existing RuleEngine for complex condition evaluation
 */
class AppRulesEngineImpl private constructor(
    private val ruleEngine: RuleEngine,
    private val businessLogicValidator: BusinessLogicValidator,
    private val businessLogicEnforcer: BusinessLogicEnforcer
) : AppRulesEngine {

    // Core storage
    private val businessRules = ConcurrentHashMap<String, BusinessRule>()
    private val ruleStatistics = ConcurrentHashMap<String, BusinessRuleStatistics>()

    // Continuous validation
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var continuousValidationJob: Job? = null
    private val validationResultsFlow = MutableSharedFlow<ValidationResult>(replay = 1)

    // Engine state
    private var startTime = System.currentTimeMillis()
    private var isShutdown = false

    override suspend fun registerBusinessRule(rule: BusinessRule): Boolean {
        if (isShutdown) return false

        return try {
            // Validate rule before registration
            if (!rule.validate()) {
                return false
            }

            // Register underlying gameplay rules if this is a gameplay business rule
            if (rule is GameplayBusinessRule) {
                registerGameplayRules(rule)
            }

            businessRules[rule.id] = rule
            initializeRuleStatistics(rule.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unregisterBusinessRule(ruleId: String): Boolean {
        if (isShutdown) return false

        return try {
            val removed = businessRules.remove(ruleId)
            ruleStatistics.remove(ruleId)
            removed != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getBusinessRule(ruleId: String): BusinessRule? {
        return businessRules[ruleId]
    }

    override suspend fun getAllBusinessRules(): List<BusinessRule> {
        return businessRules.values.toList()
    }

    override suspend fun getBusinessRulesByCategory(category: RuleCategory): List<BusinessRule> {
        return businessRules.values.filter { it.category == category }
    }

    override suspend fun getBusinessRulesByType(ruleType: BusinessRuleType): List<BusinessRule> {
        return businessRules.values.filter { it.ruleType == ruleType }
    }

    override suspend fun validateOperation(
        operation: BusinessOperation,
        context: ValidationContext
    ): ValidationResult {
        if (isShutdown) {
            return ValidationResult(
                isValid = false,
                operation = operation,
                context = context,
                errors = listOf("App Rules Engine is shutdown")
            )
        }

        return try {
            val validationResult = businessLogicValidator.validateBusinessOperation(
                operation = operation,
                context = context,
                validationStrategy = ValidationStrategy.COMPREHENSIVE
            )

            // Update statistics
            updateValidationStatistics(validationResult)

            validationResult
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                operation = operation,
                context = context,
                errors = listOf("Validation failed: ${e.message}")
            )
        }
    }

    override suspend fun enforceRules(
        operation: BusinessOperation,
        context: EnforcementContext
    ): EnforcementResult {
        if (isShutdown) {
            return EnforcementResult(
                isSuccessful = false,
                operation = operation,
                context = context,
                errors = listOf("App Rules Engine is shutdown")
            )
        }

        return try {
            // First validate the operation
            val validationContext = ValidationContext(
                operation = operation,
                userContext = context.userContext,
                applicationContext = context.applicationContext,
                environmentContext = context.environmentContext
            )

            val validationResult = validateOperation(operation, validationContext)

            if (!validationResult.isValid && !context.enforcementMode.forceExecuteOnFailure) {
                return EnforcementResult(
                    isSuccessful = false,
                    operation = operation,
                    context = context,
                    errors = listOf("Cannot enforce rules for failed validation")
                )
            }

            // Enforce the rules
            val enforcementResult = businessLogicEnforcer.enforceBusinessRules(
                validationResult = validationResult,
                context = context,
                enforcementPolicy = when (context.enforcementMode) {
                    EnforcementMode.STANDARD -> EnforcementPolicy.STANDARD
                    EnforcementMode.DRY_RUN -> EnforcementPolicy.DRY_RUN
                    EnforcementMode.FORCE_EXECUTE -> EnforcementPolicy.FORCE_EXECUTE
                    EnforcementMode.PREVIEW -> EnforcementPolicy.DRY_RUN
                }
            )

            enforcementResult
        } catch (e: Exception) {
            EnforcementResult(
                isSuccessful = false,
                operation = operation,
                context = context,
                errors = listOf("Enforcement failed: ${e.message}")
            )
        }
    }

    override suspend fun evaluateScenario(
        scenario: BusinessScenario,
        context: RuleContext
    ): List<BusinessRuleResult> {
        if (isShutdown) return emptyList()

        return try {
            // Convert scenario to operations and evaluate each
            val results = mutableListOf<BusinessRuleResult>()

            // This would implement scenario-based evaluation logic
            // For now, return empty results
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun executeBusinessActions(
        results: List<BusinessRuleResult>,
        context: ExecutionContext
    ): Boolean {
        if (isShutdown) return false

        return try {
            var allSuccessful = true

            for (result in results) {
                if (result.isAllowed) {
                    for (action in result.actions) {
                        val success = withTimeoutOrNull(5000L) {
                            action.execute(context)
                        } ?: false

                        if (!success) allSuccessful = false
                    }
                }
            }

            allSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getRulesForOperation(operationType: BusinessOperationType): List<BusinessRule> {
        return businessRules.values.filter { rule ->
            rule.appliesTo(BusinessOperation(operationType, "temp", "temp"))
        }
    }

    override suspend fun isOperationAllowed(
        operation: BusinessOperation,
        context: ValidationContext
    ): Boolean {
        val result = validateOperation(operation, context)
        return result.isValid
    }

    override suspend fun getDenialReason(
        operation: BusinessOperation,
        context: ValidationContext
    ): String? {
        val result = validateOperation(operation, context)
        return if (!result.isValid) result.getPrimaryFailureReason() else null
    }

    override fun startContinuousValidation(
        contextProvider: suspend () -> ValidationContext,
        validationInterval: Long
    ): Flow<ValidationResult> {
        if (isShutdown) return flow { }

        stopContinuousValidation()

        continuousValidationJob = engineScope.launch {
            while (isActive && !isShutdown) {
                try {
                    val context = contextProvider()
                    val validationResult = validateOperation(context.operation, context)

                    // Emit results through the flow
                    validationResultsFlow.emit(validationResult)

                    // Update statistics
                    updateValidationStatistics(validationResult)

                } catch (e: Exception) {
                    // Log error but continue validation
                }

                delay(validationInterval)
            }
        }

        return validationResultsFlow.asSharedFlow()
    }

    override fun stopContinuousValidation() {
        continuousValidationJob?.cancel()
        continuousValidationJob = null
    }

    override fun isContinuousValidationRunning(): Boolean {
        return continuousValidationJob?.isActive == true
    }

    override suspend fun getValidationStatistics(ruleId: String?): BusinessRuleStatistics {
        return if (ruleId != null) {
            ruleStatistics[ruleId] ?: BusinessRuleStatistics()
        } else {
            // Aggregate statistics for all rules
            val totalValidations = ruleStatistics.values.sumOf { it.totalValidations }
            val totalSuccessful = ruleStatistics.values.sumOf { it.successfulValidations }
            val totalExecutionTime = ruleStatistics.values.sumOf { it.totalExecutionTimeMs }

            val avgTime = if (totalValidations > 0) {
                totalExecutionTime.toDouble() / totalValidations
            } else 0.0

            BusinessRuleStatistics(
                totalValidations = totalValidations,
                successfulValidations = totalSuccessful,
                failedValidations = totalValidations - totalSuccessful,
                averageValidationTimeMs = avgTime,
                totalExecutionTimeMs = totalExecutionTime
            )
        }
    }

    override suspend fun clearStatistics() {
        ruleStatistics.clear()
    }

    override suspend fun validateAllBusinessRules(): BusinessRuleValidationResult {
        if (isShutdown) {
            return BusinessRuleValidationResult(
                isValid = false,
                errors = listOf("App Rules Engine is shutdown")
            )
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val ruleValidationResults = mutableMapOf<String, BusinessRuleValidationResult.RuleValidationResult>()

        businessRules.values.forEach { rule ->
            try {
                val isValid = rule.validate()
                val ruleErrors = mutableListOf<String>()
                val ruleWarnings = mutableListOf<String>()

                if (!isValid) {
                    ruleErrors.add("Business rule validation failed")
                }

                ruleValidationResults[rule.id] = BusinessRuleValidationResult.RuleValidationResult(
                    isValid = isValid,
                    errors = ruleErrors,
                    warnings = ruleWarnings
                )

                if (!isValid) {
                    errors.add("Business rule ${rule.id} failed validation")
                }

            } catch (e: Exception) {
                errors.add("Exception validating business rule ${rule.id}: ${e.message}")
                ruleValidationResults[rule.id] = BusinessRuleValidationResult.RuleValidationResult(
                    isValid = false,
                    errors = listOf(e.message ?: "Unknown error")
                )
            }
        }

        return BusinessRuleValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            ruleValidationResults = ruleValidationResults
        )
    }

    override suspend fun exportConfiguration(): Map<String, Any> {
        return mapOf(
            "businessRules" to businessRules.values.map { rule ->
                mapOf(
                    "id" to rule.id,
                    "name" to rule.name,
                    "description" to rule.description,
                    "category" to rule.category.name,
                    "ruleType" to rule.ruleType.name,
                    "enabled" to rule.enabled,
                    "priority" to rule.priority,
                    "tags" to rule.tags,
                    "version" to rule.version,
                    "metadata" to rule.getMetadata()
                )
            },
            "exportTime" to System.currentTimeMillis(),
            "ruleCount" to businessRules.size,
            "statistics" to getValidationStatistics()
        )
    }

    override suspend fun importConfiguration(configuration: Map<String, Any>): Boolean {
        if (isShutdown) return false

        return try {
            @Suppress("UNCHECKED_CAST")
            val rulesConfig = configuration["businessRules"] as? List<Map<String, Any>> ?: return false

            var successCount = 0

            rulesConfig.forEach { ruleConfig ->
                try {
                    // This is a simplified import - in a real implementation,
                    // you would need to reconstruct the full rule objects
                    successCount++
                } catch (e: Exception) {
                    // Log error but continue with other rules
                }
            }

            successCount == rulesConfig.size
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getEngineStatus(): AppRulesEngineStatus {
        val uptime = System.currentTimeMillis() - startTime
        val stats = getValidationStatistics()

        return AppRulesEngineStatus(
            isRunning = !isShutdown,
            registeredRuleCount = businessRules.size,
            activeRuleCount = businessRules.values.count { it.enabled },
            continuousValidationRunning = isContinuousValidationRunning(),
            lastActivityTime = ruleStatistics.values.maxOfOrNull { it.lastValidationTime ?: 0 },
            errorCount = ruleStatistics.values.sumOf { it.failedValidations },
            uptimeMs = uptime,
            validationSuccessRate = if (stats.totalValidations > 0) {
                stats.successfulValidations.toDouble() / stats.totalValidations
            } else 0.0
        )
    }

    override suspend fun shutdown() {
        stopContinuousValidation()
        businessRules.clear()
        ruleStatistics.clear()
        isShutdown = true
    }

    /**
     * Register underlying gameplay rules for a gameplay business rule
     */
    private suspend fun registerGameplayRules(businessRule: GameplayBusinessRule) {
        // Register gameplay conditions as individual rules in the underlying rule engine
        businessRule.gameplayConditions.forEach { condition ->
            // Convert GameplayCondition to a Rule for the underlying engine
            // This would require creating adapter rules
        }

        // Register gameplay actions
        businessRule.gameplayActions.forEach { action ->
            // Register actions that can be executed by the underlying engine
        }
    }

    /**
     * Initialize statistics for a newly registered rule
     */
    private fun initializeRuleStatistics(ruleId: String) {
        ruleStatistics[ruleId] = BusinessRuleStatistics()
    }

    /**
     * Update validation statistics based on validation result
     */
    private fun updateValidationStatistics(result: ValidationResult) {
        result.ruleResults.forEach { ruleResult ->
            val currentStats = ruleStatistics[ruleResult.ruleId] ?: BusinessRuleStatistics()
            val newStats = currentStats.copy(
                totalValidations = currentStats.totalValidations + 1,
                successfulValidations = currentStats.successfulValidations + if (ruleResult.isAllowed) 1 else 0,
                failedValidations = currentStats.failedValidations + if (ruleResult.isAllowed) 0 else 1,
                totalExecutionTimeMs = currentStats.totalExecutionTimeMs + ruleResult.executionTimeMs,
                lastValidationTime = ruleResult.timestamp
            ).let { stats ->
                // Recalculate average
                if (stats.totalValidations > 0) {
                    stats.copy(
                        averageValidationTimeMs = stats.totalExecutionTimeMs.toDouble() / stats.totalValidations
                    )
                } else stats
            }

            ruleStatistics[ruleResult.ruleId] = newStats
        }
    }

    companion object {
        @Volatile
        private var instance: AppRulesEngineImpl? = null

        fun getInstance(
            ruleEngine: RuleEngine,
            businessLogicValidator: BusinessLogicValidator,
            businessLogicEnforcer: BusinessLogicEnforcer
        ): AppRulesEngineImpl {
            return instance ?: synchronized(this) {
                instance ?: AppRulesEngineImpl(
                    ruleEngine,
                    businessLogicValidator,
                    businessLogicEnforcer
                ).also { instance = it }
            }
        }
    }
}

/**
 * Factory for creating AppRulesEngine instances with proper dependencies
 */
object AppRulesEngineFactory {

    /**
     * Create a new AppRulesEngine instance with default dependencies
     */
    fun create(
        ruleEngine: RuleEngine? = null,
        businessLogicValidator: BusinessLogicValidator? = null,
        businessLogicEnforcer: BusinessLogicEnforcer? = null
    ): AppRulesEngine {
        val engine = ruleEngine ?: com.roshni.games.core.utils.rules.RuleEngineImpl.getInstance()
        val validator = businessLogicValidator ?: BusinessLogicValidator()
        val enforcer = businessLogicEnforcer ?: BusinessLogicEnforcer()

        return AppRulesEngineImpl.getInstance(engine, validator, enforcer)
    }

    /**
     * Create a test instance with custom configuration
     */
    fun createTestInstance(): AppRulesEngine {
        return AppRulesEngineImpl(
            ruleEngine = com.roshni.games.core.utils.rules.RuleEngineImpl(),
            businessLogicValidator = BusinessLogicValidator(),
            businessLogicEnforcer = BusinessLogicEnforcer()
        )
    }
}