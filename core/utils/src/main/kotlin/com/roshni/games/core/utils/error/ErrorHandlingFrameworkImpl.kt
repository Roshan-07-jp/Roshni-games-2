package com.roshni.games.core.utils.error

import com.roshni.games.core.utils.integration.SystemIntegrationHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Default implementation of the ErrorHandlingFramework
 */
class ErrorHandlingFrameworkImpl(
    private val systemIntegrationHub: SystemIntegrationHub,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : ErrorHandlingFramework {

    private val mutex = Mutex()

    // State management
    private val _status = MutableStateFlow(ErrorHandlingStatus())
    private val _errorEvents = Channel<ErrorEvent>(Channel.UNLIMITED)

    // Error tracking
    private val errorStatistics = ConcurrentHashMap<String, AtomicLong>()
    private val errorHistory = mutableListOf<ErrorEvent>()
    private val recoveryStrategies = mutableListOf<ErrorRecoveryStrategy>()
    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreakerState>()

    // Configuration
    private var config = ErrorHandlingConfig()

    // Observables
    override val status: StateFlow<ErrorHandlingStatus> = _status.asStateFlow()
    override val errorEvents: Flow<ErrorEvent> = _errorEvents.receiveAsFlow()

    init {
        // Register default recovery strategies
        registerDefaultRecoveryStrategies()

        // Start background processing
        startBackgroundProcessing()
    }

    override suspend fun handleError(
        error: AppError,
        context: ErrorContext?,
        recoveryStrategies: List<ErrorRecoveryStrategy>?
    ): ErrorHandlingResult = mutex.withLock {
        val startTime = System.currentTimeMillis()
        val errorContext = context ?: createErrorContext(
            operation = "unknown",
            component = "unknown"
        )

        try {
            Timber.d("Handling error: ${error.errorType} - ${error.message}")

            // Update status
            updateStatus()

            // Create error event
            val errorEvent = ErrorEvent(
                error = error,
                context = errorContext,
                handlingResult = null,
                timestamp = System.currentTimeMillis()
            )

            // Store error event
            storeErrorEvent(errorEvent)

            // Get applicable recovery strategies
            val applicableStrategies = recoveryStrategies ?: getRecoveryStrategiesForError(error)

            // Execute recovery strategies
            val recoveryActions = mutableListOf<ErrorHandlingResult.RecoveryAction>()
            var finalError = error
            var shouldRetry = false
            var retryDelayMs: Long? = null

            for (strategy in applicableStrategies) {
                if (!strategy.canHandle(error)) continue

                val actionStartTime = System.currentTimeMillis()
                val action = ErrorHandlingResult.RecoveryAction(
                    action = strategy.strategyName,
                    success = false,
                    timestamp = actionStartTime
                )

                try {
                    val strategyResult = executeRecoveryStrategy(strategy, error, errorContext)
                    action.success = strategyResult.success

                    if (strategyResult.success) {
                        finalError = strategyResult.transformedError ?: error
                        shouldRetry = strategyResult.shouldRetry
                        retryDelayMs = strategyResult.retryDelayMs
                        break // Success, stop trying other strategies
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Recovery strategy ${strategy.strategyName} failed")
                    action.success = false
                } finally {
                    action.details = mapOf(
                        "executionTimeMs" to (System.currentTimeMillis() - actionStartTime)
                    )
                    recoveryActions.add(action)
                }
            }

            // Generate user-friendly message
            val userMessage = if (config.enableUserFriendlyMessages) {
                error.getUserFriendlyMessage()
            } else null

            // Determine if user action is required
            val requiresUserAction = error.severity.requiresImmediateAttention() ||
                applicableStrategies.any { it is ErrorRecoveryStrategy.UserInterventionStrategy }

            // Generate suggested actions
            val suggestedActions = generateSuggestedActions(error, applicableStrategies)

            // Create handling result
            val result = ErrorHandlingResult(
                success = finalError != error || shouldRetry,
                originalError = error,
                finalError = if (finalError != error) finalError else null,
                recoveryActions = recoveryActions,
                shouldRetry = shouldRetry,
                retryDelayMs = retryDelayMs,
                userMessage = userMessage,
                requiresUserAction = requiresUserAction,
                suggestedActions = suggestedActions,
                metadata = mapOf(
                    "handlingTimeMs" to (System.currentTimeMillis() - startTime),
                    "strategiesAttempted" to applicableStrategies.size
                )
            )

            // Update error event with result
            errorEvent.handlingResult = result
            _errorEvents.trySend(errorEvent)

            // Report error if enabled
            if (config.enableErrorReporting) {
                reportError(error, errorContext)
            }

            Timber.d("Error handling completed: ${result.success}")
            result

        } catch (e: Exception) {
            Timber.e(e, "Error handling framework failed")

            ErrorHandlingResult(
                success = false,
                originalError = error,
                recoveryActions = listOf(
                    ErrorHandlingResult.RecoveryAction(
                        action = "framework_error",
                        success = false,
                        timestamp = System.currentTimeMillis(),
                        details = mapOf("exception" to e.message.toString())
                    )
                ),
                metadata = mapOf("handlingTimeMs" to (System.currentTimeMillis() - startTime))
            )
        }
    }

    override suspend fun handleThrowable(
        throwable: Throwable,
        operation: String,
        component: String,
        context: ErrorContext?,
        recoveryStrategies: List<ErrorRecoveryStrategy>?
    ): ErrorHandlingResult {
        val error = AppError.fromThrowable(throwable, context)
        val errorContext = context ?: createErrorContext(operation, component)

        return handleError(error, errorContext, recoveryStrategies)
    }

    override suspend fun registerRecoveryStrategy(strategy: ErrorRecoveryStrategy): Boolean = mutex.withLock {
        try {
            if (recoveryStrategies.any { it.strategyName == strategy.strategyName }) {
                Timber.w("Recovery strategy ${strategy.strategyName} is already registered")
                return false
            }

            recoveryStrategies.add(strategy)
            updateStatus()

            Timber.d("Recovery strategy ${strategy.strategyName} registered successfully")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to register recovery strategy ${strategy.strategyName}")
            false
        }
    }

    override suspend fun unregisterRecoveryStrategy(strategyName: String): Boolean = mutex.withLock {
        try {
            val removed = recoveryStrategies.removeIf { it.strategyName == strategyName }
            if (removed) {
                updateStatus()
                Timber.d("Recovery strategy $strategyName unregistered successfully")
            }
            removed

        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister recovery strategy $strategyName")
            false
        }
    }

    override suspend fun getRecoveryStrategies(): List<ErrorRecoveryStrategy> {
        return recoveryStrategies.toList()
    }

    override suspend fun getRecoveryStrategiesForError(error: AppError): List<ErrorRecoveryStrategy> {
        return recoveryStrategies.filter { it.canHandle(error) }
    }

    override suspend fun reportError(
        error: AppError,
        context: ErrorContext?,
        includeStackTrace: Boolean
    ): ErrorReportingResult = mutex.withLock {
        try {
            val errorId = "report_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(10000)}"

            // Send to analytics system if available
            if (config.enableErrorAnalytics) {
                systemIntegrationHub.sendEvent(
                    event = "error_occurred",
                    data = mapOf(
                        "errorId" to error.errorId,
                        "errorType" to error.errorType.name,
                        "severity" to error.severity.name,
                        "message" to error.message,
                        "component" to (context?.component ?: "unknown"),
                        "operation" to (context?.operation ?: "unknown"),
                        "userId" to (context?.userId ?: "anonymous"),
                        "timestamp" to error.timestamp,
                        "stackTrace" to if (includeStackTrace && config.enableStackTraceCollection) {
                            error.cause?.stackTraceToString() ?: "No stack trace"
                        } else null
                    )
                )
            }

            // Update statistics
            updateErrorStatistics(error)

            ErrorReportingResult(
                success = true,
                errorId = errorId,
                message = "Error reported successfully"
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to report error")
            ErrorReportingResult(
                success = false,
                message = "Failed to report error: ${e.message}"
            )
        }
    }

    override suspend fun getErrorStatistics(
        timeRange: TimeRange?,
        errorType: ErrorType?,
        severity: ErrorSeverity?
    ): ErrorStatistics {
        val range = timeRange ?: TimeRange.last24Hours()
        val filteredErrors = errorHistory.filter { event ->
            val inTimeRange = event.timestamp in range.startTime..range.endTime
            val matchesType = errorType == null || event.error.errorType == errorType
            val matchesSeverity = severity == null || event.error.severity == severity
            inTimeRange && matchesType && matchesSeverity
        }

        val errorsByType = filteredErrors.groupBy { it.error.errorType }
            .mapValues { (_, events) -> events.size.toLong() }

        val errorsBySeverity = filteredErrors.groupBy { it.error.severity }
            .mapValues { (_, events) -> events.size.toLong() }

        val errorsByComponent = filteredErrors.groupBy { it.context?.component ?: "unknown" }
            .mapValues { (_, events) -> events.size.toLong() }

        val averageHandlingTime = filteredErrors
            .mapNotNull { it.handlingResult?.metadata?.get("handlingTimeMs") as? Long }
            .average()

        val recoverySuccessCount = filteredErrors.count { event ->
            event.handlingResult?.success == true
        }

        val recoverySuccessRate = if (filteredErrors.isNotEmpty()) {
            recoverySuccessCount.toDouble() / filteredErrors.size
        } else 0.0

        return ErrorStatistics(
            totalErrors = filteredErrors.size.toLong(),
            errorsByType = errorsByType,
            errorsBySeverity = errorsBySeverity,
            errorsByComponent = errorsByComponent,
            averageHandlingTimeMs = averageHandlingTime,
            recoverySuccessRate = recoverySuccessRate,
            timeRange = range
        )
    }

    override suspend fun clearErrorStatistics() = mutex.withLock {
        errorHistory.clear()
        errorStatistics.clear()
        Timber.d("Error statistics cleared")
    }

    override suspend fun configureErrorHandling(config: ErrorHandlingConfig): Boolean = mutex.withLock {
        try {
            this.config = config
            Timber.d("Error handling configuration updated")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to configure error handling")
            false
        }
    }

    override suspend fun getErrorHandlingConfig(): ErrorHandlingConfig {
        return config
    }

    override suspend fun <T> executeWithErrorHandling(
        operation: suspend () -> T,
        operationName: String,
        componentName: String,
        context: ErrorContext?,
        recoveryStrategies: List<ErrorRecoveryStrategy>?
    ): ErrorHandlingExecutionResult<T> {
        val startTime = System.currentTimeMillis()
        var attempts = 0
        val maxAttempts = config.maxRetryAttempts + 1

        while (attempts < maxAttempts) {
            attempts++

            try {
                val result = withContext(scope.coroutineContext) {
                    operation()
                }

                return ErrorHandlingExecutionResult(
                    success = true,
                    result = result,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    attempts = attempts
                )

            } catch (throwable: Throwable) {
                val errorContext = context ?: createErrorContext(operationName, componentName)
                val handlingResult = handleThrowable(
                    throwable = throwable,
                    operation = operationName,
                    component = componentName,
                    context = errorContext,
                    recoveryStrategies = recoveryStrategies
                )

                if (attempts < maxAttempts && handlingResult.shouldRetry) {
                    val delay = handlingResult.retryDelayMs ?: 1000L
                    kotlinx.coroutines.delay(delay)
                    continue
                }

                return ErrorHandlingExecutionResult(
                    success = false,
                    error = handlingResult.finalError ?: handlingResult.originalError,
                    handlingResult = handlingResult,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    attempts = attempts
                )
            }
        }

        // This should never be reached, but just in case
        return ErrorHandlingExecutionResult(
            success = false,
            executionTimeMs = System.currentTimeMillis() - startTime,
            attempts = attempts
        )
    }

    override fun observeErrors(component: String): Flow<ErrorEvent> {
        return errorEvents.filter { it.context?.component == component }
    }

    override fun observeErrorsByType(errorType: ErrorType): Flow<ErrorEvent> {
        return errorEvents.filter { it.error.errorType == errorType }
    }

    override fun observeErrorsBySeverity(severity: ErrorSeverity): Flow<ErrorEvent> {
        return errorEvents.filter { it.error.severity == severity }
    }

    override fun createErrorContext(
        operation: String,
        component: String,
        userId: String?,
        sessionId: String?,
        metadata: Map<String, Any>
    ): ErrorContext {
        return ErrorContext(
            operation = operation,
            component = component,
            userId = userId,
            sessionId = sessionId,
            metadata = metadata
        )
    }

    override suspend fun validateRecoveryStrategies(): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val strategyNames = recoveryStrategies.map { it.strategyName }
        val duplicates = strategyNames.groupBy { it }.filter { it.value.size > 1 }.keys

        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate recovery strategy names: $duplicates")
        }

        recoveryStrategies.forEach { strategy ->
            try {
                // Test strategy with sample errors
                val testErrors = listOf(
                    AppError.NetworkError.ConnectionError(),
                    AppError.GameplayError.InvalidMoveError("test", "test"),
                    AppError.ValidationError.InvalidInputError("test", "test", "test")
                )

                testErrors.forEach { error ->
                    try {
                        strategy.canHandle(error)
                    } catch (e: Exception) {
                        errors.add("Strategy ${strategy.strategyName} threw exception for error ${error.errorType}: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                errors.add("Strategy ${strategy.strategyName} validation failed: ${e.message}")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    override suspend fun getErrorTrends(
        timeRange: TimeRange,
        groupBy: ErrorGrouping
    ): ErrorTrends {
        val filteredErrors = errorHistory.filter { event ->
            event.timestamp in timeRange.startTime..timeRange.endTime
        }

        val dataPoints = when (groupBy) {
            ErrorGrouping.BY_HOUR -> {
                filteredErrors.groupBy { event ->
                    (event.timestamp / 3600000) * 3600000 // Round to hour
                }
            }
            ErrorGrouping.BY_DAY -> {
                filteredErrors.groupBy { event ->
                    (event.timestamp / 86400000) * 86400000 // Round to day
                }
            }
            else -> {
                filteredErrors.groupBy { it.error.errorType }
            }
        }.map { (key, events) ->
            ErrorTrends.ErrorDataPoint(
                timestamp = key,
                errorCount = events.size.toLong(),
                errorTypes = events.groupBy { it.error.errorType }
                    .mapValues { (_, typeEvents) -> typeEvents.size.toLong() }
            )
        }.sortedBy { it.timestamp }

        val trend = calculateTrend(dataPoints)
        val peakErrorTime = dataPoints.maxByOrNull { it.errorCount }?.timestamp
        val mostCommonErrorType = filteredErrors.groupBy { it.error.errorType }
            .maxByOrNull { it.value.size }?.key

        return ErrorTrends(
            timeRange = timeRange,
            dataPoints = dataPoints,
            trend = trend,
            peakErrorTime = peakErrorTime,
            mostCommonErrorType = mostCommonErrorType
        )
    }

    override suspend fun exportErrorData(
        timeRange: TimeRange?,
        format: ExportFormat
    ): ExportedErrorData {
        val range = timeRange ?: TimeRange.last24Hours()
        val filteredErrors = errorHistory.filter { it.timestamp in range.startTime..range.endTime }

        val data = when (format) {
            ExportFormat.JSON -> {
                // Simple JSON export - in a real implementation, use a JSON library
                """
                {
                    "exportFormat": "${format.name}",
                    "timeRange": {
                        "startTime": ${range.startTime},
                        "endTime": ${range.endTime}
                    },
                    "errorCount": ${filteredErrors.size},
                    "errors": [
                        ${filteredErrors.joinToString(",") { event ->
                            """
                            {
                                "errorId": "${event.error.errorId}",
                                "errorType": "${event.error.errorType}",
                                "severity": "${event.error.severity}",
                                "message": "${event.error.message}",
                                "timestamp": ${event.timestamp}
                            }
                            """.trimIndent()
                        }}
                    ]
                }
                """.trimIndent()
            }
            else -> "Export format $format not implemented"
        }

        return ExportedErrorData(
            format = format,
            data = data,
            timeRange = range
        )
    }

    override suspend fun importErrorData(data: ExportedErrorData): Boolean {
        // Simple implementation - in a real scenario, parse the data properly
        Timber.d("Error data import completed (placeholder implementation)")
        return true
    }

    override suspend fun initialize(): Boolean = mutex.withLock {
        try {
            _status.value = _status.value.copy(isInitialized = true)
            Timber.d("ErrorHandlingFramework initialized successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ErrorHandlingFramework")
            false
        }
    }

    override suspend fun shutdown() = mutex.withLock {
        try {
            _status.value = _status.value.copy(isShuttingDown = true)

            // Clear resources
            errorHistory.clear()
            errorStatistics.clear()
            recoveryStrategies.clear()
            circuitBreakers.clear()

            _status.value = ErrorHandlingStatus()

            Timber.d("ErrorHandlingFramework shutdown complete")

        } catch (e: Exception) {
            Timber.e(e, "Error during ErrorHandlingFramework shutdown")
        }
    }

    // Private helper methods

    private fun registerDefaultRecoveryStrategies() {
        val defaultStrategies = listOf(
            ErrorRecoveryStrategy.RetryStrategy(),
            ErrorRecoveryStrategy.FallbackStrategy("default_fallback"),
            ErrorRecoveryStrategy.CacheStrategy("default_cache"),
            ErrorRecoveryStrategy.OfflineStrategy("default_offline"),
            ErrorRecoveryStrategy.CircuitBreakerStrategy()
        )

        scope.launch {
            defaultStrategies.forEach { strategy ->
                registerRecoveryStrategy(strategy)
            }
        }
    }

    private fun startBackgroundProcessing() {
        scope.launch {
            // Background cleanup of old error history
            kotlinx.coroutines.delay(3600000) // Run every hour
            cleanupOldErrors()
        }
    }

    private suspend fun cleanupOldErrors() {
        val cutoffTime = System.currentTimeMillis() - config.errorCacheRetentionMs
        errorHistory.removeIf { it.timestamp < cutoffTime }
    }

    private suspend fun executeRecoveryStrategy(
        strategy: ErrorRecoveryStrategy,
        error: AppError,
        context: ErrorContext
    ): StrategyExecutionResult {
        return when (strategy) {
            is ErrorRecoveryStrategy.RetryStrategy -> {
                StrategyExecutionResult(
                    success = true,
                    shouldRetry = true,
                    retryDelayMs = strategy.calculateRetryDelay(0)
                )
            }
            is ErrorRecoveryStrategy.FallbackStrategy -> {
                // Execute fallback operation
                StrategyExecutionResult(success = true)
            }
            is ErrorRecoveryStrategy.CacheStrategy -> {
                // Use cached data
                StrategyExecutionResult(success = true)
            }
            is ErrorRecoveryStrategy.OfflineStrategy -> {
                // Queue for offline processing
                StrategyExecutionResult(success = true)
            }
            is ErrorRecoveryStrategy.UserInterventionStrategy -> {
                StrategyExecutionResult(
                    success = false,
                    requiresUserAction = true
                )
            }
            is ErrorRecoveryStrategy.CircuitBreakerStrategy -> {
                handleCircuitBreaker(strategy, error, context)
            }
        }
    }

    private suspend fun handleCircuitBreaker(
        strategy: ErrorRecoveryStrategy.CircuitBreakerStrategy,
        error: AppError,
        context: ErrorContext
    ): StrategyExecutionResult {
        val component = context.component
        val circuitState = circuitBreakers.getOrPut(component) {
            CircuitBreakerState()
        }

        return if (circuitState.isOpen()) {
            StrategyExecutionResult(
                success = false,
                message = "Circuit breaker is open for component: $component"
            )
        } else {
            circuitState.recordFailure()
            if (circuitState.failureCount >= strategy.failureThreshold) {
                circuitState.open()
                StrategyExecutionResult(
                    success = false,
                    message = "Circuit breaker opened for component: $component"
                )
            } else {
                StrategyExecutionResult(success = true)
            }
        }
    }

    private fun generateSuggestedActions(
        error: AppError,
        strategies: List<ErrorRecoveryStrategy>
    ): List<ErrorHandlingResult.UserAction> {
        return strategies.mapNotNull { strategy ->
            when (strategy) {
                is ErrorRecoveryStrategy.UserInterventionStrategy -> {
                    ErrorHandlingResult.UserAction(
                        action = strategy.requiredAction,
                        description = strategy.actionDescription,
                        priority = ErrorHandlingResult.UserAction.ActionPriority.HIGH
                    )
                }
                is ErrorRecoveryStrategy.RetryStrategy -> {
                    ErrorHandlingResult.UserAction(
                        action = "retry",
                        description = "Retry the operation",
                        priority = ErrorHandlingResult.UserAction.ActionPriority.NORMAL
                    )
                }
                else -> null
            }
        }
    }

    private fun storeErrorEvent(errorEvent: ErrorEvent) {
        errorHistory.add(errorEvent)

        // Limit history size
        if (errorHistory.size > config.maxErrorCacheSize) {
            errorHistory.removeAt(0)
        }
    }

    private fun updateErrorStatistics(error: AppError) {
        val key = "${error.errorType.name}_${error.severity.name}"
        errorStatistics.getOrPut(key) { AtomicLong(0) }.incrementAndGet()
    }

    private fun updateStatus() {
        _status.value = _status.value.copy(
            totalErrorsHandled = errorHistory.size.toLong(),
            activeRecoveryStrategies = recoveryStrategies.size,
            lastErrorTime = errorHistory.lastOrNull()?.timestamp
        )
    }

    private fun calculateTrend(dataPoints: List<ErrorTrends.ErrorDataPoint>): ErrorTrends.TrendDirection {
        if (dataPoints.size < 2) return ErrorTrends.TrendDirection.STABLE

        val firstHalf = dataPoints.take(dataPoints.size / 2)
        val secondHalf = dataPoints.takeLast(dataPoints.size / 2)

        val firstHalfAvg = firstHalf.map { it.errorCount }.average()
        val secondHalfAvg = secondHalf.map { it.errorCount }.average()

        val change = secondHalfAvg - firstHalfAvg
        val changePercentage = if (firstHalfAvg > 0) (change / firstHalfAvg) * 100 else 0.0

        return when {
            changePercentage > 20 -> ErrorTrends.TrendDirection.INCREASING
            changePercentage < -20 -> ErrorTrends.TrendDirection.DECREASING
            changePercentage in -20.0..20.0 -> ErrorTrends.TrendDirection.STABLE
            else -> ErrorTrends.TrendDirection.VOLATILE
        }
    }
}

/**
 * Result of strategy execution
 */
private data class StrategyExecutionResult(
    val success: Boolean = false,
    val shouldRetry: Boolean = false,
    val retryDelayMs: Long? = null,
    val requiresUserAction: Boolean = false,
    val transformedError: AppError? = null,
    val message: String? = null
)

/**
 * Circuit breaker state
 */
private data class CircuitBreakerState(
    var failureCount: Int = 0,
    var lastFailureTime: Long = 0,
    var state: CircuitBreakerStateEnum = CircuitBreakerStateEnum.CLOSED
) {

    enum class CircuitBreakerStateEnum {
        CLOSED, OPEN, HALF_OPEN
    }

    fun isOpen(): Boolean = state == CircuitBreakerStateEnum.OPEN

    fun recordFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
    }

    fun open() {
        state = CircuitBreakerStateEnum.OPEN
    }

    fun reset() {
        failureCount = 0
        state = CircuitBreakerStateEnum.CLOSED
    }
}