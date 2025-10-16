package com.roshni.games.core.utils.workflow

/**
 * Workflow for handling in-app purchase process
 */
class PurchaseWorkflow : BaseWorkflow() {

    override val id = "purchase_workflow"
    override val name = "In-App Purchase"
    override val description = "Manages the complete in-app purchase flow from selection to completion"
    override val category = "monetization"
    override val version = 1
    override val enabled = true
    override val tags = listOf("purchase", "payment", "monetization")
    override val defaultTimeoutMs = 600000L // 10 minutes
    override val createdAt = System.currentTimeMillis()
    override val modifiedAt = System.currentTimeMillis()

    override val workflowStates = listOf(
        WorkflowState(
            id = "item_selection",
            name = "Item Selection",
            description = "User selects item to purchase",
            type = WorkflowState.StateType.INITIAL,
            isInitial = true,
            entryActions = listOf(
                WorkflowAction.LogEvent(
                    id = "log_purchase_start",
                    name = "Log Purchase Start",
                    message = "User started purchase workflow",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowState(
            id = "payment_method",
            name = "Payment Method",
            description = "Select or confirm payment method",
            entryActions = listOf(
                WorkflowAction.SendNotification(
                    id = "show_payment_options",
                    name = "Show Payment Options",
                    title = "Payment Method",
                    message = "Please select your payment method",
                    type = WorkflowAction.SendNotification.NotificationType.INFO
                )
            )
        ),
        WorkflowState(
            id = "purchase_validation",
            name = "Purchase Validation",
            description = "Validate purchase details and user permissions",
            entryActions = listOf(
                WorkflowAction.ExecuteRule(
                    id = "validate_purchase",
                    name = "Validate Purchase",
                    ruleId = "purchase_permission_rule"
                )
            )
        ),
        WorkflowState(
            id = "payment_processing",
            name = "Payment Processing",
            description = "Process the payment with payment provider",
            entryActions = listOf(
                WorkflowAction.UpdateVariables(
                    id = "set_processing_status",
                    name = "Set Processing Status",
                    updates = mapOf("payment_status" to "processing")
                ),
                WorkflowAction.LogEvent(
                    id = "log_payment_processing",
                    name = "Log Payment Processing",
                    message = "Processing payment",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowState(
            id = "payment_verification",
            name = "Payment Verification",
            description = "Verify payment completion and receipt",
            entryActions = listOf(
                WorkflowAction.Wait(
                    id = "wait_verification",
                    name = "Wait for Verification",
                    durationMs = 5000 // Wait for payment verification
                )
            )
        ),
        WorkflowState(
            id = "delivery",
            name = "Content Delivery",
            description = "Deliver purchased content to user",
            entryActions = listOf(
                WorkflowAction.UpdateVariables(
                    id = "update_inventory",
                    name = "Update User Inventory",
                    updates = mapOf("inventory_updated" to true)
                ),
                WorkflowAction.LogEvent(
                    id = "log_content_delivery",
                    name = "Log Content Delivery",
                    message = "Delivering purchased content",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        ),
        WorkflowState(
            id = "purchase_complete",
            name = "Purchase Complete",
            description = "Purchase completed successfully",
            type = WorkflowState.StateType.TERMINAL,
            isTerminal = true,
            entryActions = listOf(
                WorkflowAction.SendNotification(
                    id = "purchase_success",
                    name = "Purchase Success",
                    title = "Purchase Successful!",
                    message = "Thank you for your purchase. Enjoy your new content!",
                    type = WorkflowAction.SendNotification.NotificationType.SUCCESS
                ),
                WorkflowAction.UpdateVariables(
                    id = "finalize_purchase",
                    name = "Finalize Purchase",
                    updates = mapOf(
                        "purchase_completed" to true,
                        "purchase_completed_at" to System.currentTimeMillis()
                    )
                )
            )
        ),
        WorkflowState(
            id = "payment_failed",
            name = "Payment Failed",
            description = "Payment processing failed",
            type = WorkflowState.StateType.TERMINAL,
            isTerminal = true,
            entryActions = listOf(
                WorkflowAction.SendNotification(
                    id = "payment_failed_notification",
                    name = "Payment Failed",
                    title = "Payment Failed",
                    message = "We couldn't process your payment. Please try again.",
                    type = WorkflowAction.SendNotification.NotificationType.ERROR
                ),
                WorkflowAction.LogEvent(
                    id = "log_payment_failure",
                    name = "Log Payment Failure",
                    message = "Payment processing failed",
                    level = WorkflowAction.LogEvent.LogLevel.ERROR
                )
            )
        ),
        WorkflowState(
            id = "purchase_cancelled",
            name = "Purchase Cancelled",
            description = "User cancelled the purchase",
            type = WorkflowState.StateType.TERMINAL,
            isTerminal = true,
            entryActions = listOf(
                WorkflowAction.LogEvent(
                    id = "log_purchase_cancelled",
                    name = "Log Purchase Cancelled",
                    message = "User cancelled purchase",
                    level = WorkflowAction.LogEvent.LogLevel.INFO
                )
            )
        )
    )

    override val workflowTransitions = listOf(
        WorkflowTransition(
            id = "selection_to_payment_method",
            name = "Selection to Payment Method",
            description = "Transition after item selection",
            fromState = workflowStates[0], // item_selection
            toState = workflowStates[1],   // payment_method
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<String>("selected_item_id") != null
                },
                description = "Item must be selected"
            )
        ),
        WorkflowTransition(
            id = "payment_method_to_validation",
            name = "Payment Method to Validation",
            description = "Transition after payment method selection",
            fromState = workflowStates[1], // payment_method
            toState = workflowStates[2],   // purchase_validation
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<String>("payment_method") != null
                },
                description = "Payment method must be selected"
            )
        ),
        WorkflowTransition(
            id = "validation_to_processing",
            name = "Validation to Processing",
            description = "Transition after successful validation",
            fromState = workflowStates[2], // purchase_validation
            toState = workflowStates[3],   // payment_processing
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<Boolean>("validation_passed") == true
                },
                description = "Purchase validation must pass"
            )
        ),
        WorkflowTransition(
            id = "validation_to_failed",
            name = "Validation to Failed",
            description = "Transition if validation fails",
            fromState = workflowStates[2], // purchase_validation
            toState = workflowStates[7],   // payment_failed
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<Boolean>("validation_passed") == false
                },
                description = "Purchase validation failed"
            )
        ),
        WorkflowTransition(
            id = "processing_to_verification",
            name = "Processing to Verification",
            description = "Transition after payment processing",
            fromState = workflowStates[3], // payment_processing
            toState = workflowStates[4],   // payment_verification
            condition = WorkflowTransition.TransitionCondition.AlwaysTrue("Auto transition after processing")
        ),
        WorkflowTransition(
            id = "verification_to_delivery",
            name = "Verification to Delivery",
            description = "Transition after successful payment verification",
            fromState = workflowStates[4], // payment_verification
            toState = workflowStates[5],   // delivery
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<Boolean>("payment_verified") == true
                },
                description = "Payment must be verified"
            )
        ),
        WorkflowTransition(
            id = "verification_to_failed",
            name = "Verification to Failed",
            description = "Transition if payment verification fails",
            fromState = workflowStates[4], // payment_verification
            toState = workflowStates[7],   // payment_failed
            condition = WorkflowTransition.TransitionCondition.CustomCondition(
                evaluator = { context ->
                    context.getVariable<Boolean>("payment_verified") == false
                },
                description = "Payment verification failed"
            )
        ),
        WorkflowTransition(
            id = "delivery_to_complete",
            name = "Delivery to Complete",
            description = "Transition after successful content delivery",
            fromState = workflowStates[5], // delivery
            toState = workflowStates[6],   // purchase_complete
            condition = WorkflowTransition.TransitionCondition.AlwaysTrue("Auto transition after delivery")
        ),
        // Cancellation transitions
        WorkflowTransition(
            id = "selection_to_cancelled",
            name = "Selection to Cancelled",
            description = "Cancel from item selection",
            fromState = workflowStates[0], // item_selection
            toState = workflowStates[8],   // purchase_cancelled
            condition = WorkflowTransition.TransitionCondition.EventBased(
                requiredEvents = listOf("purchase_cancelled"),
                description = "Cancellation event received"
            )
        ),
        WorkflowTransition(
            id = "payment_method_to_cancelled",
            name = "Payment Method to Cancelled",
            description = "Cancel from payment method selection",
            fromState = workflowStates[1], // payment_method
            toState = workflowStates[8],   // purchase_cancelled
            condition = WorkflowTransition.TransitionCondition.EventBased(
                requiredEvents = listOf("purchase_cancelled"),
                description = "Cancellation event received"
            )
        ),
        WorkflowTransition(
            id = "processing_to_cancelled",
            name = "Processing to Cancelled",
            description = "Cancel during payment processing",
            fromState = workflowStates[3], // payment_processing
            toState = workflowStates[8],   // purchase_cancelled
            condition = WorkflowTransition.TransitionCondition.EventBased(
                requiredEvents = listOf("purchase_cancelled"),
                description = "Cancellation event received"
            )
        )
    )

    override val workflowGlobalActions = listOf(
        WorkflowAction.LogEvent(
            id = "log_purchase_workflow",
            name = "Log Purchase Workflow",
            message = "Purchase workflow event",
            level = WorkflowAction.LogEvent.LogLevel.DEBUG
        )
    )

    override suspend fun canExecute(context: WorkflowContext): Boolean {
        // Check if user is allowed to make purchases
        val ruleContext = context.toRuleContext()
        if (ruleContext != null) {
            // TODO: Integrate with RuleEngine to check purchase permissions
            // For now, assume any user can attempt purchases
        }

        return super.canExecute(context) &&
               context.getVariable<String>("selected_item_id") != null
    }

    override fun getEstimatedExecutionTimeMs(): Long {
        return 120000L // 2 minutes estimated for purchase flow
    }
}