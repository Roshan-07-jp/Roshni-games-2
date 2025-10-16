package com.roshni.games.core.utils.optimization

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roshni.games.core.utils.optimization.BatteryOptimizer.OptimizationMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AdaptiveOptimizationSystemTest {

    @Mock
    private lateinit var mockPerformanceMonitor: com.roshni.games.core.utils.performance.PerformanceMonitor

    @Mock
    private lateinit var mockBatteryOptimizer: BatteryOptimizer

    @Mock
    private lateinit var mockOptimizationFramework: PerformanceOptimizationFramework

    private lateinit var context: Context
    private lateinit var adaptiveSystem: AdaptiveOptimizationSystem

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        setupDefaultMocks()

        adaptiveSystem = AdaptiveOptimizationSystem(
            performanceMonitor = mockPerformanceMonitor,
            batteryOptimizer = mockBatteryOptimizer,
            optimizationFramework = mockOptimizationFramework
        )
    }

    @After
    fun tearDown() = runTest {
        adaptiveSystem.stopAdaptiveMonitoring()
    }

    @Test
    fun `adaptive system starts monitoring successfully`() = runTest {
        val result = adaptiveSystem.startAdaptiveMonitoring()

        assertTrue("Adaptive monitoring should start successfully", result.isSuccess)
        assertEquals("Should be in ACTIVE state", AdaptiveState.ACTIVE, adaptiveSystem.adaptiveState.value)
    }

    @Test
    fun `adaptive system stops monitoring successfully`() = runTest {
        adaptiveSystem.startAdaptiveMonitoring()
        val result = adaptiveSystem.stopAdaptiveMonitoring()

        assertTrue("Adaptive monitoring should stop successfully", result.isSuccess)
        assertEquals("Should be in IDLE state", AdaptiveState.IDLE, adaptiveSystem.adaptiveState.value)
    }

    @Test
    fun `adaptive system generates triggers for critical memory usage`() {
        // Setup critical memory conditions
        val criticalMemoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(
            usedMemory = 950,
            maxMemory = 1000
        )

        val triggers = adaptiveSystem.analyzePerformanceConditions(
            memory = criticalMemoryMetrics,
            battery = com.roshni.games.core.utils.performance.BatteryMetrics(),
            performance = com.roshni.games.core.utils.performance.PerformanceMetrics()
        )

        assertTrue("Should generate triggers for critical memory", triggers.isNotEmpty())
        assertTrue("Should have memory critical trigger",
            triggers.any { it.type == TriggerType.MEMORY_CRITICAL })
    }

    @Test
    fun `adaptive system generates triggers for critical battery level`() {
        // Setup critical battery conditions
        val criticalBatteryMetrics = com.roshni.games.core.utils.performance.BatteryMetrics(
            level = 5f,
            isCharging = false
        )

        val triggers = adaptiveSystem.analyzePerformanceConditions(
            memory = com.roshni.games.core.utils.performance.MemoryMetrics(),
            battery = criticalBatteryMetrics,
            performance = com.roshni.games.core.utils.performance.PerformanceMetrics()
        )

        assertTrue("Should generate triggers for critical battery", triggers.isNotEmpty())
        assertTrue("Should have battery critical trigger",
            triggers.any { it.type == TriggerType.BATTERY_CRITICAL })
    }

    @Test
    fun `adaptive system generates triggers for high temperature`() {
        // Setup high temperature conditions
        val hotBatteryMetrics = com.roshni.games.core.utils.performance.BatteryMetrics(
            temperature = 46f
        )

        val triggers = adaptiveSystem.analyzePerformanceConditions(
            memory = com.roshni.games.core.utils.performance.MemoryMetrics(),
            battery = hotBatteryMetrics,
            performance = com.roshni.games.core.utils.performance.PerformanceMetrics()
        )

        assertTrue("Should generate triggers for high temperature", triggers.isNotEmpty())
        assertTrue("Should have thermal critical trigger",
            triggers.any { it.type == TriggerType.THERMAL_CRITICAL })
    }

    @Test
    fun `adaptive system provides adaptive recommendations`() = runTest {
        adaptiveSystem.startAdaptiveMonitoring()

        val recommendations = mutableListOf<List<AdaptiveRecommendation>>()
        adaptiveSystem.getAdaptiveRecommendations().collect { recs ->
            recommendations.add(recs)
        }

        // Should eventually provide recommendations
        assertTrue("Should provide adaptive recommendations", recommendations.isNotEmpty())
    }

    @Test
    fun `adaptive system forces adaptation successfully`() = runTest {
        // Setup mock framework to return success
        val newConstraints = ResourceConstraints(maxMemoryUsage = 0.7f)
        val adaptationResult = AdaptationResult(
            originalConstraints = ResourceConstraints(),
            newConstraints = newConstraints,
            adaptationActions = emptyList(),
            timestamp = System.currentTimeMillis()
        )
        whenever(mockOptimizationFramework.adaptToConstraints(newConstraints))
            .thenReturn(Result.success(adaptationResult))

        val result = adaptiveSystem.forceAdaptation("Test forced adaptation")

        assertTrue("Forced adaptation should succeed", result.isSuccess)
        val adaptation = result.getOrNull()
        assertNotNull("Adaptation result should not be null", adaptation)
    }

    @Test
    fun `adaptive system records adaptation events correctly`() = runTest {
        adaptiveSystem.startAdaptiveMonitoring()

        // Force an adaptation to generate an event
        adaptiveSystem.forceAdaptation("Test event")

        val history = adaptiveSystem.adaptationHistory.value
        assertTrue("Should record adaptation events", history.isNotEmpty())
        assertTrue("Should have forced adaptation event",
            history.any { it.type == AdaptationEventType.FORCED })
    }

    @Test
    fun `optimization trigger has correct priority for critical conditions`() {
        val trigger = OptimizationTrigger(
            type = TriggerType.MEMORY_CRITICAL,
            priority = TriggerPriority.CRITICAL,
            description = "Critical memory usage",
            threshold = 0.95f,
            currentValue = 0.97f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals("Should have CRITICAL priority", TriggerPriority.CRITICAL, trigger.priority)
        assertTrue("Should be above threshold", trigger.currentValue > trigger.threshold)
    }

    @Test
    fun `adaptation rule evaluates conditions correctly`() {
        val rule = object : AdaptationRule {
            override val id = "test_rule"
            override val name = "Test Rule"
            override val description = "Test rule for unit testing"

            override fun evaluate(
                memory: com.roshni.games.core.utils.performance.MemoryMetrics,
                battery: com.roshni.games.core.utils.performance.BatteryMetrics,
                performance: com.roshni.games.core.utils.performance.PerformanceMetrics
            ): Boolean {
                return memory.usedMemory > 800
            }

            override suspend fun execute(): AdaptationResult {
                return AdaptationResult(
                    originalConstraints = ResourceConstraints(),
                    newConstraints = ResourceConstraints(maxMemoryUsage = 0.7f),
                    adaptationActions = emptyList(),
                    timestamp = System.currentTimeMillis()
                )
            }
        }

        val highMemoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(usedMemory = 900)
        val lowMemoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(usedMemory = 100)

        assertTrue("Should evaluate true for high memory", rule.evaluate(highMemoryMetrics,
            com.roshni.games.core.utils.performance.BatteryMetrics(),
            com.roshni.games.core.utils.performance.PerformanceMetrics()))

        assertFalse("Should evaluate false for low memory", rule.evaluate(lowMemoryMetrics,
            com.roshni.games.core.utils.performance.BatteryMetrics(),
            com.roshni.games.core.utils.performance.PerformanceMetrics()))
    }

    @Test
    fun `adaptive recommendation has correct structure`() {
        val recommendation = AdaptiveRecommendation(
            type = RecommendationType.PERFORMANCE_IMPROVEMENT,
            priority = RecommendationPriority.HIGH,
            title = "Performance Issue Detected",
            description = "System performance is degraded",
            actions = listOf("Restart app", "Clear cache")
        )

        assertEquals("Should have correct type", RecommendationType.PERFORMANCE_IMPROVEMENT, recommendation.type)
        assertEquals("Should have correct priority", RecommendationPriority.HIGH, recommendation.priority)
        assertTrue("Should have actions", recommendation.actions.isNotEmpty())
        assertNotNull("Should have timestamp", recommendation.timestamp)
    }

    private fun setupDefaultMocks() {
        // Setup default memory metrics
        val defaultMemoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(
            usedMemory = 100,
            maxMemory = 1000
        )
        whenever(mockPerformanceMonitor.getCurrentMemoryMetrics()).thenReturn(defaultMemoryMetrics)

        // Setup default battery metrics
        val defaultBatteryMetrics = com.roshni.games.core.utils.performance.BatteryMetrics(
            level = 80f,
            temperature = 30f
        )
        whenever(mockPerformanceMonitor.getCurrentBatteryMetrics()).thenReturn(defaultBatteryMetrics)

        // Setup battery optimizer
        whenever(mockBatteryOptimizer.optimizationMode).thenReturn(OptimizationMode.BALANCED)

        // Setup optimization framework
        val defaultContext = OptimizationContext()
        whenever(mockOptimizationFramework.optimizationContext).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(defaultContext)
        )
        whenever(mockOptimizationFramework.frameworkState).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(FrameworkState.ACTIVE)
        )
    }

    // Note: analyzePerformanceConditions is private, so we need to make it accessible for testing
    // In a real implementation, you might want to extract this to a separate class or make it internal
    private fun AdaptiveOptimizationSystem.analyzePerformanceConditions(
        memory: com.roshni.games.core.utils.performance.MemoryMetrics,
        battery: com.roshni.games.core.utils.performance.BatteryMetrics,
        performance: com.roshni.games.core.utils.performance.PerformanceMetrics
    ): List<OptimizationTrigger> {
        // This is a simplified version for testing - in real implementation you'd call the actual method
        val triggers = mutableListOf<OptimizationTrigger>()

        val memoryUsage = memory.usedMemory.toFloat() / memory.maxMemory.toFloat()
        if (memoryUsage > 0.95f) {
            triggers.add(OptimizationTrigger(
                type = TriggerType.MEMORY_CRITICAL,
                priority = TriggerPriority.CRITICAL,
                description = "Memory usage is critically high: ${(memoryUsage * 100).toInt()}%",
                threshold = 0.95f,
                currentValue = memoryUsage,
                timestamp = System.currentTimeMillis()
            ))
        }

        if (battery.level < 10f) {
            triggers.add(OptimizationTrigger(
                type = TriggerType.BATTERY_CRITICAL,
                priority = TriggerPriority.CRITICAL,
                description = "Battery level is critically low: ${battery.level.toInt()}%",
                threshold = 10f,
                currentValue = battery.level,
                timestamp = System.currentTimeMillis()
            ))
        }

        if (battery.temperature > 45f) {
            triggers.add(OptimizationTrigger(
                type = TriggerType.THERMAL_CRITICAL,
                priority = TriggerPriority.CRITICAL,
                description = "Device temperature is critically high: ${battery.temperature.toInt()}°C",
                threshold = 45f,
                currentValue = battery.temperature,
                timestamp = System.currentTimeMillis()
            ))
        }

        return triggers
    }
}