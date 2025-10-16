package com.roshni.games.core.utils.integration

/**
 * Defines the types of integration patterns available in the system
 */
enum class IntegrationType {
    /**
     * Direct feature-to-feature communication
     */
    DIRECT,

    /**
     * Event-driven integration through the hub
     */
    EVENT_DRIVEN,

    /**
     * Data flow integration with transformation
     */
    DATA_FLOW,

    /**
     * State synchronization integration
     */
    STATE_SYNC,

    /**
     * Workflow-based integration
     */
    WORKFLOW,

    /**
     * Rule-based integration
     */
    RULE_BASED,

    /**
     * Service-oriented integration
     */
    SERVICE_ORIENTED,

    /**
     * Plugin-based integration
     */
    PLUGIN
}

/**
 * Priority levels for integration events and operations
 */
enum class EventPriority(val level: Int) {
    /**
     * Critical events that must be processed immediately
     */
    CRITICAL(100),

    /**
     * High priority events for important operations
     */
    HIGH(75),

    /**
     * Normal priority for standard operations
     */
    NORMAL(50),

    /**
     * Low priority for background operations
     */
    LOW(25),

    /**
     * Background priority for non-essential operations
     */
    BACKGROUND(10)
}

/**
 * Data flow direction for integration
 */
enum class DataFlowDirection {
    /**
     * One-way data flow from source to target
     */
    UNIDIRECTIONAL,

    /**
     * Two-way data flow between components
     */
    BIDIRECTIONAL,

    /**
     * Broadcast data flow to multiple targets
     */
    BROADCAST,

    /**
     * Aggregated data flow from multiple sources
     */
    AGGREGATED
}

/**
 * Integration state for tracking component status
 */
enum class IntegrationState {
    /**
     * Component is not initialized
     */
    UNINITIALIZED,

    /**
     * Component is initializing
     */
    INITIALIZING,

    /**
     * Component is ready for operation
     */
    READY,

    /**
     * Component is processing
     */
    PROCESSING,

    /**
     * Component encountered an error
     */
    ERROR,

    /**
     * Component is shutting down
     */
    SHUTTING_DOWN,

    /**
     * Component is shut down
     */
    SHUTDOWN
}