package com.roshni.games.core.utils.terms.integration

import com.roshni.games.core.database.model.PlayerEntity
import com.roshni.games.core.utils.terms.TermsAndConditionsManager
import com.roshni.games.core.utils.terms.TermsDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Integration manager for connecting terms and conditions with user profiles
 */
interface UserProfileIntegration {

    /**
     * Get user profile with terms compliance status
     */
    fun getUserProfileWithCompliance(userId: String): Flow<UserProfileWithCompliance>

    /**
     * Check if user can access specific features based on terms compliance
     */
    suspend fun canUserAccessFeature(userId: String, featureId: String): Boolean

    /**
     * Get required terms documents for user registration
     */
    suspend fun getRegistrationTermsRequirements(userAge: Int?, userType: String?): List<TermsDocument>

    /**
     * Validate user profile completeness including terms compliance
     */
    suspend fun validateUserProfileCompleteness(userId: String): UserProfileValidationResult

    /**
     * Update user profile with terms compliance information
     */
    suspend fun updateUserTermsCompliance(userId: String): Boolean
}

/**
 * Implementation of UserProfileIntegration
 */
class UserProfileIntegrationImpl(
    private val termsManager: TermsAndConditionsManager,
    private val playerDao: com.roshni.games.core.database.dao.PlayerDao
) : UserProfileIntegration {

    override fun getUserProfileWithCompliance(userId: String): Flow<UserProfileWithCompliance> {
        val userProfileFlow = playerDao.getPlayerById(userId).map { it }

        return combine(
            userProfileFlow,
            termsManager.getPendingTermsDocuments(userId),
            termsManager.getAcceptedTermsDocuments(userId)
        ) { profile, pending, accepted ->
            if (profile == null) {
                null
            } else {
                UserProfileWithCompliance(
                    profile = profile,
                    pendingTermsDocuments = pending,
                    acceptedTermsDocuments = accepted,
                    isCompliant = pending.isEmpty(),
                    lastComplianceCheck = kotlinx.datetime.LocalDateTime.now()
                )
            }
        }
    }

    override suspend fun canUserAccessFeature(userId: String, featureId: String): Boolean {
        return try {
            // Get user profile to check age and type
            val profile = playerDao.getPlayerById(userId) ?: return false

            // Validate terms compliance
            val compliance = termsManager.validateUserCompliance(
                userId = userId,
                userAge = calculateUserAge(profile.lastSeenAt),
                userType = determineUserType(profile)
            )

            if (!compliance.isCompliant) {
                Timber.d("User $userId cannot access feature $featureId - not compliant with terms")
                return false
            }

            // Check feature-specific terms requirements
            val featureTerms = getFeatureTermsRequirements(featureId)
            if (featureTerms.isNotEmpty()) {
                val hasAcceptedAll = featureTerms.all { termsDoc ->
                    termsManager.hasUserAcceptedTerms(userId, termsDoc.id)
                }

                if (!hasAcceptedAll) {
                    Timber.d("User $userId cannot access feature $featureId - missing feature-specific terms acceptance")
                    return false
                }
            }

            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to check feature access for user $userId")
            false
        }
    }

    override suspend fun getRegistrationTermsRequirements(userAge: Int?, userType: String?): List<TermsDocument> {
        return try {
            // Get all active terms that require acceptance
            val allActiveTerms = termsManager.activeTermsDocuments.map { listOf(it) }

            allActiveTerms.map { documents ->
                documents.filter { document ->
                    document.requiresAcceptance &&
                    document.isEffective() &&
                    document.appliesToUser(userAge, userType)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to get registration terms requirements")
            emptyList()
        }
    }

    override suspend fun validateUserProfileCompleteness(userId: String): UserProfileValidationResult {
        return try {
            val profile = playerDao.getPlayerById(userId) ?: return UserProfileValidationResult(
                isComplete = false,
                errors = listOf("User profile not found")
            )

            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Check basic profile completeness
            if (profile.name.isBlank()) {
                errors.add("User name is required")
            }

            if (profile.email.isNullOrBlank()) {
                warnings.add("Email address is recommended")
            }

            // Check terms compliance
            val compliance = termsManager.validateUserCompliance(
                userId = userId,
                userAge = calculateUserAge(profile.lastSeenAt),
                userType = determineUserType(profile)
            )

            if (!compliance.isCompliant) {
                errors.add("User has not accepted all required terms and conditions")
            }

            if (compliance.missingAcceptances.isNotEmpty()) {
                errors.add("Missing acceptance for ${compliance.missingAcceptances.size} terms documents")
            }

            if (compliance.expiredAcceptances.isNotEmpty()) {
                warnings.add("Has ${compliance.expiredAcceptances.size} expired terms acceptance(s)")
            }

            UserProfileValidationResult(
                isComplete = errors.isEmpty(),
                errors = errors,
                warnings = warnings,
                complianceStatus = compliance
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to validate user profile completeness")
            UserProfileValidationResult(
                isComplete = false,
                errors = listOf("Validation failed: ${e.message}")
            )
        }
    }

    override suspend fun updateUserTermsCompliance(userId: String): Boolean {
        return try {
            val compliance = termsManager.validateUserCompliance(userId)

            // Update user profile with compliance status
            val profile = playerDao.getPlayerById(userId) ?: return false

            // Could update profile with compliance metadata
            // For now, just validate and log

            if (!compliance.isCompliant) {
                Timber.w("User $userId has compliance issues - ${compliance.missingAcceptances.size} missing, ${compliance.expiredAcceptances.size} expired")
            } else {
                Timber.d("User $userId is fully compliant with terms")
            }

            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to update user terms compliance")
            false
        }
    }

    /**
     * Calculate user age from profile data
     */
    private fun calculateUserAge(lastSeenAt: kotlinx.datetime.LocalDateTime): Int? {
        // This would typically use birthdate from profile
        // For now, return null as we don't have birthdate in PlayerEntity
        return null
    }

    /**
     * Determine user type from profile
     */
    private fun determineUserType(profile: PlayerEntity): String? {
        // This would typically be determined from profile data
        // For now, return null as we don't have user type in PlayerEntity
        return null
    }

    /**
     * Get feature-specific terms requirements
     */
    private fun getFeatureTermsRequirements(featureId: String): List<TermsDocument> {
        // This would map features to their required terms
        // For now, return empty list
        return emptyList()
    }
}

/**
 * User profile with terms compliance information
 */
data class UserProfileWithCompliance(
    val profile: PlayerEntity,
    val pendingTermsDocuments: List<TermsDocument>,
    val acceptedTermsDocuments: List<TermsDocument>,
    val isCompliant: Boolean,
    val lastComplianceCheck: kotlinx.datetime.LocalDateTime
)

/**
 * Result of user profile validation
 */
data class UserProfileValidationResult(
    val isComplete: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val complianceStatus: com.roshni.games.core.utils.terms.TermsComplianceResult? = null
)