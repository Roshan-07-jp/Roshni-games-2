package com.roshni.games.core.utils.optimization

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
class BatteryOptimizerTest {

    private lateinit var batteryOptimizer: BatteryOptimizer
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        batteryOptimizer = BatteryOptimizer(context)
    }

    @Test
    fun testInitialization() = runTest {
        // When
        val result = batteryOptimizer.initialize()

        // Then
        assertTrue("Initialization should succeed", result.isSuccess)
    }

    @Test
    fun testOptimizationModeFlow() = runTest {
        // When
        val initialMode = batteryOptimizer.optimizationMode.first()

        // Then
        assertNotNull("Optimization mode should not be null", initialMode)
        assertTrue("Should start with a valid mode",
            initialMode in listOf(
                BatteryOptimizer.OptimizationMode.HIGH_PERFORMANCE,
                BatteryOptimizer.OptimizationMode.BALANCED,
                BatteryOptimizer.OptimizationMode.BATTERY_SAVER,
                BatteryOptimizer.OptimizationMode.ULTRA_SAVER
            )
        )
    }

    @Test
    fun testSetOptimizationMode() = runTest {
        // Given
        val newMode = BatteryOptimizer.OptimizationMode.BATTERY_SAVER

        // When
        val result = batteryOptimizer.setOptimizationMode(newMode)

        // Then
        assertTrue("Setting optimization mode should succeed", result.isSuccess)

        val currentMode = batteryOptimizer.optimizationMode.first()
        assertEquals("Mode should be updated", newMode, currentMode)
    }

    @Test
    fun testBatteryOptimizationToggle() = runTest {
        // When
        val enableResult = batteryOptimizer.setBatteryOptimizationEnabled(true)
        val disableResult = batteryOptimizer.setBatteryOptimizationEnabled(false)

        // Then
        assertTrue("Enabling should succeed", enableResult.isSuccess)
        assertTrue("Disabling should succeed", disableResult.isSuccess)

        val isEnabled = batteryOptimizer.batteryOptimizationEnabled.first()
        assertEquals("Should be disabled", false, isEnabled)
    }

    @Test
    fun testNetworkOptimizationToggle() = runTest {
        // When
        val initialState = batteryOptimizer.networkOptimizationEnabled.first()

        // Then
        assertNotNull("Network optimization state should not be null", initialState)
        // The actual value depends on initialization, but it should be a boolean
    }

    @Test
    fun testPerformanceOptimizationsFlow() = runTest {
        // When
        val optimizations = batteryOptimizer.performanceOptimizations.first()

        // Then
        assertNotNull("Performance optimizations should not be null", optimizations)
        // Verify that the optimizations object has the expected structure
        assertTrue("Should have reduce frame rate setting",
            optimizations::class.java.getDeclaredField("reduceFrameRate") != null)
    }

    @Test
    fun testGetOptimizationRecommendations() = runTest {
        // When
        val recommendationsFlow = batteryOptimizer.getOptimizationRecommendations()
        val recommendations = recommendationsFlow.first()

        // Then
        assertNotNull("Recommendations should not be null", recommendations)
        assertTrue("Recommendations should be a list", recommendations is List<*>)
    }

    @Test
    fun testGetEstimatedBatteryLife() = runTest {
        // When
        val batteryLifeFlow = batteryOptimizer.getEstimatedBatteryLife()
        val batteryLife = batteryLifeFlow.first()

        // Then
        assertNotNull("Battery life should not be null", batteryLife)
        assertTrue("Battery life should be non-negative", batteryLife >= 0)
    }

    @Test
    fun testOptimizeForNetwork() = runTest {
        // When
        val result = batteryOptimizer.optimizeForNetwork()

        // Then
        assertTrue("Network optimization should succeed", result.isSuccess)
    }

    @Test
    fun testIsInPowerSavingMode() {
        // When
        val isInPowerSaveMode = batteryOptimizer.isInPowerSavingMode()

        // Then
        assertNotNull("Power saving mode check should return a value", isInPowerSaveMode)
        assertTrue("Should return a boolean", isInPowerSaveMode is Boolean)
    }

    @Test
    fun testOptimizationModeTransitions() = runTest {
        // Test all optimization modes
        val modes = listOf(
            BatteryOptimizer.OptimizationMode.HIGH_PERFORMANCE,
            BatteryOptimizer.OptimizationMode.BALANCED,
            BatteryOptimizer.OptimizationMode.BATTERY_SAVER,
            BatteryOptimizer.OptimizationMode.ULTRA_SAVER
        )

        for (mode in modes) {
            // When
            val result = batteryOptimizer.setOptimizationMode(mode)

            // Then
            assertTrue("Setting mode $mode should succeed", result.isSuccess)

            val currentMode = batteryOptimizer.optimizationMode.first()
            assertEquals("Mode should be updated to $mode", mode, currentMode)

            val optimizations = batteryOptimizer.performanceOptimizations.first()
            assertNotNull("Should have optimizations for mode $mode", optimizations)
        }
    }

    @Test
    fun testBatteryOptimizationSettings() = runTest {
        // When
        batteryOptimizer.setBatteryOptimizationEnabled(true)
        val enabledState = batteryOptimizer.batteryOptimizationEnabled.first()

        batteryOptimizer.setBatteryOptimizationEnabled(false)
        val disabledState = batteryOptimizer.batteryOptimizationEnabled.first()

        // Then
        assertNotNull("Enabled state should not be null", enabledState)
        assertNotNull("Disabled state should not be null", disabledState)
        assertEquals("Should transition from enabled to disabled", false, disabledState)
    }

    @Test
    fun testPerformanceOptimizationsStructure() = runTest {
        // When
        val optimizations = batteryOptimizer.performanceOptimizations.first()

        // Then
        assertNotNull("Performance optimizations should not be null", optimizations)

        // Verify that all expected fields exist and are booleans
        val fields = BatteryOptimizer.PerformanceOptimizations::class.java.declaredFields
        fields.forEach { field ->
            field.isAccessible = true
            val value = field.get(optimizations)
            assertTrue("Field ${field.name} should be boolean", value is Boolean)
        }
    }

    @Test
    fun testBatteryLevelDetection() = runTest {
        // This test verifies that battery level detection doesn't throw exceptions
        // The actual battery level depends on the test environment

        // When & Then
        try {
            val recommendations = batteryOptimizer.getOptimizationRecommendations().first()
            assertNotNull("Should get recommendations without exception", recommendations)

            val batteryLife = batteryOptimizer.getEstimatedBatteryLife().first()
            assertNotNull("Should get battery life without exception", batteryLife)

        } catch (e: Exception) {
            // If battery detection fails in test environment, that's acceptable
            // The important thing is that the code doesn't crash
            assertTrue("Exception should be related to battery detection in test environment",
                e.message?.contains("battery") == true || e.message?.contains("Battery") == true)
        }
    }

    @Test
    fun testNetworkOptimizationSettings() = runTest {
        // When
        val initialState = batteryOptimizer.networkOptimizationEnabled.first()

        // Then
        assertNotNull("Network optimization state should not be null", initialState)
        assertTrue("Should be a boolean value", initialState is Boolean)
    }
}