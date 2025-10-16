package com.roshni.games.core.utils.rules

/**
 * Interface for gameplay-related rules that control game mechanics,
 * progression, and player experience.
 */
interface GameplayRule : Rule {

    /**
     * The gameplay conditions that must be met for this rule to pass
     */
    val conditions: List<GameplayCondition>

    /**
     * The actions to execute when this rule passes
     */
    val actions: List<GameplayAction>

    /**
     * Whether this rule should be evaluated continuously or only once
     */
    val continuousEvaluation: Boolean

    /**
     * The evaluation interval for continuous rules (in milliseconds)
     */
    val evaluationInterval: Long

    /**
     * Check if this rule should trigger based on the current game state
     *
     * @param context The current rule context
     * @return true if the rule should trigger, false otherwise
     */
    suspend fun shouldTrigger(context: RuleContext): Boolean

    /**
     * Get the priority of this rule for execution ordering
     */
    fun getPriority(): Int

    /**
     * Check if this rule is currently active and should be evaluated
     */
    suspend fun isActive(context: RuleContext): Boolean
}

/**
 * Interface for permission-based rules that control access to features
 * and content based on user permissions and restrictions.
 */
interface PermissionRule : Rule {

    /**
     * The permission level required for this rule to pass
     */
    val requiredPermission: String

    /**
     * Alternative permissions that can satisfy this rule
     */
    val alternativePermissions: List<String>

    /**
     * Whether to show a permission request dialog if access is denied
     */
    val showPermissionRequest: Boolean

    /**
     * Custom message to show when permission is denied
     */
    val denialMessage: String?

    /**
     * Check if the user has the required permissions
     */
    suspend fun hasPermission(context: RuleContext): Boolean

    /**
     * Get the permission rationale to show to the user
     */
    suspend fun getPermissionRationale(context: RuleContext): String
}

/**
 * Interface for feature gate rules that control the availability
 * of specific features based on various criteria.
 */
interface FeatureGateRule : Rule {

    /**
     * The feature identifier this rule controls
     */
    val featureId: String

    /**
     * The gating strategy to use (all conditions must pass, any condition must pass, etc.)
     */
    val gatingStrategy: GatingStrategy

    /**
     * Feature-specific conditions for this gate
     */
    val featureConditions: List<GameplayCondition>

    /**
     * Rollout percentage (0.0 to 1.0) for gradual feature releases
     */
    val rolloutPercentage: Float

    /**
     * Whether this feature gate is currently enabled
     */
    val enabled: Boolean

    /**
     * Check if the feature should be available for the given user/context
     */
    suspend fun isFeatureEnabled(context: RuleContext): Boolean

    /**
     * Get the reason why a feature is disabled (for debugging/logging)
     */
    suspend fun getDisabledReason(context: RuleContext): String?

    enum class GatingStrategy {
        /**
         * All conditions must pass for the feature to be enabled
         */
        ALL_CONDITIONS,

        /**
         * Any condition must pass for the feature to be enabled
         */
        ANY_CONDITION,

        /**
         * Conditions are evaluated in order, first one that passes enables the feature
         */
        FIRST_MATCH,

        /**
         * Custom evaluation logic defined by the rule implementation
         */
        CUSTOM
    }
}

/**
 * Interface for content restriction rules that control access to
 * content based on age ratings, parental controls, and other restrictions.
 */
interface ContentRestrictionRule : Rule {

    /**
     * The content identifier this rule applies to
     */
    val contentId: String

    /**
     * The age rating required to access this content
     */
    val ageRating: AgeRating

    /**
     * Content categories this rule applies to
     */
    val contentCategories: List<String>

    /**
     * Whether parental consent is required for this content
     */
    val requiresParentalConsent: Boolean

    /**
     * Alternative content to show if access is restricted
     */
    val alternativeContentId: String?

    /**
     * Check if the content is appropriate for the current user
     */
    suspend fun isContentAppropriate(context: RuleContext): Boolean

    /**
     * Get the content restriction reason for logging/auditing
     */
    suspend fun getRestrictionReason(context: RuleContext): String

    enum class AgeRating {
        EVERYONE,
        EVERYONE_10_PLUS,
        TEEN,
        MATURE_17_PLUS,
        ADULTS_ONLY
    }
}

/**
 * Interface for parental control rules that enforce restrictions
 * set by parents or guardians.
 */
interface ParentalControlRule : Rule {

    /**
     * The type of parental control this rule enforces
     */
    val controlType: ControlType

    /**
     * The severity level of this control
     */
    val severity: Severity

    /**
     * Time-based restrictions for this control
     */
    val timeRestrictions: TimeRestrictions?

    /**
     * Content-based restrictions for this control
     */
    val contentRestrictions: ContentRestrictions?

    /**
     * Whether this control can be overridden by parental PIN/code
     */
    val allowOverride: Boolean

    /**
     * Check if the parental control restriction is currently active
     */
    suspend fun isRestrictionActive(context: RuleContext): Boolean

    /**
     * Get the parental control violation reason
     */
    suspend fun getViolationReason(context: RuleContext): String

    enum class ControlType {
        PLAY_TIME_LIMIT,
        CONTENT_ACCESS,
        FEATURE_ACCESS,
        SOCIAL_FEATURES,
        PURCHASES,
        COMMUNICATION
    }

    enum class Severity {
        LOW,    // Warning only
        MEDIUM, // Partial restriction
        HIGH,   // Full restriction
        CRITICAL // Complete blocking
    }

    data class TimeRestrictions(
        val dailyLimitMinutes: Int? = null,
        val weeklyLimitMinutes: Int? = null,
        val allowedHours: List<IntRange>? = null, // Hours of day (0-23)
        val allowedDays: List<Int>? = null, // Days of week (1-7, 1=Sunday)
        val bedtimeEnforcement: Boolean = false,
        val bedtimeHour: Int? = null // Hour (0-23) when access should be restricted
    )

    data class ContentRestrictions(
        val blockedCategories: List<String> = emptyList(),
        val allowedCategories: List<String>? = null, // null = allow all except blocked
        val maxAgeRating: ContentRestrictionRule.AgeRating = ContentRestrictionRule.AgeRating.ADULTS_ONLY,
        val requireApprovalForPurchases: Boolean = false,
        val blockSocialFeatures: Boolean = false,
        val blockCommunication: Boolean = false
    )
}

/**
 * Base interface for all rule types in the system
 */
interface Rule {

    /**
     * Unique identifier for this rule
     */
    val id: String

    /**
     * Human-readable name for this rule
     */
    val name: String

    /**
     * Detailed description of what this rule does
     */
    val description: String

    /**
     * The category/type of this rule
     */
    val category: String

    /**
     * Whether this rule is currently enabled
     */
    val enabled: Boolean

    /**
     * Tags for organizing and filtering rules
     */
    val tags: List<String>

    /**
     * Creation timestamp
     */
    val createdAt: Long

    /**
     * Last modification timestamp
     */
    val modifiedAt: Long

    /**
     * Version number for tracking rule changes
     */
    val version: Int

    /**
     * Evaluate this rule against the provided context
     *
     * @param context The context to evaluate the rule against
     * @return The result of the rule evaluation
     */
    suspend fun evaluate(context: RuleContext): RuleResult

    /**
     * Validate that this rule is properly configured
     *
     * @return true if the rule is valid, false otherwise
     */
    suspend fun validate(): Boolean

    /**
     * Get metadata about this rule for debugging and monitoring
     */
    suspend fun getMetadata(): Map<String, Any>
}