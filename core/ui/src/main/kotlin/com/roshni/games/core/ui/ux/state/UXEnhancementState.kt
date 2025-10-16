package com.roshni.games.core.ui.ux.state

import com.roshni.games.core.ui.ux.model.EnhancedInteraction
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * State management for UX Enhancement system
 */
data class UXEnhancementState(
    val isInitialized: Boolean = false,
    val isProcessing: Boolean = false,
    val currentContext: UXContext? = null,
    val availableEnhancements: List<UXEnhancement> = emptyList(),
    val recentInteractions: List<EnhancedInteraction> = emptyList(),
    val userPreferences: UXContext.UserPreferences = UXContext.UserPreferences(),
    val deviceCapabilities: UXContext.DeviceCapabilities = UXContext.DeviceCapabilities(),
    val error: String? = null,
    val statistics: EnhancementStatistics = EnhancementStatistics()
) {

    /**
     * Check if system is ready for use
     */
    fun isReady(): Boolean = isInitialized && error == null

    /**
     * Get enhancements for a specific interaction type
     */
    fun getEnhancementsForInteractionType(
        interactionType: com.roshni.games.core.ui.ux.model.UserInteraction.InteractionType
    ): List<UXEnhancement> {
        return availableEnhancements.filter { enhancement ->
            // Filter enhancements that are relevant for this interaction type
            when (enhancement.type) {
                UXEnhancement.Type.VISUAL_FEEDBACK -> true
                UXEnhancement.Type.AUDIO_FEEDBACK -> userPreferences.soundEnabled
                UXEnhancement.Type.HAPTIC_FEEDBACK -> {
                    userPreferences.hapticFeedbackEnabled && deviceCapabilities.hasVibrator
                }
                UXEnhancement.Type.CONTEXTUAL_HELP -> true
                else -> true
            }
        }
    }

    /**
     * Check if accessibility enhancements are available
     */
    fun hasAccessibilityEnhancements(): Boolean {
        return availableEnhancements.any { enhancement ->
            when (enhancement.type) {
                UXEnhancement.Type.ACCESSIBILITY_AID -> true
                UXEnhancement.Type.VISUAL_FEEDBACK -> {
                    userPreferences.accessibilityProfile != UXContext.AccessibilityProfile.STANDARD
                }
                else -> false
            }
        }
    }
}

/**
 * Statistics for UX enhancement usage
 */
data class EnhancementStatistics(
    val totalInteractions: Long = 0,
    val totalEnhancementsApplied: Long = 0,
    val averageEnhancementsPerInteraction: Double = 0.0,
    val mostUsedEnhancementType: UXEnhancement.Type? = null,
    val userSatisfactionScore: Double = 0.0,
    val performanceScore: Double = 0.0,
    val accessibilityScore: Double = 0.0
)

/**
 * Actions for UX enhancement state management
 */
sealed class UXEnhancementAction {
    data object Initialize : UXEnhancementAction()
    data object Reset : UXEnhancementAction()
    data class UpdateContext(val context: UXContext) : UXEnhancementAction()
    data class ProcessInteraction(
        val interaction: com.roshni.games.core.ui.ux.model.UserInteraction
    ) : UXEnhancementAction()
    data class UpdateUserPreferences(
        val preferences: UXContext.UserPreferences
    ) : UXEnhancementAction()
    data class UpdateDeviceCapabilities(
        val capabilities: UXContext.DeviceCapabilities
    ) : UXEnhancementAction()
    data class RecordEnhancementFeedback(
        val enhancementId: String,
        val rating: Int,
        val helpful: Boolean
    ) : UXEnhancementAction()
    data class SetError(val error: String?) : UXEnhancementAction()
}

/**
 * ViewModel for UX Enhancement system
 */
class UXEnhancementViewModel(
    private val uxEnhancementService: com.roshni.games.core.ui.ux.di.UXEnhancementService
) {

    private val _state = MutableStateFlow(UXEnhancementState())
    val state: StateFlow<UXEnhancementState> = _state.asStateFlow()

    private val _actions = MutableStateFlow<UXEnhancementAction?>(null)
    val actions: Flow<UXEnhancementAction?> = _actions.asStateFlow()

    init {
        // Observe state changes and handle actions
        kotlinx.coroutines.GlobalScope.launch {
            _actions.collect { action ->
                action?.let { handleAction(it) }
            }
        }
    }

    /**
     * Dispatch an action to update state
     */
    fun dispatch(action: UXEnhancementAction) {
        _actions.value = action
    }

    /**
     * Initialize the UX enhancement system
     */
    fun initialize() {
        dispatch(UXEnhancementAction.Initialize)
    }

    /**
     * Reset the system
     */
    fun reset() {
        dispatch(UXEnhancementAction.Reset)
    }

    /**
     * Update current UX context
     */
    fun updateContext(context: UXContext) {
        dispatch(UXEnhancementAction.UpdateContext(context))
    }

    /**
     * Process a user interaction
     */
    suspend fun processInteraction(interaction: com.roshni.games.core.ui.ux.model.UserInteraction) {
        dispatch(UXEnhancementAction.ProcessInteraction(interaction))
    }

    /**
     * Update user preferences
     */
    fun updateUserPreferences(preferences: UXContext.UserPreferences) {
        dispatch(UXEnhancementAction.UpdateUserPreferences(preferences))
    }

    /**
     * Update device capabilities
     */
    fun updateDeviceCapabilities(capabilities: UXContext.DeviceCapabilities) {
        dispatch(UXEnhancementAction.UpdateDeviceCapabilities(capabilities))
    }

    /**
     * Record feedback for an enhancement
     */
    fun recordEnhancementFeedback(enhancementId: String, rating: Int, helpful: Boolean) {
        dispatch(UXEnhancementAction.RecordEnhancementFeedback(enhancementId, rating, helpful))
    }

    /**
     * Get personalized recommendations for current context
     */
    suspend fun getPersonalizedRecommendations(limit: Int = 10): List<UXEnhancement> {
        val currentContext = _state.value.currentContext ?: return emptyList()
        return uxEnhancementService.getPersonalizedRecommendations(currentContext, limit)
    }

    /**
     * Get enhancements for a specific interaction type
     */
    fun getEnhancementsForInteractionType(
        interactionType: com.roshni.games.core.ui.ux.model.UserInteraction.InteractionType
    ): List<UXEnhancement> {
        return _state.value.getEnhancementsForInteractionType(interactionType)
    }

    /**
     * Check if system has accessibility enhancements
     */
    fun hasAccessibilityEnhancements(): Boolean {
        return _state.value.hasAccessibilityEnhancements()
    }

    /**
     * Handle actions and update state
     */
    private suspend fun handleAction(action: UXEnhancementAction) {
        when (action) {
            is UXEnhancementAction.Initialize -> {
                handleInitialize()
            }
            is UXEnhancementAction.Reset -> {
                handleReset()
            }
            is UXEnhancementAction.UpdateContext -> {
                handleUpdateContext(action.context)
            }
            is UXEnhancementAction.ProcessInteraction -> {
                handleProcessInteraction(action.interaction)
            }
            is UXEnhancementAction.UpdateUserPreferences -> {
                handleUpdateUserPreferences(action.preferences)
            }
            is UXEnhancementAction.UpdateDeviceCapabilities -> {
                handleUpdateDeviceCapabilities(action.capabilities)
            }
            is UXEnhancementAction.RecordEnhancementFeedback -> {
                handleRecordEnhancementFeedback(action.enhancementId, action.rating, action.helpful)
            }
            is UXEnhancementAction.SetError -> {
                handleSetError(action.error)
            }
        }
    }

    private suspend fun handleInitialize() {
        try {
            _state.value = _state.value.copy(isProcessing = true, error = null)

            // Get current statistics
            val stats = uxEnhancementService.getStatistics()

            _state.value = _state.value.copy(
                isInitialized = true,
                isProcessing = false,
                statistics = _state.value.statistics.copy(
                    totalInteractions = stats["totalInteractions"] as? Long ?: 0,
                    totalEnhancementsApplied = stats["totalEnhancementsApplied"] as? Long ?: 0
                )
            )

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isProcessing = false,
                error = "Failed to initialize UX Enhancement system: ${e.message}"
            )
        }
    }

    private suspend fun handleReset() {
        _state.value = UXEnhancementState()
    }

    private suspend fun handleUpdateContext(context: UXContext) {
        try {
            _state.value = _state.value.copy(
                currentContext = context,
                error = null
            )

            // Update service with new context
            uxEnhancementService.updateUserContext(context)

            // Get available enhancements for new context
            val enhancements = uxEnhancementService.getPersonalizedRecommendations(context, 20)
            _state.value = _state.value.copy(availableEnhancements = enhancements)

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                error = "Failed to update context: ${e.message}"
            )
        }
    }

    private suspend fun handleProcessInteraction(interaction: com.roshni.games.core.ui.ux.model.UserInteraction) {
        try {
            _state.value = _state.value.copy(isProcessing = true, error = null)

            val currentContext = _state.value.currentContext
                ?: throw IllegalStateException("No UX context available")

            // Process interaction through service
            val enhancedInteraction = uxEnhancementService.processInteraction(interaction, currentContext)

            // Update state with new interaction
            val updatedInteractions = (_state.value.recentInteractions + enhancedInteraction)
                .takeLast(50) // Keep last 50 interactions

            _state.value = _state.value.copy(
                isProcessing = false,
                recentInteractions = updatedInteractions,
                statistics = _state.value.statistics.copy(
                    totalInteractions = _state.value.statistics.totalInteractions + 1,
                    totalEnhancementsApplied = _state.value.statistics.totalEnhancementsApplied +
                        enhancedInteraction.enhancements.size,
                    averageEnhancementsPerInteraction = if (_state.value.statistics.totalInteractions > 0) {
                        (_state.value.statistics.totalEnhancementsApplied + enhancedInteraction.enhancements.size).toDouble() /
                        (_state.value.statistics.totalInteractions + 1)
                    } else {
                        enhancedInteraction.enhancements.size.toDouble()
                    }
                )
            )

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isProcessing = false,
                error = "Failed to process interaction: ${e.message}"
            )
        }
    }

    private suspend fun handleUpdateUserPreferences(preferences: UXContext.UserPreferences) {
        _state.value = _state.value.copy(userPreferences = preferences)

        // Update context if available
        _state.value.currentContext?.let { context ->
            val updatedContext = context.copy(userPreferences = preferences)
            handleUpdateContext(updatedContext)
        }
    }

    private suspend fun handleUpdateDeviceCapabilities(capabilities: UXContext.DeviceCapabilities) {
        _state.value = _state.value.copy(deviceCapabilities = capabilities)

        // Update context if available
        _state.value.currentContext?.let { context ->
            val updatedContext = context.copy(deviceCapabilities = capabilities)
            handleUpdateContext(updatedContext)
        }
    }

    private suspend fun handleRecordEnhancementFeedback(
        enhancementId: String,
        rating: Int,
        helpful: Boolean
    ) {
        try {
            val feedback = com.roshni.games.core.ui.ux.di.UXEnhancementExtensions.createEnhancementFeedback(
                rating = rating,
                helpful = helpful
            )

            uxEnhancementService.recordFeedback(
                enhancementId = enhancementId,
                interactionId = "manual_feedback_${System.currentTimeMillis()}",
                feedback = feedback
            )

            // Update satisfaction score
            _state.value = _state.value.copy(
                statistics = _state.value.statistics.copy(
                    userSatisfactionScore = calculateUpdatedSatisfactionScore(rating, helpful)
                )
            )

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                error = "Failed to record feedback: ${e.message}"
            )
        }
    }

    private fun handleSetError(error: String?) {
        _state.value = _state.value.copy(error = error)
    }

    /**
     * Calculate updated satisfaction score
     */
    private fun calculateUpdatedSatisfactionScore(rating: Int, helpful: Boolean): Double {
        val currentScore = _state.value.statistics.userSatisfactionScore
        val currentCount = _state.value.statistics.totalInteractions

        val newScore = if (helpful) {
            (rating.toDouble() / 5.0) * 0.8 + 0.2 // Weight rating and helpfulness
        } else {
            (rating.toDouble() / 5.0) * 0.5 // Lower weight if not helpful
        }

        return if (currentCount > 0) {
            (currentScore * currentCount + newScore) / (currentCount + 1)
        } else {
            newScore
        }
    }
}

/**
 * Factory for creating UX Enhancement ViewModels
 */
object UXEnhancementViewModelFactory {

    fun create(uxEnhancementService: com.roshni.games.core.ui.ux.di.UXEnhancementService): UXEnhancementViewModel {
        return UXEnhancementViewModel(uxEnhancementService)
    }
}

/**
 * Integration with existing UI state management
 */
class UXEnhancementStateHandler(
    private val uxEnhancementViewModel: UXEnhancementViewModel
) {

    /**
     * Setup integration with existing UI state
     */
    fun setupIntegration(existingState: Flow<com.roshni.games.core.ui.state.UiState<*>>) {
        kotlinx.coroutines.GlobalScope.launch {
            existingState.collect { uiState ->
                when (uiState) {
                    is com.roshni.games.core.ui.state.UiState.Success -> {
                        handleSuccessState(uiState.data)
                    }
                    is com.roshni.games.core.ui.state.UiState.Error -> {
                        handleErrorState(uiState.throwable)
                    }
                    is com.roshni.games.core.ui.state.UiState.Loading -> {
                        handleLoadingState()
                    }
                }
            }
        }
    }

    private suspend fun handleSuccessState(data: Any?) {
        // Update UX context when data is successfully loaded
        val context = com.roshni.games.core.ui.ux.di.UXEnhancementExtensions.createUXContext(
            screenName = "current_screen",
            userPreferences = uxEnhancementViewModel.state.value.userPreferences,
            deviceCapabilities = uxEnhancementViewModel.state.value.deviceCapabilities
        )

        uxEnhancementViewModel.updateContext(context)
    }

    private suspend fun handleErrorState(throwable: Throwable) {
        uxEnhancementViewModel.dispatch(
            UXEnhancementAction.SetError("UI Error: ${throwable.message}")
        )
    }

    private suspend fun handleLoadingState() {
        // Could show loading-specific enhancements
    }
}

/**
 * Composable function to provide UX Enhancement state to composables
 */
@Composable
fun rememberUXEnhancementState(
    uxEnhancementService: com.roshni.games.core.ui.ux.di.UXEnhancementService
): UXEnhancementViewModel {
    return remember {
        UXEnhancementViewModelFactory.create(uxEnhancementService)
    }
}

/**
 * Composable function to collect UX Enhancement state
 */
@Composable
fun CollectUXEnhancementState(
    viewModel: UXEnhancementViewModel,
    content: @Composable (UXEnhancementState) -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Provide error handling UI if needed
    when {
        state.error != null -> {
            // Show error UI
            androidx.compose.material3.Text(
                text = "UX Enhancement Error: ${state.error}",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        }
        state.isProcessing -> {
            // Show loading indicator
            androidx.compose.material3.CircularProgressIndicator()
        }
        else -> {
            content(state)
        }
    }
}