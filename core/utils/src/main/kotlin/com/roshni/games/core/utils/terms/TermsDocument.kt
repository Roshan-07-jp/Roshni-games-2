package com.roshni.games.core.utils.terms

import kotlinx.datetime.LocalDateTime

/**
 * Represents a terms and conditions document with its content and metadata
 */
data class TermsDocument(
    /**
     * Unique identifier for this document
     */
    val id: String,

    /**
     * Type of terms document
     */
    val type: TermsType,

    /**
     * Version number for tracking document changes
     */
    val version: Int,

    /**
     * Human-readable title of the document
     */
    val title: String,

    /**
     * Detailed description of the document
     */
    val description: String,

    /**
     * Full content of the terms document
     */
    val content: String,

    /**
     * Document sections for better organization
     */
    val sections: List<TermsSection> = emptyList(),

    /**
     * Language code for localized documents (e.g., "en", "es", "fr")
     */
    val language: String = "en",

    /**
     * Country/region code for region-specific terms (e.g., "US", "EU", "CA")
     */
    val region: String? = null,

    /**
     * Whether this document is currently active and enforceable
     */
    val isActive: Boolean = true,

    /**
     * Whether user acceptance is required for this document
     */
    val requiresAcceptance: Boolean = true,

    /**
     * Whether this document applies to all users or specific user types
     */
    val appliesToAllUsers: Boolean = true,

    /**
     * Minimum age requirement for this document (null if no age restriction)
     */
    val minimumAge: Int? = null,

    /**
     * User types this document applies to (empty if applies to all)
     */
    val applicableUserTypes: List<String> = emptyList(),

    /**
     * When this document becomes effective
     */
    val effectiveDate: LocalDateTime,

    /**
     * When this document expires (null if no expiration)
     */
    val expirationDate: LocalDateTime? = null,

    /**
     * When this document was created
     */
    val createdAt: LocalDateTime,

    /**
     * When this document was last modified
     */
    val modifiedAt: LocalDateTime,

    /**
     * ID of the user/admin who created this document
     */
    val createdBy: String,

    /**
     * ID of the user/admin who last modified this document
     */
    val modifiedBy: String,

    /**
     * Additional metadata for the document
     */
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Check if this document is currently effective
     */
    fun isEffective(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return isActive &&
               currentTime >= effectiveDate &&
               (expirationDate == null || currentTime <= expirationDate)
    }

    /**
     * Check if this document applies to a specific user
     */
    fun appliesToUser(userAge: Int?, userType: String?): Boolean {
        if (!appliesToAllUsers && userType != null &&
            userType !in applicableUserTypes) {
            return false
        }

        if (minimumAge != null && userAge != null && userAge < minimumAge) {
            return false
        }

        return true
    }

    /**
     * Get a specific section by ID
     */
    fun getSection(sectionId: String): TermsSection? {
        return sections.find { it.id == sectionId }
    }

    /**
     * Get all sections of a specific type
     */
    fun getSectionsByType(sectionType: TermsSectionType): List<TermsSection> {
        return sections.filter { it.type == sectionType }
    }
}