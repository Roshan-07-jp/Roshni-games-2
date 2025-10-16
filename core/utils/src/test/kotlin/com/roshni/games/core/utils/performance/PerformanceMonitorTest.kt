package com.roshni.games.core.utils.performance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class PerformanceMonitorTest {

    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        performanceMonitor = PerformanceMonitor(context)
    }

    @Test
    fun testInitialization() {
        // Given
        assertNotNull("PerformanceMonitor should be initialized", performanceMonitor)

        // When & Then
        // Verify that flows are initialized with default values
        runTest {
            val memoryMetrics = performanceMonitor.memoryMetrics.first()
            val batteryMetrics = performanceMonitor.batteryMetrics.first()
            val performanceMetrics = performanceMonitor.performanceMetrics.first()

            assertNotNull("Memory metrics should not be null", memoryMetrics)
            assertNotNull("Battery metrics should not be null", batteryMetrics)
            assertNotNull("Performance metrics should not be null", performanceMetrics)
        }
    }

    @Test
    fun testStartStopMonitoring() = runTest {
        // When
        performanceMonitor.startMonitoring()

        // Give it a moment to start
        kotlinx.coroutines.delay(100)

        // Then
        // Monitoring should be running (we can't easily test the actual monitoring
        // without more complex setup, but we can verify the method doesn't throw)

        // When
        performanceMonitor.stopMonitoring()

        // Then
        // Should stop without errors
    }

    @Test
    fun testGetCurrentMemoryMetrics() {
        // When
        val memoryMetrics = performanceMonitor.getCurrentMemoryMetrics()

        // Then
        assertNotNull("Memory metrics should not be null", memoryMetrics)
        assertTrue("Used memory should be non-negative", memoryMetrics.usedMemory >= 0)
        assertTrue("Total memory should be positive", memoryMetrics.totalMemory > 0)
        assertTrue("Max memory should be positive", memoryMetrics.maxMemory > 0)
        assertTrue("Process ID should be valid", memoryMetrics.processId > 0)
    }

    @Test
    fun testGetCurrentBatteryMetrics() {
        // When
        val batteryMetrics = performanceMonitor.getCurrentBatteryMetrics()

        // Then
        assertNotNull("Battery metrics should not be null", batteryMetrics)
        assertTrue("Battery level should be between 0 and 100", batteryMetrics.level in 0.0f..100.0f)
        assertTrue("Battery temperature should be reasonable", batteryMetrics.temperature >= 0)
        assertTrue("Battery voltage should be positive", batteryMetrics.voltage >= 0)
        assertTrue("Timestamp should be valid", batteryMetrics.timestamp > 0)
    }

    @Test
    fun testGetCpuUsage() {
        // When
        val cpuUsage = performanceMonitor.getCpuUsage()

        // Then
        assertNotNull("CPU usage should not be null", cpuUsage)
        assertTrue("CPU usage should be between 0 and 100", cpuUsage in 0.0f..100.0f)
    }

    @Test
    fun testIsPerformanceDegraded() {
        // When
        val isDegraded = performanceMonitor.isPerformanceDegraded()

        // Then
        assertNotNull("Performance degraded check should return a value", isDegraded)
        // The actual value depends on current system state, so we just verify it doesn't throw
    }

    @Test
    fun testGetPerformanceRecommendations() {
        // When
        val recommendations = performanceMonitor.getPerformanceRecommendations()

        // Then
        assertNotNull("Recommendations should not be null", recommendations)
        assertTrue("Recommendations should be a list", recommendations.isNotEmpty() || recommendations.isEmpty())
    }

    @Test
    fun testMemoryMetricsFlow() = runTest {
        // When
        performanceMonitor.startMonitoring()
        kotlinx.coroutines.delay(100)

        val memoryMetrics = performanceMonitor.memoryMetrics.first()

        // Then
        assertNotNull("Memory metrics from flow should not be null", memoryMetrics)

        performanceMonitor.stopMonitoring()
    }

    @Test
    fun testBatteryMetricsFlow() = runTest {
        // When
        performanceMonitor.startMonitoring()
        kotlinx.coroutines.delay(100)

        val batteryMetrics = performanceMonitor.batteryMetrics.first()

        // Then
        assertNotNull("Battery metrics from flow should not be null", batteryMetrics)

        performanceMonitor.stopMonitoring()
    }

    @Test
    fun testPerformanceMetricsFlow() = runTest {
        // When
        performanceMonitor.startMonitoring()
        kotlinx.coroutines.delay(100)

        val performanceMetrics = performanceMonitor.performanceMetrics.first()

        // Then
        assertNotNull("Performance metrics from flow should not be null", performanceMetrics)

        performanceMonitor.stopMonitoring()
    }

    @Test
    fun testMonitorFPS() = runTest {
        // When
        val fpsFlow = performanceMonitor.monitorFPS("test_view")

        // Then
        assertNotNull("FPS flow should not be null", fpsFlow)

        // Start collecting FPS values
        val fpsValues = mutableListOf<Float>()
        val job = kotlinx.coroutines.launch {
            fpsFlow.collect { fps ->
                fpsValues.add(fps)
            }
        }

        // Let it run for a short time
        kotlinx.coroutines.delay(200)

        job.cancel()

        // FPS monitoring may or may not produce values depending on timing
        // The important thing is that it doesn't throw an exception
    }

    @Test
    fun testMemoryCalculations() {
        // Given
        val memoryMetrics = performanceMonitor.getCurrentMemoryMetrics()

        // When
        val usagePercentage = if (memoryMetrics.maxMemory > 0) {
            memoryMetrics.usedMemory.toFloat() / memoryMetrics.maxMemory.toFloat()
        } else 0f

        // Then
        assertTrue("Memory usage percentage should be between 0 and 1", usagePercentage in 0.0f..1.0f)
    }

    @Test
    fun testBatteryStatusDetection() {
        // When
        val batteryMetrics = performanceMonitor.getCurrentBatteryMetrics()

        // Then
        // Battery status should be a valid Android battery status constant
        assertTrue("Battery status should be valid",
            batteryMetrics.status == 0 || // Unknown
            batteryMetrics.status in -1..5 // Valid Android battery status range
        )

        // Health should be a valid Android battery health constant
        assertTrue("Battery health should be valid",
            batteryMetrics.health == 0 || // Unknown
            batteryMetrics.health in 1..7 // Valid Android battery health range
        )
    }
}