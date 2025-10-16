package com.roshni.games.feature.parentalcontrols.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for parental controls and security
 */

/**
 * Parental control settings
 */
@Serializable
data class ParentalControlSettings(
    val isEnabled: Boolean = false,
    val pinRequired: Boolean = true,
    val pinHash: String? = null,
    val restrictions: ContentRestrictions = ContentRestrictions(),
    val timeLimits: TimeLimits = TimeLimits(),
    val purchaseRestrictions: PurchaseRestrictions = PurchaseRestrictions(),
    val socialRestrictions: SocialRestrictions = SocialRestrictions(),
    val monitoringEnabled: Boolean = false
)

/**
 * Content restrictions
 */
@Serializable
data class ContentRestrictions(
    val maxAgeRating: AgeRating = AgeRating.EVERYONE,
    val blockMatureContent: Boolean = true,
    val blockViolence: Boolean = false,
    val blockGambling: Boolean = true,
    val blockSocialFeatures: Boolean = false,
    val allowedGameCategories: List<String> = emptyList(),
    val blockedKeywords: List<String> = emptyList()
)

/**
 * Age ratings for content
 */
@Serializable
enum class AgeRating {
    EVERYONE,       // Suitable for all ages
    EVERYONE_10_PLUS, // 10+
    TEEN,           // 13+
    MATURE_17_PLUS, // 17+
    ADULTS_ONLY     // 18+
}

/**
 * Time limits for gameplay
 */
@Serializable
data class TimeLimits(
    val dailyLimitMinutes: Int = 120, // 2 hours per day
    val weeklyLimitMinutes: Int = 840, // 14 hours per week
    val sessionLimitMinutes: Int = 60, // 1 hour per session
    val allowedHours: List<TimeRange> = listOf(
        TimeRange("08:00", "20:00") // 8 AM to 8 PM
    ),
    val enforceBedtime: Boolean = false,
    val bedtimeStart: String = "22:00",
    val bedtimeEnd: String = "08:00"
)

/**
 * Time range for allowed gameplay
 */
@Serializable
data class TimeRange(
    val startTime: String, // HH:mm format
    val endTime: String    // HH:mm format
)

/**
 * Purchase restrictions
 */
@Serializable
data class PurchaseRestrictions(
    val requireApproval: Boolean = true,
    val maxPurchaseAmount: Double = 0.0,
    val monthlySpendingLimit: Double = 0.0,
    val requirePassword: Boolean = true,
    val allowedPaymentMethods: List<String> = emptyList()
)

/**
 * Social restrictions
 */
@Serializable
data class SocialRestrictions(
    val allowFriendRequests: Boolean = false,
    val allowMessages: Boolean = false,
    val allowPublicProfiles: Boolean = false,
    val maxFriends: Int = 0,
    val requireApprovalForFriends: Boolean = true,
    val hideOnlineStatus: Boolean = true,
    val blockStrangerMessages: Boolean = true
)

/**
 * Security settings
 */
@Serializable
data class SecuritySettings(
    val biometricEnabled: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val sessionTimeoutMinutes: Int = 30,
    val requirePasswordForPurchases: Boolean = true,
    val encryptLocalData: Boolean = true,
    val autoLockEnabled: Boolean = false,
    val autoLockDelayMinutes: Int = 5
)

/**
 * Authentication attempt record
 */
@Serializable
data class AuthenticationAttempt(
    val id: String,
    val timestamp: Long,
    val type: AuthType,
    val success: Boolean,
    val ipAddress: String? = null,
    val deviceInfo: String? = null,
    val location: String? = null
)

/**
 * Authentication types
 */
@Serializable
enum class AuthType {
    PIN_LOGIN,
    BIOMETRIC_LOGIN,
    PASSWORD_LOGIN,
    PURCHASE_AUTH,
    SETTINGS_CHANGE
}

/**
 * Security alert
 */
@Serializable
data class SecurityAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val requiresAction: Boolean = false,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Security alert types
 */
@Serializable
enum class AlertType {
    FAILED_LOGIN_ATTEMPT,
    SUSPICIOUS_ACTIVITY,
    PURCHASE_ATTEMPT,
    SETTINGS_CHANGE,
    NEW_DEVICE_LOGIN,
    UNUSUAL_LOCATION
}

/**
 * Alert severity levels
 */
@Serializable
enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Data export request
 */
@Serializable
data class DataExportRequest(
    val id: String,
    val requestedBy: String,
    val requestedAt: Long = System.currentTimeMillis(),
    val status: ExportStatus,
    val format: ExportFormat,
    val includePersonalData: Boolean = true,
    val includeGameData: Boolean = true,
    val includeAnalytics: Boolean = false,
    val completedAt: Long? = null,
    val downloadUrl: String? = null,
    val expiresAt: Long? = null
)

/**
 * Export status
 */
@Serializable
enum class ExportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED
}

/**
 * Export format options
 */
@Serializable
enum class ExportFormat {
    JSON,
    CSV,
    PDF,
    XML
}

/**
 * Privacy settings
 */
@Serializable
data class PrivacySettings(
    val analyticsEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = false,
    val performanceMonitoringEnabled: Boolean = false,
    val personalizedAds: Boolean = false,
    val dataSharing: Boolean = false,
    val thirdPartyTracking: Boolean = false,
    val locationTracking: Boolean = false,
    val marketingEmails: Boolean = false
)

/**
 * Data retention policy
 */
@Serializable
data class DataRetentionPolicy(
    val keepGameDataDays: Int = 2555, // 7 years
    val keepAnalyticsDays: Int = 365,  // 1 year
    val keepCrashReportsDays: Int = 90, // 90 days
    val keepChatHistoryDays: Int = 30,  // 30 days
    val autoDeleteInactiveAccountsDays: Int = 365 // 1 year
)