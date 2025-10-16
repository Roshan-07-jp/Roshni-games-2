package com.roshni.games.core.ui.ux.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.roshni.games.core.ui.ux.model.UXContext
import com.roshni.games.core.ui.ux.model.UXEnhancement
import com.roshni.games.core.ui.ux.model.UserInteraction
import kotlinx.coroutines.delay

/**
 * Enhanced button component that applies UX enhancements
 */
@Composable
fun EnhancedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    uxContext: UXContext? = null,
    enhancements: List<UXEnhancement> = emptyList(),
    interactionId: String = "button_${System.currentTimeMillis()}",
    onInteractionProcessed: ((UserInteraction, com.roshni.games.core.ui.ux.model.EnhancedInteraction) -> Unit)? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // State for animations
    var isPressed by remember { mutableStateOf(false) }
    var showRipple by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }

    // Animation states
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = tween(durationMillis = 200),
        label = "alpha"
    )

    // Find applicable enhancements
    val visualEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.VisualFeedback>().firstOrNull()
    }

    val audioEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.AudioFeedback>().firstOrNull()
    }

    val hapticEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.HapticFeedback>().firstOrNull()
    }

    // Apply visual enhancements
    val backgroundColor = when {
        isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.primary
    }

    val shape = RoundedCornerShape(8.dp)

    // Handle interactions
    val handleInteraction = {
        if (!enabled) return@handleInteraction

        // Record interaction
        val interaction = UserInteraction(
            id = interactionId,
            type = UserInteraction.InteractionType.BUTTON_CLICK,
            timestamp = System.currentTimeMillis(),
            context = UserInteraction.InteractionContext(
                screenName = uxContext?.screenName ?: "unknown",
                componentId = interactionId,
                userId = uxContext?.userId,
                sessionId = uxContext?.sessionId,
                deviceInfo = UserInteraction.DeviceInfo()
            )
        )

        // Apply haptic feedback
        hapticEnhancement?.let { haptic ->
            try {
                when (haptic.pattern) {
                    UXEnhancement.HapticFeedback.HapticPattern.LIGHT_TICK -> {
                        hapticFeedback.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                        )
                    }
                    UXEnhancement.HapticFeedback.HapticPattern.MEDIUM_TICK -> {
                        hapticFeedback.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                        )
                    }
                    UXEnhancement.HapticFeedback.HapticPattern.HEAVY_TICK -> {
                        hapticFeedback.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                        )
                    }
                    else -> {
                        hapticFeedback.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                        )
                    }
                }
            } catch (e: Exception) {
                // Haptic feedback not available on this device
            }
        }

        // Apply audio feedback (would need audio service integration)
        audioEnhancement?.let { audio ->
            // Play sound effect - would integrate with audio service
            Timber.d("Playing audio feedback: ${audio.soundType}")
        }

        // Apply visual feedback
        visualEnhancement?.let { visual ->
            when (visual.animationType) {
                UXEnhancement.VisualFeedback.AnimationType.SCALE_UP -> {
                    scale = 1.1f
                }
                UXEnhancement.VisualFeedback.AnimationType.BOUNCE -> {
                    scale = 1.05f
                }
                UXEnhancement.VisualFeedback.AnimationType.PULSE -> {
                    showRipple = true
                }
                else -> {
                    scale = 1.05f
                }
            }
        }

        // Execute click action
        onClick()

        // Reset animations
        LaunchedEffect(scale) {
            delay(150)
            scale = 1f
            showRipple = false
        }
    }

    Box(
        modifier = modifier
            .scale(animatedScale)
            .graphicsLayer(alpha = animatedAlpha)
            .clip(shape)
            .background(
                color = backgroundColor,
                shape = shape
            )
            .clickable(
                enabled = enabled,
                onClick = handleInteraction
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPressed = true
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )

        // Ripple effect overlay
        if (showRipple) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Enhanced card component with UX enhancements
 */
@Composable
fun EnhancedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    uxContext: UXContext? = null,
    enhancements: List<UXEnhancement> = emptyList(),
    interactionId: String = "card_${System.currentTimeMillis()}",
    elevation: Dp = 4.dp
) {
    val hapticFeedback = LocalHapticFeedback.current

    var isPressed by remember { mutableStateOf(false) }
    var showGlow by remember { mutableStateOf(false) }

    val animatedElevation by animateFloatAsState(
        targetValue = if (isPressed) elevation.value * 2 else elevation.value,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "elevation"
    )

    // Find applicable enhancements
    val visualEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.VisualFeedback>().firstOrNull()
    }

    val hapticEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.HapticFeedback>().firstOrNull()
    }

    val backgroundColor = if (showGlow && visualEnhancement != null) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    androidx.compose.material3.Card(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = animatedElevation
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPressed = true
                            showGlow = true

                            // Apply haptic feedback
                            hapticEnhancement?.let { haptic ->
                                try {
                                    hapticFeedback.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                    )
                                } catch (e: Exception) {
                                    // Haptic feedback not available
                                }
                            }

                            awaitRelease()
                        } finally {
                            isPressed = false
                            showGlow = false
                        }
                    },
                    onTap = {
                        val interaction = UserInteraction(
                            id = interactionId,
                            type = UserInteraction.InteractionType.TAP,
                            timestamp = System.currentTimeMillis(),
                            context = UserInteraction.InteractionContext(
                                screenName = uxContext?.screenName ?: "unknown",
                                componentId = interactionId,
                                userId = uxContext?.userId,
                                sessionId = uxContext?.sessionId,
                                deviceInfo = UserInteraction.DeviceInfo()
                            )
                        )

                        onClick()
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        content()
    }
}

/**
 * Enhanced text component with contextual help
 */
@Composable
fun EnhancedText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.material3.Typography? = null,
    uxContext: UXContext? = null,
    enhancements: List<UXEnhancement> = emptyList(),
    showTooltip: Boolean = false,
    tooltipText: String? = null
) {
    val contextualHelp = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.ContextualHelp>().firstOrNull()
    }

    Box(modifier = modifier) {
        androidx.compose.material3.Text(
            text = text,
            style = style ?: MaterialTheme.typography.bodyMedium,
            color = if (contextualHelp != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        // Show contextual help indicator
        contextualHelp?.let { help ->
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material3.icons.Icons.Default.Info,
                contentDescription = "Help available",
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Tooltip (simplified implementation)
        if (showTooltip && tooltipText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                androidx.compose.material3.Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Enhanced input field with UX enhancements
 */
@Composable
fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    placeholder: String? = null,
    uxContext: UXContext? = null,
    enhancements: List<UXEnhancement> = emptyList(),
    interactionId: String = "textfield_${System.currentTimeMillis()}"
) {
    val hapticFeedback = LocalHapticFeedback.current

    var isFocused by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    val visualEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.VisualFeedback>().firstOrNull()
    }

    val hapticEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.HapticFeedback>().firstOrNull()
    }

    val borderColor = when {
        showSuccessAnimation -> MaterialTheme.colorScheme.primary
        isFocused -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)

            // Apply haptic feedback on text change
            hapticEnhancement?.let { haptic ->
                try {
                    hapticFeedback.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                    )
                } catch (e: Exception) {
                    // Haptic feedback not available
                }
            }

            // Show success animation for valid input
            if (newValue.isNotEmpty() && visualEnhancement != null) {
                showSuccessAnimation = true
                kotlinx.coroutines.GlobalScope.launch {
                    delay(1000)
                    showSuccessAnimation = false
                }
            }
        },
        modifier = modifier
            .graphicsLayer {
                val scale = if (showSuccessAnimation) 1.02f else 1f
                scaleX = scale
                scaleY = scale
            },
        label = { androidx.compose.material3.Text(label) },
        placeholder = placeholder?.let { { androidx.compose.material3.Text(it) } },
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = borderColor,
            unfocusedIndicatorColor = borderColor
        )
    )
}

/**
 * Enhanced loading indicator with UX enhancements
 */
@Composable
fun EnhancedLoadingIndicator(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    uxContext: UXContext? = null,
    enhancements: List<UXEnhancement> = emptyList(),
    content: @Composable () -> Unit
) {
    val visualEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.VisualFeedback>().firstOrNull()
    }

    Box(modifier = modifier) {
        content()

        if (isLoading) {
            // Enhanced loading overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )

                // Pulsing animation if visual enhancement is available
                visualEnhancement?.let { visual ->
                    if (visual.animationType == UXEnhancement.VisualFeedback.AnimationType.PULSE) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            strokeWidth = 6.dp,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced switch/toggle component
 */
@Composable
fun EnhancedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    uxContext: UXContext? = null,
    enhancements: List<UXEnhancement> = emptyList(),
    interactionId: String = "switch_${System.currentTimeMillis()}"
) {
    val hapticFeedback = LocalHapticFeedback.current

    var showAnimation by remember { mutableStateOf(false) }

    val hapticEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.HapticFeedback>().firstOrNull()
    }

    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = { newChecked ->
            // Apply haptic feedback
            hapticEnhancement?.let { haptic ->
                try {
                    hapticFeedback.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                    )
                } catch (e: Exception) {
                    // Haptic feedback not available
                }
            }

            // Show animation
            showAnimation = true
            kotlinx.coroutines.GlobalScope.launch {
                delay(300)
                showAnimation = false
            }

            onCheckedChange(newChecked)
        },
        modifier = modifier
            .graphicsLayer {
                val scale = if (showAnimation) 1.1f else 1f
                scaleX = scale
                scaleY = scale
            }
    )
}

/**
 * Enhanced slider component
 */
@Composable
fun EnhancedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    uxContext: UXContext? = null,
    enhancements: List<UXEnhancement> = emptyList(),
    interactionId: String = "slider_${System.currentTimeMillis()}"
) {
    val hapticFeedback = LocalHapticFeedback.current

    val hapticEnhancement = remember(enhancements) {
        enhancements.filterIsInstance<UXEnhancement.HapticFeedback>().firstOrNull()
    }

    androidx.compose.material3.Slider(
        value = value,
        onValueChange = { newValue ->
            // Apply haptic feedback on value change
            hapticEnhancement?.let { haptic ->
                try {
                    hapticFeedback.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                    )
                } catch (e: Exception) {
                    // Haptic feedback not available
                }
            }

            onValueChange(newValue)
        },
        modifier = modifier,
        valueRange = valueRange,
        colors = androidx.compose.material3.SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    )
}