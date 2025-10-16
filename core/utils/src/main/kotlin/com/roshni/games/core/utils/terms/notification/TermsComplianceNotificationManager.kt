package com.roshni.games.core.utils.terms.notification

import com.roshni.games.core.utils.terms.TermsAndConditionsManager
import com.roshni.games.core.utils.terms.TermsComplianceReport
import com.roshni.games.core.utils.terms.TermsDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import timber.log.Timber

/**
 * Manager for handling legal compliance notifications and monitoring
 */
interface TermsComplianceNotificationManager {

    /**
     * Flow of compliance notifications
     */
    val complianceNotifications: SharedFlow<ComplianceNotification>

    /**
     * Flow of terms update notifications
     */
    val termsUpdateNotifications: SharedFlow<TermsUpdateNotification>

    /**
     * Start monitoring compliance
     */
    fun startMonitoring()

    /**
     * Stop monitoring compliance
     */
    fun stopMonitoring()

    /**
     * Check compliance for a specific user
     */
    suspend fun checkUserCompliance(userId: String)

    /**
     * Notify about terms document updates
     */
    suspend fun notifyTermsUpdate(document: TermsDocument, affectedUsers: List<String>)

    /**
     * Send compliance report to administrators
     */
    suspend fun sendComplianceReport(report: TermsComplianceReport)

    /**
     * Configure notification settings
     */
    fun configureNotifications(config: NotificationConfig)
}

/**
 * Implementation of TermsComplianceNotificationManager
 */
class TermsComplianceNotificationManagerImpl(
    private val termsManager: TermsAndConditionsManager
) : TermsComplianceNotificationManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _complianceNotifications = MutableSharedFlow<ComplianceNotification>()
    private val _termsUpdateNotifications = MutableSharedFlow<TermsUpdateNotification>()

    private var monitoringEnabled = true
    private var notificationConfig = NotificationConfig()

    override val complianceNotifications: SharedFlow<ComplianceNotification>
        get() = _complianceNotifications.asSharedFlow()

    override val termsUpdateNotifications: SharedFlow<TermsUpdateNotification>
        get() = _termsUpdateNotifications.asSharedFlow()

    override fun startMonitoring() {
        monitoringEnabled = true
        Timber.d("Terms compliance monitoring started")

        // Start periodic compliance checks
        scope.launch {
            while (monitoringEnabled) {
                try {
                    performPeriodicComplianceCheck()
                    kotlinx.coroutines.delay(notificationConfig.checkIntervalMs)
                } catch (e: Exception) {
                    Timber.e(e, "Error in periodic compliance check")
                    kotlinx.coroutines.delay(60000) // Wait 1 minute before retrying
                }
            }
        }
    }

    override fun stopMonitoring() {
        monitoringEnabled = false
        Timber.d("Terms compliance monitoring stopped")
    }

    override suspend fun checkUserCompliance(userId: String) {
        try {
            val compliance = termsManager.validateUserCompliance(userId)

            if (!compliance.isCompliant) {
                val notification = ComplianceNotification.UserNonCompliant(
                    userId = userId,
                    missingAcceptances = compliance.missingAcceptances.size,
                    expiredAcceptances = compliance.expiredAcceptances.size,
                    timestamp = LocalDateTime.now()
                )

                _complianceNotifications.emit(notification)

                Timber.d("User $userId is non-compliant - notification sent")
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to check compliance for user: $userId")
        }
    }

    override suspend fun notifyTermsUpdate(document: TermsDocument, affectedUsers: List<String>) {
        try {
            val notification = TermsUpdateNotification(
                documentId = document.id,
                documentTitle = document.title,
                documentType = document.type,
                newVersion = document.version,
                affectedUsers = affectedUsers,
                requiresReAcceptance = document.requiresAcceptance,
                effectiveDate = document.effectiveDate,
                timestamp = LocalDateTime.now()
            )

            _termsUpdateNotifications.emit(notification)

            Timber.d("Terms update notification sent for document: ${document.title}")

        } catch (e: Exception) {
            Timber.e(e, "Failed to send terms update notification")
        }
    }

    override suspend fun sendComplianceReport(report: TermsComplianceReport) {
        try {
            val notification = ComplianceNotification.ComplianceReportGenerated(
                report = report,
                timestamp = LocalDateTime.now()
            )

            _complianceNotifications.emit(notification)

            // Check if compliance rate is below threshold
            if (report.complianceRate < notificationConfig.complianceThreshold) {
                val alertNotification = ComplianceNotification.LowComplianceAlert(
                    complianceRate = report.complianceRate,
                    threshold = notificationConfig.complianceThreshold,
                    nonCompliantUsers = report.nonCompliantUsers,
                    timestamp = LocalDateTime.now()
                )

                _complianceNotifications.emit(alertNotification)

                Timber.w("Low compliance alert sent - rate: ${report.complianceRate}")
            }

            Timber.d("Compliance report notification sent")

        } catch (e: Exception) {
            Timber.e(e, "Failed to send compliance report notification")
        }
    }

    override fun configureNotifications(config: NotificationConfig) {
        notificationConfig = config
        Timber.d("Terms compliance notification configuration updated")
    }

    /**
     * Perform periodic compliance check
     */
    private suspend fun performPeriodicComplianceCheck() {
        try {
            val report = termsManager.getComplianceReport()

            // Send compliance report if enabled
            if (notificationConfig.enablePeriodicReports) {
                sendComplianceReport(report)
            }

            // Check for documents with low acceptance rates
            for (documentInfo in report.documentsWithLowAcceptance) {
                if (documentInfo.acceptanceRate < notificationConfig.acceptanceRateThreshold) {
                    val notification = ComplianceNotification.LowDocumentAcceptance(
                        documentId = documentInfo.documentId,
                        documentTitle = documentInfo.documentTitle,
                        acceptanceRate = documentInfo.acceptanceRate,
                        acceptanceCount = documentInfo.acceptanceCount,
                        timestamp = LocalDateTime.now()
                    )

                    _complianceNotifications.emit(notification)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to perform periodic compliance check")
        }
    }
}

/**
 * Configuration for compliance notifications
 */
data class NotificationConfig(
    val enablePeriodicReports: Boolean = true,
    val checkIntervalMs: Long = 3600000, // 1 hour
    val complianceThreshold: Double = 0.8, // 80%
    val acceptanceRateThreshold: Double = 0.7, // 70%
    val enableEmailNotifications: Boolean = true,
    val enablePushNotifications: Boolean = true,
    val adminEmails: List<String> = emptyList(),
    val notificationChannels: List<String> = listOf("email", "push", "in_app")
)

/**
 * Sealed class for compliance notifications
 */
sealed class ComplianceNotification {
    abstract val timestamp: LocalDateTime

    data class UserNonCompliant(
        val userId: String,
        val missingAcceptances: Int,
        val expiredAcceptances: Int,
        override val timestamp: LocalDateTime
    ) : ComplianceNotification()

    data class LowComplianceAlert(
        val complianceRate: Double,
        val threshold: Double,
        val nonCompliantUsers: Int,
        override val timestamp: LocalDateTime
    ) : ComplianceNotification()

    data class LowDocumentAcceptance(
        val documentId: String,
        val documentTitle: String,
        val acceptanceRate: Double,
        val acceptanceCount: Int,
        override val timestamp: LocalDateTime
    ) : ComplianceNotification()

    data class ComplianceReportGenerated(
        val report: TermsComplianceReport,
        override val timestamp: LocalDateTime
    ) : ComplianceNotification()
}

/**
 * Notification for terms document updates
 */
data class TermsUpdateNotification(
    val documentId: String,
    val documentTitle: String,
    val documentType: com.roshni.games.core.utils.terms.TermsType,
    val newVersion: Int,
    val affectedUsers: List<String>,
    val requiresReAcceptance: Boolean,
    val effectiveDate: LocalDateTime,
    val timestamp: LocalDateTime
)