package com.roshni.games.core.utils.terms

import kotlinx.datetime.LocalDateTime

/**
 * Represents a user's acceptance of terms and conditions
 */
data class TermsAcceptance(
    /**
     * Unique identifier for this acceptance record
     */
    val id: String,

    /**
     * ID of the user who accepted the terms
     */
    val userId: String,

    /**
     * ID of the terms document that was accepted
     */
    val documentId: String,

    /**
     * Version of the terms document that was accepted
     */
    val documentVersion: Int,

    /**
     * Method by which the user accepted the terms
     */
    val acceptanceMethod: AcceptanceMethod,

    /**
     * When the acceptance occurred
     */
    val acceptedAt: LocalDateTime,

    /**
     * IP address from which the acceptance was recorded
     */
    val ipAddress: String? = null,

    /**
     * User agent string of the client application
     */
    val userAgent: String? = null,

    /**
     * Device identifier for tracking purposes
     */
    val deviceId: String? = null,

    /**
     * Geographic location where acceptance occurred
     */
    val location: String? = null,

    /**
     * Whether the user explicitly scrolled through the document
     */
    val scrolledToBottom: Boolean = false,

    /**
     * Time spent viewing the document before acceptance (in seconds)
     */
    val timeSpentViewing: Long? = null,

    /**
     * Whether this acceptance was given under duress or coercion
     */
    val isVoluntary: Boolean = true,

    /**
     * Whether this acceptance is still valid (not revoked)
     */
    val isValid: Boolean = true,

    /**
     * When this acceptance was revoked (null if still valid)
     */
    val revokedAt: LocalDateTime? = null,

    /**
     * Reason for revocation (null if still valid)
     */
    val revocationReason: String? = null,

    /**
     * ID of the user who revoked this acceptance (null if not revoked)
     */
    val revokedBy: String? = null,

    /**
     * Whether the user was presented with a summary before the full document
     */
    val summaryPresented: Boolean = false,

    /**
     * Whether the user acknowledged understanding of key points
     */
    val keyPointsAcknowledged: Boolean = false,

    /**
     * Language in which the terms were presented
     */
    val presentationLanguage: String = "en",

    /**
     * Whether accessibility features were used during acceptance
     */
    val accessibilityFeaturesUsed: Boolean = false,

    /**
     * Additional context or metadata about the acceptance
     */
    val context: Map<String, Any> = emptyMap(),

    /**
     * Digital signature or hash for verification purposes
     */
    val signature: String? = null,

    /**
     * When this acceptance record was created
     */
    val createdAt: LocalDateTime,

    /**
     * When this acceptance record was last modified
     */
    val modifiedAt: LocalDateTime
) {

    /**
     * Check if this acceptance is currently valid
     */
    fun isCurrentlyValid(): Boolean {
        return isValid && revokedAt == null
    }

    /**
     * Check if this acceptance has expired based on document expiration
     */
    fun isExpired(documentExpirationDate: LocalDateTime?): Boolean {
        if (documentExpirationDate == null) return false
        return acceptedAt > documentExpirationDate
    }

    /**
     * Get the duration this acceptance has been valid (in milliseconds)
     */
    fun getValidityDuration(): Long? {
        if (!isCurrentlyValid()) return null
        val endTime = revokedAt ?: LocalDateTime.now()
        return endTime.toEpochSecond() * 1000 - acceptedAt.toEpochSecond() * 1000
    }

    /**
     * Create a revocation record for this acceptance
     */
    fun revoke(
        reason: String,
        revokedBy: String,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): TermsAcceptance {
        return copy(
            isValid = false,
            revokedAt = currentTime,
            revocationReason = reason,
            this.revokedBy = revokedBy,
            modifiedAt = currentTime
        )
    }

    /**
     * Check if this acceptance meets minimum legal requirements
     */
    fun meetsLegalRequirements(): Boolean {
        return acceptanceMethod != AcceptanceMethod.IMPLIED_CONTINUED_USE ||
               (scrolledToBottom && (timeSpentViewing ?: 0) >= 30) // At least 30 seconds for implied consent
    }
}