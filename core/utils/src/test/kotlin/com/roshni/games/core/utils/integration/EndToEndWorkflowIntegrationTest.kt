package com.roshni.games.core.utils.integration

import com.roshni.games.core.utils.integration.IntegrationTestDataFactory
import com.roshni.games.core.utils.integration.IntegrationTestUtils
import com.roshni.games.core.utils.integration.IntegrationType
import kotlinx.coroutines.test.runTest
import org.junit.Test
import timber.log.Timber
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end workflow integration tests across multiple systems
 */
class EndToEndWorkflowIntegrationTest : SystemIntegrationTest() {

    @Test
    fun `test complete user onboarding workflow across all systems`() = runTest {
        Timber.d("Testing complete user onboarding workflow across all systems")

        // Register components for onboarding workflow
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent(
                id = "user_registration_component",
                name = "User Registration Component",
                type = ComponentType.DATA_PROCESSOR
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "profile_setup_component",
                name = "Profile Setup Component",
                type = ComponentType.STATE_MANAGER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "preferences_component",
                name = "Preferences Component",
                type = ComponentType.STATE_MANAGER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "notification_component",
                name = "Notification Component",
                type = ComponentType.EVENT_HANDLER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "analytics_component",
                name = "Analytics Component",
                type = ComponentType.DATA_PROCESSOR
            )
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create integrations for onboarding workflow
        val integrations = listOf(
            // User registration -> Profile setup
            Triple("user_registration_component", "profile_setup_component", "user_to_profile_integration"),
            // Profile setup -> Preferences
            Triple("profile_setup_component", "preferences_component", "profile_to_preferences_integration"),
            // Preferences -> Notifications
            Triple("preferences_component", "notification_component", "preferences_to_notifications_integration"),
            // All components -> Analytics
            *components.flatMap { source ->
                listOf(Triple(source.id, "analytics_component", "analytics_integration_${source.id}"))
            }.toTypedArray()
        )

        integrations.forEach { (source, target, integrationId) ->
            val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
                type = IntegrationType.EVENT_DRIVEN
            )

            val result = systemIntegrationHub.createIntegration(
                integrationId = integrationId,
                sourceComponent = source,
                targetComponent = target,
                integrationType = IntegrationType.EVENT_DRIVEN,
                configuration = config
            )

            assertTrue(result.isSuccess, "Integration $integrationId should be created successfully")
        }

        // Execute onboarding workflow steps
        val workflowSteps = listOf(
            "user_registration" to mapOf(
                "userId" to "test_user_123",
                "email" to "test@example.com",
                "registrationSource" to "mobile_app"
            ),
            "profile_setup" to mapOf(
                "userId" to "test_user_123",
                "displayName" to "Test User",
                "avatar" to "default_avatar"
            ),
            "preferences_setup" to mapOf(
                "userId" to "test_user_123",
                "theme" to "dark",
                "language" to "en",
                "notifications" to true
            ),
            "notification_setup" to mapOf(
                "userId" to "test_user_123",
                "welcomeNotification" to true,
                "tutorialPrompt" to true
            )
        )

        // Process each workflow step
        workflowSteps.forEach { (stepName, stepData) ->
            val context = IntegrationTestDataFactory.createTestIntegrationContext(
                sourceComponent = when (stepName) {
                    "user_registration" -> "user_registration_component"
                    "profile_setup" -> "profile_setup_component"
                    "preferences_setup" -> "preferences_component"
                    "notification_setup" -> "notification_component"
                    else -> "user_registration_component"
                }
            )

            val dataResult = systemIntegrationHub.processData(
                sourceComponent = context.sourceFeature,
                data = stepData,
                context = context
            )

            assertTrue(dataResult.isSuccess, "Workflow step '$stepName' should complete successfully")
        }

        // Verify workflow completion through analytics
        val analyticsContext = IntegrationTestDataFactory.createTestIntegrationContext(
            sourceComponent = "analytics_component"
        )

        val analyticsData = mapOf(
            "workflowCompleted" to true,
            "workflowType" to "user_onboarding",
            "userId" to "test_user_123",
            "completionTime" to System.currentTimeMillis(),
            "stepsCompleted" to workflowSteps.size
        )

        val analyticsResult = systemIntegrationHub.processData(
            sourceComponent = "analytics_component",
            data = analyticsData,
            context = analyticsContext
        )

        assertTrue(analyticsResult.isSuccess, "Analytics recording should succeed")

        Timber.d("Complete user onboarding workflow test passed")
    }

    @Test
    fun `test game session workflow across multiple systems`() = runTest {
        Timber.d("Testing game session workflow across multiple systems")

        // Register components for game session workflow
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent(
                id = "game_loader_component",
                name = "Game Loader Component",
                type = ComponentType.DATA_PROCESSOR
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "game_state_component",
                name = "Game State Component",
                type = ComponentType.STATE_MANAGER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "progression_component",
                name = "Progression Component",
                type = ComponentType.STATE_MANAGER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "achievement_component",
                name = "Achievement Component",
                type = ComponentType.EVENT_HANDLER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "social_component",
                name = "Social Component",
                type = ComponentType.EVENT_HANDLER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "analytics_component",
                name = "Analytics Component",
                type = ComponentType.DATA_PROCESSOR
            )
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create comprehensive integrations for game session
        val gameIntegrations = listOf(
            // Game loader -> Game state
            Triple("game_loader_component", "game_state_component", "game_loader_to_state"),
            // Game state -> Progression
            Triple("game_state_component", "progression_component", "state_to_progression"),
            // Progression -> Achievements
            Triple("progression_component", "achievement_component", "progression_to_achievements"),
            // Game state -> Social (for multiplayer events)
            Triple("game_state_component", "social_component", "state_to_social"),
            // All game components -> Analytics
            *components.filter { it.id != "analytics_component" }.flatMap { source ->
                listOf(Triple(source.id, "analytics_component", "analytics_integration_${source.id}"))
            }.toTypedArray()
        )

        gameIntegrations.forEach { (source, target, integrationId) ->
            val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
                type = if (integrationId.contains("analytics")) IntegrationType.DATA_FLOW else IntegrationType.EVENT_DRIVEN
            )

            val result = systemIntegrationHub.createIntegration(
                integrationId = integrationId,
                sourceComponent = source,
                targetComponent = target,
                integrationType = config.type,
                configuration = config
            )

            assertTrue(result.isSuccess, "Game integration $integrationId should be created successfully")
        }

        // Execute complete game session workflow
        val gameSessionSteps = listOf(
            "game_loading" to mapOf(
                "gameId" to "puzzle_game_001",
                "userId" to "test_user_123",
                "sessionId" to "session_456",
                "difficulty" to "normal"
            ),
            "game_initialization" to mapOf(
                "gameId" to "puzzle_game_001",
                "userId" to "test_user_123",
                "initialState" to "level_1",
                "startTime" to System.currentTimeMillis()
            ),
            "game_progression" to mapOf(
                "userId" to "test_user_123",
                "level" to 5,
                "score" to 1250,
                "achievements" to listOf("first_win", "speed_runner")
            ),
            "social_sharing" to mapOf(
                "userId" to "test_user_123",
                "achievement" to "level_master",
                "shareWith" to listOf("friends", "leaderboard")
            ),
            "session_completion" to mapOf(
                "userId" to "test_user_123",
                "sessionId" to "session_456",
                "totalScore" to 2500,
                "duration" to 1800, // 30 minutes
                "completionStatus" to "victory"
            )
        )

        // Process each game session step
        gameSessionSteps.forEach { (stepName, stepData) ->
            val sourceComponent = when (stepName) {
                "game_loading" -> "game_loader_component"
                "game_initialization" -> "game_state_component"
                "game_progression" -> "progression_component"
                "social_sharing" -> "social_component"
                "session_completion" -> "game_state_component"
                else -> "game_state_component"
            }

            val context = IntegrationTestDataFactory.createTestIntegrationContext(
                sourceComponent = sourceComponent
            )

            val dataResult = systemIntegrationHub.processData(
                sourceComponent = sourceComponent,
                data = stepData,
                context = context
            )

            assertTrue(dataResult.isSuccess, "Game session step '$stepName' should complete successfully")
        }

        // Verify complete workflow through analytics
        val finalAnalyticsContext = IntegrationTestDataFactory.createTestIntegrationContext(
            sourceComponent = "analytics_component"
        )

        val finalAnalyticsData = mapOf(
            "sessionCompleted" to true,
            "userId" to "test_user_123",
            "gameId" to "puzzle_game_001",
            "totalSteps" to gameSessionSteps.size,
            "sessionAnalytics" to mapOf(
                "engagement" to "high",
                "completionRate" to 1.0,
                "socialInteractions" to 1
            )
        )

        val finalResult = systemIntegrationHub.processData(
            sourceComponent = "analytics_component",
            data = finalAnalyticsData,
            context = finalAnalyticsContext
        )

        assertTrue(finalResult.isSuccess, "Final analytics recording should succeed")

        Timber.d("Complete game session workflow test passed")
    }

    @Test
    fun `test purchase workflow across multiple systems`() = runTest {
        Timber.d("Testing purchase workflow across multiple systems")

        // Register components for purchase workflow
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent(
                id = "payment_component",
                name = "Payment Component",
                type = ComponentType.DATA_PROCESSOR
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "inventory_component",
                name = "Inventory Component",
                type = ComponentType.STATE_MANAGER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "entitlement_component",
                name = "Entitlement Component",
                type = ComponentType.STATE_MANAGER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "notification_component",
                name = "Notification Component",
                type = ComponentType.EVENT_HANDLER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "analytics_component",
                name = "Analytics Component",
                type = ComponentType.DATA_PROCESSOR
            )
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create purchase workflow integrations
        val purchaseIntegrations = listOf(
            // Payment -> Inventory
            Triple("payment_component", "inventory_component", "payment_to_inventory"),
            // Payment -> Entitlement
            Triple("payment_component", "entitlement_component", "payment_to_entitlement"),
            // Inventory -> Notifications
            Triple("inventory_component", "notification_component", "inventory_to_notifications"),
            // Entitlement -> Notifications
            Triple("entitlement_component", "notification_component", "entitlement_to_notifications"),
            // All purchase components -> Analytics
            *components.filter { it.id != "analytics_component" }.flatMap { source ->
                listOf(Triple(source.id, "analytics_component", "purchase_analytics_${source.id}"))
            }.toTypedArray()
        )

        purchaseIntegrations.forEach { (source, target, integrationId) ->
            val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
                type = if (integrationId.contains("analytics")) IntegrationType.DATA_FLOW else IntegrationType.EVENT_DRIVEN
            )

            val result = systemIntegrationHub.createIntegration(
                integrationId = integrationId,
                sourceComponent = source,
                targetComponent = target,
                integrationType = config.type,
                configuration = config
            )

            assertTrue(result.isSuccess, "Purchase integration $integrationId should be created successfully")
        }

        // Execute complete purchase workflow
        val purchaseWorkflowSteps = listOf(
            "payment_processing" to mapOf(
                "userId" to "test_user_123",
                "productId" to "premium_currency_pack",
                "paymentMethod" to "credit_card",
                "amount" to 9.99,
                "currency" to "USD"
            ),
            "inventory_update" to mapOf(
                "userId" to "test_user_123",
                "productId" to "premium_currency_pack",
                "quantity" to 1000,
                "inventorySlot" to "premium_currency"
            ),
            "entitlement_grant" to mapOf(
                "userId" to "test_user_123",
                "entitlementType" to "premium_features",
                "duration" to "permanent",
                "features" to listOf("ad_free", "unlimited_lives", "exclusive_content")
            ),
            "purchase_notification" to mapOf(
                "userId" to "test_user_123",
                "notificationType" to "purchase_confirmation",
                "productName" to "Premium Currency Pack",
                "amount" to 9.99
            ),
            "purchase_analytics" to mapOf(
                "userId" to "test_user_123",
                "purchaseValue" to 9.99,
                "productCategory" to "currency",
                "conversionFunnel" to "direct_purchase",
                "lifetimeValue" to 25.99
            )
        )

        // Process each purchase workflow step
        purchaseWorkflowSteps.forEach { (stepName, stepData) ->
            val sourceComponent = when (stepName) {
                "payment_processing" -> "payment_component"
                "inventory_update" -> "inventory_component"
                "entitlement_grant" -> "entitlement_component"
                "purchase_notification" -> "notification_component"
                "purchase_analytics" -> "analytics_component"
                else -> "payment_component"
            }

            val context = IntegrationTestDataFactory.createTestIntegrationContext(
                sourceComponent = sourceComponent
            )

            val dataResult = systemIntegrationHub.processData(
                sourceComponent = sourceComponent,
                data = stepData,
                context = context
            )

            assertTrue(dataResult.isSuccess, "Purchase workflow step '$stepName' should complete successfully")
        }

        // Verify complete purchase workflow
        val finalHealth = systemIntegrationHub.systemHealth.value
        assertTrue(finalHealth.overallStatus == com.roshni.games.core.utils.integration.HealthStatus.HEALTHY,
            "System should remain healthy after complete purchase workflow")

        Timber.d("Complete purchase workflow test passed")
    }

    @Test
    fun `test multi-system error recovery workflow`() = runTest {
        Timber.d("Testing multi-system error recovery workflow")

        // Register components with different reliability characteristics
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent(
                id = "reliable_component",
                name = "Reliable Component"
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "unreliable_component",
                name = "Unreliable Component"
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "monitoring_component",
                name = "Monitoring Component",
                type = ComponentType.EVENT_HANDLER
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "recovery_component",
                name = "Recovery Component",
                type = ComponentType.EVENT_HANDLER
            )
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create error handling and recovery integrations
        val errorHandlingIntegrations = listOf(
            // All components -> Monitoring
            *components.flatMap { source ->
                listOf(Triple(source.id, "monitoring_component", "monitoring_integration_${source.id}"))
            }.toTypedArray(),
            // Monitoring -> Recovery
            Triple("monitoring_component", "recovery_component", "monitoring_to_recovery"),
            // Recovery -> All components (for recovery actions)
            *components.filter { it.id != "recovery_component" }.flatMap { target ->
                listOf(Triple("recovery_component", target.id, "recovery_integration_${target.id}"))
            }.toTypedArray()
        )

        errorHandlingIntegrations.forEach { (source, target, integrationId) ->
            val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
                type = IntegrationType.EVENT_DRIVEN,
                retryCount = 3
            )

            val result = systemIntegrationHub.createIntegration(
                integrationId = integrationId,
                sourceComponent = source,
                targetComponent = target,
                integrationType = IntegrationType.EVENT_DRIVEN,
                configuration = config
            )

            assertTrue(result.isSuccess, "Error handling integration $integrationId should be created successfully")
        }

        // Simulate error scenario and recovery
        val errorScenarioSteps = listOf(
            "normal_operation" to mapOf(
                "component" to "reliable_component",
                "operation" to "data_processing",
                "status" to "success"
            ),
            "error_detection" to mapOf(
                "component" to "unreliable_component",
                "errorType" to "processing_failure",
                "severity" to "high",
                "timestamp" to System.currentTimeMillis()
            ),
            "error_monitoring" to mapOf(
                "errorId" to "error_001",
                "affectedComponent" to "unreliable_component",
                "monitoringAction" to "circuit_breaker_activated"
            ),
            "recovery_initiation" to mapOf(
                "errorId" to "error_001",
                "recoveryStrategy" to "component_restart",
                "fallbackComponent" to "reliable_component"
            ),
            "recovery_completion" to mapOf(
                "errorId" to "error_001",
                "recoveryResult" to "success",
                "restoredComponents" to listOf("unreliable_component"),
                "recoveryTime" to 500
            )
        )

        // Process error recovery workflow
        errorScenarioSteps.forEach { (stepName, stepData) ->
            val sourceComponent = when (stepName) {
                "normal_operation" -> "reliable_component"
                "error_detection" -> "unreliable_component"
                "error_monitoring" -> "monitoring_component"
                "recovery_initiation" -> "monitoring_component"
                "recovery_completion" -> "recovery_component"
                else -> "monitoring_component"
            }

            val context = IntegrationTestDataFactory.createTestIntegrationContext(
                sourceComponent = sourceComponent
            )

            val dataResult = systemIntegrationHub.processData(
                sourceComponent = sourceComponent,
                data = stepData,
                context = context
            )

            assertTrue(dataResult.isSuccess, "Error recovery step '$stepName' should complete successfully")
        }

        // Verify system recovered and remains functional
        val finalHealth = systemIntegrationHub.systemHealth.value
        assertTrue(finalHealth.overallStatus == com.roshni.games.core.utils.integration.HealthStatus.HEALTHY,
            "System should recover and remain healthy")

        Timber.d("Multi-system error recovery workflow test passed")
    }

    @Test
    fun `test complex multi-workflow coordination`() = runTest {
        Timber.d("Testing complex multi-workflow coordination")

        // Register components for complex workflow scenario
        val components = listOf(
            IntegrationTestDataFactory.createTestComponent(
                id = "workflow_orchestrator",
                name = "Workflow Orchestrator",
                type = ComponentType.WORKFLOW_ENGINE
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "parallel_processor_1",
                name = "Parallel Processor 1",
                type = ComponentType.DATA_PROCESSOR
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "parallel_processor_2",
                name = "Parallel Processor 2",
                type = ComponentType.DATA_PROCESSOR
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "aggregator_component",
                name = "Aggregator Component",
                type = ComponentType.DATA_PROCESSOR
            ),
            IntegrationTestDataFactory.createTestComponent(
                id = "result_handler",
                name = "Result Handler",
                type = ComponentType.EVENT_HANDLER
            )
        )

        IntegrationTestUtils.registerComponentsAndWait(systemIntegrationHub, components)

        // Create complex workflow integrations
        val complexIntegrations = listOf(
            // Orchestrator -> Parallel processors
            Triple("workflow_orchestrator", "parallel_processor_1", "orchestrator_to_processor_1"),
            Triple("workflow_orchestrator", "parallel_processor_2", "orchestrator_to_processor_2"),
            // Parallel processors -> Aggregator
            Triple("parallel_processor_1", "aggregator_component", "processor_1_to_aggregator"),
            Triple("parallel_processor_2", "aggregator_component", "processor_2_to_aggregator"),
            // Aggregator -> Result handler
            Triple("aggregator_component", "result_handler", "aggregator_to_handler"),
            // Result handler -> Orchestrator (for workflow completion)
            Triple("result_handler", "workflow_orchestrator", "handler_to_orchestrator")
        )

        complexIntegrations.forEach { (source, target, integrationId) ->
            val config = IntegrationTestDataFactory.createTestIntegrationConfiguration(
                type = IntegrationType.EVENT_DRIVEN
            )

            val result = systemIntegrationHub.createIntegration(
                integrationId = integrationId,
                sourceComponent = source,
                targetComponent = target,
                integrationType = IntegrationType.EVENT_DRIVEN,
                configuration = config
            )

            assertTrue(result.isSuccess, "Complex integration $integrationId should be created successfully")
        }

        // Execute complex multi-workflow scenario
        val complexWorkflowSteps = listOf(
            "workflow_initiation" to mapOf(
                "workflowId" to "complex_workflow_001",
                "parallelTasks" to 2,
                "inputData" to mapOf("batchSize" to 100, "processingType" to "parallel")
            ),
            "parallel_processing_1" to mapOf(
                "processorId" to "parallel_processor_1",
                "batchId" to "batch_001",
                "dataChunk" to (1..50).map { "item_$it" },
                "processingTime" to 200
            ),
            "parallel_processing_2" to mapOf(
                "processorId" to "parallel_processor_2",
                "batchId" to "batch_002",
                "dataChunk" to (51..100).map { "item_$it" },
                "processingTime" to 250
            ),
            "result_aggregation" to mapOf(
                "aggregatorId" to "aggregator_component",
                "inputCount" to 2,
                "aggregationStrategy" to "merge_and_deduplicate",
                "outputFormat" to "consolidated_report"
            ),
            "final_handling" to mapOf(
                "handlerId" to "result_handler",
                "resultId" to "final_result_001",
                "status" to "completed",
                "completionTime" to System.currentTimeMillis()
            )
        )

        // Process complex workflow steps
        complexWorkflowSteps.forEach { (stepName, stepData) ->
            val sourceComponent = when (stepName) {
                "workflow_initiation" -> "workflow_orchestrator"
                "parallel_processing_1" -> "parallel_processor_1"
                "parallel_processing_2" -> "parallel_processor_2"
                "result_aggregation" -> "aggregator_component"
                "final_handling" -> "result_handler"
                else -> "workflow_orchestrator"
            }

            val context = IntegrationTestDataFactory.createTestIntegrationContext(
                sourceComponent = sourceComponent
            )

            val dataResult = systemIntegrationHub.processData(
                sourceComponent = sourceComponent,
                data = stepData,
                context = context
            )

            assertTrue(dataResult.isSuccess, "Complex workflow step '$stepName' should complete successfully")
        }

        // Verify workflow coordination completion
        val finalMetrics = systemIntegrationHub.getIntegrationMetrics()
        assertTrue(finalMetrics.totalEventsProcessed > 0, "Should have processed workflow events")

        Timber.d("Complex multi-workflow coordination test passed")
    }
}