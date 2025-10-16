package com.roshni.games.core.utils.rules.apprules

import com.roshni.games.core.utils.rules.RuleContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Priority-based execution system for business rules
 * Handles rule ordering, parallel execution, and dependency management
 */
class RulePriorityExecutor {

    /**
     * Execute rules with priority-based ordering and dependency management
     */
    suspend fun executeRulesWithPriority(
        rules: List<BusinessRule>,
        context: RuleContext,
        executionConfig: ExecutionConfig = ExecutionConfig()
    ): List<BusinessRuleResult> = coroutineScope {

        // Filter enabled rules
        val enabledRules = rules.filter { it.enabled }

        // Group rules by priority for sequential execution within same priority
        val rulesByPriority = enabledRules.groupBy { it.priority }
            .toSortedMap(reverseOrder()) // Higher priority first

        val results = mutableListOf<BusinessRuleResult>()

        for ((priority, priorityRules) in rulesByPriority) {
            // Sort rules within same priority by execution order
            val sortedRules = priorityRules.sortedBy { it.getExecutionOrder() }

            // Execute rules in this priority group
            val priorityResults = if (executionConfig.parallelExecution) {
                executeRulesInParallel(sortedRules, context, executionConfig)
            } else {
                executeRulesSequentially(sortedRules, context, executionConfig)
            }

            results.addAll(priorityResults)

            // Check if we should stop execution based on results and configuration
            if (shouldStopExecution(priorityResults, executionConfig)) {
                break
            }
        }

        results
    }

    /**
     * Execute rules in parallel within the same priority group
     */
    private suspend fun executeRulesInParallel(
        rules: List<BusinessRule>,
        context: RuleContext,
        config: ExecutionConfig
    ): List<BusinessRuleResult> = coroutineScope {

        val deferredResults = rules.map { rule ->
            async {
                executeSingleRuleWithTimeout(rule, context, config.timeoutMs)
            }
        }

        // Wait for all rules to complete or timeout
        val results = withTimeoutOrNull(config.groupTimeoutMs) {
            deferredResults.awaitAll()
        } ?: emptyList()

        // Handle any rules that didn't complete due to timeout
        val timeoutResults = rules.zip(deferredResults).mapNotNull { (rule, deferred) ->
            if (deferred.isActive) {
                BusinessRuleResult.failure(
                    ruleId = rule.id,
                    ruleType = rule.ruleType,
                    reason = "Rule execution timed out",
                    executionTimeMs = config.timeoutMs,
                    errors = listOf("Execution timeout after ${config.timeoutMs}ms")
                )
            } else {
                null
            }
        }

        results + timeoutResults
    }

    /**
     * Execute rules sequentially within the same priority group
     */
    private suspend fun executeRulesSequentially(
        rules: List<BusinessRule>,
        context: RuleContext,
        config: ExecutionConfig
    ): List<BusinessRuleResult> {
        val results = mutableListOf<BusinessRuleResult>()

        for (rule in rules) {
            val result = executeSingleRuleWithTimeout(rule, context, config.timeoutMs)
            results.add(result)

            // Check if we should stop execution
            if (shouldStopExecution(results, config)) {
                break
            }
        }

        results
    }

    /**
     * Execute a single rule with timeout protection
     */
    private suspend fun executeSingleRuleWithTimeout(
        rule: BusinessRule,
        context: RuleContext,
        timeoutMs: Long
    ): BusinessRuleResult {
        return try {
            withTimeoutOrNull(timeoutMs) {
                val startTime = System.currentTimeMillis()
                val result = rule.evaluate(context)
                result.copy(executionTimeMs = System.currentTimeMillis() - startTime)
            } ?: BusinessRuleResult.failure(
                ruleId = rule.id,
                ruleType = rule.ruleType,
                reason = "Rule execution timed out",
                executionTimeMs = timeoutMs,
                errors = listOf("Execution timeout after ${timeoutMs}ms")
            )
        } catch (e: Exception) {
            BusinessRuleResult.failure(
                ruleId = rule.id,
                ruleType = rule.ruleType,
                reason = "Exception during rule execution: ${e.message}",
                executionTimeMs = 0L,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }

    /**
     * Determine if execution should stop based on results and configuration
     */
    private fun shouldStopExecution(
        results: List<BusinessRuleResult>,
        config: ExecutionConfig
    ): Boolean {
        if (!config.stopOnFirstFailure) return false

        return results.any { !it.isAllowed && it.hasErrors() }
    }

    /**
     * Execute rules by category with priority ordering
     */
    suspend fun executeRulesByCategory(
        rules: List<BusinessRule>,
        categories: List<RuleCategory>,
        context: RuleContext,
        executionConfig: ExecutionConfig = ExecutionConfig()
    ): Map<RuleCategory, List<BusinessRuleResult>> {
        val results = mutableMapOf<RuleCategory, List<BusinessRuleResult>>()

        for (category in categories) {
            val categoryRules = rules.filter { it.category == category }
            if (categoryRules.isNotEmpty()) {
                val categoryResults = executeRulesWithPriority(categoryRules, context, executionConfig)
                results[category] = categoryResults
            }
        }

        return results
    }

    /**
     * Execute rules with dependency management
     */
    suspend fun executeRulesWithDependencies(
        rules: List<BusinessRule>,
        context: RuleContext,
        dependencies: Map<String, List<String>> = emptyMap(), // ruleId -> list of dependent rule IDs
        executionConfig: ExecutionConfig = ExecutionConfig()
    ): List<BusinessRuleResult> {

        val results = mutableMapOf<String, BusinessRuleResult>()
        val executedRules = mutableSetOf<String>()

        // Topological sort based on dependencies
        val sortedRules = topologicalSort(rules, dependencies)

        for (rule in sortedRules) {
            // Check if dependencies are satisfied
            val dependenciesSatisfied = dependencies[rule.id]?.all { depId ->
                val depResult = results[depId]
                depResult?.isAllowed == true
            } ?: true

            if (!dependenciesSatisfied) {
                results[rule.id] = BusinessRuleResult.failure(
                    ruleId = rule.id,
                    ruleType = rule.ruleType,
                    reason = "Dependencies not satisfied",
                    errors = listOf("Required dependencies failed")
                )
                continue
            }

            // Execute the rule
            val result = executeSingleRuleWithTimeout(rule, context, executionConfig.timeoutMs)
            results[rule.id] = result
            executedRules.add(rule.id)
        }

        return results.values.toList()
    }

    /**
     * Perform topological sort of rules based on dependencies
     */
    private fun topologicalSort(
        rules: List<BusinessRule>,
        dependencies: Map<String, List<String>>
    ): List<BusinessRule> {
        val ruleMap = rules.associateBy { it.id }
        val inDegree = mutableMapOf<String, Int>()
        val queue = mutableListOf<BusinessRule>()
        val result = mutableListOf<BusinessRule>()

        // Initialize in-degrees
        rules.forEach { rule ->
            inDegree[rule.id] = dependencies[rule.id]?.size ?: 0
            if (inDegree[rule.id] == 0) {
                queue.add(rule)
            }
        }

        // Process rules with no dependencies first
        while (queue.isNotEmpty()) {
            val rule = queue.removeAt(0)
            result.add(rule)

            // Reduce in-degree for dependent rules
            rules.forEach { otherRule ->
                dependencies[otherRule.id]?.forEach { depId ->
                    if (depId == rule.id) {
                        inDegree[otherRule.id] = (inDegree[otherRule.id] ?: 0) - 1
                        if (inDegree[otherRule.id] == 0) {
                            queue.add(otherRule)
                        }
                    }
                }
            }
        }

        // Check for circular dependencies
        if (result.size != rules.size) {
            // Add remaining rules (circular dependency detected)
            rules.forEach { rule ->
                if (rule !in result) {
                    result.add(rule)
                }
            }
        }

        return result
    }
}

/**
 * Configuration for rule execution
 */
data class ExecutionConfig(
    val parallelExecution: Boolean = true,
    val timeoutMs: Long = 5000L,
    val groupTimeoutMs: Long = 30000L,
    val stopOnFirstFailure: Boolean = false,
    val maxConcurrentRules: Int = 10,
    val retryFailedRules: Boolean = false,
    val maxRetryAttempts: Int = 3,
    val retryDelayMs: Long = 1000L
)

/**
 * Rule execution statistics
 */
data class RuleExecutionStatistics(
    val totalRules: Int = 0,
    val executedRules: Int = 0,
    val failedRules: Int = 0,
    val skippedRules: Int = 0,
    val averageExecutionTimeMs: Double = 0.0,
    val totalExecutionTimeMs: Long = 0L,
    val timeoutCount: Int = 0,
    val errorCount: Int = 0,
    val priorityGroups: Map<Int, PriorityGroupStats> = emptyMap()
) {

    data class PriorityGroupStats(
        val priority: Int,
        val ruleCount: Int,
        val averageExecutionTimeMs: Double,
        val successRate: Float
    )
}

/**
 * Rule category manager for organizing and filtering rules
 */
class RuleCategoryManager {

    /**
     * Get all available rule categories
     */
    fun getAllCategories(): List<RuleCategory> {
        return RuleCategory.values().toList()
    }

    /**
     * Get categories by priority order
     */
    fun getCategoriesByPriority(): List<RuleCategory> {
        return RuleCategory.values().sortedBy { it.priority }
    }

    /**
     * Get category by name (case-insensitive)
     */
    fun getCategoryByName(name: String): RuleCategory? {
        return RuleCategory.values().find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Filter rules by categories
     */
    fun filterRulesByCategories(
        rules: List<BusinessRule>,
        categories: List<RuleCategory>
    ): List<BusinessRule> {
        return rules.filter { it.category in categories }
    }

    /**
     * Group rules by category
     */
    fun groupRulesByCategory(rules: List<BusinessRule>): Map<RuleCategory, List<BusinessRule>> {
        return rules.groupBy { it.category }
    }

    /**
     * Get rules for specific operation types by category
     */
    suspend fun getRulesForOperationByCategory(
        rules: List<BusinessRule>,
        operation: BusinessOperation,
        categories: List<RuleCategory>? = null
    ): Map<RuleCategory, List<BusinessRule>> {
        val applicableRules = rules.filter { rule ->
            rule.appliesTo(operation) && (categories == null || rule.category in categories)
        }

        return groupRulesByCategory(applicableRules)
    }

    /**
     * Validate category configuration
     */
    fun validateCategoryConfiguration(rules: List<BusinessRule>): List<String> {
        val errors = mutableListOf<String>()
        val categories = getAllCategories()

        // Check for unknown categories
        rules.forEach { rule ->
            if (rule.category !in categories) {
                errors.add("Rule ${rule.id} has unknown category: ${rule.category}")
            }
        }

        // Check for category-specific validation
        val gameplayRules = rules.filter { it.category == RuleCategory.GAMEPLAY }
        val monetizationRules = rules.filter { it.category == RuleCategory.MONETIZATION }
        val socialRules = rules.filter { it.category == RuleCategory.SOCIAL }

        // Add category-specific validation logic here if needed

        return errors
    }
}

/**
 * Rule priority manager for handling rule ordering and conflicts
 */
class RulePriorityManager {

    /**
     * Resolve priority conflicts between rules
     */
    fun resolvePriorityConflicts(rules: List<BusinessRule>): List<BusinessRule> {
        val rulesByPriority = rules.groupBy { it.priority }

        return rulesByPriority.flatMap { (priority, priorityRules) ->
            if (priorityRules.size > 1) {
                // Resolve conflicts within same priority using execution order
                priorityRules.sortedBy { it.getExecutionOrder() }
            } else {
                priorityRules
            }
        }
    }

    /**
     * Adjust rule priorities based on dependencies
     */
    fun adjustPrioritiesForDependencies(
        rules: List<BusinessRule>,
        dependencies: Map<String, List<String>>
    ): List<BusinessRule> {
        // This would implement dependency-based priority adjustment
        // For now, return rules as-is
        return rules
    }

    /**
     * Get the effective priority for a rule considering its category and type
     */
    fun getEffectivePriority(rule: BusinessRule): Int {
        val basePriority = rule.priority
        val categoryPriority = rule.category.priority
        val typePriority = when (rule.ruleType) {
            BusinessRuleType.VALIDATION -> 100
            BusinessRuleType.ENFORCEMENT -> 200
            BusinessRuleType.AUTHORIZATION -> 300
            BusinessRuleType.AUTOMATION -> 50
            BusinessRuleType.MONITORING -> 25
            BusinessRuleType.CUSTOM -> 0
        }

        return basePriority + categoryPriority * 1000 + typePriority
    }

    /**
     * Validate priority configuration
     */
    fun validatePriorityConfiguration(rules: List<BusinessRule>): List<String> {
        val errors = mutableListOf<String>()

        rules.forEach { rule ->
            if (rule.priority < 0) {
                errors.add("Rule ${rule.id} has negative priority: ${rule.priority}")
            }

            if (rule.priority > 1000) {
                errors.add("Rule ${rule.id} has priority too high: ${rule.priority}")
            }
        }

        return errors
    }
}