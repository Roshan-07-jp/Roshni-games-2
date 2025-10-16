package com.roshni.games.core.utils.terms

import kotlinx.datetime.LocalDateTime

/**
 * Represents a section within a terms and conditions document
 */
data class TermsSection(
    /**
     * Unique identifier for this section
     */
    val id: String,

    /**
     * Title of this section
     */
    val title: String,

    /**
     * Type/category of this section
     */
    val type: TermsSectionType,

    /**
     * Content of this section
     */
    val content: String,

    /**
     * Order/position of this section within the document
     */
    val order: Int,

    /**
     * Whether this section is required for acceptance
     */
    val isRequired: Boolean = true,

    /**
     * Whether this section requires explicit user acknowledgment
     */
    val requiresAcknowledgment: Boolean = false,

    /**
     * Whether this section can be skipped or is optional
     */
    val isOptional: Boolean = false,

    /**
     * Nested subsections within this section
     */
    val subsections: List<TermsSection> = emptyList(),

    /**
     * When this section was created
     */
    val createdAt: LocalDateTime,

    /**
     * When this section was last modified
     */
    val modifiedAt: LocalDateTime,

    /**
     * Additional metadata for this section
     */
    val metadata: Map<String, Any> = emptyMap()
) {

    /**
     * Get all subsections recursively
     */
    fun getAllSubsections(): List<TermsSection> {
        val allSubsections = mutableListOf<TermsSection>()
        for (subsection in subsections) {
            allSubsections.add(subsection)
            allSubsections.addAll(subsection.getAllSubsections())
        }
        return allSubsections
    }

    /**
     * Check if this section has any subsections
     */
    fun hasSubsections(): Boolean {
        return subsections.isNotEmpty()
    }

    /**
     * Get the total word count of this section including subsections
     */
    fun getTotalWordCount(): Int {
        val thisSectionWords = content.split("\\s+".toRegex()).size
        val subsectionWords = subsections.sumOf { it.getTotalWordCount() }
        return thisSectionWords + subsectionWords
    }
}

/**
 * Types of sections within terms and conditions documents
 */
enum class TermsSectionType {
    /**
     * Introduction and overview section
     */
    INTRODUCTION,

    /**
     * User eligibility and account requirements
     */
    ELIGIBILITY,

    /**
     * License and usage rights
     */
    LICENSE,

    /**
     * User obligations and responsibilities
     */
    USER_OBLIGATIONS,

    /**
     * Platform rights and limitations
     */
    PLATFORM_RIGHTS,

    /**
     * Content ownership and intellectual property
     */
    CONTENT_OWNERSHIP,

    /**
     * Privacy and data collection policies
     */
    PRIVACY,

    /**
     * Payment and subscription terms
     */
    PAYMENT,

    /**
     * User conduct and community guidelines
     */
    USER_CONDUCT,

    /**
     * Prohibited activities and violations
     */
    PROHIBITED_ACTIVITIES,

    /**
     * Account suspension and termination policies
     */
    TERMINATION,

    /**
     * Disclaimers and liability limitations
     */
    DISCLAIMERS,

    /**
     * Indemnification requirements
     */
    INDEMNIFICATION,

    /**
     * Governing law and jurisdiction
     */
    GOVERNING_LAW,

    /**
     * Dispute resolution procedures
     */
    DISPUTE_RESOLUTION,

    /**
     * Contact information and support
     */
    CONTACT,

    /**
     * Custom section type for specific requirements
     */
    CUSTOM
}