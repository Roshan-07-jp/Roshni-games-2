package com.roshni.games.core.utils.error

/**
 * Enumeration of error severity levels for proper error handling and user communication
 */
enum class ErrorSeverity {
    /**
     * Low severity errors that don't affect core functionality
     * Examples: Optional feature failures, non-critical data issues
     */
    LOW,

    /**
     * Medium severity errors that affect some functionality but allow continued use
     * Examples: Network timeouts, partial data loading failures
     */
    MEDIUM,

    /**
     * High severity errors that significantly impact user experience
     * Examples: Game save failures, critical feature unavailability
     */
    HIGH,

    /**
     * Critical errors that prevent core application functionality
     * Examples: Authentication failures, system-level errors, data corruption
     */
    CRITICAL,

    /**
     * Fatal errors that require immediate attention or app restart
     * Examples: Out of memory, storage full, hardware failures
     */
    FATAL
}

/**
 * Extension function to determine if an error severity requires immediate user attention
 */
fun ErrorSeverity.requiresImmediateAttention(): Boolean {
    return this == HIGH || this == CRITICAL || this == FATAL
}

/**
 * Extension function to determine if an error severity should show a dialog
 */
fun ErrorSeverity.shouldShowDialog(): Boolean {
    return this == CRITICAL || this == FATAL
}

/**
 * Extension function to determine if an error severity should be reported to analytics
 */
fun ErrorSeverity.shouldReportToAnalytics(): Boolean {
    return this != LOW
}

/**
 * Extension function to determine if an error severity should trigger error recovery
 */
fun ErrorSeverity.shouldTriggerRecovery(): Boolean {
    return this == MEDIUM || this == HIGH || this == CRITICAL
}