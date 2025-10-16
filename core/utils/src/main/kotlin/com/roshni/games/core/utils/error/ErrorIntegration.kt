package com.roshni.games.core.utils.error

import com.roshni.games.core.utils.feature.FeatureContext
import com.roshni.games.core.utils.feature.FeatureManager
import com.roshni.games.core.utils.integration.SystemIntegrationHub
import com.roshni.games.core.utils.workflow.WorkflowContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Integration layer for connecting error handling with existing systems
 */
class ErrorIntegration(
    private val errorHandlingFramework: ErrorHandlingFramework,
    private val systemIntegrationHub: SystemIntegrationHub,
    private val scope: CoroutineScope
) {

    init {
        setupErrorEventListeners()
        registerErrorRecoveryStrategies()
    }

    /**
     * Handle error from feature execution
     */
    suspend fun handleFeatureError(
        error: Throwable,
        featureContext: FeatureContext,
        featureId: String
    ): ErrorHandlingResult {
        val errorContext = ErrorContext.fromFeatureContext(
            operation = "feature_execution",
            component = "feature_manager",
            featureContext = featureContext
        ).copy(
            metadata = featureContext.metadata + mapOf(
                "featureId" to featureId,
                "executionId" to featureContext.executionId
            )
        )

        return errorHandlingFramework.handleThrowable(
            throwable = error,
            operation = "feature_execution",
            component = "feature_manager",
            context = errorContext
        )
    }

    /**
     * Handle error from workflow execution
     */
    suspend fun handleWorkflowError(
        error: Throwable,
        workflowContext: WorkflowContext,
        workflowId: String
    ): ErrorHandlingResult {
        val errorContext = ErrorContext(
            operation = "workflow_execution",
            component = "workflow_engine",
            userId = workflowContext.userId,
            sessionId = workflowContext.sessionId,
            metadata = workflowContext.variables + mapOf(
                "workflowId" to workflowId,
                "currentStep" to (workflowContext.currentStep?.toString() ?: "unknown")
            )
        )

        return errorHandlingFramework.handleThrowable(
            throwable = error,
            operation = "workflow_execution",
            component = "workflow_engine",
            context = errorContext
        )
    }

    /**
     * Handle error from system integration
     */
    suspend fun handleIntegrationError(
        error: Throwable,
        integrationType: String,
        operation: String,
        metadata: Map<String, Any> = emptyMap()
    ): ErrorHandlingResult {
        val errorContext = ErrorContext(
            operation = operation,
            component = "system_integration",
            metadata = metadata + mapOf(
                "integrationType" to integrationType,
                "operation" to operation
            )
        )

        return errorHandlingFramework.handleThrowable(
            throwable = error,
            operation = operation,
            component = "system_integration",
            context = errorContext
        )
    }

    /**
     * Setup automatic error event listeners
     */
    private fun setupErrorEventListeners() {
        scope.launch {
            errorHandlingFramework.errorEvents.collect { errorEvent ->
                try {
                    // Send error events to system integration hub
                    systemIntegrationHub.sendEvent(
                        event = "error_occurred",
                        data = mapOf(
                            "errorId" to errorEvent.error.errorId,
                            "errorType" to errorEvent.error.errorType.name,
                            "severity" to errorEvent.error.severity.name,
                            "component" to (errorEvent.context?.component ?: "unknown"),
                            "operation" to (errorEvent.context?.operation ?: "unknown"),
                            "handled" to (errorEvent.handlingResult?.success ?: false),
                            "timestamp" to errorEvent.timestamp
                        )
                    )

                    // Log error for debugging
                    Timber.tag("ErrorIntegration")
                        .d("Error event: ${errorEvent.error.errorType} - ${errorEvent.error.message}")

                } catch (e: Exception) {
                    Timber.e(e, "Failed to process error event")
                }
            }
        }
    }

    /**
     * Register error recovery strategies with existing systems
     */
    private fun registerErrorRecoveryStrategies() {
        scope.launch {
            try {
                // Register gaming-specific strategies
                val gamingStrategies = ErrorRecoveryStrategyFactory.createGamingStrategies()
                gamingStrategies.forEach { strategy ->
                    errorHandlingFramework.registerRecoveryStrategy(strategy)
                }

                Timber.d("Registered ${gamingStrategies.size} error recovery strategies")

            } catch (e: Exception) {
                Timber.e(e, "Failed to register error recovery strategies")
            }
        }
    }
}

/**
 * Extension functions for easy error handling integration
 */

/**
 * Execute operation with automatic error handling for features
 */
suspend fun <T> FeatureManager.executeWithErrorHandling(
    featureId: String,
    context: FeatureContext,
    operation: suspend () -> T
): ErrorHandlingExecutionResult<T> {
    // Get error handling framework from service locator or DI
    // This would be injected in a real implementation
    val errorHandlingFramework = getErrorHandlingFramework()

    return errorHandlingFramework.executeWithErrorHandling(
        operation = operation,
        operationName = "feature_execution",
        componentName = "feature_manager",
        context = ErrorContext.fromFeatureContext(
            operation = "feature_execution",
            component = "feature_manager",
            featureContext = context
        )
    )
}

/**
 * Execute operation with automatic error handling for workflows
 */
suspend fun <T> executeWorkflowWithErrorHandling(
    workflowId: String,
    context: WorkflowContext,
    operation: suspend () -> T
): ErrorHandlingExecutionResult<T> {
    val errorHandlingFramework = getErrorHandlingFramework()

    return errorHandlingFramework.executeWithErrorHandling(
        operation = operation,
        operationName = "workflow_execution",
        componentName = "workflow_engine",
        context = ErrorContext(
            operation = "workflow_execution",
            component = "workflow_engine",
            userId = context.userId,
            sessionId = context.sessionId,
            metadata = context.variables + mapOf("workflowId" to workflowId)
        )
    )
}

/**
 * Global error handler for uncaught exceptions
 */
object GlobalErrorHandler {

    private lateinit var errorHandlingFramework: ErrorHandlingFramework

    fun initialize(errorHandlingFramework: ErrorHandlingFramework) {
        this.errorHandlingFramework = errorHandlingFramework
    }

    suspend fun handleUncaughtException(
        thread: Thread,
        throwable: Throwable,
        operation: String = "unknown",
        component: String = "global"
    ) {
        if (!::errorHandlingFramework.isInitialized) {
            Timber.e(throwable, "Global error handler not initialized")
            return
        }

        val errorContext = ErrorContext(
            operation = operation,
            component = component,
            metadata = mapOf(
                "threadName" to thread.name,
                "threadId" to thread.id,
                "uncaughtException" to true
            )
        )

        try {
            errorHandlingFramework.handleThrowable(
                throwable = throwable,
                operation = operation,
                component = component,
                context = errorContext
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle uncaught exception")
        }
    }
}

/**
 * Error handling extensions for common operations
 */
object ErrorHandlingExtensions {

    /**
     * Wrap network operation with error handling
     */
    suspend fun <T> withNetworkErrorHandling(
        operation: suspend () -> T,
        operationName: String,
        url: String? = null
    ): ErrorHandlingExecutionResult<T> {
        val errorHandlingFramework = getErrorHandlingFramework()

        return errorHandlingFramework.executeWithErrorHandling(
            operation = operation,
            operationName = operationName,
            componentName = "network",
            context = ErrorContext(
                operation = operationName,
                component = "network",
                metadata = mapOf("url" to (url ?: "unknown"))
            )
        )
    }

    /**
     * Wrap file operation with error handling
     */
    suspend fun <T> withFileErrorHandling(
        operation: suspend () -> T,
        operationName: String,
        filePath: String
    ): ErrorHandlingExecutionResult<T> {
        val errorHandlingFramework = getErrorHandlingFramework()

        return errorHandlingFramework.executeWithErrorHandling(
            operation = operation,
            operationName = operationName,
            componentName = "file_system",
            context = ErrorContext(
                operation = operationName,
                component = "file_system",
                metadata = mapOf("filePath" to filePath)
            )
        )
    }

    /**
     * Wrap database operation with error handling
     */
    suspend fun <T> withDatabaseErrorHandling(
        operation: suspend () -> T,
        operationName: String,
        tableName: String? = null
    ): ErrorHandlingExecutionResult<T> {
        val errorHandlingFramework = getErrorHandlingFramework()

        return errorHandlingFramework.executeWithErrorHandling(
            operation = operation,
            operationName = operationName,
            componentName = "database",
            context = ErrorContext(
                operation = operationName,
                component = "database",
                metadata = mapOf("tableName" to (tableName ?: "unknown"))
            )
        )
    }
}

/**
 * Error handling middleware for feature manager
 */
class FeatureErrorHandlingMiddleware(
    private val errorHandlingFramework: ErrorHandlingFramework
) {

    suspend fun <T> executeFeatureWithErrorHandling(
        featureId: String,
        context: FeatureContext,
        operation: suspend () -> T
    ): ErrorHandlingExecutionResult<T> {
        return errorHandlingFramework.executeWithErrorHandling(
            operation = operation,
            operationName = "feature_execution",
            componentName = "feature_manager",
            context = ErrorContext.fromFeatureContext(
                operation = "feature_execution",
                component = "feature_manager",
                featureContext = context
            ).copy(
                metadata = context.metadata + mapOf("featureId" to featureId)
            )
        )
    }
}

/**
 * Error handling middleware for workflow engine
 */
class WorkflowErrorHandlingMiddleware(
    private val errorHandlingFramework: ErrorHandlingFramework
) {

    suspend fun <T> executeWorkflowWithErrorHandling(
        workflowId: String,
        context: WorkflowContext,
        operation: suspend () -> T
    ): ErrorHandlingExecutionResult<T> {
        return errorHandlingFramework.executeWithErrorHandling(
            operation = operation,
            operationName = "workflow_execution",
            componentName = "workflow_engine",
            context = ErrorContext(
                operation = "workflow_execution",
                component = "workflow_engine",
                userId = context.userId,
                sessionId = context.sessionId,
                metadata = context.variables + mapOf(
                    "workflowId" to workflowId,
                    "currentStep" to (context.currentStep?.toString() ?: "unknown")
                )
            )
        )
    }
}

/**
 * Integration with existing error handling systems
 */
class LegacyErrorHandlingIntegration(
    private val errorHandlingFramework: ErrorHandlingFramework
) {

    /**
     * Convert legacy error to new error handling system
     */
    suspend fun handleLegacyError(
        legacyError: Any, // Could be Exception or legacy error type
        operation: String,
        component: String,
        context: ErrorContext? = null
    ): ErrorHandlingResult {
        val error = when (legacyError) {
            is Throwable -> AppError.fromThrowable(legacyError, context)
            else -> AppError.generic(
                message = legacyError.toString(),
                errorType = ErrorType.UNKNOWN_ERROR,
                severity = ErrorSeverity.MEDIUM,
                context = context
            )
        }

        return errorHandlingFramework.handleError(
            error = error,
            context = context ?: ErrorContext(
                operation = operation,
                component = component
            )
        )
    }

    /**
     * Bridge for existing error reporting systems
     */
    suspend fun bridgeToLegacyReporting(
        error: AppError,
        context: ErrorContext? = null
    ) {
        // Send to existing analytics or reporting systems
        try {
            // Example: Send to existing analytics system
            // existingAnalytics.track("error", error.toAnalyticsData())

            Timber.d("Error bridged to legacy reporting: ${error.errorId}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to bridge error to legacy reporting")
        }
    }
}

/**
 * Get error handling framework instance (would be injected in real implementation)
 */
private fun getErrorHandlingFramework(): ErrorHandlingFramework {
    // In a real implementation, this would be injected via DI
    // For now, return a placeholder
    throw NotImplementedError("ErrorHandlingFramework should be injected via dependency injection")
}

/**
 * Error handling integration for UI components
 */
object UIErrorIntegration {

    /**
     * Handle UI-related errors with user-friendly presentation
     */
    suspend fun handleUIError(
        error: Throwable,
        operation: String,
        component: String,
        showUserFriendlyMessage: Boolean = true
    ) {
        val errorHandlingFramework = getErrorHandlingFramework()

        val result = errorHandlingFramework.handleThrowable(
            throwable = error,
            operation = operation,
            component = component
        )

        if (showUserFriendlyMessage && result.userMessage != null) {
            ErrorPresentation.displayCustomError(
                title = "Error",
                message = result.userMessage,
                severity = result.originalError.severity,
                style = com.roshni.games.core.utils.error.PresentationStyle.TOAST
            )
        }
    }

    /**
     * Show error dialog for critical UI errors
     */
    suspend fun showErrorDialog(
        error: AppError,
        context: ErrorContext? = null,
        actions: List<com.roshni.games.core.utils.error.UserAction> = emptyList()
    ) {
        ErrorPresentation.displayError(
            error = error,
            context = context,
            presentationStyle = com.roshni.games.core.utils.error.PresentationStyle.DIALOG,
            actions = actions
        )
    }
}