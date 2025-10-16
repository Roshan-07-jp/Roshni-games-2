package com.roshni.games.core.utils.terms

import com.roshni.games.core.database.model.TermsDocumentEntity
import kotlinx.datetime.LocalDateTime

/**
 * Extension functions for Terms and Conditions domain models
 */

/**
 * Convert TermsDocumentEntity to TermsDocument
 */
fun TermsDocumentEntity.toDomainModel(): TermsDocument {
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

/**
 * Convert TermsDocument to TermsDocumentEntity
 */
fun TermsDocument.toEntity(): TermsDocumentEntity {
    return TermsDocumentEntity(
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

/**
 * Convert TermsAcceptanceEntity to TermsAcceptance
 */
fun com.roshni.games.core.database.model.TermsAcceptanceEntity.toDomainModel(): TermsAcceptance {
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

/**
 * Convert TermsAcceptance to TermsAcceptanceEntity
 */
fun TermsAcceptance.toEntity(): com.roshni.games.core.database.model.TermsAcceptanceEntity {
    return com.roshni.games.core.database.model.TermsAcceptanceEntity(
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