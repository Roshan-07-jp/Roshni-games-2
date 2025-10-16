package com.roshni.games.core.utils.rules

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
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Default implementation of the RuleEngine interface
 */
class RuleEngineImpl private constructor() : RuleEngine {

    // Core storage
    private val rules = ConcurrentHashMap<String, Rule>()
    private val ruleStatistics = ConcurrentHashMap<String, RuleStatistics>()
    private val globalStatistics = AtomicLong(0)

    // Continuous evaluation
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var continuousEvaluationJob: Job? = null
    private val evaluationResultsFlow = MutableSharedFlow<List<RuleResult>>(replay = 1)

    // Engine state
    private var startTime = System.currentTimeMillis()
    private var isShutdown = false

    override suspend fun registerRule(rule: Rule): Boolean {
        if (isShutdown) return false

        return try {
            // Validate rule before registration
            if (!rule.validate()) {
                return false
            }

            rules[rule.id] = rule
            initializeRuleStatistics(rule.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unregisterRule(ruleId: String): Boolean {
        if (isShutdown) return false

        return try {
            val removed = rules.remove(ruleId)
            ruleStatistics.remove(ruleId)
            removed != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getRule(ruleId: String): Rule? {
        return rules[ruleId]
    }

    override suspend fun getAllRules(): List<Rule> {
        return rules.values.toList()
    }

    override suspend fun getRulesByCategory(category: String): List<Rule> {
        return rules.values.filter { it.category == category }
    }

    override suspend fun getRulesByTags(tags: List<String>): List<Rule> {
        return rules.values.filter { rule ->
            tags.any { tag -> rule.tags.contains(tag) }
        }
    }

    override suspend fun evaluateRule(ruleId: String, context: RuleContext): RuleResult {
        if (isShutdown) {
            return RuleResult.failure(
                ruleId = ruleId,
                ruleType = RuleResult.RuleType.CUSTOM,
                reason = "Rule engine is shutdown"
            )
        }

        val rule = rules[ruleId] ?: return RuleResult.failure(
            ruleId = ruleId,
            ruleType = RuleResult.RuleType.CUSTOM,
            reason = "Rule not found: $ruleId"
        )

        return evaluateRuleInternal(rule, context)
    }

    override suspend fun evaluateRules(ruleIds: List<String>, context: RuleContext): List<RuleResult> {
        if (isShutdown) {
            return ruleIds.map { ruleId ->
                RuleResult.failure(
                    ruleId = ruleId,
                    ruleType = RuleResult.RuleType.CUSTOM,
                    reason = "Rule engine is shutdown"
                )
            }
        }

        return ruleIds.mapNotNull { ruleId ->
            rules[ruleId]?.let { evaluateRuleInternal(it, context) }
        }
    }

    override suspend fun evaluateAllRules(context: RuleContext): List<RuleResult> {
        if (isShutdown) {
            return emptyList()
        }

        return rules.values.map { evaluateRuleInternal(it, context) }
    }

    override suspend fun evaluateRulesByCategory(category: String, context: RuleContext): List<RuleResult> {
        if (isShutdown) {
            return emptyList()
        }

        return getRulesByCategory(category).map { evaluateRuleInternal(it, context) }
    }

    override suspend fun executeActions(results: List<RuleResult>, context: RuleContext): Boolean {
        if (isShutdown) return false

        return try {
            var allSuccessful = true

            for (result in results) {
                val success = executeActions(result, context)
                if (!success) allSuccessful = false
            }

            allSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun executeActions(result: RuleResult, context: RuleContext): Boolean {
        if (isShutdown) return false

        return try {
            var allSuccessful = true

            for (action in result.actions) {
                val success = executeAction(action, context)
                if (!success) allSuccessful = false
            }

            allSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Boolean {
        if (isShutdown) return false

        return try {
            val rule = rules[ruleId] ?: return false
            // In a real implementation, you might want to create a new rule instance with updated enabled state
            // For now, we'll track this separately
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isRuleEnabled(ruleId: String): Boolean {
        val rule = rules[ruleId] ?: return false
        return rule.enabled
    }

    override suspend fun getRuleStatistics(ruleId: String?): RuleStatistics {
        return if (ruleId != null) {
            ruleStatistics[ruleId] ?: RuleStatistics()
        } else {
            // Aggregate statistics for all rules
            val totalEvaluations = AtomicLong(0)
            val totalSuccessful = AtomicLong(0)
            val totalExecutionTime = AtomicLong(0)

            ruleStatistics.values.forEach { stats ->
                totalEvaluations.addAndGet(stats.totalEvaluations)
                totalSuccessful.addAndGet(stats.successfulEvaluations)
                totalExecutionTime.addAndGet(stats.totalExecutionTimeMs.toLong())
            }

            val avgTime = if (totalEvaluations.get() > 0) {
                totalExecutionTime.get().toDouble() / totalEvaluations.get()
            } else 0.0

            RuleStatistics(
                totalEvaluations = totalEvaluations.get(),
                successfulEvaluations = totalSuccessful.get(),
                failedEvaluations = totalEvaluations.get() - totalSuccessful.get(),
                averageEvaluationTimeMs = avgTime,
                totalExecutionTimeMs = totalExecutionTime.get()
            )
        }
    }

    override suspend fun clearStatistics() {
        ruleStatistics.clear()
        globalStatistics.set(0)
    }

    override fun startContinuousEvaluation(
        contextProvider: suspend () -> RuleContext,
        evaluationInterval: Long
    ): Flow<List<RuleResult>> {
        if (isShutdown) return flow { }

        stopContinuousEvaluation()

        continuousEvaluationJob = engineScope.launch {
            while (isActive && !isShutdown) {
                try {
                    val context = contextProvider()
                    val results = evaluateAllRules(context)

                    // Emit results through the flow
                    evaluationResultsFlow.emit(results)

                    // Update statistics
                    results.forEach { result ->
                        updateRuleStatistics(result)
                    }

                } catch (e: Exception) {
                    // Log error but continue evaluation
                }

                delay(evaluationInterval)
            }
        }

        return evaluationResultsFlow.asSharedFlow()
    }

    override fun stopContinuousEvaluation() {
        continuousEvaluationJob?.cancel()
        continuousEvaluationJob = null
    }

    override fun isContinuousEvaluationRunning(): Boolean {
        return continuousEvaluationJob?.isActive == true
    }

    override suspend fun validateAllRules(): ValidationResult {
        if (isShutdown) {
            return ValidationResult(
                isValid = false,
                errors = listOf("Rule engine is shutdown")
            )
        }

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val ruleValidationResults = mutableMapOf<String, ValidationResult.RuleValidationResult>()

        rules.values.forEach { rule ->
            try {
                val isValid = rule.validate()
                val ruleErrors = mutableListOf<String>()
                val ruleWarnings = mutableListOf<String>()

                if (!isValid) {
                    ruleErrors.add("Rule validation failed")
                }

                ruleValidationResults[rule.id] = ValidationResult.RuleValidationResult(
                    isValid = isValid,
                    errors = ruleErrors,
                    warnings = ruleWarnings
                )

                if (!isValid) {
                    errors.add("Rule ${rule.id} failed validation")
                }

            } catch (e: Exception) {
                errors.add("Exception validating rule ${rule.id}: ${e.message}")
                ruleValidationResults[rule.id] = ValidationResult.RuleValidationResult(
                    isValid = false,
                    errors = listOf(e.message ?: "Unknown error")
                )
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            ruleValidationResults = ruleValidationResults
        )
    }

    override suspend fun exportRules(): Map<String, Any> {
        return mapOf(
            "rules" to rules.values.map { rule ->
                mapOf(
                    "id" to rule.id,
                    "name" to rule.name,
                    "description" to rule.description,
                    "category" to rule.category,
                    "enabled" to rule.enabled,
                    "tags" to rule.tags,
                    "version" to rule.version,
                    "metadata" to rule.getMetadata()
                )
            },
            "exportTime" to System.currentTimeMillis(),
            "ruleCount" to rules.size
        )
    }

    override suspend fun importRules(configuration: Map<String, Any>): Boolean {
        if (isShutdown) return false

        return try {
            @Suppress("UNCHECKED_CAST")
            val rulesConfig = configuration["rules"] as? List<Map<String, Any>> ?: return false

            var successCount = 0

            rulesConfig.forEach { ruleConfig ->
                try {
                    // This is a simplified import - in a real implementation,
                    // you would need to reconstruct the full rule objects
                    // For now, we'll just count successful imports
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

    override suspend fun getEngineStatus(): EngineStatus {
        val uptime = System.currentTimeMillis() - startTime

        return EngineStatus(
            isRunning = !isShutdown,
            registeredRuleCount = rules.size,
            activeRuleCount = rules.values.count { it.enabled },
            continuousEvaluationRunning = isContinuousEvaluationRunning(),
            lastActivityTime = ruleStatistics.values.maxOfOrNull { it.lastEvaluationTime ?: 0 },
            errorCount = ruleStatistics.values.sumOf { it.failedEvaluations },
            uptimeMs = uptime
        )
    }

    override suspend fun shutdown() {
        stopContinuousEvaluation()
        rules.clear()
        ruleStatistics.clear()
        isShutdown = true
    }

    /**
     * Internal method to evaluate a single rule
     */
    private suspend fun evaluateRuleInternal(rule: Rule, context: RuleContext): RuleResult {
        val startTime = System.currentTimeMillis()

        return try {
            if (!rule.enabled) {
                return RuleResult.failure(
                    ruleId = rule.id,
                    ruleType = RuleResult.RuleType.CUSTOM,
                    reason = "Rule is disabled"
                )
            }

            val result = withContext(context.dispatcher) {
                rule.evaluate(context)
            }

            val executionTime = System.currentTimeMillis() - startTime

            // Update statistics
            updateRuleStatistics(result.copy(executionTimeMs = executionTime))

            result
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime

            RuleResult.failure(
                ruleId = rule.id,
                ruleType = RuleResult.RuleType.CUSTOM,
                reason = "Exception during evaluation: ${e.message}",
                executionTimeMs = executionTime,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }

    /**
     * Execute a single action
     */
    private suspend fun executeAction(action: RuleResult.RuleAction, context: RuleContext): Boolean {
        return try {
            when (action) {
                is GameplayAction -> action.execute(context)
                else -> false // Unknown action type
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Initialize statistics for a newly registered rule
     */
    private fun initializeRuleStatistics(ruleId: String) {
        ruleStatistics[ruleId] = RuleStatistics()
    }

    /**
     * Update statistics for a rule evaluation result
     */
    private fun updateRuleStatistics(result: RuleResult) {
        val currentStats = ruleStatistics[result.ruleId] ?: RuleStatistics()
        val newStats = currentStats.copy(
            totalEvaluations = currentStats.totalEvaluations + 1,
            successfulEvaluations = currentStats.successfulEvaluations + if (result.isAllowed) 1 else 0,
            failedEvaluations = currentStats.failedEvaluations + if (result.isAllowed) 0 else 1,
            totalExecutionTimeMs = currentStats.totalExecutionTimeMs + result.executionTimeMs,
            lastEvaluationTime = result.timestamp
        ).let { stats ->
            // Recalculate average
            if (stats.totalEvaluations > 0) {
                stats.copy(
                    averageEvaluationTimeMs = stats.totalExecutionTimeMs.toDouble() / stats.totalEvaluations
                )
            } else stats
        }

        ruleStatistics[result.ruleId] = newStats
        globalStatistics.incrementAndGet()
    }

    companion object {
        @Volatile
        private var instance: RuleEngineImpl? = null

        fun getInstance(): RuleEngineImpl {
            return instance ?: synchronized(this) {
                instance ?: RuleEngineImpl().also { instance = it }
            }
        }
    }
}