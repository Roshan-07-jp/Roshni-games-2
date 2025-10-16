package com.roshni.games.core.utils.error

import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Sealed class representing all possible application errors
 */
sealed class AppError(
    open val errorType: ErrorType,
    open val severity: ErrorSeverity,
    open val message: String,
    open val cause: Throwable? = null,
    open val context: ErrorContext? = null,
    open val timestamp: Long = System.currentTimeMillis(),
    open val errorId: String = generateErrorId()
) : Exception(message, cause) {

    // Network related errors
    sealed class NetworkError(
        errorType: ErrorType,
        message: String,
        cause: Throwable? = null,
        context: ErrorContext? = null
    ) : AppError(errorType, ErrorSeverity.HIGH, message, cause, context) {

        data class ConnectionError(
            val url: String? = null,
            val httpStatusCode: Int? = null,
            override val message: String = "Network connection failed",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : NetworkError(ErrorType.NETWORK_ERROR, message, cause, context)

        data class TimeoutError(
            val timeoutMs: Long,
            val operation: String,
            override val message: String = "Network operation timed out after $timeoutMs ms",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : NetworkError(ErrorType.NETWORK_TIMEOUT, message, cause, context)

        data class UnavailableError(
            val retryAfterMs: Long? = null,
            override val message: String = "Network service unavailable",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : NetworkError(ErrorType.NETWORK_UNAVAILABLE, message, cause, context)

        data class AuthenticationError(
            val authMethod: String? = null,
            override val message: String = "Network authentication failed",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : NetworkError(ErrorType.NETWORK_AUTHENTICATION_FAILED, message, cause, context)

        data class ServerError(
            val serverMessage: String? = null,
            val errorCode: String? = null,
            override val message: String = "Server error occurred",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : NetworkError(ErrorType.NETWORK_SERVER_ERROR, message, cause, context)

        data class RateLimitedError(
            val retryAfterMs: Long,
            val limitType: String? = null,
            override val message: String = "Rate limit exceeded",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : NetworkError(ErrorType.NETWORK_RATE_LIMITED, message, cause, context)
    }

    // Gameplay related errors
    sealed class GameplayError(
        errorType: ErrorType,
        message: String,
        cause: Throwable? = null,
        context: ErrorContext? = null
    ) : AppError(errorType, ErrorSeverity.MEDIUM, message, cause, context) {

        data class InvalidMoveError(
            val gameId: String,
            val moveDescription: String,
            val ruleViolation: String? = null,
            override val message: String = "Invalid move attempted",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : GameplayError(ErrorType.GAMEPLAY_INVALID_MOVE, message, cause, context)

        data class RuleViolationError(
            val gameId: String,
            val violatedRule: String,
            val ruleDescription: String? = null,
            override val message: String = "Game rule violation",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : GameplayError(ErrorType.GAMEPLAY_RULE_VIOLATION, message, cause, context)

        data class InvalidStateError(
            val gameId: String,
            val expectedState: String,
            val actualState: String,
            override val message: String = "Game state is invalid",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : GameplayError(ErrorType.GAMEPLAY_STATE_INVALID, message, cause, context)

        data class SessionExpiredError(
            val sessionId: String,
            val expiryTime: Long,
            override val message: String = "Game session has expired",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : GameplayError(ErrorType.GAMEPLAY_SESSION_EXPIRED, message, cause, context)

        data class SaveFailedError(
            val gameId: String,
            val saveOperation: String,
            override val message: String = "Failed to save game progress",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : GameplayError(ErrorType.GAMEPLAY_SAVE_FAILED, message, cause, context)

        data class LoadFailedError(
            val gameId: String,
            val loadOperation: String,
            override val message: String = "Failed to load game data",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : GameplayError(ErrorType.GAMEPLAY_LOAD_FAILED, message, cause, context)

        data class SyncFailedError(
            val gameId: String,
            val syncOperation: String,
            val conflictDetails: String? = null,
            override val message: String = "Game synchronization failed",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : GameplayError(ErrorType.GAMEPLAY_SYNC_FAILED, message, cause, context)
    }

    // Permission related errors
    sealed class PermissionError(
        errorType: ErrorType,
        message: String,
        cause: Throwable? = null,
        context: ErrorContext? = null
    ) : AppError(errorType, ErrorSeverity.HIGH, message, cause, context) {

        data class DeniedError(
            val permission: String,
            val rationale: String? = null,
            override val message: String = "Permission denied: $permission",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : PermissionError(ErrorType.PERMISSION_DENIED, message, cause, context)

        data class NotGrantedError(
            val permission: String,
            val grantResult: String? = null,
            override val message: String = "Permission not granted: $permission",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : PermissionError(ErrorType.PERMISSION_NOT_GRANTED, message, cause, context)

        data class RestrictedError(
            val permission: String,
            val restrictionReason: String,
            override val message: String = "Permission restricted: $permission",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : PermissionError(ErrorType.PERMISSION_RESTRICTED, message, cause, context)

        data class ExpiredError(
            val permission: String,
            val expiryTime: Long,
            override val message: String = "Permission expired: $permission",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : PermissionError(ErrorType.PERMISSION_EXPIRED, message, cause, context)

        data class InsufficientScopeError(
            val permission: String,
            val requiredScope: String,
            val currentScope: String,
            override val message: String = "Insufficient permission scope",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : PermissionError(ErrorType.PERMISSION_SCOPE_INSUFFICIENT, message, cause, context)
    }

    // Validation related errors
    sealed class ValidationError(
        errorType: ErrorType,
        message: String,
        cause: Throwable? = null,
        context: ErrorContext? = null
    ) : AppError(errorType, ErrorSeverity.MEDIUM, message, cause, context) {

        data class InvalidInputError(
            val fieldName: String,
            val inputValue: Any?,
            val validationRule: String,
            override val message: String = "Invalid input for field: $fieldName",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : ValidationError(ErrorType.VALIDATION_INVALID_INPUT, message, cause, context)

        data class MissingRequiredFieldError(
            val fieldName: String,
            val fieldType: String? = null,
            override val message: String = "Required field missing: $fieldName",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : ValidationError(ErrorType.VALIDATION_MISSING_REQUIRED_FIELD, message, cause, context)

        data class InvalidFormatError(
            val fieldName: String,
            val expectedFormat: String,
            val actualValue: String,
            override val message: String = "Invalid format for field: $fieldName",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : ValidationError(ErrorType.VALIDATION_FORMAT_INVALID, message, cause, context)

        data class OutOfBoundsError(
            val fieldName: String,
            val minValue: Any?,
            val maxValue: Any?,
            val actualValue: Any?,
            override val message: String = "Value out of bounds for field: $fieldName",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : ValidationError(ErrorType.VALIDATION_RANGE_OUT_OF_BOUNDS, message, cause, context)

        data class ConstraintViolationError(
            val constraintName: String,
            val constraintDescription: String,
            val violatingValue: Any?,
            override val message: String = "Constraint violation: $constraintName",
            override val cause: Throwable? = null,
            override val context: ErrorContext? = null
        ) : ValidationError(ErrorType.VALIDATION_CONSTRAINT_VIOLATION, message, cause, context)
    }

    // Utility functions for error handling
    companion object {
        private fun generateErrorId(): String {
            return "err_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(10000)}"
        }

        /**
         * Convert a Throwable to an AppError
         */
        fun fromThrowable(throwable: Throwable, context: ErrorContext? = null): AppError {
            return when (throwable) {
                is AppError -> throwable
                is IOException -> NetworkError.ConnectionError(
                    message = "IO operation failed: ${throwable.message}",
                    cause = throwable,
                    context = context
                )
                is TimeoutException -> NetworkError.TimeoutError(
                    timeoutMs = 30000, // Default timeout
                    operation = "unknown",
                    message = "Operation timed out: ${throwable.message}",
                    cause = throwable,
                    context = context
                )
                is SecurityException -> PermissionError.DeniedError(
                    permission = "unknown",
                    message = "Security exception: ${throwable.message}",
                    cause = throwable,
                    context = context
                )
                is IllegalArgumentException -> ValidationError.InvalidInputError(
                    fieldName = "unknown",
                    inputValue = null,
                    validationRule = "argument_validation",
                    message = "Invalid argument: ${throwable.message}",
                    cause = throwable,
                    context = context
                )
                is CancellationException -> AppError(
                    errorType = ErrorType.OPERATION_CANCELLED,
                    severity = ErrorSeverity.LOW,
                    message = "Operation was cancelled",
                    cause = throwable,
                    context = context
                )
                else -> AppError(
                    errorType = ErrorType.UNKNOWN_ERROR,
                    severity = ErrorSeverity.MEDIUM,
                    message = "Unknown error: ${throwable.message}",
                    cause = throwable,
                    context = context
                )
            }
        }

        /**
         * Create a generic error
         */
        fun generic(
            message: String,
            errorType: ErrorType = ErrorType.UNKNOWN_ERROR,
            severity: ErrorSeverity = ErrorSeverity.MEDIUM,
            cause: Throwable? = null,
            context: ErrorContext? = null
        ): AppError {
            return AppError(errorType, severity, message, cause, context)
        }
    }
}

/**
 * Extension function to check if an error is recoverable
 */
fun AppError.isRecoverable(): Boolean {
    return when (this) {
        is AppError.NetworkError -> true
        is AppError.GameplayError -> {
            // Some gameplay errors might be recoverable
            this !is AppError.GameplayError.InvalidStateError
        }
        is AppError.PermissionError -> {
            // Permission errors might be recoverable through user action
            this !is AppError.PermissionError.RestrictedError
        }
        is AppError.ValidationError -> true // Validation errors are typically recoverable
        else -> false
    }
}

/**
 * Extension function to get user-friendly error message
 */
fun AppError.getUserFriendlyMessage(): String {
    return when (this) {
        is AppError.NetworkError.ConnectionError -> "Connection problem. Please check your internet and try again."
        is AppError.NetworkError.TimeoutError -> "Request timed out. Please try again."
        is AppError.NetworkError.UnavailableError -> "Service temporarily unavailable. Please try again later."
        is AppError.NetworkError.AuthenticationError -> "Authentication failed. Please sign in again."
        is AppError.NetworkError.ServerError -> "Server error occurred. Please try again later."
        is AppError.NetworkError.RateLimitedError -> "Too many requests. Please wait a moment and try again."

        is AppError.GameplayError.InvalidMoveError -> "That move isn't allowed right now. Please try something else."
        is AppError.GameplayError.RuleViolationError -> "This action violates the game rules."
        is AppError.GameplayError.InvalidStateError -> "Game is in an invalid state. Please restart the game."
        is AppError.GameplayError.SessionExpiredError -> "Your game session has expired. Please start a new game."
        is AppError.GameplayError.SaveFailedError -> "Failed to save your progress. Please check your storage space."
        is AppError.GameplayError.LoadFailedError -> "Failed to load game data. Please try again."
        is AppError.GameplayError.SyncFailedError -> "Failed to sync game data. Playing offline for now."

        is AppError.PermissionError.DeniedError -> "Permission required. Please grant the necessary permissions in settings."
        is AppError.PermissionError.NotGrantedError -> "Permission not granted. Some features may not work properly."
        is AppError.PermissionError.RestrictedError -> "Permission restricted. Please contact support if you need access."
        is AppError.PermissionError.ExpiredError -> "Permission expired. Please grant permission again."
        is AppError.PermissionError.InsufficientScopeError -> "Insufficient permissions. Please contact support."

        is AppError.ValidationError.InvalidInputError -> "Please check your input and try again."
        is AppError.ValidationError.MissingRequiredFieldError -> "Please fill in all required fields."
        is AppError.ValidationError.InvalidFormatError -> "Please check the format and try again."
        is AppError.ValidationError.OutOfBoundsError -> "Value is out of allowed range."
        is AppError.ValidationError.ConstraintViolationError -> "This action isn't allowed."

        else -> "Something went wrong. Please try again."
    }
}