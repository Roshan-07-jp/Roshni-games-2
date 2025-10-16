# Workflow Engine

A comprehensive state machine-based workflow engine for managing complex business processes in the Roshni Games platform.

## Overview

The Workflow Engine provides a robust framework for defining, executing, and managing workflows using finite state machines. It supports:

- **State Machine Logic**: Define workflows as collections of states and transitions
- **Conditional Transitions**: Use rule-based conditions for dynamic workflow paths
- **Event-Driven Execution**: Respond to events and trigger state transitions
- **Async Processing**: Non-blocking workflow execution with coroutines
- **Lifecycle Management**: Complete workflow lifecycle from registration to completion
- **Integration**: Seamless integration with the existing RuleEngine

## Core Components

### WorkflowEngine
The main interface for workflow operations:
- Register and manage workflows
- Start, pause, resume, and cancel executions
- Monitor execution status and results
- Collect statistics and metrics

### Workflow
Interface representing a complete workflow definition:
- States and transitions
- Entry/exit actions
- Validation and metadata

### WorkflowState
Represents a state in the workflow state machine:
- State types (initial, terminal, error, decision)
- Entry and exit actions
- Timeout configuration

### WorkflowTransition
Defines transitions between states:
- Transition conditions (always, rule-based, time-based, event-based)
- Priority-based execution
- Transition actions

### WorkflowContext
Execution context containing:
- Workflow variables and state
- User and session information
- Event tracking
- Rule engine integration

## Predefined Workflows

### OnboardingWorkflow
Guides new users through the initial app setup:
- Welcome screen
- Profile setup
- Game tutorial
- Preferences configuration

### GameSessionWorkflow
Manages complete game session lifecycle:
- Session initialization
- Game loading
- Active gameplay
- Pause/resume handling
- Game over processing
- Session cleanup

### PurchaseWorkflow
Handles in-app purchase flow:
- Item selection
- Payment method selection
- Purchase validation
- Payment processing
- Content delivery
- Error handling and cancellation

## Usage Examples

### Basic Workflow Execution

```kotlin
// Get workflow service instance
val workflowService: WorkflowService = get()

// Start onboarding workflow
val executionId = workflowService.startOnboardingWorkflow(
    userId = "user123",
    ruleContext = ruleContext
)

// Monitor execution status
val status = workflowService.getWorkflowStatus(executionId)
when (status?.status) {
    WorkflowStatus.COMPLETED -> {
        println("Onboarding completed successfully")
    }
    WorkflowStatus.FAILED -> {
        println("Onboarding failed: ${status.error}")
    }
    else -> {
        println("Onboarding in progress")
    }
}
```

### Custom Workflow Definition

```kotlin
class CustomWorkflow : BaseWorkflow() {
    override val id = "custom_workflow"
    override val name = "Custom Process"
    override val description = "Example custom workflow"

    override val workflowStates = listOf(
        WorkflowState(
            id = "start",
            name = "Start",
            type = WorkflowState.StateType.INITIAL,
            isInitial = true
        ),
        WorkflowState(
            id = "complete",
            name = "Complete",
            type = WorkflowState.StateType.TERMINAL,
            isTerminal = true
        )
    )

    override val workflowTransitions = listOf(
        WorkflowTransition(
            id = "start_to_complete",
            name = "Start to Complete",
            fromState = workflowStates[0],
            toState = workflowStates[1],
            condition = TransitionCondition.AlwaysTrue()
        )
    )
}
```

### Event-Driven Transitions

```kotlin
// Send events to control workflow execution
workflowService.sendEventToWorkflow(executionId, "pause_requested")
workflowService.sendEventToWorkflow(executionId, "resume_requested")
workflowService.sendEventToWorkflow(executionId, "purchase_cancelled")
```

### Variable Management

```kotlin
// Update workflow variables
workflowService.updateWorkflowVariables(
    executionId,
    mapOf(
        "score" to 1500,
        "level" to 5,
        "lives" to 2
    )
)
```

## Integration with RuleEngine

The workflow engine integrates with the existing RuleEngine for conditional transitions:

```kotlin
// Rule-based transition condition
WorkflowTransition(
    id = "rule_based_transition",
    fromState = state1,
    toState = state2,
    condition = TransitionCondition.RuleBased(
        ruleId = "level_completion_rule",
        description = "Check if level is completed"
    )
)
```

## Architecture Patterns

### State Machine Design
- **Finite State Machine**: Each workflow is a finite state machine
- **State Types**: Initial, normal, decision, terminal, and error states
- **Transition Conditions**: Multiple condition types for flexible logic

### Event-Driven Architecture
- **Event Channels**: Asynchronous event processing
- **Observer Pattern**: Reactive status and result monitoring
- **Non-blocking Execution**: Coroutine-based async processing

### Dependency Injection
- **Koin Integration**: Full DI support with Koin module
- **Service Layer**: High-level WorkflowService API
- **Testable Design**: Easy mocking and testing

## Error Handling

The workflow engine provides comprehensive error handling:

- **State Timeouts**: Automatic timeout handling for stuck states
- **Transition Failures**: Graceful handling of failed transitions
- **Validation Errors**: Workflow and state validation
- **Execution Errors**: Runtime error capture and reporting

## Monitoring and Analytics

- **Execution Statistics**: Performance metrics and success rates
- **Status Monitoring**: Real-time execution status
- **Event Logging**: Comprehensive audit trail
- **Progress Tracking**: Execution progress calculation

## Best Practices

1. **Workflow Design**:
   - Keep workflows focused on single responsibilities
   - Use descriptive state and transition names
   - Include proper error states and handling

2. **State Management**:
   - Use appropriate state types (initial, terminal, decision)
   - Configure reasonable timeouts for states
   - Include entry and exit actions for state cleanup

3. **Transition Logic**:
   - Use priority levels for competing transitions
   - Implement proper condition checking
   - Handle rollback scenarios where needed

4. **Error Handling**:
   - Always include error states in workflows
   - Implement proper cleanup in exit actions
   - Log errors for debugging and monitoring

5. **Performance**:
   - Use async actions for long-running operations
   - Implement proper timeout handling
   - Monitor workflow statistics regularly

## Testing

The workflow engine supports comprehensive testing:

```kotlin
@Test
fun testWorkflowExecution() {
    // Test workflow registration
    val workflow = OnboardingWorkflow()
    assertTrue(workflowEngine.registerWorkflow(workflow))

    // Test workflow execution
    val executionId = workflowService.startOnboardingWorkflow("test_user")

    // Verify execution status
    val status = workflowService.getWorkflowStatus(executionId)
    assertNotNull(status)
    assertEquals(WorkflowStatus.COMPLETED, status.status)
}
```

## Future Enhancements

- **Visual Workflow Designer**: Drag-and-drop workflow creation
- **Workflow Templates**: Reusable workflow patterns
- **Advanced Conditions**: Machine learning-based conditions
- **Workflow Analytics**: Detailed execution analytics
- **Workflow Versioning**: Workflow evolution and migration