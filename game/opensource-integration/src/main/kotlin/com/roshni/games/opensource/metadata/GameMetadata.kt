package com.roshni.games.opensource.metadata

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Comprehensive metadata for open source games
 */
@Serializable
data class GameMetadata(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val longDescription: String? = null,
    val version: String,
    val category: GameCategory,
    val subcategory: String? = null,
    val difficulty: GameDifficulty,
    val ageRating: AgeRating,
    val license: OpenSourceLicense,
    val sourceCode: SourceCodeInfo,
    val author: AuthorInfo,
    val contributors: List<AuthorInfo> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val iconUrl: String? = null,
    val tags: List<String> = emptyList(),
    val features: List<GameFeature> = emptyList(),
    val requirements: GameRequirements,
    val statistics: GameStatistics,
    val attribution: AttributionInfo,
    val lastUpdated: LocalDateTime,
    val createdDate: LocalDateTime,
    val isVerified: Boolean = false,
    val verificationDate: LocalDateTime? = null
)

/**
 * Game difficulty levels
 */
enum class GameDifficulty {
    VERY_EASY, EASY, MEDIUM, HARD, VERY_HARD, EXPERT
}

/**
 * Age ratings (equivalent to ESRB/PEGI)
 */
enum class AgeRating {
    EARLY_CHILDHOOD, // 3+
    EVERYONE,       // 6+
    EVERYONE_10,    // 10+
    TEEN,          // 13+
    MATURE,        // 17+
    ADULTS_ONLY    // 18+
}

/**
 * Open source license information
 */
@Serializable
data class OpenSourceLicense(
    val type: LicenseType,
    val name: String,
    val url: String,
    val isCompatible: Boolean,
    val requiresAttribution: Boolean,
    val allowsCommercial: Boolean,
    val allowsModification: Boolean,
    val shareAlike: Boolean
)

/**
 * License types
 */
enum class LicenseType {
    MIT, GPL_V2, GPL_V3, APACHE_2, BSD_2, BSD_3,
    ISC, LGPL_V2, LGPL_V3, MPL_2, EPL_1, CDDL_1,
    WTFPL, UNLICENSE, CC_BY, CC_BY_SA, CC0, ZLIB
}

/**
 * Source code repository information
 */
@Serializable
data class SourceCodeInfo(
    val repositoryUrl: String,
    val repositoryType: RepositoryType,
    val branch: String = "main",
    val commitHash: String? = null,
    val lastCommitDate: LocalDateTime? = null,
    val stars: Int = 0,
    val forks: Int = 0,
    val issues: Int = 0,
    val licenseFile: String? = null,
    val readmeUrl: String? = null
)

/**
 * Repository types
 */
enum class RepositoryType {
    GITHUB, GITLAB, BITBUCKET, SOURCEFORGE, OTHER
}

/**
 * Author information
 */
@Serializable
data class AuthorInfo(
    val name: String,
    val email: String? = null,
    val website: String? = null,
    val github: String? = null,
    val role: AuthorRole = AuthorRole.DEVELOPER
)

/**
 * Author roles
 */
enum class AuthorRole {
    DEVELOPER, DESIGNER, ARTIST, MUSICIAN, TRANSLATOR, MAINTAINER
}

/**
 * Game features
 */
enum class GameFeature {
    SINGLE_PLAYER, MULTIPLAYER, SAVE_LOAD, HIGH_SCORE,
    ACHIEVEMENTS, LEADERBOARD, SETTINGS, TUTORIAL,
    SOUND_EFFECTS, MUSIC, FULLSCREEN, WINDOWED
}

/**
 * Game requirements
 */
@Serializable
data class GameRequirements(
    val minAndroidVersion: Int,
    val recommendedAndroidVersion: Int,
    val minRamMb: Int = 64,
    val recommendedRamMb: Int = 128,
    val minStorageMb: Long = 10,
    val recommendedStorageMb: Long = 50,
    val requiresInternet: Boolean = false,
    val supportsOffline: Boolean = true,
    val permissions: List<String> = emptyList()
)

/**
 * Game statistics
 */
@Serializable
data class GameStatistics(
    val downloadCount: Long = 0,
    val rating: Float = 0.0f,
    val ratingCount: Int = 0,
    val playCount: Long = 0,
    val averagePlayTime: Long = 0, // minutes
    val completionRate: Float = 0.0f,
    val lastPlayed: LocalDateTime? = null
)

/**
 * Attribution information for proper credit display
 */
@Serializable
data class AttributionInfo(
    val displayText: String,
    val copyrightText: String? = null,
    val licenseText: String,
    val sourceUrl: String,
    val attributionRequired: Boolean = true,
    val modificationAllowed: Boolean = true,
    val logoRequired: Boolean = false,
    val logoUrl: String? = null
)

/**
 * Game category enumeration
 */
enum class GameCategory {
    PUZZLE, CARD, ARCADE, STRATEGY, TRIVIA,
    ACTION, BOARD, CASUAL, WORD, MATH,
    MEMORY, LOGIC, ADVENTURE, SIMULATION,
    RACING, SPORTS, MUSIC, EDUCATIONAL
}