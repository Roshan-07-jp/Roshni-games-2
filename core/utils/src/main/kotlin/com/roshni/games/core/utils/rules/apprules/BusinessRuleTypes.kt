package com.roshni.games.core.utils.rules.apprules

import com.roshni.games.core.utils.rules.GameplayAction
import com.roshni.games.core.utils.rules.GameplayCondition
import com.roshni.games.core.utils.rules.RuleContext

/**
 * Gameplay business rule implementation
 * Handles rules related to game mechanics, progression, and player experience
 */
data class GameplayBusinessRule(
    override val id: String,
    override val name: String,
    override val description: String,
    override val category: RuleCategory = RuleCategory.GAMEPLAY,
    override val ruleType: BusinessRuleType = BusinessRuleType.VALIDATION,
    override val enabled: Boolean = true,
    override val priority: Int = 1,
    override val tags: List<String> = emptyList(),
    override val createdAt: Long = System.currentTimeMillis(),
    override val modifiedAt: Long = System.currentTimeMillis(),
    override val version: Int = 1,

    // Gameplay-specific properties
    val gameplayConditions: List<GameplayCondition> = emptyList(),
    val gameplayActions: List<GameplayAction> = emptyList(),
    val applicableOperations: List<BusinessOperationType> = listOf(
        BusinessOperationType.GAME_START,
        BusinessOperationType.GAME_END,
        BusinessOperationType.LEVEL_COMPLETE,
        BusinessOperationType.ACHIEVEMENT_UNLOCK,
        BusinessOperationType.PROGRESS_SAVE,
        BusinessOperationType.GAME_STATE_CHANGE
    ),
    val gameModes: List<String> = emptyList(), // Specific game modes this rule applies to
    val difficultyLevels: List<String> = emptyList(), // Difficulty levels this rule applies to
    val continuousEvaluation: Boolean = false,
    val evaluationInterval: Long = 1000L
) : BusinessRule {

    override suspend fun evaluate(context: RuleContext): BusinessRuleResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Evaluate gameplay conditions
            val conditionResults = gameplayConditions.map { condition ->
                condition.evaluate(context) to condition.getConfidence(context)
            }

            // Check if all conditions are met
            val allConditionsMet = conditionResults.all { it.first }
            val averageConfidence = conditionResults.map { it.second }.average().toFloat()

            if (!allConditionsMet) {
                return BusinessRuleResult.failure(
                    ruleId = id,
                    ruleType = ruleType,
                    reason = "Gameplay conditions not met",
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    errors = listOf("One or more gameplay conditions failed")
                )
            }

            // Generate actions based on successful evaluation
            val actions = gameplayActions.map { gameplayAction ->
                BusinessRuleAction.CustomAction(
                    actionId = "${id}_${gameplayAction.id}",
                    executor = { execContext ->
                        // Convert GameplayAction to work with ExecutionContext
                        true
                    },
                    priority = gameplayAction.priority,
                    immediate = gameplayAction.immediate
                )
            }

            BusinessRuleResult.success(
                ruleId = id,
                ruleType = ruleType,
                reason = "Gameplay rule evaluation passed",
                actions = actions,
                metadata = mapOf(
                    "gameModes" to gameModes,
                    "difficultyLevels" to difficultyLevels,
                    "conditionCount" to gameplayConditions.size,
                    "actionCount" to gameplayActions.size
                ),
                executionTimeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            BusinessRuleResult.failure(
                ruleId = id,
                ruleType = ruleType,
                reason = "Exception during gameplay rule evaluation: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }

    override suspend fun validate(): Boolean {
        return try {
            // Validate rule configuration
            id.isNotBlank() &&
            name.isNotBlank() &&
            description.isNotBlank() &&
            gameplayConditions.isNotEmpty() &&
            priority >= 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getMetadata(): Map<String, Any> {
        return mapOf(
            "gameplayConditions" to gameplayConditions.map { it.id },
            "gameplayActions" to gameplayActions.map { it.id },
            "applicableOperations" to applicableOperations.map { it.name },
            "gameModes" to gameModes,
            "difficultyLevels" to difficultyLevels,
            "continuousEvaluation" to continuousEvaluation,
            "evaluationInterval" to evaluationInterval
        )
    }

    override suspend fun appliesTo(operation: BusinessOperation): Boolean {
        return operation.operationType in applicableOperations
    }

    override fun getExecutionOrder(): Int {
        return priority * 100 + version
    }
}

/**
 * Monetization business rule implementation
 * Handles rules related to in-app purchases, subscriptions, and revenue generation
 */
data class MonetizationBusinessRule(
    override val id: String,
    override val name: String,
    override val description: String,
    override val category: RuleCategory = RuleCategory.MONETIZATION,
    override val ruleType: BusinessRuleType = BusinessRuleType.ENFORCEMENT,
    override val enabled: Boolean = true,
    override val priority: Int = 2,
    override val tags: List<String> = emptyList(),
    override val createdAt: Long = System.currentTimeMillis(),
    override val modifiedAt: Long = System.currentTimeMillis(),
    override val version: Int = 1,

    // Monetization-specific properties
    val purchaseConditions: List<MonetizationCondition> = emptyList(),
    val pricingRules: List<PricingRule> = emptyList(),
    val revenueTargets: RevenueTarget? = null,
    val applicableOperations: List<BusinessOperationType> = listOf(
        BusinessOperationType.PURCHASE_INITIATE,
        BusinessOperationType.PURCHASE_COMPLETE,
        BusinessOperationType.SUBSCRIPTION_CHANGE,
        BusinessOperationType.REWARD_CLAIM,
        BusinessOperationType.CURRENCY_SPEND,
        BusinessOperationType.AD_VIEW
    ),
    val supportedCurrencies: List<String> = listOf("USD", "EUR", "GBP"),
    val regionalPricing: Map<String, Double> = emptyMap(), // Region code to multiplier
    val promotionalRules: List<PromotionalRule> = emptyList()
) : BusinessRule {

    override suspend fun evaluate(context: RuleContext): BusinessRuleResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Evaluate monetization conditions
            val conditionResults = purchaseConditions.map { condition ->
                condition.evaluate(context)
            }

            val allConditionsMet = conditionResults.all { it }

            if (!allConditionsMet) {
                return BusinessRuleResult.failure(
                    ruleId = id,
                    ruleType = ruleType,
                    reason = "Monetization conditions not met",
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    errors = listOf("Purchase conditions validation failed")
                )
            }

            // Generate monetization-specific actions
            val actions = mutableListOf<BusinessRuleAction>()

            // Add pricing validation actions
            pricingRules.forEach { pricingRule ->
                actions.add(
                    BusinessRuleAction.DataModification(
                        actionId = "${id}_pricing_${pricingRule.id}",
                        modifications = mapOf(
                            "priceValidation" to true,
                            "pricingRule" to pricingRule.id
                        ),
                        reason = "Apply pricing rules",
                        priority = 2
                    )
                )
            }

            // Add promotional actions
            promotionalRules.forEach { promoRule ->
                if (promoRule.isActive) {
                    actions.add(
                        BusinessRuleAction.DataModification(
                            actionId = "${id}_promo_${promoRule.id}",
                            modifications = mapOf(
                                "promotionApplied" to true,
                                "discount" to promoRule.discountPercentage
                            ),
                            reason = "Apply promotional discount",
                            priority = 1
                        )
                    )
                }
            }

            BusinessRuleResult.success(
                ruleId = id,
                ruleType = ruleType,
                reason = "Monetization rule evaluation passed",
                actions = actions,
                metadata = mapOf(
                    "supportedCurrencies" to supportedCurrencies,
                    "regionalPricing" to regionalPricing,
                    "activePromotions" to promotionalRules.count { it.isActive },
                    "pricingRules" to pricingRules.size
                ),
                executionTimeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            BusinessRuleResult.failure(
                ruleId = id,
                ruleType = ruleType,
                reason = "Exception during monetization rule evaluation: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }

    override suspend fun validate(): Boolean {
        return try {
            id.isNotBlank() &&
            name.isNotBlank() &&
            description.isNotBlank() &&
            priority >= 0 &&
            supportedCurrencies.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getMetadata(): Map<String, Any> {
        return mapOf(
            "purchaseConditions" to purchaseConditions.map { it.id },
            "pricingRules" to pricingRules.map { it.id },
            "revenueTargets" to revenueTargets?.toString(),
            "applicableOperations" to applicableOperations.map { it.name },
            "supportedCurrencies" to supportedCurrencies,
            "regionalPricing" to regionalPricing,
            "promotionalRules" to promotionalRules.map { it.id }
        )
    }

    override suspend fun appliesTo(operation: BusinessOperation): Boolean {
        return operation.operationType in applicableOperations
    }

    override fun getExecutionOrder(): Int {
        return priority * 100 + version
    }
}

/**
 * Social business rule implementation
 * Handles rules related to social features, interactions, and community guidelines
 */
data class SocialBusinessRule(
    override val id: String,
    override val name: String,
    override val description: String,
    override val category: RuleCategory = RuleCategory.SOCIAL,
    override val ruleType: BusinessRuleType = BusinessRuleType.AUTHORIZATION,
    override val enabled: Boolean = true,
    override val priority: Int = 3,
    override val tags: List<String> = emptyList(),
    override val createdAt: Long = System.currentTimeMillis(),
    override val modifiedAt: Long = System.currentTimeMillis(),
    override val version: Int = 1,

    // Social-specific properties
    val socialConditions: List<SocialCondition> = emptyList(),
    val interactionRules: List<InteractionRule> = emptyList(),
    val communityGuidelines: List<CommunityGuideline> = emptyList(),
    val applicableOperations: List<BusinessOperationType> = listOf(
        BusinessOperationType.FRIEND_ADD,
        BusinessOperationType.MESSAGE_SEND,
        BusinessOperationType.LEADERBOARD_UPDATE,
        BusinessOperationType.SOCIAL_SHARE,
        BusinessOperationType.PROFILE_UPDATE,
        BusinessOperationType.GROUP_JOIN
    ),
    val ageRestrictions: AgeRestriction? = null,
    val contentModeration: ContentModerationSettings = ContentModerationSettings(),
    val privacySettings: PrivacySettings = PrivacySettings()
) : BusinessRule {

    override suspend fun evaluate(context: RuleContext): BusinessRuleResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Evaluate social conditions
            val conditionResults = socialConditions.map { condition ->
                condition.evaluate(context)
            }

            val allConditionsMet = conditionResults.all { it }

            if (!allConditionsMet) {
                return BusinessRuleResult.failure(
                    ruleId = id,
                    ruleType = ruleType,
                    reason = "Social conditions not met",
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    errors = listOf("Social interaction conditions failed")
                )
            }

            // Check community guidelines compliance
            val guidelineViolations = communityGuidelines.filter { guideline ->
                !guideline.isCompliant(context)
            }

            if (guidelineViolations.isNotEmpty()) {
                return BusinessRuleResult.failure(
                    ruleId = id,
                    ruleType = ruleType,
                    reason = "Community guidelines violation",
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    errors = guidelineViolations.map { it.description }
                )
            }

            // Generate social interaction actions
            val actions = mutableListOf<BusinessRuleAction>()

            // Add interaction control actions
            interactionRules.forEach { interactionRule ->
                actions.add(
                    BusinessRuleAction.OperationControl(
                        actionId = "${id}_interaction_${interactionRule.id}",
                        operationType = BusinessOperationType.MESSAGE_SEND,
                        allowOperation = interactionRule.allowed,
                        reason = interactionRule.reason,
                        priority = 1
                    )
                )
            }

            // Add content moderation actions if needed
            if (contentModeration.enabled) {
                actions.add(
                    BusinessRuleAction.AuditLog(
                        actionId = "${id}_moderation",
                        eventType = "content_moderation",
                        eventData = mapOf(
                            "ruleId" to id,
                            "contentModerationEnabled" to true
                        ),
                        priority = 0
                    )
                )
            }

            BusinessRuleResult.success(
                ruleId = id,
                ruleType = ruleType,
                reason = "Social rule evaluation passed",
                actions = actions,
                metadata = mapOf(
                    "interactionRules" to interactionRules.size,
                    "communityGuidelines" to communityGuidelines.size,
                    "ageRestrictions" to ageRestrictions?.toString(),
                    "contentModerationEnabled" to contentModeration.enabled
                ),
                executionTimeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            BusinessRuleResult.failure(
                ruleId = id,
                ruleType = ruleType,
                reason = "Exception during social rule evaluation: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }

    override suspend fun validate(): Boolean {
        return try {
            id.isNotBlank() &&
            name.isNotBlank() &&
            description.isNotBlank() &&
            priority >= 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getMetadata(): Map<String, Any> {
        return mapOf(
            "socialConditions" to socialConditions.map { it.id },
            "interactionRules" to interactionRules.map { it.id },
            "communityGuidelines" to communityGuidelines.map { it.id },
            "applicableOperations" to applicableOperations.map { it.name },
            "ageRestrictions" to ageRestrictions?.toString(),
            "contentModeration" to contentModeration.enabled,
            "privacySettings" to privacySettings.toString()
        )
    }

    override suspend fun appliesTo(operation: BusinessOperation): Boolean {
        return operation.operationType in applicableOperations
    }

    override fun getExecutionOrder(): Int {
        return priority * 100 + version
    }
}

// Supporting data classes for business rule types

data class MonetizationCondition(
    val id: String,
    val conditionType: String,
    val evaluator: suspend (RuleContext) -> Boolean
) {
    suspend fun evaluate(context: RuleContext): Boolean {
        return try {
            evaluator(context)
        } catch (e: Exception) {
            false
        }
    }
}

data class PricingRule(
    val id: String,
    val productId: String,
    val basePrice: Double,
    val currency: String,
    val discountRules: List<DiscountRule> = emptyList()
)

data class DiscountRule(
    val id: String,
    val discountPercentage: Double,
    val conditions: List<String>
)

data class RevenueTarget(
    val targetAmount: Double,
    val currency: String,
    val timePeriod: String,
    val currentAmount: Double = 0.0
)

data class PromotionalRule(
    val id: String,
    val name: String,
    val discountPercentage: Double,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean
) {
    val isCurrentlyActive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return isActive && now in startDate..endDate
        }
}

data class SocialCondition(
    val id: String,
    val conditionType: String,
    val evaluator: suspend (RuleContext) -> Boolean
) {
    suspend fun evaluate(context: RuleContext): Boolean {
        return try {
            evaluator(context)
        } catch (e: Exception) {
            false
        }
    }
}

data class InteractionRule(
    val id: String,
    val interactionType: String,
    val allowed: Boolean,
    val reason: String,
    val restrictions: List<String> = emptyList()
)

data class CommunityGuideline(
    val id: String,
    val name: String,
    val description: String,
    val severity: GuidelineSeverity,
    val checker: suspend (RuleContext) -> Boolean
) {
    suspend fun isCompliant(context: RuleContext): Boolean {
        return try {
            checker(context)
        } catch (e: Exception) {
            false
        }
    }

    enum class GuidelineSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}

data class AgeRestriction(
    val minAge: Int,
    val maxAge: Int? = null,
    val requiresVerification: Boolean = false
)

data class ContentModerationSettings(
    val enabled: Boolean = true,
    val autoModeration: Boolean = false,
    val manualReviewThreshold: Double = 0.8,
    val blockedWords: List<String> = emptyList(),
    val allowedContentTypes: List<String> = listOf("text", "image", "video")
)

data class PrivacySettings(
    val allowFriendRequests: Boolean = true,
    val allowMessages: Boolean = true,
    val allowProfileViewing: Boolean = true,
    val allowDataSharing: Boolean = false,
    val requireConsent: Boolean = true
)