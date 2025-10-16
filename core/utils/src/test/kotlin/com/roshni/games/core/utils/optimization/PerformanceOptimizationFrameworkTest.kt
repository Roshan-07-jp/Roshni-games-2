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
class PerformanceOptimizationFrameworkTest {

    @Mock
    private lateinit var mockPerformanceMonitor: com.roshni.games.core.utils.performance.PerformanceMonitor

    @Mock
    private lateinit var mockBatteryOptimizer: BatteryOptimizer

    private lateinit var context: Context
    private lateinit var framework: PerformanceOptimizationFramework

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Setup default mock behaviors
        setupDefaultMocks()

        framework = PerformanceOptimizationFrameworkImpl(
            performanceMonitor = mockPerformanceMonitor,
            batteryOptimizer = mockBatteryOptimizer,
            context = context
        )
    }

    @After
    fun tearDown() = runTest {
        framework.stop()
    }

    @Test
    fun `framework starts successfully`() = runTest {
        val result = framework.start()

        assertTrue("Framework should start successfully", result.isSuccess)
        assertEquals("Framework should be in ACTIVE state",
            FrameworkState.ACTIVE, framework.frameworkState.value)
    }

    @Test
    fun `framework stops successfully`() = runTest {
        framework.start()
        val result = framework.stop()

        assertTrue("Framework should stop successfully", result.isSuccess)
        assertEquals("Framework should be in IDLE state",
            FrameworkState.IDLE, framework.frameworkState.value)
    }

    @Test
    fun `framework executes optimizations successfully`() = runTest {
        framework.start()

        val result = framework.executeOptimizations()

        assertTrue("Optimization execution should succeed", result.isSuccess)
        val optimizations = result.getOrNull()
        assertNotNull("Optimizations should not be null", optimizations)
        assertTrue("Should have executed some optimizations", optimizations?.isNotEmpty() == true)
    }

    @Test
    fun `framework adapts to constraints successfully`() = runTest {
        framework.start()

        val newConstraints = ResourceConstraints(
            maxMemoryUsage = 0.6f,
            maxBatteryDrain = 0.4f
        )

        val result = framework.adaptToConstraints(newConstraints)

        assertTrue("Constraint adaptation should succeed", result.isSuccess)
        val adaptation = result.getOrNull()
        assertNotNull("Adaptation result should not be null", adaptation)
        assertEquals("Should have new constraints", newConstraints, adaptation?.newConstraints)
    }

    @Test
    fun `framework provides performance recommendations`() = runTest {
        framework.start()

        // Collect recommendations
        val recommendations = mutableListOf<List<PerformanceRecommendation>>()
        framework.getPerformanceRecommendations().collect { recs ->
            recommendations.add(recs)
        }

        // Should eventually provide some recommendations
        assertTrue("Should provide recommendations", recommendations.isNotEmpty())
    }

    @Test
    fun `framework registers custom strategy successfully`() = runTest {
        framework.start()

        val customStrategy = object : OptimizationStrategy(
            id = "test_strategy",
            name = "Test Strategy",
            description = "Test strategy for unit testing"
        ) {
            override suspend fun execute(context: OptimizationContext): OptimizationResult {
                return OptimizationResult(
                    strategyId = id,
                    success = true,
                    message = "Test strategy executed",
                    timestamp = System.currentTimeMillis()
                )
            }

            override fun shouldExecute(context: OptimizationContext): Boolean = true

            override fun getEstimatedImpact(context: OptimizationContext): OptimizationImpact {
                return OptimizationImpact(memoryReduction = 1000)
            }
        }

        val result = framework.registerStrategy(customStrategy)

        assertTrue("Strategy registration should succeed", result.isSuccess)
    }

    @Test
    fun `framework unregisters strategy successfully`() = runTest {
        framework.start()

        val strategyId = "memory_optimization" // Default strategy
        val result = framework.unregisterStrategy(strategyId)

        assertTrue("Strategy unregistration should succeed", result.isSuccess)
    }

    @Test
    fun `optimization context triggers optimization correctly`() {
        val context = OptimizationContext(
            memoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(
                usedMemory = 900, // 90% of 1000 max
                maxMemory = 1000
            )
        )

        assertTrue("Should trigger optimization for high memory usage",
            context.shouldTriggerOptimization())
    }

    @Test
    fun `optimization context calculates performance score correctly`() {
        val context = OptimizationContext(
            memoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(
                usedMemory = 500, // 50% of 1000 max
                maxMemory = 1000
            ),
            batteryMetrics = com.roshni.games.core.utils.performance.BatteryMetrics(
                level = 80f, // Good battery level
                temperature = 30f // Normal temperature
            )
        )

        val score = context.getPerformanceScore()
        assertTrue("Performance score should be high for good conditions", score > 80)
    }

    @Test
    fun `optimization context determines priority correctly`() {
        val context = OptimizationContext(
            memoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(
                usedMemory = 950, // 95% of 1000 max
                maxMemory = 1000
            )
        )

        val priority = context.getOptimizationPriority()
        assertEquals("Should be CRITICAL priority for high memory usage",
            OptimizationPriority.CRITICAL, priority)
    }

    @Test
    fun `optimization result calculates improvement correctly`() {
        val result = OptimizationResult(
            strategyId = "test",
            success = true,
            message = "Test",
            timestamp = System.currentTimeMillis(),
            metricsBefore = OptimizationMetrics(memoryUsage = 1000, performanceScore = 60),
            metricsAfter = OptimizationMetrics(memoryUsage = 800, performanceScore = 75)
        )

        val improvement = result.getImprovement()
        assertTrue("Memory improvement should be negative (good)", improvement.memoryImprovement < 0)
        assertTrue("Performance improvement should be positive (good)", improvement.performanceImprovement > 0)
        assertTrue("Should be effective", result.isEffective())
    }

    @Test
    fun `adaptation result detects changes correctly`() {
        val originalConstraints = ResourceConstraints(maxMemoryUsage = 0.8f)
        val newConstraints = ResourceConstraints(maxMemoryUsage = 0.6f)

        val result = AdaptationResult(
            originalConstraints = originalConstraints,
            newConstraints = newConstraints,
            adaptationActions = listOf(
                AdaptationAction(
                    type = AdaptationActionType.MEMORY_OPTIMIZATION,
                    description = "Reduced memory usage",
                    priority = AdaptationPriority.HIGH
                )
            ),
            timestamp = System.currentTimeMillis()
        )

        assertTrue("Should detect adaptation was needed", result.isAdaptationNeeded())
        assertEquals("Should be MINOR severity", AdaptationSeverity.MINOR, result.getAdaptationSeverity())
    }

    private fun setupDefaultMocks() {
        // Setup default memory metrics
        val defaultMemoryMetrics = com.roshni.games.core.utils.performance.MemoryMetrics(
            usedMemory = 100,
            maxMemory = 1000,
            availableMemory = 900
        )
        whenever(mockPerformanceMonitor.getCurrentMemoryMetrics()).thenReturn(defaultMemoryMetrics)

        // Setup default battery metrics
        val defaultBatteryMetrics = com.roshni.games.core.utils.performance.BatteryMetrics(
            level = 80f,
            temperature = 30f,
            isCharging = false
        )
        whenever(mockPerformanceMonitor.getCurrentBatteryMetrics()).thenReturn(defaultBatteryMetrics)

        // Setup battery optimizer
        whenever(mockBatteryOptimizer.optimizationMode).thenReturn(OptimizationMode.BALANCED)
    }
}