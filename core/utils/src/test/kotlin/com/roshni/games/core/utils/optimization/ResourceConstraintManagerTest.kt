package com.roshni.games.core.utils.optimization

import androidx.test.ext.junit.runners.AndroidJUnit4
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
class ResourceConstraintManagerTest {

    @Mock
    private lateinit var mockOptimizationFramework: PerformanceOptimizationFramework

    @Mock
    private lateinit var mockAdaptiveSystem: AdaptiveOptimizationSystem

    private lateinit var constraintManager: ResourceConstraintManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        setupDefaultMocks()

        constraintManager = ResourceConstraintManager(
            optimizationFramework = mockOptimizationFramework,
            adaptiveSystem = mockAdaptiveSystem
        )
    }

    @After
    fun tearDown() {
        // Cleanup if needed
    }

    @Test
    fun `constraint manager analyzes and provides recommendations`() = runTest {
        val recommendations = mutableListOf<List<ConstraintAdaptationRecommendation>>()
        constraintManager.analyzeAndRecommend().collect { recs ->
            recommendations.add(recs)
        }

        // Should eventually provide recommendations
        assertTrue("Should provide constraint recommendations", recommendations.isNotEmpty())
    }

    @Test
    fun `constraint manager applies adaptations successfully`() = runTest {
        val recommendations = listOf(
            ConstraintAdaptationRecommendation(
                constraintType = ConstraintType.MEMORY,
                currentValue = 0.8f,
                recommendedValue = 0.7f,
                priority = AdaptationPriority.HIGH,
                reason = "High memory usage",
                expectedImpact = "Reduce memory pressure",
                riskLevel = RiskLevel.MEDIUM,
                estimatedImprovement = 25f
            )
        )

        // Setup mock to return success
        val adaptationResult = AdaptationResult(
            originalConstraints = ResourceConstraints(maxMemoryUsage = 0.8f),
            newConstraints = ResourceConstraints(maxMemoryUsage = 0.7f),
            adaptationActions = listOf(
                AdaptationAction(
                    type = AdaptationActionType.MEMORY_OPTIMIZATION,
                    description = "Reduced memory usage limit",
                    priority = AdaptationPriority.HIGH
                )
            ),
            timestamp = System.currentTimeMillis()
        )
        whenever(mockOptimizationFramework.adaptToConstraints(ResourceConstraints(maxMemoryUsage = 0.7f)))
            .thenReturn(Result.success(adaptationResult))

        val result = constraintManager.applyAdaptations(recommendations)

        assertTrue("Adaptation application should succeed", result.isSuccess)
        val adaptations = result.getOrNull()
        assertNotNull("Adaptations should not be null", adaptations)
        assertTrue("Should have applied adaptations", adaptations?.isNotEmpty() == true)
    }

    @Test
    fun `constraint manager provides optimization suggestions`() = runTest {
        val suggestions = mutableListOf<List<ConstraintOptimizationSuggestion>>()
        constraintManager.getOptimizationSuggestions().collect { suggs ->
            suggestions.add(suggs)
        }

        // Should eventually provide suggestions
        assertTrue("Should provide optimization suggestions", suggestions.isNotEmpty())
    }

    @Test
    fun `constraint manager predicts resource needs correctly`() = runTest {
        val predictions = mutableListOf<ResourcePrediction>()
        constraintManager.predictResourceNeeds(24).collect { prediction ->
            predictions.add(prediction)
        }

        assertTrue("Should provide resource predictions", predictions.isNotEmpty())
        val prediction = predictions.first()
        assertEquals("Should predict for correct timeframe", 24, prediction.timeFrame)
        assertTrue("Should have reasonable confidence", prediction.confidence >= 0.0f)
        assertTrue("Should have reasonable confidence", prediction.confidence <= 1.0f)
    }

    @Test
    fun `constraint manager adds custom analyzers correctly`() {
        val customAnalyzer = object : ConstraintAnalyzer {
            override val name = "Custom Test Analyzer"
            override val description = "Custom analyzer for testing"

            override fun analyze(context: OptimizationContext): List<ConstraintAdaptationRecommendation> {
                return listOf(
                    ConstraintAdaptationRecommendation(
                        constraintType = ConstraintType.MEMORY,
                        currentValue = 0.8f,
                        recommendedValue = 0.6f,
                        priority = AdaptationPriority.MEDIUM,
                        reason = "Custom analysis triggered",
                        expectedImpact = "Custom optimization",
                        riskLevel = RiskLevel.LOW,
                        estimatedImprovement = 15f
                    )
                )
            }
        }

        constraintManager.addConstraintAnalyzer(customAnalyzer)

        // Verify analyzer was added (we can't directly test this without exposing internals,
        // but we can test that it doesn't throw an exception)
        assertDoesNotThrow("Adding analyzer should not throw exception") {
            constraintManager.addConstraintAnalyzer(customAnalyzer)
        }
    }

    @Test
    fun `constraint manager adds custom policies correctly`() {
        val customPolicy = object : AdaptationPolicy {
            override val name = "Custom Test Policy"
            override val description = "Custom policy for testing"

            override fun apply(recommendations: List<ConstraintAdaptationRecommendation>): List<ConstraintAdaptationRecommendation> {
                return recommendations.filter { it.priority == AdaptationPriority.CRITICAL }
            }
        }

        constraintManager.addAdaptationPolicy(customPolicy)

        // Verify policy was added (we can't directly test this without exposing internals,
        // but we can test that it doesn't throw an exception)
        assertDoesNotThrow("Adding policy should not throw exception") {
            constraintManager.addAdaptationPolicy(customPolicy)
        }
    }

    @Test
    fun `constraint adaptation recommendation has correct structure`() {
        val recommendation = ConstraintAdaptationRecommendation(
            constraintType = ConstraintType.MEMORY,
            currentValue = 0.8f,
            recommendedValue = 0.7f,
            priority = AdaptationPriority.HIGH,
            reason = "High memory usage detected",
            expectedImpact = "Reduce memory pressure",
            riskLevel = RiskLevel.MEDIUM,
            estimatedImprovement = 25f
        )

        assertEquals("Should have correct constraint type", ConstraintType.MEMORY, recommendation.constraintType)
        assertEquals("Should have correct priority", AdaptationPriority.HIGH, recommendation.priority)
        assertTrue("Recommended value should be lower", recommendation.recommendedValue < recommendation.currentValue)
        assertTrue("Should have positive improvement", recommendation.estimatedImprovement > 0)
        assertNotNull("Should have timestamp", recommendation.timestamp)
    }

    @Test
    fun `constraint snapshot records correctly`() {
        val constraints = ResourceConstraints(maxMemoryUsage = 0.7f)
        val recommendations = listOf(
            ConstraintAdaptationRecommendation(
                constraintType = ConstraintType.MEMORY,
                currentValue = 0.8f,
                recommendedValue = 0.7f,
                priority = AdaptationPriority.HIGH,
                reason = "Test recommendation",
                expectedImpact = "Test impact",
                riskLevel = RiskLevel.LOW,
                estimatedImprovement = 10f
            )
        )

        val snapshot = ConstraintSnapshot(
            constraints = constraints,
            appliedRecommendations = recommendations,
            timestamp = System.currentTimeMillis()
        )

        assertEquals("Should record correct constraints", constraints, snapshot.constraints)
        assertEquals("Should record correct recommendations", recommendations, snapshot.appliedRecommendations)
        assertNotNull("Should have timestamp", snapshot.timestamp)
    }

    @Test
    fun `resource prediction has correct structure`() {
        val constraints = ResourceConstraints(maxMemoryUsage = 0.7f)
        val prediction = ResourcePrediction(
            timeFrame = 24,
            predictedConstraints = constraints,
            confidence = 0.8f,
            basedOnSamples = 50
        )

        assertEquals("Should have correct timeframe", 24, prediction.timeFrame)
        assertEquals("Should have correct constraints", constraints, prediction.predictedConstraints)
        assertEquals("Should have correct confidence", 0.8f, prediction.confidence)
        assertEquals("Should have correct sample count", 50, prediction.basedOnSamples)
        assertNotNull("Should have prediction method", prediction.predictionMethod)
    }

    @Test
    fun `constraint optimization suggestion has correct structure`() {
        val suggestion = ConstraintOptimizationSuggestion(
            type = SuggestionType.PREVENTIVE,
            title = "Memory Optimization Suggestion",
            description = "Consider optimizing memory usage patterns",
            actions = listOf("Review memory allocation", "Implement pooling"),
            potentialBenefit = "Improved performance and stability"
        )

        assertEquals("Should have correct type", SuggestionType.PREVENTIVE, suggestion.type)
        assertEquals("Should have correct title", "Memory Optimization Suggestion", suggestion.title)
        assertTrue("Should have actions", suggestion.actions.isNotEmpty())
        assertNotNull("Should have potential benefit", suggestion.potentialBenefit)
    }

    @Test
    fun `constraint types are properly defined`() {
        assertEquals("Should have MEMORY type", ConstraintType.MEMORY, ConstraintType.valueOf("MEMORY"))
        assertEquals("Should have BATTERY type", ConstraintType.BATTERY, ConstraintType.valueOf("BATTERY"))
        assertEquals("Should have NETWORK type", ConstraintType.NETWORK, ConstraintType.valueOf("NETWORK"))
        assertEquals("Should have THERMAL type", ConstraintType.THERMAL, ConstraintType.valueOf("THERMAL"))
        assertEquals("Should have PERFORMANCE type", ConstraintType.PERFORMANCE, ConstraintType.valueOf("PERFORMANCE"))
    }

    @Test
    fun `risk levels are properly defined`() {
        assertEquals("Should have LOW risk", RiskLevel.LOW, RiskLevel.valueOf("LOW"))
        assertEquals("Should have MEDIUM risk", RiskLevel.MEDIUM, RiskLevel.valueOf("MEDIUM"))
        assertEquals("Should have HIGH risk", RiskLevel.HIGH, RiskLevel.valueOf("HIGH"))
    }

    private fun setupDefaultMocks() {
        val defaultContext = OptimizationContext()
        whenever(mockOptimizationFramework.optimizationContext).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(defaultContext)
        )
        whenever(mockOptimizationFramework.frameworkState).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(FrameworkState.ACTIVE)
        )
    }
}