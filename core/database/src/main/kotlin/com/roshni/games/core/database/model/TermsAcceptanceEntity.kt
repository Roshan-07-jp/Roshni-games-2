package com.roshni.games.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.roshni.games.core.database.converter.AcceptanceMethodConverter
import com.roshni.games.core.database.converter.LocalDateTimeConverter
import com.roshni.games.core.database.converter.MapConverter
import com.roshni.games.core.utils.terms.AcceptanceMethod
import com.roshni.games.core.utils.terms.TermsAcceptance
import kotlinx.datetime.LocalDateTime

@Entity(
    tableName = "terms_acceptances",
    foreignKeys = [
        ForeignKey(
            entity = TermsDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["documentId"]),
        Index(value = ["userId", "documentId"], unique = true),
        Index(value = ["acceptedAt"]),
        Index(value = ["isValid"])
    ]
)
@TypeConverters(
    AcceptanceMethodConverter::class,
    LocalDateTimeConverter::class,
    MapConverter::class
)
data class TermsAcceptanceEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val documentId: String,
    val documentVersion: Int,
    val acceptanceMethod: AcceptanceMethod,
    val acceptedAt: LocalDateTime,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val deviceId: String? = null,
    val location: String? = null,
    val scrolledToBottom: Boolean = false,
    val timeSpentViewing: Long? = null,
    val isVoluntary: Boolean = true,
    val isValid: Boolean = true,
    val revokedAt: LocalDateTime? = null,
    val revocationReason: String? = null,
    val revokedBy: String? = null,
    val summaryPresented: Boolean = false,
    val keyPointsAcknowledged: Boolean = false,
    val presentationLanguage: String = "en",
    val accessibilityFeaturesUsed: Boolean = false,
    val context: Map<String, Any> = emptyMap(),
    val signature: String? = null,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime
) {

    /**
     * Convert to domain model
     */
    fun toDomainModel(): TermsAcceptance {
        return TermsAcceptance(
            id = id,
            userId = userId,
            documentId = documentId,
            documentVersion = documentVersion,
            acceptanceMethod = acceptanceMethod,
            acceptedAt = acceptedAt,
            ipAddress = ipAddress,
            userAgent = userAgent,
            deviceId = deviceId,
            location = location,
            scrolledToBottom = scrolledToBottom,
            timeSpentViewing = timeSpentViewing,
            isVoluntary = isVoluntary,
            isValid = isValid,
            revokedAt = revokedAt,
            revocationReason = revocationReason,
            revokedBy = revokedBy,
            summaryPresented = summaryPresented,
            keyPointsAcknowledged = keyPointsAcknowledged,
            presentationLanguage = presentationLanguage,
            accessibilityFeaturesUsed = accessibilityFeaturesUsed,
            context = context,
            signature = signature,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Create from domain model
         */
        fun fromDomainModel(domainModel: TermsAcceptance): TermsAcceptanceEntity {
            return TermsAcceptanceEntity(
                id = domainModel.id,
                userId = domainModel.userId,
                documentId = domainModel.documentId,
                documentVersion = domainModel.documentVersion,
                acceptanceMethod = domainModel.acceptanceMethod,
                acceptedAt = domainModel.acceptedAt,
                ipAddress = domainModel.ipAddress,
                userAgent = domainModel.userAgent,
                deviceId = domainModel.deviceId,
                location = domainModel.location,
                scrolledToBottom = domainModel.scrolledToBottom,
                timeSpentViewing = domainModel.timeSpentViewing,
                isVoluntary = domainModel.isVoluntary,
                isValid = domainModel.isValid,
                revokedAt = domainModel.revokedAt,
                revocationReason = domainModel.revocationReason,
                revokedBy = domainModel.revokedBy,
                summaryPresented = domainModel.summaryPresented,
                keyPointsAcknowledged = domainModel.keyPointsAcknowledged,
                presentationLanguage = domainModel.presentationLanguage,
                accessibilityFeaturesUsed = domainModel.accessibilityFeaturesUsed,
                context = domainModel.context,
                signature = domainModel.signature,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }
    }
}