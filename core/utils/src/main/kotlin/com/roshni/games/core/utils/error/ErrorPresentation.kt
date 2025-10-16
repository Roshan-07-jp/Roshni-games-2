package com.roshni.games.core.utils.error

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * User-friendly error presentation system for displaying errors to users
 */
object ErrorPresentation {

    private val _errorDisplayEvents = MutableSharedFlow<ErrorDisplayEvent>(extraBufferCapacity = 10)
    val errorDisplayEvents: Flow<ErrorDisplayEvent> = _errorDisplayEvents.asSharedFlow()

    /**
     * Display an error to the user with appropriate presentation
     */
    suspend fun displayError(
        error: AppError,
        context: ErrorContext? = null,
        presentationStyle: PresentationStyle = PresentationStyle.AUTO,
        actions: List<UserAction> = emptyList()
    ) {
        val displayEvent = ErrorDisplayEvent(
            error = error,
            context = context,
            presentation = createPresentation(error, context, presentationStyle),
            actions = actions,
            timestamp = System.currentTimeMillis()
        )

        _errorDisplayEvents.emit(displayEvent)
    }

    /**
     * Display an error with custom presentation
     */
    suspend fun displayCustomError(
        title: String,
        message: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        style: PresentationStyle = PresentationStyle.DIALOG,
        actions: List<UserAction> = emptyList(),
        icon: ErrorIcon? = null,
        context: ErrorContext? = null
    ) {
        val customError = AppError(
            errorType = ErrorType.UNKNOWN_ERROR,
            severity = severity,
            message = message
        )

        val presentation = ErrorPresentation(
            title = title,
            message = message,
            style = style,
            icon = icon ?: getDefaultIcon(severity),
            colorScheme = getColorScheme(severity),
            animation = getAnimation(severity),
            hapticFeedback = getHapticFeedback(severity)
        )

        val displayEvent = ErrorDisplayEvent(
            error = customError,
            context = context,
            presentation = presentation,
            actions = actions,
            timestamp = System.currentTimeMillis()
        )

        _errorDisplayEvents.emit(displayEvent)
    }

    /**
     * Create presentation for an error
     */
    private fun createPresentation(
        error: AppError,
        context: ErrorContext?,
        style: PresentationStyle
    ): ErrorPresentation {
        val title = getErrorTitle(error)
        val message = getErrorMessage(error, context)
        val icon = getErrorIcon(error)
        val colorScheme = getColorScheme(error.severity)
        val animation = getAnimation(error.severity)
        val hapticFeedback = getHapticFeedback(error.severity)

        return ErrorPresentation(
            title = title,
            message = message,
            style = style,
            icon = icon,
            colorScheme = colorScheme,
            animation = animation,
            hapticFeedback = hapticFeedback
        )
    }

    /**
     * Get user-friendly title for an error
     */
    private fun getErrorTitle(error: AppError): String {
        return when (error) {
            is AppError.NetworkError -> "Connection Problem"
            is AppError.GameplayError -> "Game Error"
            is AppError.PermissionError -> "Permission Required"
            is AppError.ValidationError -> "Invalid Input"
            else -> "Something Went Wrong"
        }
    }

    /**
     * Get user-friendly message for an error
     */
    private fun getErrorMessage(error: AppError, context: ErrorContext?): String {
        val baseMessage = error.getUserFriendlyMessage()

        // Add context-specific information
        val contextInfo = when (error) {
            is AppError.GameplayError -> {
                when (error) {
                    is AppError.GameplayError.SaveFailedError -> " Your game progress couldn't be saved."
                    is AppError.GameplayError.LoadFailedError -> " Your game data couldn't be loaded."
                    else -> ""
                }
            }
            is AppError.NetworkError -> {
                when (error) {
                    is AppError.NetworkError.TimeoutError -> " The request took too long to complete."
                    is AppError.NetworkError.UnavailableError -> " The service is temporarily unavailable."
                    else -> ""
                }
            }
            else -> ""
        }

        return baseMessage + contextInfo
    }

    /**
     * Get appropriate icon for an error
     */
    private fun getErrorIcon(error: AppError): ErrorIcon {
        return when (error) {
            is AppError.NetworkError -> ErrorIcon.NETWORK
            is AppError.GameplayError -> ErrorIcon.GAME
            is AppError.PermissionError -> ErrorIcon.PERMISSION
            is AppError.ValidationError -> ErrorIcon.VALIDATION
            else -> ErrorIcon.GENERIC
        }
    }

    /**
     * Get default icon for severity
     */
    private fun getDefaultIcon(severity: ErrorSeverity): ErrorIcon {
        return when (severity) {
            ErrorSeverity.LOW -> ErrorIcon.INFO
            ErrorSeverity.MEDIUM -> ErrorIcon.WARNING
            ErrorSeverity.HIGH -> ErrorIcon.ERROR
            ErrorSeverity.CRITICAL -> ErrorIcon.CRITICAL
            ErrorSeverity.FATAL -> ErrorIcon.FATAL
        }
    }

    /**
     * Get color scheme for error severity
     */
    private fun getColorScheme(severity: ErrorSeverity): ColorScheme {
        return when (severity) {
            ErrorSeverity.LOW -> ColorScheme(
                background = "#E8F5E8",
                foreground = "#2E7D32",
                accent = "#4CAF50"
            )
            ErrorSeverity.MEDIUM -> ColorScheme(
                background = "#FFF3E0",
                foreground = "#EF6C00",
                accent = "#FF9800"
            )
            ErrorSeverity.HIGH -> ColorScheme(
                background = "#FFEBEE",
                foreground = "#C62828",
                accent = "#F44336"
            )
            ErrorSeverity.CRITICAL -> ColorScheme(
                background = "#F3E5F5",
                foreground = "#7B1FA2",
                accent = "#9C27B0"
            )
            ErrorSeverity.FATAL -> ColorScheme(
                background = "#000000",
                foreground = "#FFFFFF",
                accent = "#FF0000"
            )
        }
    }

    /**
     * Get animation for error severity
     */
    private fun getAnimation(severity: ErrorSeverity): AnimationStyle {
        return when (severity) {
            ErrorSeverity.LOW -> AnimationStyle.FADE
            ErrorSeverity.MEDIUM -> AnimationStyle.SLIDE_DOWN
            ErrorSeverity.HIGH -> AnimationStyle.SHAKE
            ErrorSeverity.CRITICAL -> AnimationStyle.PULSE
            ErrorSeverity.FATAL -> AnimationStyle.BOUNCE
        }
    }

    /**
     * Get haptic feedback for error severity
     */
    private fun getHapticFeedback(severity: ErrorSeverity): HapticFeedback {
        return when (severity) {
            ErrorSeverity.LOW -> HapticFeedback.NONE
            ErrorSeverity.MEDIUM -> HapticFeedback.LIGHT
            ErrorSeverity.HIGH -> HapticFeedback.MEDIUM
            ErrorSeverity.CRITICAL -> HapticFeedback.HEAVY
            ErrorSeverity.FATAL -> HapticFeedback.DOUBLE_HEAVY
        }
    }
}

/**
 * Error display event
 */
data class ErrorDisplayEvent(
    val error: AppError,
    val context: ErrorContext?,
    val presentation: ErrorPresentation,
    val actions: List<UserAction>,
    val timestamp: Long
)

/**
 * Error presentation configuration
 */
data class ErrorPresentation(
    val title: String,
    val message: String,
    val style: PresentationStyle,
    val icon: ErrorIcon,
    val colorScheme: ColorScheme,
    val animation: AnimationStyle,
    val hapticFeedback: HapticFeedback,
    val autoDismissDelay: Long? = null,
    val showRetryButton: Boolean = true,
    val showDetailsButton: Boolean = false,
    val maxLines: Int = 3
)

/**
 * Presentation style for error display
 */
enum class PresentationStyle {
    AUTO,           // Automatically choose based on severity
    TOAST,          // Short toast notification
    SNACKBAR,       // Snackbar with action
    DIALOG,         // Modal dialog
    BANNER,         // Top banner
    INLINE,         // Inline with content
    FULLSCREEN,     // Full screen overlay
    NOTIFICATION    // System notification
}

/**
 * Error icons
 */
enum class ErrorIcon {
    GENERIC, INFO, WARNING, ERROR, CRITICAL, FATAL,
    NETWORK, GAME, PERMISSION, VALIDATION, SECURITY, SYSTEM
}

/**
 * Color scheme for error presentation
 */
data class ColorScheme(
    val background: String,
    val foreground: String,
    val accent: String
)

/**
 * Animation style for error presentation
 */
enum class AnimationStyle {
    NONE, FADE, SLIDE_DOWN, SLIDE_UP, SHAKE, PULSE, BOUNCE, SCALE
}

/**
 * Haptic feedback type
 */
enum class HapticFeedback {
    NONE, LIGHT, MEDIUM, HEAVY, DOUBLE_HEAVY
}

/**
 * User action for error resolution
 */
data class UserAction(
    val id: String,
    val label: String,
    val action: suspend () -> Unit,
    val style: ActionStyle = ActionStyle.PRIMARY,
    val icon: ErrorIcon? = null
)

/**
 * Style for user action buttons
 */
enum class ActionStyle {
    PRIMARY, SECONDARY, DESTRUCTIVE, TEXT_ONLY
}

/**
 * Error presentation builder for fluent API
 */
class ErrorPresentationBuilder {
    private var title: String = ""
    private var message: String = ""
    private var style: PresentationStyle = PresentationStyle.AUTO
    private var icon: ErrorIcon = ErrorIcon.GENERIC
    private var colorScheme: ColorScheme? = null
    private var animation: AnimationStyle = AnimationStyle.FADE
    private var hapticFeedback: HapticFeedback = HapticFeedback.NONE
    private var autoDismissDelay: Long? = null
    private var showRetryButton: Boolean = true
    private var showDetailsButton: Boolean = false
    private var maxLines: Int = 3

    fun title(title: String) = apply { this.title = title }
    fun message(message: String) = apply { this.message = message }
    fun style(style: PresentationStyle) = apply { this.style = style }
    fun icon(icon: ErrorIcon) = apply { this.icon = icon }
    fun colorScheme(colorScheme: ColorScheme) = apply { this.colorScheme = colorScheme }
    fun animation(animation: AnimationStyle) = apply { this.animation = animation }
    fun hapticFeedback(hapticFeedback: HapticFeedback) = apply { this.hapticFeedback = hapticFeedback }
    fun autoDismissDelay(delay: Long) = apply { this.autoDismissDelay = delay }
    fun showRetryButton(show: Boolean) = apply { this.showRetryButton = show }
    fun showDetailsButton(show: Boolean) = apply { this.showDetailsButton = show }
    fun maxLines(lines: Int) = apply { this.maxLines = lines }

    fun build(): ErrorPresentation {
        return ErrorPresentation(
            title = title,
            message = message,
            style = style,
            icon = icon,
            colorScheme = colorScheme ?: ColorScheme("#FFFFFF", "#000000", "#007ACC"),
            animation = animation,
            hapticFeedback = hapticFeedback,
            autoDismissDelay = autoDismissDelay,
            showRetryButton = showRetryButton,
            showDetailsButton = showDetailsButton,
            maxLines = maxLines
        )
    }
}

/**
 * Extension function for creating error presentations with fluent API
 */
fun errorPresentation(block: ErrorPresentationBuilder.() -> Unit): ErrorPresentation {
    return ErrorPresentationBuilder().apply(block).build()
}

/**
 * Error presentation manager for handling multiple error displays
 */
class ErrorPresentationManager {

    private val activePresentations = mutableMapOf<String, ErrorDisplayEvent>()
    private val presentationQueue = mutableListOf<ErrorDisplayEvent>()

    /**
     * Display error with queue management
     */
    suspend fun displayError(
        error: AppError,
        context: ErrorContext? = null,
        priority: PresentationPriority = PresentationPriority.NORMAL
    ) {
        val presentation = createPresentation(error, context)
        val displayEvent = ErrorDisplayEvent(
            error = error,
            context = context,
            presentation = presentation,
            actions = createDefaultActions(error),
            timestamp = System.currentTimeMillis()
        )

        when (priority) {
            PresentationPriority.IMMEDIATE -> {
                ErrorPresentation.displayError(error, context, presentation.style, displayEvent.actions)
                activePresentations[error.errorId] = displayEvent
            }
            PresentationPriority.HIGH -> {
                presentationQueue.add(0, displayEvent) // Add to front
                processQueue()
            }
            PresentationPriority.NORMAL -> {
                presentationQueue.add(displayEvent)
                processQueue()
            }
            PresentationPriority.LOW -> {
                presentationQueue.add(displayEvent) // Add to end
                processQueue()
            }
        }
    }

    /**
     * Dismiss error presentation
     */
    fun dismissError(errorId: String) {
        activePresentations.remove(errorId)
        processQueue()
    }

    /**
     * Dismiss all error presentations
     */
    fun dismissAll() {
        activePresentations.clear()
        presentationQueue.clear()
    }

    private fun createPresentation(error: AppError, context: ErrorContext?): ErrorPresentation {
        return ErrorPresentation(
            title = getErrorTitle(error),
            message = getErrorMessage(error, context),
            style = getPresentationStyle(error),
            icon = getErrorIcon(error),
            colorScheme = getColorScheme(error.severity),
            animation = getAnimation(error.severity),
            hapticFeedback = getHapticFeedback(error.severity)
        )
    }

    private fun createDefaultActions(error: AppError): List<UserAction> {
        val actions = mutableListOf<UserAction>()

        if (error.isRecoverable()) {
            actions.add(UserAction(
                id = "retry",
                label = "Retry",
                action = { /* Retry logic */ },
                style = ActionStyle.PRIMARY
            ))
        }

        actions.add(UserAction(
            id = "dismiss",
            label = "OK",
            action = { /* Dismiss logic */ },
            style = ActionStyle.SECONDARY
        ))

        return actions
    }

    private suspend fun processQueue() {
        if (activePresentations.size < MAX_CONCURRENT_PRESENTATIONS && presentationQueue.isNotEmpty()) {
            val nextError = presentationQueue.removeAt(0)
            ErrorPresentation.displayError(
                nextError.error,
                nextError.context,
                nextError.presentation.style,
                nextError.actions
            )
            activePresentations[nextError.error.errorId] = nextError
        }
    }

    companion object {
        const val MAX_CONCURRENT_PRESENTATIONS = 3
    }
}

/**
 * Priority for error presentation
 */
enum class PresentationPriority {
    IMMEDIATE, HIGH, NORMAL, LOW
}

// Helper functions (implementations would be similar to the ones in ErrorPresentation object)
private fun getErrorTitle(error: AppError): String = "Error"
private fun getErrorMessage(error: AppError, context: ErrorContext?): String = error.getUserFriendlyMessage()
private fun getPresentationStyle(error: AppError): PresentationStyle = PresentationStyle.AUTO