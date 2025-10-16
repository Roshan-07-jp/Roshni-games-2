package com.roshni.games.core.utils.integration

import com.roshni.games.core.utils.feature.Feature
import com.roshni.games.core.utils.feature.FeatureCategory
import com.roshni.games.core.utils.feature.FeatureConfig
import com.roshni.games.core.utils.feature.FeatureContext
import com.roshni.games.core.utils.feature.FeatureManager
import com.roshni.games.core.utils.feature.FeatureManagerContext
import com.roshni.games.core.utils.feature.FeatureManagerImpl
import com.roshni.games.core.utils.feature.FeatureResult
import com.roshni.games.core.utils.feature.FeatureState
import com.roshni.games.core.utils.rules.Rule
import com.roshni.games.core.utils.rules.RuleContext
import com.roshni.games.core.utils.rules.RuleEngine
import com.roshni.games.core.utils.rules.RuleEngineImpl
import com.roshni.games.core.utils.rules.RuleResult
import com.roshni.games.core.utils.workflow.Workflow
import com.roshni.games.core.utils.workflow.WorkflowContext
import com.roshni.games.core.utils.workflow.WorkflowEngine
import com.roshni.games.core.utils.workflow.WorkflowEngineImpl
import com.roshni.games.core.utils.workflow.WorkflowResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import timber.log.Timber

/**
 * Base class for system integration tests that provides common functionality
 * for testing interactions between multiple systems in the Roshni Games platform.
 */
abstract class SystemIntegrationTest {

    protected lateinit var systemIntegrationHub: SystemIntegrationHub
    protected lateinit var featureManager: FeatureManager
    protected lateinit var ruleEngine: RuleEngine
    protected lateinit var workflowEngine: WorkflowEngine

    protected lateinit var testScope: TestScope

    @Before
    fun setUp() = runTest {
        testScope = TestScope()

        // Initialize core systems
        initializeCoreSystems()

        // Setup test data
        setupTestData()

        // Initialize integration hub
        initializeIntegrationHub()

        Timber.d("System integration test setup completed")
    }

    @After
    fun tearDown() = runTest {
        // Cleanup in reverse order
        cleanupIntegrationHub()
        cleanupTestData()
        cleanupCoreSystems()

        Timber.d("System integration test cleanup completed")
    }

    /**
     * Initialize core systems for testing
     */
    private suspend fun initializeCoreSystems() {
        // Initialize rule engine
        ruleEngine = RuleEngineImpl()

        // Initialize workflow engine
        workflowEngine = WorkflowEngineImpl()

        // Initialize feature manager
        featureManager = FeatureManagerImpl(ruleEngine, workflowEngine)

        Timber.d("Core systems initialized for testing")
    }

    /**
     * Setup test data and mock objects
     */
    protected open suspend fun setupTestData() {
        // Register test features
        registerTestFeatures()

        // Register test rules
        registerTestRules()

        // Register test workflows
        registerTestWorkflows()

        Timber.d("Test data setup completed")
    }

    /**
     * Initialize the system integration hub
     */
    private suspend fun initializeIntegrationHub() {
        systemIntegrationHub = SystemIntegrationHubImpl(
            featureManager = featureManager,
            ruleEngine = ruleEngine,
            workflowEngine = workflowEngine
        )

        val result = systemIntegrationHub.initialize()
        check(result.isSuccess) { "Failed to initialize SystemIntegrationHub: ${result.exceptionOrNull()?.message}" }

        Timber.d("System integration hub initialized")
    }

    /**
     * Cleanup integration hub
     */
    private suspend fun cleanupIntegrationHub() {
        if (::systemIntegrationHub.isInitialized) {
            val result = systemIntegrationHub.shutdown()
            if (result.isFailure) {
                Timber.e("Error shutting down SystemIntegrationHub: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Cleanup core systems
     */
    private suspend fun cleanupCoreSystems() {
        if (::featureManager.isInitialized) {
            featureManager.shutdown()
        }

        if (::workflowEngine.isInitialized) {
            workflowEngine.shutdown()
        }

        if (::ruleEngine.isInitialized) {
            ruleEngine.shutdown()
        }

        Timber.d("Core systems cleanup completed")
    }

    /**
     * Cleanup test data
     */
    protected open suspend fun cleanupTestData() {
        // Override in subclasses for specific cleanup
    }

    /**
     * Register test features for integration testing
     */
    protected open suspend fun registerTestFeatures() {
        // Register a basic test feature
        val testFeature = createTestFeature(
            id = "test_integration_feature",
            name = "Test Integration Feature",
            category = FeatureCategory.GAMEPLAY
        )

        val registered = featureManager.registerFeature(testFeature)
        check(registered) { "Failed to register test feature" }
    }

    /**
     * Register test rules for integration testing
     */
    protected open suspend fun registerTestRules() {
        // Register a basic test rule
        val testRule = createTestRule(
            id = "test_integration_rule",
            name = "Test Integration Rule"
        )

        val registered = ruleEngine.registerRule(testRule)
        check(registered) { "Failed to register test rule" }
    }

    /**
     * Register test workflows for integration testing
     */
    protected open suspend fun registerTestWorkflows() {
        // Register a basic test workflow
        val testWorkflow = createTestWorkflow(
            id = "test_integration_workflow",
            name = "Test Integration Workflow"
        )

        val registered = workflowEngine.registerWorkflow(testWorkflow)
        check(registered) { "Failed to register test workflow" }
    }

    /**
     * Create a test feature for integration testing
     */
    protected fun createTestFeature(
        id: String,
        name: String,
        category: FeatureCategory,
        enabled: Boolean = true,
        tags: List<String> = emptyList()
    ): Feature {
        return object : Feature {
            override val id: String = id
            override val name: String = name
            override val category: FeatureCategory = category
            override val version: String = "1.0.0"
            override val description: String = "Test feature for integration testing"
            override val tags: List<String> = tags
            override val dependencies: List<com.roshni.games.core.utils.feature.FeatureDependency> = emptyList()
            override val config: FeatureConfig = FeatureConfig(
                timeoutMs = 5000,
                retryCount = 3,
                enabledByDefault = enabled
            )

            override val enabled: Boolean = enabled
            override val state = kotlinx.coroutines.flow.MutableStateFlow(
                if (enabled) FeatureState.ENABLED else FeatureState.DISABLED
            )

            override suspend fun initialize(context: FeatureContext): Boolean {
                return true
            }

            override suspend fun enable(context: FeatureContext): Boolean {
                state.value = FeatureState.ENABLED
                return true
            }

            override suspend fun disable(context: FeatureContext): Boolean {
                state.value = FeatureState.DISABLED
                return true
            }

            override suspend fun execute(context: FeatureContext): FeatureResult {
                return FeatureResult(
                    success = true,
                    data = mapOf("executed" to true),
                    executionTimeMs = 100
                )
            }

            override suspend fun cleanup() {
                // No cleanup needed for test feature
            }

            override fun validate(): com.roshni.games.core.utils.feature.FeatureValidationResult {
                return com.roshni.games.core.utils.feature.FeatureValidationResult(
                    isValid = true,
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }

            override suspend fun handleEvent(
                event: com.roshni.games.core.utils.feature.FeatureEvent,
                context: FeatureContext
            ): Boolean {
                return true
            }
        }
    }

    /**
     * Create a test rule for integration testing
     */
    protected fun createTestRule(
        id: String,
        name: String,
        category: String = "integration_test"
    ): Rule {
        return object : Rule {
            override val id: String = id
            override val name: String = name
            override val category: String = category
            override val description: String = "Test rule for integration testing"
            override val priority: Int = 1
            override val enabled: Boolean = true
            override val conditions: List<com.roshni.games.core.utils.rules.RuleCondition> = emptyList()
            override val actions: List<com.roshni.games.core.utils.rules.RuleAction> = emptyList()

            override suspend fun evaluate(context: RuleContext): RuleResult {
                return RuleResult(
                    success = true,
                    ruleId = id,
                    actions = emptyList(),
                    executionTimeMs = 50
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
     * Create a test workflow for integration testing
     */
    protected fun createTestWorkflow(
        id: String,
        name: String,
        category: String = "integration_test"
    ): Workflow {
        return object : Workflow {
            override val id: String = id
            override val name: String = name
            override val category: String = category
            override val description: String = "Test workflow for integration testing"
            override val version: String = "1.0.0"
            override val enabled: Boolean = true
            override val tags: List<String> = emptyList()
            override val states: List<com.roshni.games.core.utils.workflow.WorkflowState> = listOf(
                com.roshni.games.core.utils.workflow.WorkflowState("start", "Start State"),
                com.roshni.games.core.utils.workflow.WorkflowState("end", "End State")
            )
            override val transitions: List<com.roshni.games.core.utils.workflow.WorkflowTransition> = listOf(
                com.roshni.games.core.utils.workflow.WorkflowTransition(
                    fromState = "start",
                    toState = "end",
                    condition = { true },
                    action = { context ->
                        com.roshni.games.core.utils.workflow.WorkflowResult(
                            success = true,
                            state = "end",
                            outputData = mapOf("completed" to true),
                            executionTimeMs = 100
                        )
                    }
                )
            )
            override val initialState: String = "start"
            override val timeoutMs: Long = 10000

            override suspend fun execute(context: WorkflowContext): WorkflowResult {
                return com.roshni.games.core.utils.workflow.WorkflowResult(
                    success = true,
                    state = "end",
                    outputData = mapOf("workflow_executed" to true),
                    executionTimeMs = 100,
                    stepsExecuted = 1
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

    /**
     * Create a test component for integration testing
     */
    protected fun createTestComponent(
        id: String,
        name: String,
        type: ComponentType = ComponentType.CUSTOM
    ): IntegratedComponent {
        return IntegratedComponent(
            id = id,
            name = name,
            type = type,
            version = "1.0.0",
            isActive = true,
            capabilities = listOf("test_capability"),
            metadata = mapOf("test_type" to "integration_test")
        )
    }

    /**
     * Create test integration context
     */
    protected fun createTestIntegrationContext(
        sourceComponent: String = "test_source",
        eventId: String = "test_event_${System.currentTimeMillis()}",
        priority: com.roshni.games.core.utils.integration.EventPriority = com.roshni.games.core.utils.integration.EventPriority.NORMAL
    ): com.roshni.games.core.utils.integration.IntegrationContext {
        return com.roshni.games.core.utils.integration.IntegrationContext(
            eventId = eventId,
            sourceFeature = sourceComponent,
            timestamp = System.currentTimeMillis(),
            priority = priority,
            metadata = mapOf("test_context" to true)
        )
    }

    /**
     * Assert that all systems are properly initialized and ready
     */
    protected suspend fun assertSystemsReady() {
        // Check integration hub status
        val hubStatus = systemIntegrationHub.status.value
        assert(hubStatus == IntegrationHubStatus.READY) {
            "SystemIntegrationHub should be ready, but was: $hubStatus"
        }

        // Check feature manager status
        val featureManagerStatus = featureManager.status.value
        assert(featureManagerStatus.isInitialized) {
            "FeatureManager should be initialized"
        }

        // Check that core components are registered
        val metrics = systemIntegrationHub.getIntegrationMetrics()
        assert(metrics.totalComponents >= 3) { // At least FeatureManager, RuleEngine, WorkflowEngine
            "Should have at least 3 core components registered, but found: ${metrics.totalComponents}"
        }

        Timber.d("All systems verified as ready")
    }

    /**
     * Wait for system stabilization
     */
    protected suspend fun waitForSystemStabilization(timeoutMs: Long = 5000) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val metrics = systemIntegrationHub.getIntegrationMetrics()

            // Wait for all core components to be active
            if (metrics.activeComponents >= 3) {
                break
            }

            kotlinx.coroutines.delay(100)
        }

        val finalMetrics = systemIntegrationHub.getIntegrationMetrics()
        Timber.d("System stabilization completed. Active components: ${finalMetrics.activeComponents}")
    }
}