package com.roshni.games.core.utils.terms.repository

import com.roshni.games.core.database.dao.TermsDao
import com.roshni.games.core.database.model.TermsAcceptanceEntity
import com.roshni.games.core.database.model.TermsDocumentEntity
import com.roshni.games.core.utils.terms.TermsAcceptance
import com.roshni.games.core.utils.terms.TermsDocument
import com.roshni.games.core.utils.terms.TermsType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import timber.log.Timber

/**
 * Repository interface for terms and conditions data access
 */
interface TermsRepository {

    /**
     * Get all active terms documents
     */
    fun getActiveTermsDocuments(): Flow<List<TermsDocument>>

    /**
     * Get terms document by ID
     */
    suspend fun getTermsDocument(documentId: String): TermsDocument?

    /**
     * Get latest active terms document by type and language
     */
    suspend fun getLatestTermsDocument(type: TermsType, language: String = "en"): TermsDocument?

    /**
     * Get all versions of terms documents by type
     */
    suspend fun getTermsDocumentVersions(type: TermsType): List<TermsDocument>

    /**
     * Save terms document
     */
    suspend fun saveTermsDocument(document: TermsDocument): Boolean

    /**
     * Delete terms document
     */
    suspend fun deleteTermsDocument(documentId: String): Boolean

    /**
     * Get user's acceptance records
     */
    fun getUserAcceptances(userId: String): Flow<List<TermsAcceptance>>

    /**
     * Get specific acceptance record
     */
    suspend fun getTermsAcceptance(acceptanceId: String): TermsAcceptance?

    /**
     * Get latest valid acceptance for user and document
     */
    suspend fun getLatestUserAcceptance(userId: String, documentId: String): TermsAcceptance?

    /**
     * Save terms acceptance
     */
    suspend fun saveTermsAcceptance(acceptance: TermsAcceptance): Boolean

    /**
     * Delete terms acceptance
     */
    suspend fun deleteTermsAcceptance(acceptanceId: String): Boolean

    /**
     * Get pending terms documents for user
     */
    fun getPendingTermsDocuments(userId: String): Flow<List<TermsDocument>>

    /**
     * Get accepted terms documents for user
     */
    fun getAcceptedTermsDocuments(userId: String): Flow<List<TermsDocument>>

    /**
     * Check if user has accepted specific terms
     */
    suspend fun hasUserAcceptedTerms(userId: String, documentId: String): Boolean

    /**
     * Check if user has accepted latest version of terms
     */
    suspend fun hasUserAcceptedLatestTerms(userId: String, type: TermsType, language: String = "en"): Boolean
}

/**
 * Implementation of TermsRepository
 */
class TermsRepositoryImpl(
    private val termsDao: TermsDao
) : TermsRepository {

    override fun getActiveTermsDocuments(): Flow<List<TermsDocument>> {
        return termsDao.getAllActiveTermsDocuments()
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun getTermsDocument(documentId: String): TermsDocument? {
        return try {
            termsDao.getTermsDocument(documentId)?.toDomainModel()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get terms document: $documentId")
            null
        }
    }

    override suspend fun getLatestTermsDocument(type: TermsType, language: String): TermsDocument? {
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

    override suspend fun saveTermsDocument(document: TermsDocument): Boolean {
        return try {
            val entity = TermsDocumentEntity.fromDomainModel(document)
            val result = termsDao.insertTermsDocument(entity)
            result > 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to save terms document: ${document.id}")
            false
        }
    }

    override suspend fun deleteTermsDocument(documentId: String): Boolean {
        return try {
            val result = termsDao.deleteTermsDocument(documentId)
            result > 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete terms document: $documentId")
            false
        }
    }

    override fun getUserAcceptances(userId: String): Flow<List<TermsAcceptance>> {
        return termsDao.getAllAcceptancesForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun getTermsAcceptance(acceptanceId: String): TermsAcceptance? {
        return try {
            termsDao.getTermsAcceptance(acceptanceId)?.toDomainModel()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get terms acceptance: $acceptanceId")
            null
        }
    }

    override suspend fun getLatestUserAcceptance(userId: String, documentId: String): TermsAcceptance? {
        return try {
            termsDao.getLatestValidAcceptance(userId, documentId)?.toDomainModel()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get latest user acceptance")
            null
        }
    }

    override suspend fun saveTermsAcceptance(acceptance: TermsAcceptance): Boolean {
        return try {
            val entity = TermsAcceptanceEntity.fromDomainModel(acceptance)
            val result = termsDao.insertTermsAcceptance(entity)
            result > 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to save terms acceptance: ${acceptance.id}")
            false
        }
    }

    override suspend fun deleteTermsAcceptance(acceptanceId: String): Boolean {
        return try {
            val result = termsDao.deleteTermsAcceptance(acceptanceId)
            result > 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete terms acceptance: $acceptanceId")
            false
        }
    }

    override fun getPendingTermsDocuments(userId: String): Flow<List<TermsDocument>> {
        return termsDao.getPendingTermsDocumentsForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getAcceptedTermsDocuments(userId: String): Flow<List<TermsDocument>> {
        return termsDao.getAcceptedTermsDocumentsForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun hasUserAcceptedTerms(userId: String, documentId: String): Boolean {
        return try {
            termsDao.getLatestValidAcceptance(userId, documentId) != null
        } catch (e: Exception) {
            Timber.e(e, "Failed to check user acceptance")
            false
        }
    }

    override suspend fun hasUserAcceptedLatestTerms(userId: String, type: TermsType, language: String): Boolean {
        return try {
            val latestDocument = getLatestTermsDocument(type, language) ?: return false
            hasUserAcceptedTerms(userId, latestDocument.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check latest terms acceptance")
            false
        }
    }
}