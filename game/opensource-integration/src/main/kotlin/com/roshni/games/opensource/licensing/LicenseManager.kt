package com.roshni.games.opensource.licensing

import kotlinx.coroutines.flow.StateFlow
import com.roshni.games.opensource.metadata.GameMetadata
import com.roshni.games.opensource.metadata.LicenseType

/**
 * Manages license verification and attribution for open source games
 */
interface LicenseManager {

    /**
     * Initialize license manager
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Verify game license compatibility
     */
    suspend fun verifyLicense(gameId: String, metadata: GameMetadata): LicenseVerificationResult

    /**
     * Check if license is compatible with framework
     */
    suspend fun isLicenseCompatible(license: com.roshni.games.opensource.metadata.OpenSourceLicense): Boolean

    /**
     * Get attribution text for display
     */
    fun getAttributionText(gameId: String): StateFlow<AttributionDisplay>

    /**
     * Generate license compliance report
     */
    suspend fun generateComplianceReport(gameIds: List<String>): ComplianceReport

    /**
     * Validate source code repository
     */
    suspend fun validateRepository(repositoryUrl: String): RepositoryValidationResult

    /**
     * Check for license updates
     */
    suspend fun checkLicenseUpdates(gameId: String): LicenseUpdateResult

    /**
     * Get license statistics
     */
    fun getLicenseStats(): LicenseStatistics
}

/**
 * License verification result
 */
data class LicenseVerificationResult(
    val isValid: Boolean,
    val isCompatible: Boolean,
    val license: com.roshni.games.opensource.metadata.OpenSourceLicense?,
    val issues: List<LicenseIssue>,
    val warnings: List<String>,
    val recommendations: List<String>,
    val verificationDate: Long
)

/**
 * License issue
 */
data class LicenseIssue(
    val type: LicenseIssueType,
    val severity: LicenseIssueSeverity,
    val description: String,
    val solution: String? = null,
    val affectedFiles: List<String> = emptyList()
)

/**
 * License issue types
 */
enum class LicenseIssueType {
    EXPIRED, INCOMPATIBLE, MISSING_ATTRIBUTION,
    VIOLATION, AMBIGUOUS, UNVERIFIED_SOURCE
}

/**
 * License issue severity
 */
enum class LicenseIssueSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

/**
 * Attribution display information
 */
data class AttributionDisplay(
    val gameId: String,
    val displayText: String,
    val copyrightText: String? = null,
    val licenseText: String,
    val sourceUrl: String,
    val logoUrl: String? = null,
    val shouldShowLogo: Boolean = false,
    val position: AttributionPosition = AttributionPosition.BOTTOM
)

/**
 * Attribution position options
 */
enum class AttributionPosition {
    TOP, BOTTOM, CENTER, OVERLAY, SEPARATE_SCREEN
}

/**
 * Compliance report for multiple games
 */
data class ComplianceReport(
    val totalGames: Int,
    val compliantGames: Int,
    val nonCompliantGames: Int,
    val warningGames: Int,
    val licenseDistribution: Map<LicenseType, Int>,
    val issues: List<ComplianceIssue>,
    val generatedDate: Long,
    val reportVersion: String
)

/**
 * Compliance issue
 */
data class ComplianceIssue(
    val gameId: String,
    val gameName: String,
    val issueType: LicenseIssueType,
    val severity: LicenseIssueSeverity,
    val description: String,
    val remediation: String
)

/**
 * Repository validation result
 */
data class RepositoryValidationResult(
    val isValid: Boolean,
    val repositoryType: com.roshni.games.opensource.metadata.RepositoryType?,
    val licenseFound: Boolean,
    val readmeFound: Boolean,
    val lastCommitDate: Long? = null,
    val stars: Int = 0,
    val forks: Int = 0,
    val issues: List<String> = emptyList()
)

/**
 * License update check result
 */
data class LicenseUpdateResult(
    val hasUpdates: Boolean,
    val currentLicense: com.roshni.games.opensource.metadata.OpenSourceLicense?,
    val newLicense: com.roshni.games.opensource.metadata.OpenSourceLicense?,
    val updateType: LicenseUpdateType?,
    val requiresAction: Boolean,
    val actionDescription: String? = null
)

/**
 * License update types
 */
enum class LicenseUpdateType {
    COMPATIBILITY_CHANGE, VERSION_UPDATE, NEW_LICENSE, EXPIRED
}

/**
 * License statistics
 */
data class LicenseStatistics(
    val totalLicenses: Int,
    val compatibleLicenses: Int,
    val incompatibleLicenses: Int,
    val licenseDistribution: Map<LicenseType, Int>,
    val lastVerification: Long,
    val verificationSuccessRate: Float,
    val pendingVerifications: Int
)

/**
 * License compatibility checker
 */
interface LicenseCompatibilityChecker {

    /**
     * Check if two licenses are compatible
     */
    fun areLicensesCompatible(
        license1: com.roshni.games.opensource.metadata.OpenSourceLicense,
        license2: com.roshni.games.opensource.metadata.OpenSourceLicense
    ): Boolean

    /**
     * Get compatibility issues between licenses
     */
    fun getCompatibilityIssues(
        license1: com.roshni.games.opensource.metadata.OpenSourceLicense,
        license2: com.roshni.games.opensource.metadata.OpenSourceLicense
    ): List<String>

    /**
     * Check if license allows commercial use
     */
    fun allowsCommercialUse(license: com.roshni.games.opensource.metadata.OpenSourceLicense): Boolean

    /**
     * Check if license requires attribution
     */
    fun requiresAttribution(license: com.roshni.games.opensource.metadata.OpenSourceLicense): Boolean

    /**
     * Check if license allows modifications
     */
    fun allowsModifications(license: com.roshni.games.opensource.metadata.OpenSourceLicense): Boolean
}