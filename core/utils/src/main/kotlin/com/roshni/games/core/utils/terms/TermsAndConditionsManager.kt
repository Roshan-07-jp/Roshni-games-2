package com.roshni.games.core.utils.terms

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

/**
 * Core interface for managing terms and conditions in the gaming platform
 */
interface TermsAndConditionsManager {

    /**
     * Current status of the terms and conditions manager
     */
    val status: TermsManagerStatus

    /**
     * Flow of all active terms documents
     */
    val activeTermsDocuments: Flow<List<TermsDocument>>

    /**
     * Flow of pending terms documents for a specific user
     */
    fun getPendingTermsDocuments(userId: String): Flow<List<TermsDocument>>

    /**
     * Flow of accepted terms documents for a specific user
     */
    fun getAcceptedTermsDocuments(userId: String): Flow<List<TermsDocument>>

    /**
     * Initialize the terms and conditions manager
     */
    suspend fun initialize(): Boolean

    /**
     * Create a new terms document
     */
    suspend fun createTermsDocument(
        type: TermsType,
        title: String,
        content: String,
        version: Int,
        language: String = "en",
        region: String? = null,
        requiresAcceptance: Boolean = true,
        effectiveDate: LocalDateTime = LocalDateTime.now(),
        createdBy: String
    ): TermsDocument?

    /**
     * Update an existing terms document
     */
    suspend fun updateTermsDocument(
        documentId: String,
        title: String? = null,
        content: String? = null,
        sections: List<TermsSection>? = null,
        isActive: Boolean? = null,
        modifiedBy: String
    ): Boolean

    /**
     * Get a specific terms document by ID
     */
    suspend fun getTermsDocument(documentId: String): TermsDocument?

    /**
     * Get the latest active version of a terms document type
     */
    suspend fun getLatestTermsDocument(
        type: TermsType,
        language: String = "en"
    ): TermsDocument?

    /**
     * Get all versions of a terms document type
     */
    suspend fun getTermsDocumentVersions(type: TermsType): List<TermsDocument>

    /**
     * Record user acceptance of terms
     */
    suspend fun recordTermsAcceptance(
        userId: String,
        documentId: String,
        acceptanceMethod: AcceptanceMethod,
        ipAddress: String? = null,
        userAgent: String? = null,
        deviceId: String? = null,
        scrolledToBottom: Boolean = false,
        timeSpentViewing: Long? = null,
        context: Map<String, Any> = emptyMap()
    ): TermsAcceptance?

    /**
     * Check if a user has accepted specific terms
     */
    suspend fun hasUserAcceptedTerms(
        userId: String,
        documentId: String
    ): Boolean

    /**
     * Check if a user has accepted the latest version of terms
     */
    suspend fun hasUserAcceptedLatestTerms(
        userId: String,
        type: TermsType,
        language: String = "en"
    ): Boolean

    /**
     * Get user's acceptance record for specific terms
     */
    suspend fun getUserTermsAcceptance(
        userId: String,
        documentId: String
    ): TermsAcceptance?

    /**
     * Get all acceptance records for a user
     */
    suspend fun getUserAcceptances(userId: String): List<TermsAcceptance>

    /**
     * Revoke a user's acceptance of terms
     */
    suspend fun revokeTermsAcceptance(
        acceptanceId: String,
        reason: String,
        revokedBy: String
    ): Boolean

    /**
     * Check if terms require acceptance for a user
     */
    suspend fun requiresTermsAcceptance(
        userId: String,
        userAge: Int? = null,
        userType: String? = null
    ): List<TermsDocument>

    /**
     * Validate that a user has accepted all required terms
     */
    suspend fun validateUserCompliance(
        userId: String,
        userAge: Int? = null,
        userType: String? = null
    ): TermsComplianceResult

    /**
     * Get terms acceptance statistics
     */
    suspend fun getAcceptanceStatistics(
        documentId: String? = null,
        type: TermsType? = null
    ): TermsAcceptanceStatistics

    /**
     * Get compliance report for all users
     */
    suspend fun getComplianceReport(): TermsComplianceReport

    /**
     * Archive old versions of terms documents
     */
    suspend fun archiveOldVersions(
        type: TermsType,
        keepVersions: Int = 5
    ): Int

    /**
     * Clean up expired acceptance records
     */
    suspend fun cleanupExpiredAcceptances(): Int

    /**
     * Export terms and acceptance data for backup
     */
    suspend fun exportTermsData(): TermsExportData

    /**
     * Import terms and acceptance data from backup
     */
    suspend fun importTermsData(data: TermsExportData): Boolean

    /**
     * Shutdown the terms and conditions manager
     */
    suspend fun shutdown()
}

/**
 * Status of the terms and conditions manager
 */
data class TermsManagerStatus(
    val isInitialized: Boolean = false,
    val totalDocuments: Int = 0,
    val activeDocuments: Int = 0,
    val totalAcceptances: Int = 0,
    val validAcceptances: Int = 0,
    val lastActivityTime: LocalDateTime? = null,
    val isShuttingDown: Boolean = false
)

/**
 * Result of terms compliance validation
 */
data class TermsComplianceResult(
    val isCompliant: Boolean,
    val missingAcceptances: List<TermsDocument> = emptyList(),
    val expiredAcceptances: List<TermsAcceptance> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Statistics about terms acceptance
 */
data class TermsAcceptanceStatistics(
    val totalAcceptances: Long = 0,
    val validAcceptances: Long = 0,
    val revokedAcceptances: Long = 0,
    val acceptanceRate: Double = 0.0,
    val averageTimeToAccept: Long = 0,
    val acceptanceByMethod: Map<AcceptanceMethod, Long> = emptyMap(),
    val acceptanceByDocument: Map<String, Long> = emptyMap()
)

/**
 * Compliance report for all users
 */
data class TermsComplianceReport(
    val totalUsers: Int = 0,
    val compliantUsers: Int = 0,
    val nonCompliantUsers: Int = 0,
    val complianceRate: Double = 0.0,
    val documentsWithLowAcceptance: List<DocumentComplianceInfo> = emptyList(),
    val recentAcceptances: List<TermsAcceptance> = emptyList(),
    val generatedAt: LocalDateTime = LocalDateTime.now()
) {

    data class DocumentComplianceInfo(
        val documentId: String,
        val documentTitle: String,
        val acceptanceCount: Int,
        val acceptanceRate: Double
    )
}

/**
 * Export data for terms and conditions
 */
data class TermsExportData(
    val documents: List<TermsDocument> = emptyList(),
    val acceptances: List<TermsAcceptance> = emptyList(),
    val exportedAt: LocalDateTime = LocalDateTime.now(),
    val version: String = "1.0"
)