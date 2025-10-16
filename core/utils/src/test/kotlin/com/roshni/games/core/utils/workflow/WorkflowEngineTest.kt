package com.roshni.games.core.utils.workflow

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkflowEngineTest {

    private lateinit var workflowEngine: WorkflowEngine

    @Before
    fun setup() {
        workflowEngine = WorkflowEngineImpl.getInstance()
    }

    @Test
    fun testWorkflowRegistration() = runTest {
        val workflow = OnboardingWorkflow()

        val registered = workflowEngine.registerWorkflow(workflow)
        assertTrue(registered)

        val retrieved = workflowEngine.getWorkflow(workflow.id)
        assertNotNull(retrieved)
        assertEquals(workflow.id, retrieved.id)
    }

    @Test
    fun testWorkflowExecution() = runTest {
        val workflow = OnboardingWorkflow()
        workflowEngine.registerWorkflow(workflow)

        val context = workflow.createContext(
            userId = "test_user",
            initialVariables = mapOf("test_mode" to true)
        )

        val executionId = workflowEngine.startWorkflow(workflow.id, context)

        // Wait a bit for execution to complete
        kotlinx.coroutines.delay(1000)

        val result = workflowEngine.getWorkflowResult(executionId)
        assertNotNull(result)
        assertTrue(result.isSuccessful())
    }

    @Test
    fun testWorkflowValidation() = runTest {
        val workflow = OnboardingWorkflow()
        val validationResult = workflowEngine.validateAllWorkflows()

        assertTrue(validationResult.isValid)
    }

    @Test
    fun testEngineStatus() = runTest {
        val status = workflowEngine.getEngineStatus()

        assertTrue(status.isRunning)
        assertTrue(status.registeredWorkflowCount >= 0)
        assertTrue(status.activeExecutionCount >= 0)
    }
}