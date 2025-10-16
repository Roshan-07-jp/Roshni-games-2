package com.roshni.games.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.roshni.games.core.database.converter.LocalDateTimeConverter
import com.roshni.games.core.database.converter.MapConverter
import com.roshni.games.core.database.converter.TermsSectionConverter
import com.roshni.games.core.database.converter.TermsTypeConverter
import com.roshni.games.core.utils.terms.TermsDocument
import com.roshni.games.core.utils.terms.TermsSection
import com.roshni.games.core.utils.terms.TermsType
import kotlinx.datetime.LocalDateTime

@Entity(tableName = "terms_documents")
@TypeConverters(
    TermsTypeConverter::class,
    LocalDateTimeConverter::class,
    TermsSectionConverter::class,
    MapConverter::class
)
data class TermsDocumentEntity(
    @PrimaryKey
    val id: String,
    val type: TermsType,
    val version: Int,
    val title: String,
    val description: String,
    val content: String,
    val sections: List<TermsSection> = emptyList(),
    val language: String = "en",
    val region: String? = null,
    val isActive: Boolean = true,
    val requiresAcceptance: Boolean = true,
    val appliesToAllUsers: Boolean = true,
    val minimumAge: Int? = null,
    val applicableUserTypes: List<String> = emptyList(),
    val effectiveDate: LocalDateTime,
    val expirationDate: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime,
    val createdBy: String,
    val modifiedBy: String,
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Convert to domain model
     */
    fun toDomainModel(): TermsDocument {
        return TermsDocument(
            id = id,
            type = type,
            version = version,
            title = title,
            description = description,
            content = content,
            sections = sections,
            language = language,
            region = region,
            isActive = isActive,
            requiresAcceptance = requiresAcceptance,
            appliesToAllUsers = appliesToAllUsers,
            minimumAge = minimumAge,
            applicableUserTypes = applicableUserTypes,
            effectiveDate = effectiveDate,
            expirationDate = expirationDate,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            createdBy = createdBy,
            modifiedBy = modifiedBy,
            metadata = metadata
        )
    }

    companion object {
        /**
         * Create from domain model
         */
        fun fromDomainModel(domainModel: TermsDocument): TermsDocumentEntity {
            return TermsDocumentEntity(
                id = domainModel.id,
                type = domainModel.type,
                version = domainModel.version,
                title = domainModel.title,
                description = domainModel.description,
                content = domainModel.content,
                sections = domainModel.sections,
                language = domainModel.language,
                region = domainModel.region,
                isActive = domainModel.isActive,
                requiresAcceptance = domainModel.requiresAcceptance,
                appliesToAllUsers = domainModel.appliesToAllUsers,
                minimumAge = domainModel.minimumAge,
                applicableUserTypes = domainModel.applicableUserTypes,
                effectiveDate = domainModel.effectiveDate,
                expirationDate = domainModel.expirationDate,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt,
                createdBy = domainModel.createdBy,
                modifiedBy = domainModel.modifiedBy,
                metadata = domainModel.metadata
            )
        }
    }
}