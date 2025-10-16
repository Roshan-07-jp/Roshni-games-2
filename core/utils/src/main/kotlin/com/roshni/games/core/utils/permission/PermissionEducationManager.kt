package com.roshni.games.core.utils.permission

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Manages user education flows for permission requests and denials
 */
class PermissionEducationManager {

    private val _educationHistory = MutableStateFlow<Map<RuntimePermission, EducationRecord>>(emptyMap())
    val educationHistory: StateFlow<Map<RuntimePermission, EducationRecord>> = _educationHistory.asStateFlow()

    private val _educationPreferences = MutableStateFlow<Map<String, Any>>(emptyMap())
    val educationPreferences: StateFlow<Map<String, Any>> = _educationPreferences.asStateFlow()

    /**
     * Record of education shown for a permission
     */
    data class EducationRecord(
        val permission: RuntimePermission,
        val educationType: EducationType,
        val shownAt: LocalDateTime,
        val userResponse: EducationResponse? = null,
        val wasEffective: Boolean? = null,
        val metadata: Map<String, Any> = emptyMap()
    )

    /**
     * Types of education that can be shown
     */
    enum class EducationType(val displayName: String) {
        RATIONALE_DIALOG("Permission Rationale Dialog"),
        EDUCATION_SCREEN("Detailed Education Screen"),
        TOOLTIP("Tooltip Explanation"),
        ONBOARDING("Onboarding Flow"),
        SETTINGS_GUIDE("Settings Guide"),
        CUSTOM_DIALOG("Custom Education Dialog")
    }

    /**
     * User responses to education
     */
    enum class EducationResponse(val displayName: String) {
        GRANTED_PERMISSION("Granted Permission"),
        DENIED_PERMISSION("Denied Permission"),
        LEARNED_MORE("Learned More"),
        DISMISSED("Dismissed"),
        POSTPONED("Postponed")
    }

    /**
     * Configuration for education flows
     */
    data class EducationConfig(
        val maxEducationAttempts: Int = 3,
        val educationCooldownHours: Long = 24,
        val showProgressiveDisclosure: Boolean = true,
        val requireMinimumVersion: Boolean = true,
        val trackEffectiveness: Boolean = true,
        val customMessages: Map<RuntimePermission, EducationMessage> = emptyMap()
    )

    /**
     * Custom messages for specific permissions
     */
    data class EducationMessage(
        val title: String,
        val message: String,
        val benefits: List<String> = emptyList(),
        val consequences: List<String> = emptyList(),
        val actionButtonText: String = "Grant Permission",
        val dismissButtonText: String = "Not Now"
    )

    private var config = EducationConfig()

    /**
     * Update education configuration
     */
    fun updateConfig(newConfig: EducationConfig) {
        config = newConfig
    }

    /**
     * Check if education should be shown for a permission
     */
    fun shouldShowEducation(
        permission: RuntimePermission,
        currentStatus: PermissionStatus,
        featureRequirements: FeaturePermissionRequirements? = null
    ): Boolean {
        if (currentStatus !is PermissionStatus.Denied) {
            return false
        }

        val history = _educationHistory.value[permission] ?: return true

        // Check if we've exceeded max attempts
        if (history.shownAt.isAfter(LocalDateTime.now().minusHours(config.educationCooldownHours))) {
            return false
        }

        // Check if user has been educated too many times
        val recentEducations = _educationHistory.value.values.filter {
            it.permission == permission &&
            it.shownAt.isAfter(LocalDateTime.now().minusDays(7))
        }

        return recentEducations.size < config.maxEducationAttempts
    }

    /**
     * Record that education was shown
     */
    fun recordEducationShown(
        permission: RuntimePermission,
        educationType: EducationType,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val record = EducationRecord(
            permission = permission,
            educationType = educationType,
            shownAt = LocalDateTime.now(),
            metadata = metadata
        )

        val current = _educationHistory.value.toMutableMap()
        current[permission] = record
        _educationHistory.value = current

        Timber.d("Education shown for permission: ${permission.androidPermission}, type: $educationType")
    }

    /**
     * Record user response to education
     */
    fun recordEducationResponse(
        permission: RuntimePermission,
        response: EducationResponse,
        wasEffective: Boolean? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val current = _educationHistory.value.toMutableMap()
        val existing = current[permission]

        if (existing != null) {
            val updated = existing.copy(
                userResponse = response,
                wasEffective = wasEffective,
                metadata = existing.metadata + metadata
            )
            current[permission] = updated
            _educationHistory.value = current

            Timber.d("Education response recorded for permission: ${permission.androidPermission}, response: $response")
        }
    }

    /**
     * Get education effectiveness for a permission
     */
    fun getEducationEffectiveness(permission: RuntimePermission): EducationEffectiveness {
        val history = _educationHistory.value[permission] ?: return EducationEffectiveness.UNKNOWN

        if (!config.trackEffectiveness || history.wasEffective == null) {
            return EducationEffectiveness.UNKNOWN
        }

        return if (history.wasEffective) {
            EducationEffectiveness.EFFECTIVE
        } else {
            EducationEffectiveness.NOT_EFFECTIVE
        }
    }

    /**
     * Get recommended education type for a permission
     */
    fun getRecommendedEducationType(
        permission: RuntimePermission,
        previousAttempts: Int = 0
    ): EducationType {
        return when {
            previousAttempts == 0 -> EducationType.RATIONALE_DIALOG
            previousAttempts == 1 -> EducationType.EDUCATION_SCREEN
            previousAttempts >= 2 -> EducationType.SETTINGS_GUIDE
            else -> EducationType.CUSTOM_DIALOG
        }
    }

    /**
     * Get education message for a permission
     */
    fun getEducationMessage(permission: RuntimePermission): EducationMessage {
        return config.customMessages[permission] ?: getDefaultEducationMessage(permission)
    }

    /**
     * Check if user should be educated about a feature's permissions
     */
    fun shouldEducateAboutFeature(
        featureRequirements: FeaturePermissionRequirements,
        deniedPermissions: Set<RuntimePermission>
    ): Boolean {
        if (deniedPermissions.isEmpty()) return false

        // Check if any critical permissions are denied
        val criticalDenied = deniedPermissions.any { it.isCritical }
        if (criticalDenied) return true

        // Check if feature rationale exists
        if (featureRequirements.rationale != null) return true

        // Check education history for these permissions
        return deniedPermissions.any { permission ->
            shouldShowEducation(permission, PermissionStatus.Denied(permission))
        }
    }

    /**
     * Get education strategy for a set of denied permissions
     */
    fun getEducationStrategy(
        deniedPermissions: Set<RuntimePermission>,
        featureRequirements: FeaturePermissionRequirements? = null
    ): EducationStrategy {
        val criticalPermissions = deniedPermissions.filter { it.isCritical }
        val nonCriticalPermissions = deniedPermissions - criticalPermissions

        return when {
            criticalPermissions.isNotEmpty() -> {
                EducationStrategy.Aggressive(
                    permissions = criticalPermissions,
                    maxAttempts = 3,
                    showConsequences = true,
                    requireAllGranted = true
                )
            }
            nonCriticalPermissions.size > 1 -> {
                EducationStrategy.Batch(
                    permissions = nonCriticalPermissions,
                    batchSize = 2,
                    delayBetweenBatches = 1000L
                )
            }
            else -> {
                EducationStrategy.Standard(
                    permissions = deniedPermissions,
                    maxAttempts = 2,
                    showBenefits = true
                )
            }
        }
    }

    /**
     * Update education preferences
     */
    fun updateEducationPreference(key: String, value: Any) {
        val current = _educationPreferences.value.toMutableMap()
        current[key] = value
        _educationPreferences.value = current
    }

    /**
     * Get education preference
     */
    fun getEducationPreference(key: String, defaultValue: Any): Any {
        return _educationPreferences.value[key] ?: defaultValue
    }

    /**
     * Clear education history for a permission
     */
    fun clearEducationHistory(permission: RuntimePermission) {
        val current = _educationHistory.value.toMutableMap()
        current.remove(permission)
        _educationHistory.value = current
    }

    /**
     * Clear all education history
     */
    fun clearAllEducationHistory() {
        _educationHistory.value = emptyMap()
    }

    /**
     * Get education statistics
     */
    fun getEducationStatistics(): EducationStatistics {
        val history = _educationHistory.value.values
        val totalEducations = history.size
        val effectiveEducations = history.count { it.wasEffective == true }
        val ineffectiveEducations = history.count { it.wasEffective == false }

        val effectivenessRate = if (totalEducations > 0) {
            effectiveEducations.toDouble() / totalEducations
        } else 0.0

        val responses = history.groupBy { it.userResponse }
        val grantsAfterEducation = responses[EducationResponse.GRANTED_PERMISSION]?.size ?: 0

        return EducationStatistics(
            totalEducations = totalEducations,
            effectiveEducations = effectiveEducations,
            ineffectiveEducations = ineffectiveEducations,
            effectivenessRate = effectivenessRate,
            grantsAfterEducation = grantsAfterEducation,
            averageAttemptsPerPermission = if (history.isNotEmpty()) {
                history.groupBy { it.permission }.values.map { it.size }.average()
            } else 0.0
        )
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private fun getDefaultEducationMessage(permission: RuntimePermission): EducationMessage {
        return when (permission) {
            is RuntimePermission.CameraPermission -> EducationMessage(
                title = "Camera Access Needed",
                message = "This app needs camera access to take photos and record videos for your games.",
                benefits = listOf("Capture game moments", "Record gameplay", "Share with friends"),
                consequences = listOf("Some features will be unavailable")
            )
            is RuntimePermission.MicrophonePermission -> EducationMessage(
                title = "Microphone Access Needed",
                message = "This app needs microphone access to record audio and enable voice features.",
                benefits = listOf("Voice chat in games", "Record commentary", "Voice commands"),
                consequences = listOf("Voice features will be disabled")
            )
            is RuntimePermission.LocationFinePermission -> EducationMessage(
                title = "Location Access Needed",
                message = "This app needs precise location access for location-based games and features.",
                benefits = listOf("Location-based gameplay", "Find nearby players", "Local events"),
                consequences = listOf("Location features will be unavailable")
            )
            is RuntimePermission.StorageReadPermission -> EducationMessage(
                title = "Storage Access Needed",
                message = "This app needs storage access to save games and load your content.",
                benefits = listOf("Save game progress", "Access downloaded content", "Import media"),
                consequences = listOf("Save/load features will be limited")
            )
            is RuntimePermission.NotificationPermission -> EducationMessage(
                title = "Notification Access Needed",
                message = "This app needs notification access to keep you updated about games and events.",
                benefits = listOf("Game invitations", "Achievement alerts", "Important updates"),
                consequences = listOf("You may miss important notifications")
            )
            else -> EducationMessage(
                title = "Permission Needed",
                message = "This app needs access to ${permission.displayName.lowercase()} for full functionality.",
                benefits = listOf("Enhanced app experience", "Access to all features"),
                consequences = listOf("Some features may be limited")
            )
        }
    }
}

/**
 * Education effectiveness indicator
 */
enum class EducationEffectiveness(val displayName: String) {
    EFFECTIVE("Effective"),
    NOT_EFFECTIVE("Not Effective"),
    UNKNOWN("Unknown")
}

/**
 * Strategy for educating users about permissions
 */
sealed class EducationStrategy {
    abstract val permissions: Set<RuntimePermission>

    data class Standard(
        override val permissions: Set<RuntimePermission>,
        val maxAttempts: Int = 2,
        val showBenefits: Boolean = true
    ) : EducationStrategy()

    data class Aggressive(
        override val permissions: Set<RuntimePermission>,
        val maxAttempts: Int = 3,
        val showConsequences: Boolean = true,
        val requireAllGranted: Boolean = true
    ) : EducationStrategy()

    data class Batch(
        override val permissions: Set<RuntimePermission>,
        val batchSize: Int = 2,
        val delayBetweenBatches: Long = 1000L
    ) : EducationStrategy()
}

/**
 * Statistics about education effectiveness
 */
data class EducationStatistics(
    val totalEducations: Int,
    val effectiveEducations: Int,
    val ineffectiveEducations: Int,
    val effectivenessRate: Double,
    val grantsAfterEducation: Int,
    val averageAttemptsPerPermission: Double
)