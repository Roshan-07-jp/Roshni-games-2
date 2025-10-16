package com.roshni.games.core.utils.rules.apprules

import com.roshni.games.core.utils.rules.RuleContext

/**
 * Core business logic validation system
 * Handles complex validation scenarios and business rule orchestration
 */
class BusinessLogicValidator(
    private val rulePriorityExecutor: RulePriorityExecutor = RulePriorityExecutor(),
    private val ruleCategoryManager: RuleCategoryManager = RuleCategoryManager()
) {

    /**
     * Validate a business operation using comprehensive rule evaluation
     */
    suspend fun validateBusinessOperation(
        operation: BusinessOperation,
        context: ValidationContext,
        validationStrategy: ValidationStrategy = ValidationStrategy.COMPREHENSIVE
    ): ValidationResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Get applicable rules for the operation
            val applicableRules = getApplicableRules(operation, context, validationStrategy)

            if (applicableRules.isEmpty()) {
                return ValidationResult(
                    isValid = true,
                    operation = operation,
                    context = context,
                    ruleResults = emptyList(),
                    summary = ValidationSummary(),
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Execute rules with priority-based ordering
            val ruleResults = rulePriorityExecutor.executeRulesWithPriority(
                rules = applicableRules,
                context = context.toRuleContext(),
                executionConfig = ExecutionConfig(
                    parallelExecution = validationStrategy.supportsParallelExecution,
                    timeoutMs = validationStrategy.ruleTimeoutMs,
                    stopOnFirstFailure = validationStrategy.stopOnFirstFailure
                )
            )

            // Analyze results and create summary
            val summary = createValidationSummary(ruleResults, applicableRules)

            // Generate recommendations based on validation results
            val recommendations = generateRecommendations(operation, ruleResults, context)

            // Determine overall validity
            val isValid = determineOverallValidity(ruleResults, validationStrategy)

            ValidationResult(
                isValid = isValid,
                operation = operation,
                context = context,
                ruleResults = ruleResults,
                summary = summary,
                recommendations = recommendations,
                warnings = extractWarnings(ruleResults),
                errors = extractErrors(ruleResults),
                executionTimeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                operation = operation,
                context = context,
                errors = listOf("Validation failed with exception: ${e.message}"),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Validate multiple operations in batch
     */
    suspend fun validateBusinessOperations(
        operations: List<BusinessOperation>,
        context: ValidationContext,
        validationStrategy: ValidationStrategy = ValidationStrategy.BATCH
    ): List<ValidationResult> {
        return operations.map { operation ->
            val childContext = context.createChildContext(operation)
            validateBusinessOperation(operation, childContext, validationStrategy)
        }
    }

    /**
     * Get applicable rules for a business operation
     */
    private suspend fun getApplicableRules(
        operation: BusinessOperation,
        context: ValidationContext,
        strategy: ValidationStrategy
    ): List<BusinessRule> {
        // This would typically come from a rule registry/repository
        // For now, return empty list as this would be injected
        return emptyList()
    }

    /**
     * Create validation summary from rule results
     */
    private fun createValidationSummary(
        ruleResults: List<BusinessRuleResult>,
        allRules: List<BusinessRule>
    ): ValidationSummary {
        val passedRules = ruleResults.filter { it.isAllowed }
        val failedRules = ruleResults.filter { !it.isAllowed }
        val averageConfidence = ruleResults.map { it.confidence }.average().toFloat()
        val averageExecutionTime = ruleResults.map { it.executionTimeMs }.average()

        return ValidationSummary(
            totalRules = ruleResults.size,
            passedRules = passedRules.size,
            failedRules = failedRules.size,
            skippedRules = allRules.size - ruleResults.size,
            averageConfidence = averageConfidence,
            averageExecutionTimeMs = averageExecutionTime,
            categoriesValidated = ruleResults.mapNotNull { result ->
                // Would need to map result back to rule category
                null
            }.toSet(),
            ruleTypesValidated = ruleResults.map { it.ruleType }.toSet()
        )
    }

    /**
     * Generate recommendations based on validation results
     */
    private fun generateRecommendations(
        operation: BusinessOperation,
        ruleResults: List<BusinessRuleResult>,
        context: ValidationContext
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Analyze failed rules for recommendations
        val failedRules = ruleResults.filter { !it.isAllowed }

        failedRules.forEach { failedRule ->
            when (failedRule.ruleType) {
                BusinessRuleType.AUTHORIZATION -> {
                    recommendations.add("User may need additional permissions for this operation")
                }
                BusinessRuleType.VALIDATION -> {
                    recommendations.add("Operation data may need to be corrected or completed")
                }
                BusinessRuleType.ENFORCEMENT -> {
                    recommendations.add("Business process may need to be reviewed")
                }
                else -> {
                    recommendations.add("Review ${failedRule.ruleId} requirements")
                }
            }
        }

        // Add context-specific recommendations
        if (context.validationLevel == ValidationLevel.STRICT && failedRules.size > 3) {
            recommendations.add("Consider using NORMAL validation level for better user experience")
        }

        return recommendations.distinct()
    }

    /**
     * Determine overall validity based on results and strategy
     */
    private fun determineOverallValidity(
        ruleResults: List<BusinessRuleResult>,
        strategy: ValidationStrategy
    ): Boolean {
        if (ruleResults.isEmpty()) return true

        return when (strategy.overallValidityMode) {
            OverallValidityMode.ALL_MUST_PASS -> ruleResults.all { it.isAllowed }
            OverallValidityMode.MAJORITY_MUST_PASS -> {
                val passedCount = ruleResults.count { it.isAllowed }
                passedCount >= (ruleResults.size * 0.7) // 70% must pass
            }
            OverallValidityMode.CRITICAL_MUST_PASS -> {
                val criticalRules = ruleResults.filter { it.metadata["critical"] == true }
                criticalRules.all { it.isAllowed }
            }
            OverallValidityMode.AT_LEAST_ONE_MUST_PASS -> ruleResults.any { it.isAllowed }
        }
    }

    /**
     * Extract warnings from rule results
     */
    private fun extractWarnings(ruleResults: List<BusinessRuleResult>): List<String> {
        return ruleResults.flatMap { it.warnings }
    }

    /**
     * Extract errors from rule results
     */
    private fun extractErrors(ruleResults: List<BusinessRuleResult>): List<String> {
        return ruleResults.flatMap { it.errors }
    }
}

/**
 * Business logic enforcement system
 * Handles execution of business rule actions and enforcement policies
 */
class BusinessLogicEnforcer(
    private val rulePriorityExecutor: RulePriorityExecutor = RulePriorityExecutor()
) {

    /**
     * Enforce business rules for a validated operation
     */
    suspend fun enforceBusinessRules(
        validationResult: ValidationResult,
        context: EnforcementContext,
        enforcementPolicy: EnforcementPolicy = EnforcementPolicy.STANDARD
    ): EnforcementResult {
        val startTime = System.currentTimeMillis()

        return try {
            if (!validationResult.isValid && !enforcementPolicy.forceExecuteOnFailure) {
                return EnforcementResult(
                    isSuccessful = false,
                    operation = validationResult.operation,
                    context = context,
                    errors = listOf("Cannot enforce rules for failed validation"),
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Extract actions from successful rule results
            val actionsToExecute = extractActionsForExecution(
                validationResult.ruleResults,
                enforcementPolicy
            )

            if (actionsToExecute.isEmpty()) {
                return EnforcementResult(
                    isSuccessful = true,
                    operation = validationResult.operation,
                    context = context,
                    summary = EnforcementSummary(),
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Execute actions based on policy
            val executionResults = executeActionsWithPolicy(
                actionsToExecute,
                context,
                enforcementPolicy
            )

            // Handle rollback if needed
            val rollbackResults = if (enforcementPolicy.rollbackOnFailure &&
                executionResults.hasFailures()) {
                performRollback(executionResults.executedActions, context)
            } else {
                emptyList()
            }

            // Generate notifications if needed
            val notifications = generateNotifications(
                validationResult,
                executionResults,
                context,
                enforcementPolicy
            )

            val summary = createEnforcementSummary(executionResults, rollbackResults, notifications)

            EnforcementResult(
                isSuccessful = !executionResults.hasFailures() || enforcementPolicy.allowPartialSuccess,
                operation = validationResult.operation,
                context = context,
                executedActions = executionResults.executedActions,
                failedActions = executionResults.failedActions,
                rollbackActions = rollbackResults,
                notifications = notifications,
                summary = summary,
                executionTimeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            EnforcementResult(
                isSuccessful = false,
                operation = validationResult.operation,
                context = context,
                errors = listOf("Enforcement failed with exception: ${e.message}"),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Extract actions that should be executed based on enforcement policy
     */
    private fun extractActionsForExecution(
        ruleResults: List<BusinessRuleResult>,
        policy: EnforcementPolicy
    ): List<BusinessRuleAction> {
        val actions = mutableListOf<BusinessRuleAction>()

        ruleResults.filter { it.isAllowed }.forEach { result ->
            actions.addAll(result.actions)
        }

        // Filter actions based on policy
        return actions.filter { action ->
            when (policy.actionFilterMode) {
                ActionFilterMode.ALL -> true
                ActionFilterMode.IMMEDIATE_ONLY -> action.immediate
                ActionFilterMode.HIGH_PRIORITY_ONLY -> action.priority >= policy.minPriority
                ActionFilterMode.CUSTOM -> policy.actionFilter(action)
            }
        }
    }

    /**
     * Execute actions according to enforcement policy
     */
    private suspend fun executeActionsWithPolicy(
        actions: List<BusinessRuleAction>,
        context: EnforcementContext,
        policy: EnforcementPolicy
    ): ActionExecutionResults {

        val executedActions = mutableListOf<ExecutedAction>()
        val failedActions = mutableListOf<FailedAction>()

        // Group actions by priority for ordered execution
        val actionsByPriority = actions.groupBy { it.priority }
            .toSortedMap(reverseOrder())

        for ((priority, priorityActions) in actionsByPriority) {
            val results = executePriorityGroup(priorityActions, context, policy)

            executedActions.addAll(results.executedActions)
            failedActions.addAll(results.failedActions)

            // Check if we should stop based on policy
            if (results.hasFailures() && policy.stopOnFirstFailure) {
                break
            }
        }

        return ActionExecutionResults(executedActions, failedActions)
    }

    /**
     * Execute a group of actions with the same priority
     */
    private suspend fun executePriorityGroup(
        actions: List<BusinessRuleAction>,
        context: EnforcementContext,
        policy: EnforcementPolicy
    ): ActionExecutionResults {
        val executedActions = mutableListOf<ExecutedAction>()
        val failedActions = mutableListOf<FailedAction>()

        for (action in actions) {
            try {
                val canExecute = action.canExecute(context.toExecutionContext())

                if (!canExecute) {
                    failedActions.add(
                        FailedAction(
                            actionId = action.actionId,
                            actionType = action.javaClass.simpleName,
                            error = "Action cannot be executed in current context",
                            canRetry = false
                        )
                    )
                    continue
                }

                val startTime = System.currentTimeMillis()
                val success = action.execute(context.toExecutionContext())
                val executionTime = System.currentTimeMillis() - startTime

                if (success) {
                    executedActions.add(
                        ExecutedAction(
                            actionId = action.actionId,
                            actionType = action.javaClass.simpleName,
                            executionTimeMs = executionTime
                        )
                    )
                } else {
                    failedActions.add(
                        FailedAction(
                            actionId = action.actionId,
                            actionType = action.javaClass.simpleName,
                            error = "Action execution returned false",
                            canRetry = policy.retryFailedActions
                        )
                    )
                }

            } catch (e: Exception) {
                failedActions.add(
                    FailedAction(
                        actionId = action.actionId,
                        actionType = action.javaClass.simpleName,
                        error = e.message ?: "Unknown error",
                        canRetry = policy.retryFailedActions
                    )
                )
            }
        }

        return ActionExecutionResults(executedActions, failedActions)
    }

    /**
     * Perform rollback of previously executed actions
     */
    private suspend fun performRollback(
        executedActions: List<ExecutedAction>,
        context: EnforcementContext
    ): List<ExecutedAction> {
        // Implementation would depend on action types and rollback capabilities
        // For now, return empty list
        return emptyList()
    }

    /**
     * Generate notifications based on enforcement results
     */
    private fun generateNotifications(
        validationResult: ValidationResult,
        executionResults: ActionExecutionResults,
        context: EnforcementContext,
        policy: EnforcementPolicy
    ): List<NotificationResult> {
        val notifications = mutableListOf<NotificationResult>()

        // Generate notifications for failures if configured
        if (policy.notifyOnFailure && executionResults.hasFailures()) {
            executionResults.failedActions.forEach { failedAction ->
                notifications.add(
                    NotificationResult(
                        notificationId = "enforcement_failure_${failedAction.actionId}",
                        notificationType = "error",
                        recipient = context.userContext.userId,
                        sentSuccessfully = true, // Would actually send notification
                        metadata = mapOf(
                            "failedAction" to failedAction.actionId,
                            "error" to failedAction.error
                        )
                    )
                )
            }
        }

        return notifications
    }

    /**
     * Create enforcement summary
     */
    private fun createEnforcementSummary(
        executionResults: ActionExecutionResults,
        rollbackResults: List<ExecutedAction>,
        notifications: List<NotificationResult>
    ): EnforcementSummary {
        val totalActions = executionResults.executedActions.size + executionResults.failedActions.size
        val averageExecutionTime = executionResults.executedActions
            .map { it.executionTimeMs }
            .average()

        return EnforcementSummary(
            totalActions = totalActions,
            executedActions = executionResults.executedActions.size,
            failedActions = executionResults.failedActions.size,
            rollbackActions = rollbackResults.size,
            notificationsSent = notifications.size,
            averageExecutionTimeMs = averageExecutionTime,
            actionTypes = executionResults.executedActions.map { it.actionType }.toSet()
        )
    }
}

/**
 * Validation strategies for different scenarios
 */
enum class ValidationStrategy(
    val supportsParallelExecution: Boolean,
    val ruleTimeoutMs: Long,
    val stopOnFirstFailure: Boolean,
    val overallValidityMode: OverallValidityMode
) {
    COMPREHENSIVE(true, 10000L, false, OverallValidityMode.CRITICAL_MUST_PASS),
    STRICT(false, 5000L, true, OverallValidityMode.ALL_MUST_PASS),
    PERFORMANCE(true, 2000L, false, OverallValidityMode.MAJORITY_MUST_PASS),
    BATCH(true, 15000L, false, OverallValidityMode.AT_LEAST_ONE_MUST_PASS),
    QUICK(false, 1000L, true, OverallValidityMode.CRITICAL_MUST_PASS)
}

/**
 * Overall validity determination modes
 */
enum class OverallValidityMode {
    ALL_MUST_PASS,
    MAJORITY_MUST_PASS,
    CRITICAL_MUST_PASS,
    AT_LEAST_ONE_MUST_PASS
}

/**
 * Enforcement policies for different scenarios
 */
enum class EnforcementPolicy(
    val forceExecuteOnFailure: Boolean,
    val rollbackOnFailure: Boolean,
    val notifyOnFailure: Boolean,
    val allowPartialSuccess: Boolean,
    val stopOnFirstFailure: Boolean,
    val retryFailedActions: Boolean,
    val actionFilterMode: ActionFilterMode,
    val minPriority: Int
) {
    STANDARD(false, true, true, false, false, true, ActionFilterMode.ALL, 0),
    FORCE_EXECUTE(true, false, true, true, false, true, ActionFilterMode.ALL, 0),
    DRY_RUN(false, false, false, true, true, false, ActionFilterMode.ALL, 0),
    CRITICAL_ONLY(false, true, true, false, true, false, ActionFilterMode.HIGH_PRIORITY_ONLY, 2),
    RELAXED(false, false, false, true, false, false, ActionFilterMode.IMMEDIATE_ONLY, 0)
}

/**
 * Action filtering modes
 */
enum class ActionFilterMode {
    ALL,
    IMMEDIATE_ONLY,
    HIGH_PRIORITY_ONLY,
    CUSTOM
}

/**
 * Results of action execution
 */
data class ActionExecutionResults(
    val executedActions: List<ExecutedAction>,
    val failedActions: List<FailedAction>
) {
    fun hasFailures(): Boolean = failedActions.isNotEmpty()
}

/**
 * Extension function to convert EnforcementContext to ExecutionContext
 */
private fun EnforcementContext.toExecutionContext(): ExecutionContext {
    return ExecutionContext(
        operation = operation,
        businessRuleResults = validationResult.ruleResults,
        userContext = userContext,
        applicationContext = applicationContext,
        environmentContext = environmentContext,
        executionMode = ExecutionContext.ExecutionMode.SYNCHRONOUS,
        timeoutMs = 30000L,
        retryCount = 0,
        metadata = metadata
    )
}