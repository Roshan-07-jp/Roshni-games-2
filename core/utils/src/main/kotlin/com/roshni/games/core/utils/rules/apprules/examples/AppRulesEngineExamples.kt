package com.roshni.games.core.utils.rules.apprules.examples

import com.roshni.games.core.utils.rules.RuleContext
import com.roshni.games.core.utils.rules.apprules.*
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Practical examples demonstrating App Rules Engine usage
 * These examples show real-world scenarios and best practices
 */
object AppRulesEngineExamples {

    /**
     * Example 1: E-commerce Purchase Flow with Rule Validation
     */
    suspend fun demonstratePurchaseFlow(
        appRulesEngine: AppRulesEngine,
        userId: String,
        productId: String,
        price: Double
    ) {
        println("=== Purchase Flow Example ===")

        // Create purchase operation
        val operation = BusinessOperation(
            operationType = BusinessOperationType.PURCHASE_INITIATE,
            operationId = UUID.randomUUID().toString(),
            userId = userId,
            data = mapOf(
                "productId" to productId,
                "price" to price,
                "currency" to "USD",
                "paymentMethod" to "credit_card"
            )
        )

        // Create comprehensive validation context
        val context = ValidationContext(
            operation = operation,
            userContext = UserContext(
                userId = userId,
                userProfile = RuleContext.UserProfile(
                    age = 25,
                    subscriptionTier = "premium",
                    isPremium = true,
                    parentalControlsEnabled = false,
                    preferences = mapOf(
                        "preferredCurrency" to "USD",
                        "notificationsEnabled" to true
                    )
                ),
                permissions = listOf("purchase", "premium_features"),
                roles = listOf("user", "premium_subscriber")
            ),
            applicationContext = ApplicationContext(
                appId = "com.roshni.games",
                appVersion = "2.1.0",
                gameId = "premium_game",
                gameState = RuleContext.GameState(
                    currentLevel = 15,
                    score = 50000L,
                    achievements = listOf("first_purchase", "premium_user")
                ),
                featureFlags = mapOf(
                    "premium_features" to true,
                    "advanced_analytics" to true
                )
            ),
            environmentContext = EnvironmentContext(
                deviceInfo = RuleContext.DeviceInfo(
                    platform = "android",
                    osVersion = "13",
                    deviceModel = "Pixel 7",
                    isTablet = false,
                    locale = "en_US",
                    timezone = "America/New_York",
                    networkType = "wifi"
                ),
                networkInfo = EnvironmentContext.NetworkInfo(
                    networkType = "wifi",
                    bandwidth = 100000L,
                    isConnected = true
                ),
                timeInfo = EnvironmentContext.TimeInfo(
                    currentTime = System.currentTimeMillis(),
                    isBusinessHours = true
                )
            )
        )

        // Validate the purchase
        val validationResult = appRulesEngine.validateOperation(operation, context)

        println("Validation Result:")
        println("- Is Valid: ${validationResult.isValid}")
        println("- Total Rules Evaluated: ${validationResult.ruleResults.size}")
        println("- Passed Rules: ${validationResult.getPassedRules().size}")
        println("- Failed Rules: ${validationResult.getFailedRules().size}")

        if (validationResult.hasWarnings()) {
            println("- Warnings: ${validationResult.warnings}")
        }

        if (!validationResult.isValid) {
            println("- Errors: ${validationResult.errors}")
            println("- Primary Failure Reason: ${validationResult.getPrimaryFailureReason()}")
            return
        }

        // If validation passed, enforce the rules
        val enforcementContext = EnforcementContext(
            operation = operation,
            validationResult = validationResult,
            userContext = context.userContext,
            applicationContext = context.applicationContext,
            environmentContext = context.environmentContext,
            enforcementMode = EnforcementMode.STANDARD,
            rollbackOnFailure = true,
            notifyOnFailure = true
        )

        val enforcementResult = appRulesEngine.enforceRules(operation, enforcementContext)

        println("Enforcement Result:")
        println("- Is Successful: ${enforcementResult.isSuccessful}")
        println("- Executed Actions: ${enforcementResult.executedActions.size}")
        println("- Failed Actions: ${enforcementResult.failedActions.size}")

        if (enforcementResult.hasRollbacks()) {
            println("- Rollback Actions: ${enforcementResult.rollbackActions.size}")
        }

        // Process the results
        if (enforcementResult.isSuccessful) {
            println("✅ Purchase flow completed successfully!")
            processSuccessfulPurchase(enforcementResult)
        } else {
            println("❌ Purchase flow failed!")
            handleFailedPurchase(enforcementResult)
        }
    }

    /**
     * Example 2: Game Progression with Achievement Unlocking
     */
    suspend fun demonstrateGameProgression(
        appRulesEngine: AppRulesEngine,
        userId: String,
        gameId: String,
        newScore: Long,
        completedLevel: Int
    ) {
        println("\n=== Game Progression Example ===")

        // Create game end operation
        val operation = BusinessOperation(
            operationType = BusinessOperationType.GAME_END,
            operationId = UUID.randomUUID().toString(),
            userId = userId,
            data = mapOf(
                "gameId" to gameId,
                "score" to newScore,
                "completedLevel" to completedLevel,
                "playTime" to 1800L // 30 minutes
            )
        )

        val context = ValidationContext(
            operation = operation,
            userContext = UserContext(
                userId = userId,
                userProfile = RuleContext.UserProfile(
                    age = 16,
                    subscriptionTier = "basic",
                    isPremium = false,
                    parentalControlsEnabled = false,
                    playTime = RuleContext.UserProfile.PlayTimeInfo(
                        totalPlayTime = 36000L, // 10 hours
                        dailyPlayTime = 7200L,  // 2 hours today
                        weeklyPlayTime = 18000L // 5 hours this week
                    ),
                    achievements = listOf("first_game", "score_1000")
                )
            ),
            applicationContext = ApplicationContext(
                appId = "com.roshni.games",
                appVersion = "2.1.0",
                gameId = gameId,
                gameState = RuleContext.GameState(
                    currentLevel = completedLevel,
                    score = newScore,
                    achievements = listOf("level_master", "high_scorer")
                )
            ),
            environmentContext = EnvironmentContext(
                deviceInfo = RuleContext.DeviceInfo(
                    platform = "android",
                    osVersion = "12",
                    deviceModel = "Samsung Galaxy S22",
                    isTablet = false,
                    locale = "en_US",
                    timezone = "America/Los_Angeles"
                )
            )
        )

        val validationResult = appRulesEngine.validateOperation(operation, context)

        if (validationResult.isValid) {
            val enforcementResult = appRulesEngine.enforceRules(
                operation,
                EnforcementContext(
                    operation = operation,
                    validationResult = validationResult,
                    userContext = context.userContext,
                    applicationContext = context.applicationContext,
                    environmentContext = context.environmentContext
                )
            )

            if (enforcementResult.isSuccessful) {
                println("✅ Game progression completed!")
                println("🎮 New achievements unlocked:")
                enforcementResult.executedActions.forEach { action ->
                    when (action.actionType) {
                        "UnlockContent" -> println("  - Unlocked new content")
                        "ShowMessage" -> println("  - Achievement notification shown")
                        "ModifyProgression" -> println("  - Player progression updated")
                    }
                }
            }
        }
    }

    /**
     * Example 3: Social Feature Access Control
     */
    suspend fun demonstrateSocialFeatures(
        appRulesEngine: AppRulesEngine,
        userId: String,
        friendUserId: String,
        action: String
    ) {
        println("\n=== Social Features Example ===")

        val operationType = when (action) {
            "add_friend" -> BusinessOperationType.FRIEND_ADD
            "send_message" -> BusinessOperationType.MESSAGE_SEND
            "view_profile" -> BusinessOperationType.PROFILE_UPDATE
            else -> BusinessOperationType.CUSTOM
        }

        val operation = BusinessOperation(
            operationType = operationType,
            operationId = UUID.randomUUID().toString(),
            userId = userId,
            data = mapOf(
                "targetUserId" to friendUserId,
                "action" to action,
                "timestamp" to System.currentTimeMillis()
            )
        )

        val context = ValidationContext(
            operation = operation,
            userContext = UserContext(
                userId = userId,
                userProfile = RuleContext.UserProfile(
                    age = 14,
                    subscriptionTier = "basic",
                    isPremium = false,
                    parentalControlsEnabled = true,
                    preferences = mapOf(
                        "privacyLevel" to "friends_only",
                        "allowFriendRequests" to true
                    )
                ),
                permissions = listOf("social_basic"),
                roles = listOf("user")
            ),
            applicationContext = ApplicationContext(
                appId = "com.roshni.games",
                appVersion = "2.1.0",
                gameState = RuleContext.GameState()
            ),
            environmentContext = EnvironmentContext(
                deviceInfo = RuleContext.DeviceInfo(
                    platform = "android",
                    osVersion = "11",
                    deviceModel = "OnePlus 9",
                    isTablet = false,
                    locale = "en_US",
                    timezone = "Europe/London"
                )
            )
        )

        val validationResult = appRulesEngine.validateOperation(operation, context)

        println("Social Action: $action")
        println("- Is Allowed: ${validationResult.isValid}")

        if (!validationResult.isValid) {
            println("- Reason: ${validationResult.getPrimaryFailureReason()}")
            if (validationResult.hasWarnings()) {
                println("- Warnings: ${validationResult.warnings}")
            }
        } else {
            println("✅ Social action permitted")
        }
    }

    /**
     * Example 4: Continuous Validation for Real-time Monitoring
     */
    fun demonstrateContinuousValidation(
        appRulesEngine: AppRulesEngine,
        userId: String
    ) {
        println("\n=== Continuous Validation Example ===")

        // Start continuous validation
        val validationFlow = appRulesEngine.startContinuousValidation(
            contextProvider = {
                // Provide current context for continuous evaluation
                ValidationContext(
                    operation = BusinessOperation(
                        operationType = BusinessOperationType.CUSTOM,
                        operationId = UUID.randomUUID().toString(),
                        userId = userId
                    ),
                    userContext = UserContext(
                        userId = userId,
                        userProfile = RuleContext.UserProfile()
                    ),
                    applicationContext = ApplicationContext(
                        appId = "com.roshni.games",
                        appVersion = "2.1.0",
                        gameState = RuleContext.GameState()
                    ),
                    environmentContext = EnvironmentContext(
                        deviceInfo = RuleContext.DeviceInfo()
                    )
                )
            },
            validationInterval = 3000L // Validate every 3 seconds
        )

        // Observe validation results (in a real app, this would update UI)
        println("🔄 Continuous validation started...")
        println("📊 Monitoring rule compliance in real-time...")

        // Note: In a real application, you would collect this flow
        // and update the UI based on validation results
    }

    /**
     * Example 5: Batch Operations with Rule Validation
     */
    suspend fun demonstrateBatchOperations(
        appRulesEngine: AppRulesEngine,
        userId: String,
        operations: List<Pair<BusinessOperationType, Map<String, Any>>>
    ) {
        println("\n=== Batch Operations Example ===")

        val businessOperations = operations.map { (operationType, data) ->
            BusinessOperation(
                operationType = operationType,
                operationId = UUID.randomUUID().toString(),
                userId = userId,
                data = data
            )
        }

        val context = ValidationContext(
            operation = businessOperations.first(),
            userContext = UserContext(
                userId = userId,
                userProfile = RuleContext.UserProfile()
            ),
            applicationContext = ApplicationContext(
                appId = "com.roshni.games",
                appVersion = "2.1.0",
                gameState = RuleContext.GameState()
            ),
            environmentContext = EnvironmentContext(
                deviceInfo = RuleContext.DeviceInfo()
            )
        )

        // Validate all operations in batch
        val validationResults = listOf<ValidationResult>() // Would use actual batch validation

        println("Batch Validation Results:")
        validationResults.forEachIndexed { index, result ->
            val operationType = operations[index].first
            println("$operationType: ${if (result.isValid) "✅ PASS" else "❌ FAIL"}")
        }

        val successCount = validationResults.count { it.isValid }
        println("Overall Success Rate: $successCount/${validationResults.size}")
    }

    /**
     * Example 6: Custom Business Rule Implementation
     */
    fun createCustomBusinessRule(): GameplayBusinessRule {
        return GameplayBusinessRule(
            id = "weekend_bonus_rule",
            name = "Weekend Bonus Rule",
            description = "Provides bonus rewards for gameplay during weekends",
            priority = 5,
            gameplayConditions = listOf(
                com.roshni.games.core.utils.rules.GameplayCondition.CustomCondition(
                    id = "weekend_condition",
                    evaluator = { context ->
                        val isWeekend = when (java.time.DayOfWeek.of(
                            java.time.LocalDate.now().dayOfWeek.value
                        )) {
                            java.time.DayOfWeek.SATURDAY,
                            java.time.DayOfWeek.SUNDAY -> true
                            else -> false
                        }
                        isWeekend
                    }
                ),
                com.roshni.games.core.utils.rules.GameplayCondition.ScoreCondition(
                    id = "minimum_score_condition",
                    minScore = 5000L
                )
            ),
            gameplayActions = listOf(
                com.roshni.games.core.utils.rules.GameplayAction.ModifyProgression(
                    id = "weekend_bonus_action",
                    statChanges = mapOf(
                        "bonusMultiplier" to 1.5,
                        "weekendBonus" to true
                    ),
                    reason = "Weekend bonus applied"
                ),
                com.roshni.games.core.utils.rules.GameplayAction.ShowMessage(
                    id = "weekend_bonus_message",
                    message = "🎉 Weekend Bonus Active! Earn 50% more points!",
                    messageType = com.roshni.games.core.utils.rules.GameplayAction.ShowMessage.MessageType.INFO
                )
            ),
            tags = listOf("weekend", "bonus", "promotion")
        )
    }

    /**
     * Example 7: Rule Statistics and Monitoring
     */
    suspend fun demonstrateRuleStatistics(appRulesEngine: AppRulesEngine) {
        println("\n=== Rule Statistics Example ===")

        // Get overall statistics
        val overallStats = appRulesEngine.getValidationStatistics()
        println("Overall Statistics:")
        println("- Total Validations: ${overallStats.totalValidations}")
        println("- Success Rate: ${String.format("%.2f", overallStats.successfulValidations.toDouble() / maxOf(overallStats.totalValidations, 1) * 100)}%")
        println("- Average Execution Time: ${String.format("%.2f", overallStats.averageValidationTimeMs)}ms")

        // Get engine status
        val engineStatus = appRulesEngine.getEngineStatus()
        println("\nEngine Status:")
        println("- Is Running: ${engineStatus.isRunning}")
        println("- Registered Rules: ${engineStatus.registeredRuleCount}")
        println("- Active Rules: ${engineStatus.activeRuleCount}")
        println("- Continuous Validation: ${engineStatus.continuousValidationRunning}")
        println("- Uptime: ${engineStatus.uptimeMs / 1000}s")

        // Export configuration for backup
        val config = appRulesEngine.exportConfiguration()
        println("\nConfiguration Export:")
        println("- Exported Rules: ${config["ruleCount"]}")
        println("- Export Timestamp: ${config["exportTime"]}")
    }

    // Helper functions for the examples

    private fun processSuccessfulPurchase(result: EnforcementResult) {
        println("Processing successful purchase...")
        result.executedActions.forEach { action ->
            println("Executed: ${action.actionType} (${action.executionTimeMs}ms)")
        }
    }

    private fun handleFailedPurchase(result: EnforcementResult) {
        println("Handling failed purchase...")
        result.failedActions.forEach { action ->
            println("Failed: ${action.actionType} - ${action.error}")
        }

        if (result.hasRollbacks()) {
            println("Rollback actions were executed")
        }
    }

    private fun getCurrentContext(): ValidationContext {
        return ValidationContext(
            operation = BusinessOperation(
                operationType = BusinessOperationType.CUSTOM,
                operationId = UUID.randomUUID().toString(),
                userId = "current_user"
            ),
            userContext = UserContext(
                userId = "current_user",
                userProfile = RuleContext.UserProfile()
            ),
            applicationContext = ApplicationContext(
                appId = "com.roshni.games",
                appVersion = "2.1.0",
                gameState = RuleContext.GameState()
            ),
            environmentContext = EnvironmentContext(
                deviceInfo = RuleContext.DeviceInfo()
            )
        )
    }
}

/**
 * Usage example runner
 */
suspend fun runAllExamples(appRulesEngine: AppRulesEngine) {
    println("🚀 Running App Rules Engine Examples\n")

    // Run individual examples
    AppRulesEngineExamples.demonstratePurchaseFlow(
        appRulesEngine = appRulesEngine,
        userId = "user_123",
        productId = "premium_skin_pack",
        price = 4.99
    )

    AppRulesEngineExamples.demonstrateGameProgression(
        appRulesEngine = appRulesEngine,
        userId = "user_123",
        gameId = "adventure_game",
        newScore = 15000L,
        completedLevel = 10
    )

    AppRulesEngineExamples.demonstrateSocialFeatures(
        appRulesEngine = appRulesEngine,
        userId = "user_123",
        friendUserId = "friend_456",
        action = "add_friend"
    )

    AppRulesEngineExamples.demonstrateContinuousValidation(
        appRulesEngine = appRulesEngine,
        userId = "user_123"
    )

    AppRulesEngineExamples.demonstrateBatchOperations(
        appRulesEngine = appRulesEngine,
        userId = "user_123",
        operations = listOf(
            BusinessOperationType.GAME_START to mapOf("gameId" to "game1"),
            BusinessOperationType.LEVEL_COMPLETE to mapOf("level" to 5),
            BusinessOperationType.ACHIEVEMENT_UNLOCK to mapOf("achievement" to "level_master")
        )
    )

    AppRulesEngineExamples.demonstrateRuleStatistics(appRulesEngine)

    println("\n✨ All examples completed!")
}

/**
 * Example of how to use the App Rules Engine in a ViewModel
 */
class GameViewModel(private val appRulesEngine: AppRulesEngine) {

    suspend fun onGameCompleted(userId: String, gameId: String, finalScore: Long) {
        val operation = BusinessOperation(
            operationType = BusinessOperationType.GAME_END,
            operationId = UUID.randomUUID().toString(),
            userId = userId,
            data = mapOf(
                "gameId" to gameId,
                "finalScore" to finalScore,
                "completionTime" to System.currentTimeMillis()
            )
        )

        // Create context (in real app, this would use actual user data)
        val context = ValidationContext(
            operation = operation,
            userContext = UserContext(
                userId = userId,
                userProfile = RuleContext.UserProfile()
            ),
            applicationContext = ApplicationContext(
                appId = "com.roshni.games",
                appVersion = "2.1.0",
                gameId = gameId,
                gameState = RuleContext.GameState(score = finalScore)
            ),
            environmentContext = EnvironmentContext(
                deviceInfo = RuleContext.DeviceInfo()
            )
        )

        val result = appRulesEngine.validateOperation(operation, context)

        if (result.isValid) {
            val enforcementResult = appRulesEngine.enforceRules(
                operation,
                EnforcementContext(
                    operation = operation,
                    validationResult = result,
                    userContext = context.userContext,
                    applicationContext = context.applicationContext,
                    environmentContext = context.environmentContext
                )
            )

            // Update UI based on enforcement results
            updateUIWithGameResults(enforcementResult)
        } else {
            // Show error message to user
            showErrorToUser(result.getPrimaryFailureReason())
        }
    }

    private fun updateUIWithGameResults(result: EnforcementResult) {
        // Update UI with achievements, unlocks, etc.
        result.executedActions.forEach { action ->
            when (action.actionType) {
                "ShowMessage" -> {
                    // Show achievement notification
                }
                "UnlockContent" -> {
                    // Update available content
                }
                "ModifyProgression" -> {
                    // Update player stats
                }
            }
        }
    }

    private fun showErrorToUser(reason: String?) {
        // Show user-friendly error message
        println("Game completion failed: ${reason ?: "Unknown error"}")
    }
}