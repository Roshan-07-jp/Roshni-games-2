package com.roshni.games.core.utils.integration

import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UserInteraction
import com.roshni.games.core.ui.ux.model.UserPreferences
import com.roshni.games.core.utils.feature.CrossFeatureEvent
import com.roshni.games.core.utils.feature.FeatureCategory
import com.roshni.games.core.utils.feature.FeatureConfig
import com.roshni.games.core.utils.feature.FeatureContext
import com.roshni.games.core.utils.feature.FeatureDependency
import com.roshni.games.core.utils.feature.FeatureEvent
import com.roshni.games.core.utils.feature.FeatureResult
import com.roshni.games.core.utils.feature.FeatureState
import com.roshni.games.core.utils.rules.RuleAction
import com.roshni.games.core.utils.rules.RuleCondition
import com.roshni.games.core.utils.rules.RuleContext
import com.roshni.games.core.utils.rules.RuleResult
import com.roshni.games.core.utils.workflow.WorkflowAction
import com.roshni.games.core.utils.workflow.WorkflowContext
import com.roshni.games.core.utils.workflow.WorkflowResult
import com.roshni.games.core.utils.workflow.WorkflowState
import com.roshni.games.core.utils.workflow.WorkflowTransition
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

/**
 * Factory class for creating test data for integration testing
 */
object IntegrationTestDataFactory {

    /**
     * Create a test user interaction
     */
    fun createTestUserInteraction(
        id: String = "test_interaction_${System.currentTimeMillis()}",
        type: UserInteraction.InteractionType = UserInteraction.InteractionType.TAP,
        screenName: String = "test_screen",
        userId: String = "test_user_123",
        sessionId: String = "test_session_456",
        coordinates: Pair<Float, Float> = Pair(100f, 200f),
        timestamp: Long = System.currentTimeMillis()
    ): UserInteraction {
        return UserInteraction(
            id = id,
            type = type,
            context = UXContext(
                screenName = screenName,
                userId = userId,
                sessionId = sessionId,
                timestamp = timestamp,
                interactionHistory = emptyList(),
                userPreferences = createTestUserPreferences(),
                deviceInfo = mapOf(
                    "screenWidth" to 1080,
                    "screenHeight" to 1920,
                    "deviceType" to "mobile",
                    "osVersion" to "Android 13"
                ),
                environmentalFactors = mapOf(
                    "lighting" to "normal",
                    "noiseLevel" to "low",
                    "connectionType" to "wifi"
                )
            ),
            coordinates = coordinates,
            timestamp = timestamp,
            metadata = mapOf(
                "testData" to true,
                "source" to "integration_test"
            )
        )
    }

    /**
     * Create test user preferences
     */
    fun createTestUserPreferences(
        theme: UserPreferences.Theme = UserPreferences.Theme.SYSTEM,
        animationSpeed: UserPreferences.AnimationSpeed = UserPreferences.AnimationSpeed.NORMAL,
        soundEnabled: Boolean = true,
        hapticFeedbackEnabled: Boolean = true,
        language: String = "en"
    ): UserPreferences {
        return UserPreferences(
            theme = theme,
            animationSpeed = animationSpeed,
            soundEnabled = soundEnabled,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            language = language,
            accessibilitySettings = mapOf(
                "highContrast" to false,
                "largeText" to false,
                "screenReader" to false
            ),
            gameplayPreferences = mapOf(
                "difficulty" to "normal",
                "autoSave" to true,
                "tutorialEnabled" to true
            )
        )
    }

    /**
     * Create a test cross-feature event
     */
    fun createTestCrossFeatureEvent(
        id: String = "test_event_${System.currentTimeMillis()}",
        eventType: String = "TEST_EVENT",
        sourceFeature: String = "test_feature",
        targetFeatures: List<String> = listOf("target_feature_1", "target_feature_2"),
        priority: com.roshni.games.core.utils.integration.EventPriority = com.roshni.games.core.utils.integration.EventPriority.NORMAL,
        payload: Map<String, Any> = mapOf("testPayload" to true),
        timestamp: Long = System.currentTimeMillis()
    ): CrossFeatureEvent {
        return CrossFeatureEvent(
            id = id,
            eventType = eventType,
            sourceFeature = sourceFeature,
            targetFeatures = targetFeatures,
            priority = priority,
            payload = payload,
            timestamp = timestamp,
            metadata = mapOf("testEvent" to true)
        )
    }

    /**
     * Create a test feature context
     */
    fun createTestFeatureContext(
        featureId: String = "test_feature",
        executionId: String = "test_execution_${System.currentTimeMillis()}",
        userId: String? = "test_user_123",
        sessionId: String? = "test_session_456",
        ruleContext: RuleContext = RuleContext()
    ): FeatureContext {
        return FeatureContext(
            featureId = featureId,
            executionId = executionId,
            userId = userId,
            sessionId = sessionId,
            ruleContext = ruleContext,
            variables = mapOf(
                "testVariable" to "testValue",
                "timestamp" to System.currentTimeMillis()
            ),
            metadata = mapOf("testContext" to true)
        )
    }

    /**
     * Create a test rule context
     */
    fun createTestRuleContext(
        userId: String? = "test_user_123",
        sessionId: String? = "test_session_456",
        variables: Map<String, Any> = mapOf(
            "testVariable" to "testValue",
            "userType" to "premium",
            "gameLevel" to 5
        )
    ): RuleContext {
        return RuleContext(
            userId = userId,
            sessionId = sessionId,
            variables = variables,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf("testContext" to true)
        )
    }

    /**
     * Create a test workflow context
     */
    fun createTestWorkflowContext(
        workflowId: String = "test_workflow",
        userId: String = "test_user_123",
        sessionId: String = "test_session_456",
        inputData: Map<String, Any> = mapOf(
            "input1" to "value1",
            "input2" to 42,
            "startTime" to System.currentTimeMillis()
        ),
        metadata: Map<String, Any> = mapOf("testWorkflow" to true)
    ): WorkflowContext {
        return WorkflowContext(
            workflowId = workflowId,
            userId = userId,
            sessionId = sessionId,
            inputData = inputData,
            metadata = metadata,
            currentState = "start",
            executionId = "test_workflow_execution_${System.currentTimeMillis()}"
        )
    }

    /**
     * Create test integration configuration
     */
    fun createTestIntegrationConfiguration(
        type: com.roshni.games.core.utils.integration.IntegrationType = com.roshni.games.core.utils.integration.IntegrationType.EVENT_DRIVEN,
        priority: com.roshni.games.core.utils.integration.EventPriority = com.roshni.games.core.utils.integration.EventPriority.NORMAL,
        dataFlowDirection: com.roshni.games.core.utils.integration.DataFlowDirection = com.roshni.games.core.utils.integration.DataFlowDirection.BIDIRECTIONAL,
        retryCount: Int = 3,
        timeoutMs: Long = 5000
    ): com.roshni.games.core.utils.integration.IntegrationConfiguration {
        return com.roshni.games.core.utils.integration.IntegrationConfiguration(
            type = type,
            priority = priority,
            dataFlowDirection = dataFlowDirection,
            retryCount = retryCount,
            timeoutMs = timeoutMs,
            metadata = mapOf(
                "testConfiguration" to true,
                "createdAt" to System.currentTimeMillis()
            )
        )
    }

    /**
     * Create a comprehensive test scenario with multiple components
     */
    fun createTestScenario(
        scenarioId: String = "test_scenario_${System.currentTimeMillis()}",
        componentCount: Int = 5,
        interactionCount: Int = 10,
        includeErrors: Boolean = false
    ): TestScenario {
        val components = mutableListOf<IntegratedComponent>()
        val interactions = mutableListOf<UserInteraction>()
        val events = mutableListOf<CrossFeatureEvent>()

        // Create test components
        repeat(componentCount) { index ->
            components.add(createTestComponent(
                id = "test_component_$index",
                name = "Test Component $index",
                type = ComponentType.CUSTOM
            ))
        }

        // Create test interactions
        repeat(interactionCount) { index ->
            interactions.add(createTestUserInteraction(
                id = "test_interaction_$index",
                userId = "test_user_${index % 3}", // Rotate between 3 users
                screenName = "test_screen_${index % 2}" // Rotate between 2 screens
            ))
        }

        // Create test events
        repeat(interactionCount / 2) { index ->
            events.add(createTestCrossFeatureEvent(
                eventType = "TEST_EVENT_TYPE_$index",
                sourceFeature = components[index % components.size].id,
                targetFeatures = listOf(components[(index + 1) % components.size].id)
            ))
        }

        return TestScenario(
            id = scenarioId,
            components = components,
            interactions = interactions,
            events = events,
            includeErrors = includeErrors,
            metadata = mapOf(
                "componentCount" to componentCount,
                "interactionCount" to interactionCount,
                "eventCount" to events.size,
                "createdAt" to System.currentTimeMillis()
            )
        )
    }

    /**
     * Create test component for integration testing
     */
    fun createTestComponent(
        id: String,
        name: String,
        type: ComponentType = ComponentType.CUSTOM,
        version: String = "1.0.0",
        isActive: Boolean = true,
        capabilities: List<String> = listOf("test_capability"),
        metadata: Map<String, Any> = emptyMap()
    ): IntegratedComponent {
        return IntegratedComponent(
            id = id,
            name = name,
            type = type,
            version = version,
            isActive = isActive,
            capabilities = capabilities,
            metadata = metadata + mapOf("testComponent" to true)
        )
    }

    /**
     * Create a mock feature for testing
     */
    fun createMockFeature(
        id: String,
        name: String,
        category: FeatureCategory = FeatureCategory.GAMEPLAY,
        enabled: Boolean = true,
        dependencies: List<FeatureDependency> = emptyList()
    ): com.roshni.games.core.utils.feature.Feature {
        return object : com.roshni.games.core.utils.feature.Feature {
            override val id: String = id
            override val name: String = name
            override val category: FeatureCategory = category
            override val version: String = "1.0.0"
            override val description: String = "Mock feature for testing"
            override val tags: List<String> = listOf("test", "mock")
            override val dependencies: List<FeatureDependency> = dependencies
            override val config: FeatureConfig = FeatureConfig(
                timeoutMs = 5000,
                retryCount = 3,
                enabledByDefault = enabled
            )

            override val enabled: Boolean = enabled
            override val state = MutableStateFlow(
                if (enabled) FeatureState.ENABLED else FeatureState.DISABLED
            )

            override suspend fun initialize(context: FeatureContext): Boolean {
                Timber.d("Mock feature $id initialized")
                return true
            }

            override suspend fun enable(context: FeatureContext): Boolean {
                state.value = FeatureState.ENABLED
                Timber.d("Mock feature $id enabled")
                return true
            }

            override suspend fun disable(context: FeatureContext): Boolean {
                state.value = FeatureState.DISABLED
                Timber.d("Mock feature $id disabled")
                return true
            }

            override suspend fun execute(context: FeatureContext): FeatureResult {
                return FeatureResult(
                    success = true,
                    data = mapOf(
                        "featureId" to id,
                        "executedAt" to System.currentTimeMillis(),
                        "context" to context.featureId
                    ),
                    executionTimeMs = 100,
                    metadata = mapOf("mockExecution" to true)
                )
            }

            override suspend fun cleanup() {
                Timber.d("Mock feature $id cleaned up")
            }

            override fun validate(): com.roshni.games.core.utils.feature.FeatureValidationResult {
                return com.roshni.games.core.utils.feature.FeatureValidationResult(
                    isValid = true,
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }

            override suspend fun handleEvent(
                event: FeatureEvent,
                context: FeatureContext
            ): Boolean {
                Timber.d("Mock feature $id handled event: ${event.eventType}")
                return true
            }
        }
    }

    /**
     * Create a mock rule for testing
     */
    fun createMockRule(
        id: String,
        name: String,
        category: String = "test",
        priority: Int = 1,
        shouldSucceed: Boolean = true
    ): com.roshni.games.core.utils.rules.Rule {
        return object : com.roshni.games.core.utils.rules.Rule {
            override val id: String = id
            override val name: String = name
            override val category: String = category
            override val description: String = "Mock rule for testing"
            override val priority: Int = priority
            override val enabled: Boolean = true
            override val conditions: List<RuleCondition> = emptyList()
            override val actions: List<RuleAction> = emptyList()

            override suspend fun evaluate(context: RuleContext): RuleResult {
                return RuleResult(
                    success = shouldSucceed,
                    ruleId = id,
                    actions = if (shouldSucceed) emptyList() else emptyList(),
                    executionTimeMs = 50,
                    error = if (!shouldSucceed) "Mock rule failure" else null
                )
            }

            override fun validate(): com.roshni.games.core.utils.rules.ValidationResult {
                return com.roshni.games.core.utils.rules.ValidationResult(
                    isValid = true,
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
        }
    }

    /**
     * Create a mock workflow for testing
     */
    fun createMockWorkflow(
        id: String,
        name: String,
        category: String = "test",
        shouldSucceed: Boolean = true,
        executionTimeMs: Long = 100
    ): com.roshni.games.core.utils.workflow.Workflow {
        return object : com.roshni.games.core.utils.workflow.Workflow {
            override val id: String = id
            override val name: String = name
            override val category: String = category
            override val description: String = "Mock workflow for testing"
            override val version: String = "1.0.0"
            override val enabled: Boolean = true
            override val tags: List<String> = listOf("test", "mock")
            override val states: List<WorkflowState> = listOf(
                WorkflowState("start", "Start State"),
                WorkflowState("processing", "Processing State"),
                WorkflowState("end", "End State")
            )
            override val transitions: List<WorkflowTransition> = listOf(
                WorkflowTransition(
                    fromState = "start",
                    toState = "processing",
                    condition = { true },
                    action = { context ->
                        WorkflowResult(
                            success = shouldSucceed,
                            state = if (shouldSucceed) "processing" else "error",
                            outputData = mapOf("step1" to true),
                            executionTimeMs = executionTimeMs / 2
                        )
                    }
                ),
                WorkflowTransition(
                    fromState = "processing",
                    toState = "end",
                    condition = { true },
                    action = { context ->
                        WorkflowResult(
                            success = shouldSucceed,
                            state = "end",
                            outputData = mapOf(
                                "step2" to true,
                                "workflowId" to id,
                                "completedAt" to System.currentTimeMillis()
                            ),
                            executionTimeMs = executionTimeMs / 2
                        )
                    }
                )
            )
            override val initialState: String = "start"
            override val timeoutMs: Long = 10000

            override suspend fun execute(context: WorkflowContext): WorkflowResult {
                return WorkflowResult(
                    success = shouldSucceed,
                    state = if (shouldSucceed) "end" else "error",
                    outputData = mapOf(
                        "workflowId" to id,
                        "executedAt" to System.currentTimeMillis(),
                        "context" to context.workflowId
                    ),
                    executionTimeMs = executionTimeMs,
                    stepsExecuted = if (shouldSucceed) 2 else 1,
                    error = if (!shouldSucceed) "Mock workflow failure" else null
                )
            }

            override fun validate(): com.roshni.games.core.utils.workflow.WorkflowValidationResult {
                return com.roshni.games.core.utils.workflow.WorkflowValidationResult(
                    isValid = true,
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
        }
    }
}

/**
 * Data class representing a complete test scenario
 */
data class TestScenario(
    val id: String,
    val components: List<IntegratedComponent>,
    val interactions: List<UserInteraction>,
    val events: List<CrossFeatureEvent>,
    val includeErrors: Boolean = false,
    val metadata: Map<String, Any> = emptyMap()
)