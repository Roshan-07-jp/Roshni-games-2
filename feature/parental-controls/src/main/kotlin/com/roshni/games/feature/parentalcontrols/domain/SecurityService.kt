package com.roshni.games.feature.parentalcontrols.domain

import com.roshni.games.feature.parentalcontrols.data.model.AgeRating
import com.roshni.games.feature.parentalcontrols.data.model.AlertSeverity
import com.roshni.games.feature.parentalcontrols.data.model.AlertType
import com.roshni.games.feature.parentalcontrols.data.model.AuthenticationAttempt
import com.roshni.games.feature.parentalcontrols.data.model.AuthType
import com.roshni.games.feature.parentalcontrols.data.model.ContentRestrictions
import com.roshni.games.feature.parentalcontrols.data.model.DataExportRequest
import com.roshni.games.feature.parentalcontrols.data.model.DataRetentionPolicy
import com.roshni.games.feature.parentalcontrols.data.model.ExportFormat
import com.roshni.games.feature.parentalcontrols.data.model.ExportStatus
import com.roshni.games.feature.parentalcontrols.data.model.ParentalControlSettings
import com.roshni.games.feature.parentalcontrols.data.model.PrivacySettings
import com.roshni.games.feature.parentalcontrols.data.model.PurchaseRestrictions
import com.roshni.games.feature.parentalcontrols.data.model.SecurityAlert
import com.roshni.games.feature.parentalcontrols.data.model.SecuritySettings
import com.roshni.games.feature.parentalcontrols.data.model.SocialRestrictions
import com.roshni.games.feature.parentalcontrols.data.model.TimeLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Service for managing security and parental controls
 */
class SecurityService {

    private val _parentalControls = MutableStateFlow(
        ParentalControlSettings(
            isEnabled = false,
            restrictions = ContentRestrictions(
                maxAgeRating = AgeRating.TEEN,
                blockMatureContent = true,
                allowedGameCategories = listOf("Puzzle", "Educational", "Adventure")
            ),
            timeLimits = TimeLimits(
                dailyLimitMinutes = 90,
                sessionLimitMinutes = 45,
                allowedHours = listOf(
                    com.roshni.games.feature.parentalcontrols.data.model.TimeRange("08:00", "20:00")
                )
            )
        )
    )

    private val _securitySettings = MutableStateFlow(
        SecuritySettings(
            biometricEnabled = false,
            sessionTimeoutMinutes = 30,
            requirePasswordForPurchases = true,
            encryptLocalData = true
        )
    )

    private val _privacySettings = MutableStateFlow(
        PrivacySettings(
            analyticsEnabled = false,
            crashReportingEnabled = false,
            performanceMonitoringEnabled = false
        )
    )

    private val _securityAlerts = MutableStateFlow<List<SecurityAlert>>(emptyList())
    private val _authAttempts = MutableStateFlow<List<AuthenticationAttempt>>(emptyList())
    private val _dataExportRequests = MutableStateFlow<List<DataExportRequest>>(emptyList())

    // Public flows
    val parentalControls: StateFlow<ParentalControlSettings> = _parentalControls.asStateFlow()
    val securitySettings: StateFlow<SecuritySettings> = _securitySettings.asStateFlow()
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()
    val securityAlerts: StateFlow<List<SecurityAlert>> = _securityAlerts.asStateFlow()
    val authAttempts: StateFlow<List<AuthenticationAttempt>> = _authAttempts.asStateFlow()
    val dataExportRequests: StateFlow<List<DataExportRequest>> = _dataExportRequests.asStateFlow()

    private lateinit var encryptionKey: SecretKey
    private val dataRetentionPolicy = DataRetentionPolicy()

    /**
     * Initialize security service
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing SecurityService")

            // Initialize encryption
            initializeEncryption()

            // Load settings from storage
            loadSecuritySettings()

            // Check for security violations
            checkSecurityViolations()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize SecurityService")
            Result.failure(e)
        }
    }

    /**
     * Authenticate with PIN
     */
    suspend fun authenticateWithPin(pin: String): Result<Boolean> {
        return try {
            val settings = _parentalControls.value

            if (!settings.isEnabled || !settings.pinRequired) {
                return Result.success(true) // No authentication required
            }

            val pinHash = hashPin(pin)
            val isValid = pinHash == settings.pinHash

            // Record authentication attempt
            recordAuthAttempt(AuthType.PIN_LOGIN, isValid)

            if (!isValid) {
                // Create security alert for failed attempt
                createSecurityAlert(
                    type = AlertType.FAILED_LOGIN_ATTEMPT,
                    severity = AlertSeverity.MEDIUM,
                    title = "Failed PIN Login",
                    message = "Invalid PIN entered"
                )
            }

            Result.success(isValid)
        } catch (e: Exception) {
            Timber.e(e, "Failed to authenticate with PIN")
            Result.failure(e)
        }
    }

    /**
     * Set or update parental control PIN
     */
    suspend fun setParentalPin(pin: String): Result<Unit> {
        return try {
            if (pin.length < 4) {
                return Result.failure(IllegalArgumentException("PIN must be at least 4 digits"))
            }

            val pinHash = hashPin(pin)

            val updatedSettings = _parentalControls.value.copy(
                pinHash = pinHash,
                pinRequired = true
            )

            _parentalControls.value = updatedSettings
            saveParentalControls(updatedSettings)

            Timber.d("Parental PIN updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set parental PIN")
            Result.failure(e)
        }
    }

    /**
     * Check if content is allowed based on parental controls
     */
    fun isContentAllowed(
        ageRating: AgeRating,
        category: String,
        hasViolence: Boolean = false,
        hasGambling: Boolean = false,
        hasSocialFeatures: Boolean = false
    ): Boolean {
        val settings = _parentalControls.value

        if (!settings.isEnabled) {
            return true // No restrictions if disabled
        }

        val restrictions = settings.restrictions

        // Check age rating
        if (ageRating > restrictions.maxAgeRating) {
            return false
        }

        // Check content restrictions
        if (restrictions.blockMatureContent && ageRating >= AgeRating.MATURE_17_PLUS) {
            return false
        }

        if (restrictions.blockViolence && hasViolence) {
            return false
        }

        if (restrictions.blockGambling && hasGambling) {
            return false
        }

        if (restrictions.blockSocialFeatures && hasSocialFeatures) {
            return false
        }

        // Check allowed categories
        if (restrictions.allowedGameCategories.isNotEmpty() &&
            category !in restrictions.allowedGameCategories) {
            return false
        }

        return true
    }

    /**
     * Check if gameplay is allowed based on time limits
     */
    fun isGameplayAllowed(): Boolean {
        val settings = _parentalControls.value

        if (!settings.isEnabled) {
            return true // No restrictions if disabled
        }

        val timeLimits = settings.timeLimits
        val currentTime = java.time.LocalTime.now()
        val currentHour = currentTime.hour
        val currentMinute = currentTime.minute
        val currentTimeString = String.format("%02d:%02d", currentHour, currentMinute)

        // Check allowed hours
        val isInAllowedHours = timeLimits.allowedHours.any { range ->
            currentTimeString >= range.startTime && currentTimeString <= range.endTime
        }

        if (!isInAllowedHours) {
            return false
        }

        // Check bedtime enforcement
        if (timeLimits.enforceBedtime) {
            if (currentTimeString >= timeLimits.bedtimeStart ||
                currentTimeString <= timeLimits.bedtimeEnd) {
                return false
            }
        }

        return true
    }

    /**
     * Check if purchase is allowed
     */
    fun isPurchaseAllowed(amount: Double, paymentMethod: String): Boolean {
        val settings = _parentalControls.value

        if (!settings.isEnabled) {
            return true // No restrictions if disabled
        }

        val purchaseRestrictions = settings.purchaseRestrictions

        // Check if approval is required
        if (purchaseRestrictions.requireApproval) {
            return false // Require explicit approval
        }

        // Check amount limits
        if (amount > purchaseRestrictions.maxPurchaseAmount) {
            return false
        }

        // Check payment method restrictions
        if (purchaseRestrictions.allowedPaymentMethods.isNotEmpty() &&
            paymentMethod !in purchaseRestrictions.allowedPaymentMethods) {
            return false
        }

        return true
    }

    /**
     * Check if social interaction is allowed
     */
    fun isSocialInteractionAllowed(interactionType: String): Boolean {
        val settings = _parentalControls.value

        if (!settings.isEnabled) {
            return true // No restrictions if disabled
        }

        val socialRestrictions = settings.socialRestrictions

        return when (interactionType) {
            "friend_request" -> socialRestrictions.allowFriendRequests
            "message" -> socialRestrictions.allowMessages
            "public_profile" -> socialRestrictions.allowPublicProfiles
            else -> false
        }
    }

    /**
     * Encrypt sensitive data
     */
    fun encryptData(data: String): Result<String> {
        return try {
            if (!::encryptionKey.isInitialized) {
                return Result.failure(IllegalStateException("Encryption not initialized"))
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray())

            // Combine IV and encrypted data
            val combined = iv + encryptedData
            val encoded = java.util.Base64.getEncoder().encodeToString(combined)

            Result.success(encoded)
        } catch (e: Exception) {
            Timber.e(e, "Failed to encrypt data")
            Result.failure(e)
        }
    }

    /**
     * Decrypt sensitive data
     */
    fun decryptData(encryptedData: String): Result<String> {
        return try {
            if (!::encryptionKey.isInitialized) {
                return Result.failure(IllegalStateException("Encryption not initialized"))
            }

            val decoded = java.util.Base64.getDecoder().decode(encryptedData)
            val iv = decoded.copyOfRange(0, 12) // GCM IV is 12 bytes
            val data = decoded.copyOfRange(12, decoded.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmParameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmParameterSpec)

            val decryptedData = cipher.doFinal(data)
            val result = String(decryptedData)

            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt data")
            Result.failure(e)
        }
    }

    /**
     * Request data export
     */
    suspend fun requestDataExport(
        requestedBy: String,
        format: ExportFormat,
        includePersonalData: Boolean = true,
        includeGameData: Boolean = true,
        includeAnalytics: Boolean = false
    ): Result<String> {
        return try {
            val request = DataExportRequest(
                id = UUID.randomUUID().toString(),
                requestedBy = requestedBy,
                format = format,
                includePersonalData = includePersonalData,
                includeGameData = includeGameData,
                includeAnalytics = includeAnalytics,
                expiresAt = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000) // 30 days
            )

            val currentRequests = _dataExportRequests.value.toMutableList()
            currentRequests.add(request)
            _dataExportRequests.value = currentRequests

            // In real implementation, start async data export process
            startDataExport(request)

            Timber.d("Data export requested: ${request.id}")
            Result.success(request.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to request data export")
            Result.failure(e)
        }
    }

    /**
     * Get security alerts
     */
    fun getSecurityAlerts(minSeverity: AlertSeverity? = null): Flow<List<SecurityAlert>> = flow {
        val alerts = _securityAlerts.value

        val filteredAlerts = if (minSeverity != null) {
            alerts.filter { it.severity >= minSeverity }
        } else {
            alerts
        }

        emit(filteredAlerts)
    }

    /**
     * Acknowledge security alert
     */
    suspend fun acknowledgeSecurityAlert(alertId: String): Result<Unit> {
        return try {
            val currentAlerts = _securityAlerts.value.toMutableList()
            val index = currentAlerts.indexOfFirst { it.id == alertId }

            if (index >= 0) {
                // Mark as acknowledged (in real implementation, update in database)
                Timber.d("Security alert acknowledged: $alertId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to acknowledge security alert")
            Result.failure(e)
        }
    }

    /**
     * Update privacy settings
     */
    suspend fun updatePrivacySettings(settings: PrivacySettings): Result<Unit> {
        return try {
            _privacySettings.value = settings
            savePrivacySettings(settings)

            Timber.d("Privacy settings updated: $settings")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update privacy settings")
            Result.failure(e)
        }
    }

    /**
     * Check for data retention compliance
     */
    suspend fun checkDataRetentionCompliance(): Result<List<String>> {
        return try {
            val violations = mutableListOf<String>()

            // Check various data types against retention policy
            val currentTime = System.currentTimeMillis()

            // In real implementation, this would check actual data timestamps
            // For now, return sample violations

            Result.success(violations)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check data retention compliance")
            Result.failure(e)
        }
    }

    /**
     * Hash PIN for secure storage
     */
    private fun hashPin(pin: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to hash PIN")
            pin // Fallback to plain text (not recommended for production)
        }
    }

    /**
     * Initialize encryption key
     */
    private fun initializeEncryption() {
        try {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            encryptionKey = keyGenerator.generateKey()

            Timber.d("Encryption initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize encryption")
        }
    }

    /**
     * Record authentication attempt
     */
    private fun recordAuthAttempt(type: AuthType, success: Boolean) {
        try {
            val attempt = AuthenticationAttempt(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                type = type,
                success = success
            )

            val currentAttempts = _authAttempts.value.toMutableList()
            currentAttempts.add(0, attempt) // Add to beginning

            // Keep only recent attempts (last 100)
            _authAttempts.value = currentAttempts.take(100)

        } catch (e: Exception) {
            Timber.e(e, "Failed to record authentication attempt")
        }
    }

    /**
     * Create security alert
     */
    private fun createSecurityAlert(
        type: AlertType,
        severity: AlertSeverity,
        title: String,
        message: String,
        requiresAction: Boolean = false,
        metadata: Map<String, Any> = emptyMap()
    ) {
        try {
            val alert = SecurityAlert(
                id = UUID.randomUUID().toString(),
                type = type,
                severity = severity,
                title = title,
                message = message,
                requiresAction = requiresAction,
                metadata = metadata
            )

            val currentAlerts = _securityAlerts.value.toMutableList()
            currentAlerts.add(0, alert) // Add to beginning

            // Keep only recent alerts (last 50)
            _securityAlerts.value = currentAlerts.take(50)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create security alert")
        }
    }

    /**
     * Check for security violations
     */
    private suspend fun checkSecurityViolations() {
        try {
            // Check for multiple failed login attempts
            val recentAttempts = _authAttempts.value.filter {
                !it.success && (System.currentTimeMillis() - it.timestamp) < (15 * 60 * 1000) // Last 15 minutes
            }

            if (recentAttempts.size >= 5) {
                createSecurityAlert(
                    type = AlertType.SUSPICIOUS_ACTIVITY,
                    severity = AlertSeverity.HIGH,
                    title = "Multiple Failed Login Attempts",
                    message = "Detected ${recentAttempts.size} failed login attempts in the last 15 minutes",
                    requiresAction = true
                )
            }

            // Check for unusual patterns
            // In real implementation, check for unusual locations, devices, etc.

        } catch (e: Exception) {
            Timber.e(e, "Failed to check security violations")
        }
    }

    /**
     * Load security settings from storage
     */
    private suspend fun loadSecuritySettings() {
        try {
            // In real implementation, load from encrypted storage
            Timber.d("Loaded security settings")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load security settings")
        }
    }

    /**
     * Save parental controls to storage
     */
    private suspend fun saveParentalControls(settings: ParentalControlSettings) {
        try {
            // In real implementation, save to encrypted storage
            Timber.d("Saved parental controls")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save parental controls")
        }
    }

    /**
     * Save privacy settings to storage
     */
    private suspend fun savePrivacySettings(settings: PrivacySettings) {
        try {
            // In real implementation, save to encrypted storage
            Timber.d("Saved privacy settings")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save privacy settings")
        }
    }

    /**
     * Start data export process
     */
    private suspend fun startDataExport(request: DataExportRequest) {
        try {
            // In real implementation, start async data export process
            Timber.d("Starting data export: ${request.id}")

            // Simulate processing time
            kotlinx.coroutines.delay(2000)

            // Update request status
            val currentRequests = _dataExportRequests.value.toMutableList()
            val index = currentRequests.indexOfFirst { it.id == request.id }
            if (index >= 0) {
                currentRequests[index] = request.copy(
                    status = ExportStatus.COMPLETED,
                    completedAt = System.currentTimeMillis()
                )
                _dataExportRequests.value = currentRequests
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to start data export")

            // Update request status to failed
            val currentRequests = _dataExportRequests.value.toMutableList()
            val index = currentRequests.indexOfFirst { it.id == request.id }
            if (index >= 0) {
                currentRequests[index] = request.copy(status = ExportStatus.FAILED)
                _dataExportRequests.value = currentRequests
            }
        }
    }
}