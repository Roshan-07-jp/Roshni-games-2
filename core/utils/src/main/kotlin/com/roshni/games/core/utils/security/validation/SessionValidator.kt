package com.roshni.games.core.utils.security.validation

import com.roshni.games.core.utils.security.model.SecurityContext
import com.roshni.games.core.utils.security.model.SessionInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Comprehensive session validation system for security management
 */
class SessionValidator(
    private val maxSessionTimeHours: Long = 24,
    private val maxInactiveTimeMinutes: Long = 30,
    private val maxConcurrentSessions: Int = 5
) {

    private val mutex = Mutex()
    private val activeSessions = mutableMapOf<String, SessionValidationInfo>()

    /**
     * Information about session validation
     */
    data class SessionValidationInfo(
        val sessionId: String,
        val userId: String,
        val deviceId: String,
        val createdAt: LocalDateTime,
        val lastValidatedAt: LocalDateTime,
        val validationCount: Int = 0,
        val isValid: Boolean = true,
        val metadata: Map<String, Any> = emptyMap()
    )

    /**
     * Result of session validation
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
        data class Expired(val expiredAt: LocalDateTime) : ValidationResult()
        data class TooManySessions(val currentCount: Int, val maxAllowed: Int) : ValidationResult()
        data class SuspiciousActivity(val reason: String) : ValidationResult()
    }

    /**
     * Validate a security context and session
     */
    suspend fun validateSession(
        context: SecurityContext,
        sessionInfo: SessionInfo? = null
    ): ValidationResult {
        return try {
            mutex.withLock {
                // Check if session exists in our tracking
                val validationInfo = activeSessions[context.sessionId]

                // Basic validation checks
                val basicValidation = performBasicValidation(context, sessionInfo)
                if (basicValidation != ValidationResult.Valid) {
                    return basicValidation
                }

                // Check for suspicious activity
                val suspiciousCheck = checkSuspiciousActivity(context, validationInfo)
                if (suspiciousCheck != ValidationResult.Valid) {
                    return suspiciousCheck
                }

                // Update validation tracking
                updateValidationTracking(context)

                ValidationResult.Valid
            }
        } catch (e: Exception) {
            Timber.e(e, "Session validation error")
            ValidationResult.Invalid("Validation error: ${e.message}")
        }
    }

    /**
     * Register a new session for tracking
     */
    suspend fun registerSession(
        context: SecurityContext,
        sessionInfo: SessionInfo
    ): Result<Unit> {
        return try {
            mutex.withLock {
                // Check if user already has too many active sessions
                val userSessions = activeSessions.values.filter { it.userId == context.userId }
                if (userSessions.size >= maxConcurrentSessions) {
                    return Result.failure(SecurityException("Too many concurrent sessions"))
                }

                val validationInfo = SessionValidationInfo(
                    sessionId = context.sessionId,
                    userId = context.userId,
                    deviceId = context.deviceId,
                    createdAt = LocalDateTime.now(),
                    lastValidatedAt = LocalDateTime.now(),
                    metadata = context.metadata
                )

                activeSessions[context.sessionId] = validationInfo
                Timber.d("Session registered: ${context.sessionId}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Session registration error")
            Result.failure(e)
        }
    }

    /**
     * Invalidate a session
     */
    suspend fun invalidateSession(sessionId: String, reason: String = "Manual invalidation"): Result<Unit> {
        return try {
            mutex.withLock {
                val validationInfo = activeSessions.remove(sessionId)
                if (validationInfo != null) {
                    Timber.d("Session invalidated: $sessionId, reason: $reason")
                    Result.success(Unit)
                } else {
                    Result.failure(SecurityException("Session not found"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Session invalidation error")
            Result.failure(e)
        }
    }

    /**
     * Clean up expired sessions
     */
    suspend fun cleanupExpiredSessions(): Int {
        return try {
            mutex.withLock {
                val now = LocalDateTime.now()
                val expiredSessions = activeSessions.entries.filter { (_, info) ->
                    now.isAfter(info.createdAt.plusHours(maxSessionTimeHours)) ||
                    now.isAfter(info.lastValidatedAt.plusMinutes(maxInactiveTimeMinutes))
                }

                expiredSessions.forEach { (sessionId, _) ->
                    activeSessions.remove(sessionId)
                }

                val cleanedCount = expiredSessions.size
                if (cleanedCount > 0) {
                    Timber.d("Cleaned up $cleanedCount expired sessions")
                }
                cleanedCount
            }
        } catch (e: Exception) {
            Timber.e(e, "Session cleanup error")
            0
        }
    }

    /**
     * Get session statistics
     */
    suspend fun getSessionStatistics(): Map<String, Any> {
        return try {
            mutex.withLock {
                val now = LocalDateTime.now()
                val totalSessions = activeSessions.size
                val validSessions = activeSessions.values.count { it.isValid }
                val expiredSessions = activeSessions.values.count { info ->
                    now.isAfter(info.createdAt.plusHours(maxSessionTimeHours))
                }
                val inactiveSessions = activeSessions.values.count { info ->
                    now.isAfter(info.lastValidatedAt.plusMinutes(maxInactiveTimeMinutes))
                }

                mapOf(
                    "totalSessions" to totalSessions,
                    "validSessions" to validSessions,
                    "expiredSessions" to expiredSessions,
                    "inactiveSessions" to inactiveSessions,
                    "maxConcurrentSessions" to maxConcurrentSessions,
                    "cleanupThresholdHours" to maxSessionTimeHours,
                    "inactivityThresholdMinutes" to maxInactiveTimeMinutes
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Session statistics error")
            emptyMap()
        }
    }

    /**
     * Check if user has too many sessions
     */
    suspend fun checkUserSessionLimit(userId: String): ValidationResult {
        return try {
            mutex.withLock {
                val userSessions = activeSessions.values.filter { it.userId == userId }
                if (userSessions.size >= maxConcurrentSessions) {
                    ValidationResult.TooManySessions(userSessions.size, maxConcurrentSessions)
                } else {
                    ValidationResult.Valid
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "User session limit check error")
            ValidationResult.Invalid("Check error: ${e.message}")
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private fun performBasicValidation(
        context: SecurityContext,
        sessionInfo: SessionInfo?
    ): ValidationResult {
        // Check if context is valid
        if (!context.isValid()) {
            return ValidationResult.Invalid("Invalid security context")
        }

        // Check if session has expired
        if (context.isExpired()) {
            return ValidationResult.Expired(context.expiresAt)
        }

        // Check session info consistency
        if (sessionInfo != null) {
            if (context.userId != sessionInfo.userId) {
                return ValidationResult.Invalid("Session user mismatch")
            }
            if (context.deviceId != sessionInfo.deviceId) {
                return ValidationResult.Invalid("Session device mismatch")
            }
        }

        return ValidationResult.Valid
    }

    private fun checkSuspiciousActivity(
        context: SecurityContext,
        validationInfo: SessionValidationInfo?
    ): ValidationResult {
        val now = LocalDateTime.now()

        // Check for rapid validation attempts
        if (validationInfo != null) {
            val timeSinceLastValidation = java.time.Duration.between(
                validationInfo.lastValidatedAt, now
            ).toMinutes()

            if (timeSinceLastValidation < 1 && validationInfo.validationCount > 10) {
                return ValidationResult.SuspiciousActivity("Rapid validation attempts detected")
            }
        }

        // Check for unusual time patterns
        val hour = now.hour
        if (hour < 6 || hour > 23) {
            // Late night/early morning access - could be suspicious
            // In a real implementation, you might want to make this configurable
        }

        return ValidationResult.Valid
    }

    private suspend fun updateValidationTracking(context: SecurityContext) {
        val currentInfo = activeSessions[context.sessionId]
        if (currentInfo != null) {
            val updatedInfo = currentInfo.copy(
                lastValidatedAt = LocalDateTime.now(),
                validationCount = currentInfo.validationCount + 1
            )
            activeSessions[context.sessionId] = updatedInfo
        }
    }
}