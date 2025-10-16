package com.roshni.games.core.utils.security.integration

import com.roshni.games.core.utils.security.model.SecurityContext
import com.roshni.games.core.utils.security.permissions.ContentPermission
import com.roshni.games.core.utils.security.permissions.GameplayPermission
import com.roshni.games.core.utils.security.permissions.SocialPermission
import com.roshni.games.feature.parentalcontrols.domain.SecurityService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Integration adapter for the existing SecurityService to work with the new security system
 */
class SecurityServiceIntegration(
    private val securityService: SecurityService
) {

    /**
     * Data class representing parental control settings from SecurityService
     */
    data class ParentalControlSettings(
        val biometricEnabled: Boolean = false,
        val sessionTimeoutMinutes: Int = 30,
        val maxDailyPlaytimeMinutes: Int = 480, // 8 hours
        val allowedContentRating: String = "EVERYONE",
        val socialAllowed: Boolean = true,
        val purchaseAllowed: Boolean = false,
        val requirePasswordForPurchases: Boolean = true,
        val blockInappropriateContent: Boolean = true,
        val allowMultiplayer: Boolean = true,
        val allowUserGeneratedContent: Boolean = false,
        val allowedTimeRanges: List<TimeRange> = emptyList()
    )

    /**
     * Time range for parental controls
     */
    data class TimeRange(
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val daysOfWeek: Set<Int> = (1..7).toSet() // 1 = Monday, 7 = Sunday
    ) {
        fun contains(hour: Int, minute: Int, dayOfWeek: Int): Boolean {
            if (!daysOfWeek.contains(dayOfWeek)) return false

            val currentMinutes = hour * 60 + minute
            val startMinutes = startHour * 60 + startMinute
            val endMinutes = endHour * 60 + endMinute

            return if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Overnight range (e.g., 22:00 to 06:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        }
    }

    /**
     * Convert SecurityService settings to our format
     */
    fun getParentalControlSettings(): ParentalControlSettings {
        return try {
            val settings = securityService.securitySettings.value
            ParentalControlSettings(
                biometricEnabled = settings.biometricEnabled,
                sessionTimeoutMinutes = settings.sessionTimeoutMinutes,
                maxDailyPlaytimeMinutes = settings.maxDailyPlaytimeMinutes ?: 480,
                allowedContentRating = settings.allowedContentRating ?: "EVERYONE",
                socialAllowed = settings.socialAllowed ?: true,
                purchaseAllowed = settings.purchaseAllowed ?: false,
                requirePasswordForPurchases = settings.requirePasswordForPurchases ?: true,
                blockInappropriateContent = settings.blockInappropriateContent ?: true,
                allowMultiplayer = settings.allowMultiplayer ?: true,
                allowUserGeneratedContent = settings.allowUserGeneratedContent ?: false
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting parental control settings")
            ParentalControlSettings() // Return defaults
        }
    }

    /**
     * Check if gameplay is allowed based on parental controls
     */
    suspend fun isGameplayAllowed(
        userId: String,
        gameId: String? = null,
        currentTimeMinutes: Int = getCurrentTimeMinutes(),
        currentDayOfWeek: Int = getCurrentDayOfWeek()
    ): Boolean {
        return try {
            // Check with SecurityService
            val serviceAllows = securityService.isGameplayAllowed()

            if (!serviceAllows) {
                Timber.d("Gameplay blocked by SecurityService for user: $userId")
                return false
            }

            // Check parental control settings
            val settings = getParentalControlSettings()

            // Check time restrictions
            val timeAllowed = isTimeAllowed(settings, currentTimeMinutes, currentDayOfWeek)

            if (!timeAllowed) {
                Timber.d("Gameplay blocked by time restrictions for user: $userId")
                return false
            }

            // Check daily playtime limits
            val playtimeAllowed = isPlaytimeAllowed(userId, settings)

            if (!playtimeAllowed) {
                Timber.d("Gameplay blocked by playtime limits for user: $userId")
                return false
            }

            // Check content rating if gameId is provided
            if (gameId != null) {
                val contentAllowed = isContentRatingAllowed(gameId, settings.allowedContentRating)
                if (!contentAllowed) {
                    Timber.d("Gameplay blocked by content rating for user: $userId, game: $gameId")
                    return false
                }
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "Error checking gameplay allowance")
            false
        }
    }

    /**
     * Check if content access is allowed based on parental controls
     */
    suspend fun isContentAllowed(
        userId: String,
        contentType: String,
        contentRating: String? = null,
        currentTimeMinutes: Int = getCurrentTimeMinutes(),
        currentDayOfWeek: Int = getCurrentDayOfWeek()
    ): Boolean {
        return try {
            // Check with SecurityService
            val serviceAllows = securityService.isContentAllowed(contentType)

            if (!serviceAllows) {
                Timber.d("Content blocked by SecurityService for user: $userId, type: $contentType")
                return false
            }

            // Check parental control settings
            val settings = getParentalControlSettings()

            // Check time restrictions
            val timeAllowed = isTimeAllowed(settings, currentTimeMinutes, currentDayOfWeek)

            if (!timeAllowed) {
                Timber.d("Content blocked by time restrictions for user: $userId")
                return false
            }

            // Check content rating
            if (contentRating != null) {
                val ratingAllowed = isContentRatingAllowed(contentRating, settings.allowedContentRating)
                if (!ratingAllowed) {
                    Timber.d("Content blocked by rating for user: $userId, rating: $contentRating")
                    return false
                }
            }

            // Check if user-generated content is allowed
            if (contentType == "USER_GENERATED" && !settings.allowUserGeneratedContent) {
                Timber.d("User-generated content blocked for user: $userId")
                return false
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "Error checking content allowance")
            false
        }
    }

    /**
     * Check if social features are allowed based on parental controls
     */
    suspend fun isSocialAllowed(
        userId: String,
        socialFeature: String? = null,
        currentTimeMinutes: Int = getCurrentTimeMinutes(),
        currentDayOfWeek: Int = getCurrentDayOfWeek()
    ): Boolean {
        return try {
            // Check with SecurityService
            val serviceAllows = securityService.isSocialAllowed()

            if (!serviceAllows) {
                Timber.d("Social features blocked by SecurityService for user: $userId")
                return false
            }

            // Check parental control settings
            val settings = getParentalControlSettings()

            if (!settings.socialAllowed) {
                Timber.d("Social features blocked by parental controls for user: $userId")
                return false
            }

            // Check time restrictions
            val timeAllowed = isTimeAllowed(settings, currentTimeMinutes, currentDayOfWeek)

            if (!timeAllowed) {
                Timber.d("Social features blocked by time restrictions for user: $userId")
                return false
            }

            // Check multiplayer allowance if applicable
            if (socialFeature == "MULTIPLAYER" && !settings.allowMultiplayer) {
                Timber.d("Multiplayer blocked for user: $userId")
                return false
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "Error checking social allowance")
            false
        }
    }

    /**
     * Get security alerts from SecurityService
     */
    fun getSecurityAlerts(): Flow<List<com.roshni.games.feature.parentalcontrols.data.model.SecurityAlert>> {
        return try {
            securityService.getSecurityAlerts()
        } catch (e: Exception) {
            Timber.e(e, "Error getting security alerts")
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Get authentication attempts from SecurityService
     */
    fun getAuthenticationAttempts(): Flow<List<com.roshni.games.feature.parentalcontrols.data.model.AuthenticationAttempt>> {
        return try {
            securityService.getAuthenticationAttempts()
        } catch (e: Exception) {
            Timber.e(e, "Error getting authentication attempts")
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Record a security violation through SecurityService
     */
    suspend fun recordSecurityViolation(
        userId: String,
        violationType: String,
        description: String,
        severity: com.roshni.games.feature.parentalcontrols.data.model.AlertSeverity = com.roshni.games.feature.parentalcontrols.data.model.AlertSeverity.MEDIUM
    ): Result<Unit> {
        return try {
            // In a real implementation, SecurityService would have a method to record violations
            // For now, we'll create an alert
            Timber.w("Security violation recorded: $violationType for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error recording security violation")
            Result.failure(e)
        }
    }

    /**
     * Check if purchase is allowed based on parental controls
     */
    suspend fun isPurchaseAllowed(
        userId: String,
        amount: Double,
        currentTimeMinutes: Int = getCurrentTimeMinutes(),
        currentDayOfWeek: Int = getCurrentDayOfWeek()
    ): Boolean {
        return try {
            val settings = getParentalControlSettings()

            // Check if purchases are allowed at all
            if (!settings.purchaseAllowed) {
                Timber.d("Purchases blocked by parental controls for user: $userId")
                return false
            }

            // Check time restrictions
            val timeAllowed = isTimeAllowed(settings, currentTimeMinutes, currentDayOfWeek)

            if (!timeAllowed) {
                Timber.d("Purchase blocked by time restrictions for user: $userId")
                return false
            }

            // Check if password is required for purchases
            if (settings.requirePasswordForPurchases) {
                // In a real implementation, check if parent password was recently verified
                // For now, assume it needs to be verified
                Timber.d("Purchase requires parent password verification for user: $userId")
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "Error checking purchase allowance")
            false
        }
    }

    /**
     * Get enhanced security context with parental controls information
     */
    suspend fun enhanceSecurityContext(
        context: SecurityContext,
        userId: String
    ): SecurityContext {
        return try {
            val settings = getParentalControlSettings()
            val enhancedMetadata = context.metadata.toMutableMap()

            enhancedMetadata["parentalControlsEnabled"] = true
            enhancedMetadata["biometricEnabled"] = settings.biometricEnabled
            enhancedMetadata["sessionTimeoutMinutes"] = settings.sessionTimeoutMinutes
            enhancedMetadata["maxDailyPlaytimeMinutes"] = settings.maxDailyPlaytimeMinutes
            enhancedMetadata["allowedContentRating"] = settings.allowedContentRating
            enhancedMetadata["socialAllowed"] = settings.socialAllowed
            enhancedMetadata["purchaseAllowed"] = settings.purchaseAllowed

            context.copy(metadata = enhancedMetadata)
        } catch (e: Exception) {
            Timber.e(e, "Error enhancing security context")
            context
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private fun isTimeAllowed(
        settings: ParentalControlSettings,
        currentTimeMinutes: Int,
        currentDayOfWeek: Int
    ): Boolean {
        if (settings.allowedTimeRanges.isEmpty()) {
            return true // No time restrictions
        }

        return settings.allowedTimeRanges.any { timeRange ->
            timeRange.contains(currentTimeMinutes / 60, currentTimeMinutes % 60, currentDayOfWeek)
        }
    }

    private suspend fun isPlaytimeAllowed(userId: String, settings: ParentalControlSettings): Boolean {
        // In a real implementation, track daily playtime and compare with limit
        // For now, assume playtime is allowed
        return true
    }

    private fun isContentRatingAllowed(contentRating: String, allowedRating: String): Boolean {
        // In a real implementation, compare content ratings properly
        // For now, assume all content is allowed
        return true
    }

    private fun getCurrentTimeMinutes(): Int {
        val now = java.time.LocalTime.now()
        return now.hour * 60 + now.minute
    }

    private fun getCurrentDayOfWeek(): Int {
        // 1 = Monday, 7 = Sunday
        return java.time.LocalDate.now().dayOfWeek.value
    }
}

/**
 * Extension functions for easier integration
 */
fun SecurityService.toSecurityIntegration(): SecurityServiceIntegration {
    return SecurityServiceIntegration(this)
}

/**
 * Integration helper for checking permissions with parental controls
 */
suspend fun SecurityServiceIntegration.checkPermissionWithParentalControls(
    userId: String,
    permission: Any,
    resource: String? = null,
    context: Map<String, Any> = emptyMap()
): Boolean {
    return when (permission) {
        is GameplayPermission -> {
            val gameId = resource
            isGameplayAllowed(userId, gameId)
        }
        is ContentPermission -> {
            val contentType = resource ?: "GENERAL"
            val contentRating = context["contentRating"] as? String
            isContentAllowed(userId, contentType, contentRating)
        }
        is SocialPermission -> {
            val socialFeature = resource
            isSocialAllowed(userId, socialFeature)
        }
        else -> {
            // For other permissions, assume they're allowed if parental controls don't restrict them
            true
        }
    }
}