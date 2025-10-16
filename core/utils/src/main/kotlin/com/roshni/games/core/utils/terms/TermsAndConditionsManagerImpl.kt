package com.roshni.games.core.utils.terms

import com.roshni.games.core.database.dao.TermsDao
import com.roshni.games.core.database.model.TermsAcceptanceEntity
import com.roshni.games.core.database.model.TermsDocumentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import timber.log.Timber
import java.util.UUID

/**
 * Default implementation of the TermsAndConditionsManager interface
 */
class TermsAndConditionsManagerImpl(
    private val termsDao: TermsDao
) : TermsAndConditionsManager {

    private val mutex = Mutex()
    private val _status = MutableStateFlow(TermsManagerStatus())
    private val _activeTermsDocuments = MutableStateFlow<List<TermsDocument>>(emptyList())

    override val status: TermsManagerStatus
        get() = _status.value

    override val activeTermsDocuments: Flow<List<TermsDocument>>
        get() = _activeTermsDocuments.asStateFlow()

    override fun getPendingTermsDocuments(userId: String): Flow<List<TermsDocument>> {
        return termsDao.getPendingTermsDocumentsForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getAcceptedTermsDocuments(userId: String): Flow<List<TermsDocument>> {
        return termsDao.getAcceptedTermsDocumentsForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun initialize(): Boolean = mutex.withLock {
        try {
            Timber.d("Initializing TermsAndConditionsManager")

            // Load active documents
            val activeDocuments = termsDao.getAllActiveTermsDocuments()
            _activeTermsDocuments.value = activeDocuments.map { it.toDomainModel() }

            // Update status
            updateStatus()

            _status.value = _status.value.copy(isInitialized = true)
            Timber.d("TermsAndConditionsManager initialized successfully")

            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize TermsAndConditionsManager")
            false
        }
    }

    override suspend fun createTermsDocument(
        type: TermsType,
        title: String,
        content: String,
        version: Int,
        language: String,
        region: String?,
        requiresAcceptance: Boolean,
        effectiveDate: LocalDateTime,
        createdBy: String
    ): TermsDocument? = mutex.withLock {
        try {
            // Check if document with same type, language, and region already exists
            val existingDocument = termsDao.getLatestActiveTermsDocument(type, language)
            if (existingDocument != null && existingDocument.region == region) {
                Timber.w("Terms document of type $type for language $language already exists")
                return null
            }

            val document = TermsDocument(
                id = generateDocumentId(),
                type = type,
                version = version,
                title = title,
                description = "Terms document: $title",
                content = content,
                language = language,
                region = region,
                requiresAcceptance = requiresAcceptance,
                effectiveDate = effectiveDate,
                createdAt = LocalDateTime.now(),
                modifiedAt = LocalDateTime.now(),
                createdBy = createdBy,
                modifiedBy = createdBy
            )

            val entity = TermsDocumentEntity.fromDomainModel(document)
            val result = termsDao.insertTermsDocument(entity)

            if (result > 0) {
                // Update active documents
                refreshActiveDocuments()
                updateStatus()
                Timber.d("Terms document created successfully: ${document.id}")
                document
            } else {
                Timber.e("Failed to insert terms document")
                null
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to create terms document")
            null
        }
    }

    override suspend fun updateTermsDocument(
        documentId: String,
        title: String?,
        content: String?,
        sections: List<TermsSection>?,
        isActive: Boolean?,
        modifiedBy: String
    ): Boolean = mutex.withLock {
        try {
            val existingDocument = termsDao.getTermsDocument(documentId) ?: return false

            val updatedDocument = existingDocument.copy(
                title = title ?: existingDocument.title,
                content = content ?: existingDocument.content,
                sections = sections ?: existingDocument.sections,
                isActive = isActive ?: existingDocument.isActive,
                modifiedAt = LocalDateTime.now(),
                modifiedBy = modifiedBy
            )

            val result = termsDao.updateTermsDocument(updatedDocument)

            if (result > 0) {
                // Update active documents if status changed
                if (isActive != null) {
                    refreshActiveDocuments()
                }
                updateStatus()
                Timber.d("Terms document updated successfully: $documentId")
                true
            } else {
                Timber.e("Failed to update terms document: $documentId")
                false
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to update terms document: $documentId")
            false
        }
    }

    override suspend fun getTermsDocument(documentId: String): TermsDocument? {
        return try {
            termsDao.getTermsDocument(documentId)?.toDomainModel()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get terms document: $documentId")
            null
        }
    }

    override suspend fun getLatestTermsDocument(
        type: TermsType,
        language: String
    ): TermsDocument? {
        return try {
            termsDao.getLatestActiveTermsDocument(type, language)?.toDomainModel()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get latest terms document for type: $type")
            null
        }
    }

    override suspend fun getTermsDocumentVersions(type: TermsType): List<TermsDocument> {
        return try {
            termsDao.getTermsDocumentsByType(type).map { it.toDomainModel() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get terms document versions for type: $type")
            emptyList()
        }
    }

    override suspend fun recordTermsAcceptance(
        userId: String,
        documentId: String,
        acceptanceMethod: AcceptanceMethod,
        ipAddress: String?,
        userAgent: String?,
        deviceId: String?,
        scrolledToBottom: Boolean,
        timeSpentViewing: Long?,
        context: Map<String, Any>
    ): TermsAcceptance? = mutex.withLock {
        try {
            // Verify document exists and is active
            val document = getTermsDocument(documentId) ?: return null
            if (!document.isActive || !document.requiresAcceptance) {
                Timber.w("Document $documentId is not active or doesn't require acceptance")
                return null
            }

            // Check if user already has a valid acceptance
            if (hasUserAcceptedTerms(userId, documentId)) {
                Timber.d("User $userId already has valid acceptance for document $documentId")
                return getUserTermsAcceptance(userId, documentId)
            }

            val acceptance = TermsAcceptance(
                id = generateAcceptanceId(),
                userId = userId,
                documentId = documentId,
                documentVersion = document.version,
                acceptanceMethod = acceptanceMethod,
                acceptedAt = LocalDateTime.now(),
                ipAddress = ipAddress,
                userAgent = userAgent,
                deviceId = deviceId,
                scrolledToBottom = scrolledToBottom,
                timeSpentViewing = timeSpentViewing,
                context = context,
                createdAt = LocalDateTime.now(),
                modifiedAt = LocalDateTime.now()
            )

            val entity = TermsAcceptanceEntity.fromDomainModel(acceptance)
            val result = termsDao.insertTermsAcceptance(entity)

            if (result > 0) {
                updateStatus()
                Timber.d("Terms acceptance recorded for user $userId, document $documentId")
                acceptance
            } else {
                Timber.e("Failed to record terms acceptance")
                null
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to record terms acceptance")
            null
        }
    }

    override suspend fun hasUserAcceptedTerms(
        userId: String,
        documentId: String
    ): Boolean {
        return try {
            termsDao.getLatestValidAcceptance(userId, documentId) != null
        } catch (e: Exception) {
            Timber.e(e, "Failed to check terms acceptance for user $userId")
            false
        }
    }

    override suspend fun hasUserAcceptedLatestTerms(
        userId: String,
        type: TermsType,
        language: String
    ): Boolean {
        return try {
            val latestDocument = getLatestTermsDocument(type, language) ?: return false
            hasUserAcceptedTerms(userId, latestDocument.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check latest terms acceptance for user $userId")
            false
        }
    }

    override suspend fun getUserTermsAcceptance(
        userId: String,
        documentId: String
    ): TermsAcceptance? {
        return try {
            termsDao.getLatestValidAcceptance(userId, documentId)?.toDomainModel()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user terms acceptance")
            null
        }
    }

    override suspend fun getUserAcceptances(userId: String): List<TermsAcceptance> {
        return try {
            termsDao.getAllAcceptancesForUser(userId).map { it.toDomainModel() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user acceptances")
            emptyList()
        }
    }

    override suspend fun revokeTermsAcceptance(
        acceptanceId: String,
        reason: String,
        revokedBy: String
    ): Boolean = mutex.withLock {
        try {
            val acceptance = termsDao.getTermsAcceptance(acceptanceId) ?: return false

            val updatedAcceptance = acceptance.copy(
                isValid = false,
                revokedAt = LocalDateTime.now(),
                revocationReason = reason,
                revokedBy = revokedBy,
                modifiedAt = LocalDateTime.now()
            )

            val result = termsDao.updateTermsAcceptance(updatedAcceptance)

            if (result > 0) {
                updateStatus()
                Timber.d("Terms acceptance revoked: $acceptanceId")
                true
            } else {
                Timber.e("Failed to revoke terms acceptance: $acceptanceId")
                false
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to revoke terms acceptance")
            false
        }
    }

    override suspend fun requiresTermsAcceptance(
        userId: String,
        userAge: Int?,
        userType: String?
    ): List<TermsDocument> {
        return try {
            termsDao.getPendingTermsDocumentsForUser(userId).map { it.toDomainModel() }
                .filter { document ->
                    document.appliesToUser(userAge, userType) &&
                    document.requiresAcceptance &&
                    document.isEffective()
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check terms requirements for user $userId")
            emptyList()
        }
    }

    override suspend fun validateUserCompliance(
        userId: String,
        userAge: Int?,
        userType: String?
    ): TermsComplianceResult {
        return try {
            val requiredDocuments = requiresTermsAcceptance(userId, userAge, userType)
            val missingAcceptances = mutableListOf<TermsDocument>()
            val expiredAcceptances = mutableListOf<TermsAcceptance>()

            for (document in requiredDocuments) {
                val acceptance = getUserTermsAcceptance(userId, document.id)
                if (acceptance == null) {
                    missingAcceptances.add(document)
                } else if (acceptance.isExpired(document.expirationDate)) {
                    expiredAcceptances.add(acceptance)
                }
            }

            val warnings = mutableListOf<String>()
            if (expiredAcceptances.isNotEmpty()) {
                warnings.add("User has ${expiredAcceptances.size} expired acceptance(s)")
            }

            TermsComplianceResult(
                isCompliant = missingAcceptances.isEmpty() && expiredAcceptances.isEmpty(),
                missingAcceptances = missingAcceptances,
                expiredAcceptances = expiredAcceptances,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to validate user compliance")
            TermsComplianceResult(
                isCompliant = false,
                warnings = listOf("Compliance validation failed: ${e.message}")
            )
        }
    }

    override suspend fun getAcceptanceStatistics(
        documentId: String?,
        type: TermsType?
    ): TermsAcceptanceStatistics {
        return try {
            val allAcceptances = if (documentId != null) {
                termsDao.getValidAcceptancesForDocument(documentId).map { it.toDomainModel() }
            } else {
                // Get all valid acceptances
                termsDao.getAllAcceptancesForUser("").map { it.toDomainModel() }
                    .filter { acceptance ->
                        type == null || getTermsDocument(acceptance.documentId)?.type == type
                    }
            }

            val validAcceptances = allAcceptances.filter { it.isCurrentlyValid() }
            val revokedAcceptances = allAcceptances.filter { !it.isCurrentlyValid() }

            val acceptanceByMethod = validAcceptances.groupBy { it.acceptanceMethod }
                .mapValues { it.value.size.toLong() }

            val acceptanceByDocument = validAcceptances.groupBy { it.documentId }
                .mapValues { it.value.size.toLong() }

            val averageTimeToAccept = if (validAcceptances.isNotEmpty()) {
                validAcceptances.mapNotNull { it.timeSpentViewing }.average().toLong()
            } else 0

            TermsAcceptanceStatistics(
                totalAcceptances = allAcceptances.size.toLong(),
                validAcceptances = validAcceptances.size.toLong(),
                revokedAcceptances = revokedAcceptances.size.toLong(),
                acceptanceRate = if (allAcceptances.isNotEmpty()) {
                    validAcceptances.size.toDouble() / allAcceptances.size
                } else 0.0,
                averageTimeToAccept = averageTimeToAccept,
                acceptanceByMethod = acceptanceByMethod,
                acceptanceByDocument = acceptanceByDocument
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to get acceptance statistics")
            TermsAcceptanceStatistics()
        }
    }

    override suspend fun getComplianceReport(): TermsComplianceReport {
        return try {
            val allDocuments = termsDao.getAllActiveTermsDocuments().map { it.toDomainModel() }
            val allAcceptances = termsDao.getAllAcceptancesForUser("").map { it.toDomainModel() }

            val documentsWithLowAcceptance = allDocuments.map { document ->
                val acceptanceCount = allAcceptances.count { it.documentId == document.id && it.isCurrentlyValid() }
                val acceptanceRate = if (allAcceptances.isNotEmpty()) {
                    acceptanceCount.toDouble() / allAcceptances.size
                } else 0.0

                TermsComplianceReport.DocumentComplianceInfo(
                    documentId = document.id,
                    documentTitle = document.title,
                    acceptanceCount = acceptanceCount,
                    acceptanceRate = acceptanceRate
                )
            }.filter { it.acceptanceRate < 0.8 } // Less than 80% acceptance rate

            val recentAcceptances = allAcceptances
                .filter { it.isCurrentlyValid() }
                .sortedByDescending { it.acceptedAt }
                .take(10)

            TermsComplianceReport(
                totalUsers = allAcceptances.distinctBy { it.userId }.size,
                compliantUsers = allAcceptances.distinctBy { it.userId }
                    .count { userId ->
                        allDocuments.all { document ->
                            allAcceptances.any {
                                it.userId == userId &&
                                it.documentId == document.id &&
                                it.isCurrentlyValid()
                            }
                        }
                    },
                nonCompliantUsers = 0, // Calculated above
                complianceRate = 0.0, // Would need more complex calculation
                documentsWithLowAcceptance = documentsWithLowAcceptance,
                recentAcceptances = recentAcceptances
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to generate compliance report")
            TermsComplianceReport()
        }
    }

    override suspend fun archiveOldVersions(
        type: TermsType,
        keepVersions: Int
    ): Int = mutex.withLock {
        try {
            val allVersions = getTermsDocumentVersions(type)
                .sortedByDescending { it.version }

            if (allVersions.size <= keepVersions) {
                return 0
            }

            val toArchive = allVersions.drop(keepVersions)
            var archivedCount = 0

            for (document in toArchive) {
                val result = termsDao.updateTermsDocument(
                    document.toEntity().copy(isActive = false)
                )
                if (result > 0) {
                    archivedCount++
                }
            }

            if (archivedCount > 0) {
                refreshActiveDocuments()
                updateStatus()
            }

            Timber.d("Archived $archivedCount old versions of $type")
            archivedCount

        } catch (e: Exception) {
            Timber.e(e, "Failed to archive old versions")
            0
        }
    }

    override suspend fun cleanupExpiredAcceptances(): Int = mutex.withLock {
        try {
            val currentTime = LocalDateTime.now()
            val expiredAcceptances = termsDao.getAllAcceptancesForUser("").map { it.toDomainModel() }
                .filter { it.isExpired(null) || !it.isCurrentlyValid() }

            var cleanedCount = 0
            for (acceptance in expiredAcceptances) {
                val result = termsDao.deleteTermsAcceptance(acceptance.id)
                if (result > 0) {
                    cleanedCount++
                }
            }

            if (cleanedCount > 0) {
                updateStatus()
            }

            Timber.d("Cleaned up $cleanedCount expired acceptance records")
            cleanedCount

        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup expired acceptances")
            0
        }
    }

    override suspend fun exportTermsData(): TermsExportData {
        return try {
            val documents = termsDao.getAllActiveTermsDocuments().map { it.toDomainModel() }
            val acceptances = termsDao.getAllAcceptancesForUser("").map { it.toDomainModel() }

            TermsExportData(
                documents = documents,
                acceptances = acceptances
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to export terms data")
            TermsExportData()
        }
    }

    override suspend fun importTermsData(data: TermsExportData): Boolean = mutex.withLock {
        try {
            var successCount = 0

            // Import documents
            for (document in data.documents) {
                try {
                    val entity = TermsDocumentEntity.fromDomainModel(document)
                    termsDao.insertTermsDocument(entity)
                    successCount++
                } catch (e: Exception) {
                    Timber.e(e, "Failed to import document: ${document.id}")
                }
            }

            // Import acceptances
            for (acceptance in data.acceptances) {
                try {
                    val entity = TermsAcceptanceEntity.fromDomainModel(acceptance)
                    termsDao.insertTermsAcceptance(entity)
                    successCount++
                } catch (e: Exception) {
                    Timber.e(e, "Failed to import acceptance: ${acceptance.id}")
                }
            }

            refreshActiveDocuments()
            updateStatus()

            Timber.d("Imported $successCount terms data records")
            successCount > 0

        } catch (e: Exception) {
            Timber.e(e, "Failed to import terms data")
            false
        }
    }

    override suspend fun shutdown() {
        try {
            Timber.d("Shutting down TermsAndConditionsManager")
            _status.value = _status.value.copy(isShuttingDown = true)
            _activeTermsDocuments.value = emptyList()
            Timber.d("TermsAndConditionsManager shutdown complete")
        } catch (e: Exception) {
            Timber.e(e, "Error during TermsAndConditionsManager shutdown")
        }
    }

    /**
     * Refresh the active terms documents list
     */
    private suspend fun refreshActiveDocuments() {
        try {
            val activeDocuments = termsDao.getAllActiveTermsDocuments()
            _activeTermsDocuments.value = activeDocuments.map { it.toDomainModel() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh active documents")
        }
    }

    /**
     * Update manager status
     */
    private suspend fun updateStatus() {
        try {
            val totalDocuments = termsDao.getAllActiveTermsDocuments().size
            val totalAcceptances = termsDao.getAllAcceptancesForUser("").size
            val validAcceptances = termsDao.getAllAcceptancesForUser("").count { it.isValid }

            _status.value = _status.value.copy(
                totalDocuments = totalDocuments,
                activeDocuments = totalDocuments,
                totalAcceptances = totalAcceptances,
                validAcceptances = validAcceptances,
                lastActivityTime = LocalDateTime.now()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to update status")
        }
    }

    /**
     * Generate a unique document ID
     */
    private fun generateDocumentId(): String {
        return "terms_${UUID.randomUUID()}"
    }

    /**
     * Generate a unique acceptance ID
     */
    private fun generateAcceptanceId(): String {
        return "acceptance_${UUID.randomUUID()}"
    }
}