package com.roshni.games.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roshni.games.core.database.model.TermsDocumentEntity
import com.roshni.games.core.utils.terms.TermsType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
interface TermsDao {

    // TermsDocument operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTermsDocument(document: TermsDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTermsDocuments(documents: List<TermsDocumentEntity>): List<Long>

    @Update
    suspend fun updateTermsDocument(document: TermsDocumentEntity): Int

    @Query("SELECT * FROM terms_documents WHERE id = :documentId")
    suspend fun getTermsDocument(documentId: String): TermsDocumentEntity?

    @Query("SELECT * FROM terms_documents WHERE type = :type AND isActive = 1 ORDER BY version DESC LIMIT 1")
    suspend fun getLatestActiveTermsDocument(type: TermsType): TermsDocumentEntity?

    @Query("SELECT * FROM terms_documents WHERE type = :type AND language = :language AND isActive = 1 ORDER BY version DESC LIMIT 1")
    suspend fun getLatestActiveTermsDocument(type: TermsType, language: String): TermsDocumentEntity?

    @Query("SELECT * FROM terms_documents WHERE isActive = 1 ORDER BY type, version DESC")
    fun getAllActiveTermsDocuments(): Flow<List<TermsDocumentEntity>>

    @Query("SELECT * FROM terms_documents WHERE type = :type ORDER BY version DESC")
    fun getTermsDocumentsByType(type: TermsType): Flow<List<TermsDocumentEntity>>

    @Query("SELECT * FROM terms_documents WHERE requiresAcceptance = 1 AND isActive = 1")
    fun getTermsDocumentsRequiringAcceptance(): Flow<List<TermsDocumentEntity>>

    @Query("SELECT * FROM terms_documents WHERE effectiveDate <= :currentTime AND (expirationDate IS NULL OR expirationDate > :currentTime) AND isActive = 1")
    fun getEffectiveTermsDocuments(currentTime: LocalDateTime): Flow<List<TermsDocumentEntity>>

    @Query("SELECT * FROM terms_documents WHERE minimumAge IS NOT NULL AND minimumAge <= :userAge AND isActive = 1")
    fun getTermsDocumentsForUserAge(userAge: Int): Flow<List<TermsDocumentEntity>>

    @Query("SELECT * FROM terms_documents WHERE :userType IN (applicableUserTypes) OR appliesToAllUsers = 1")
    fun getTermsDocumentsForUserType(userType: String): Flow<List<TermsDocumentEntity>>

    @Query("DELETE FROM terms_documents WHERE id = :documentId")
    suspend fun deleteTermsDocument(documentId: String): Int

    @Query("DELETE FROM terms_documents WHERE type = :type")
    suspend fun deleteTermsDocumentsByType(type: TermsType): Int

    @Query("SELECT COUNT(*) FROM terms_documents WHERE type = :type AND isActive = 1")
    suspend fun getActiveDocumentCount(type: TermsType): Int

    @Query("SELECT * FROM terms_documents WHERE modifiedAt > :since ORDER BY modifiedAt DESC")
    fun getRecentlyModifiedDocuments(since: LocalDateTime): Flow<List<TermsDocumentEntity>>

    // TermsAcceptance operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTermsAcceptance(acceptance: com.roshni.games.core.database.model.TermsAcceptanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTermsAcceptances(acceptances: List<com.roshni.games.core.database.model.TermsAcceptanceEntity>): List<Long>

    @Update
    suspend fun updateTermsAcceptance(acceptance: com.roshni.games.core.database.model.TermsAcceptanceEntity): Int

    @Query("SELECT * FROM terms_acceptances WHERE id = :acceptanceId")
    suspend fun getTermsAcceptance(acceptanceId: String): com.roshni.games.core.database.model.TermsAcceptanceEntity?

    @Query("SELECT * FROM terms_acceptances WHERE userId = :userId AND documentId = :documentId AND isValid = 1 ORDER BY acceptedAt DESC LIMIT 1")
    suspend fun getLatestValidAcceptance(userId: String, documentId: String): com.roshni.games.core.database.model.TermsAcceptanceEntity?

    @Query("SELECT * FROM terms_acceptances WHERE userId = :userId AND isValid = 1 ORDER BY acceptedAt DESC")
    fun getValidAcceptancesForUser(userId: String): Flow<List<com.roshni.games.core.database.model.TermsAcceptanceEntity>>

    @Query("SELECT * FROM terms_acceptances WHERE documentId = :documentId AND isValid = 1 ORDER BY acceptedAt DESC")
    fun getValidAcceptancesForDocument(documentId: String): Flow<List<com.roshni.games.core.database.model.TermsAcceptanceEntity>>

    @Query("SELECT * FROM terms_acceptances WHERE userId = :userId ORDER BY acceptedAt DESC")
    fun getAllAcceptancesForUser(userId: String): Flow<List<com.roshni.games.core.database.model.TermsAcceptanceEntity>>

    @Query("SELECT * FROM terms_acceptances WHERE acceptedAt >= :since ORDER BY acceptedAt DESC")
    fun getRecentAcceptances(since: LocalDateTime): Flow<List<com.roshni.games.core.database.model.TermsAcceptanceEntity>>

    @Query("SELECT * FROM terms_acceptances WHERE isValid = 0 AND revokedAt IS NOT NULL ORDER BY revokedAt DESC")
    fun getRevokedAcceptances(): Flow<List<com.roshni.games.core.database.model.TermsAcceptanceEntity>>

    @Query("SELECT * FROM terms_acceptances WHERE acceptanceMethod = :method AND isValid = 1")
    fun getAcceptancesByMethod(method: com.roshni.games.core.utils.terms.AcceptanceMethod): Flow<List<com.roshni.games.core.database.model.TermsAcceptanceEntity>>

    @Query("SELECT COUNT(*) FROM terms_acceptances WHERE userId = :userId AND documentId = :documentId AND isValid = 1")
    suspend fun getAcceptanceCount(userId: String, documentId: String): Int

    @Query("SELECT COUNT(*) FROM terms_acceptances WHERE documentId = :documentId AND isValid = 1")
    suspend fun getTotalAcceptanceCount(documentId: String): Int

    @Query("DELETE FROM terms_acceptances WHERE id = :acceptanceId")
    suspend fun deleteTermsAcceptance(acceptanceId: String): Int

    @Query("DELETE FROM terms_acceptances WHERE userId = :userId")
    suspend fun deleteAllAcceptancesForUser(userId: String): Int

    @Query("DELETE FROM terms_acceptances WHERE documentId = :documentId")
    suspend fun deleteAllAcceptancesForDocument(documentId: String): Int

    // Combined queries

    @Query("""
        SELECT t.* FROM terms_documents t
        INNER JOIN terms_acceptances a ON t.id = a.documentId
        WHERE a.userId = :userId AND a.isValid = 1 AND t.isActive = 1
        ORDER BY t.type, t.version DESC
    """)
    fun getAcceptedTermsDocumentsForUser(userId: String): Flow<List<TermsDocumentEntity>>

    @Query("""
        SELECT t.* FROM terms_documents t
        LEFT JOIN terms_acceptances a ON t.id = a.documentId AND a.userId = :userId AND a.isValid = 1
        WHERE t.isActive = 1 AND t.requiresAcceptance = 1 AND a.id IS NULL
        ORDER BY t.type, t.version DESC
    """)
    fun getPendingTermsDocumentsForUser(userId: String): Flow<List<TermsDocumentEntity>>

    @Query("""
        SELECT t.*, a.acceptedAt as lastAcceptedAt FROM terms_documents t
        LEFT JOIN terms_acceptances a ON t.id = a.documentId AND a.userId = :userId AND a.isValid = 1
        WHERE t.isActive = 1 AND t.requiresAcceptance = 1
        ORDER BY a.acceptedAt DESC NULLS LAST, t.type
    """)
    fun getTermsDocumentsWithAcceptanceStatus(userId: String): Flow<List<TermsDocumentWithAcceptance>>
}

data class TermsDocumentWithAcceptance(
    val document: TermsDocumentEntity,
    val lastAcceptedAt: LocalDateTime?
)