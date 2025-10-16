package com.roshni.games.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Accessibility configuration for Roshni Games
 */

// Accessibility configuration
data class AccessibilityConfig(
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reduceMotion: Boolean = false,
    val colorBlindFriendly: Boolean = false,
    val screenReaderOptimized: Boolean = false,
    val touchTargetSize: TouchTargetSize = TouchTargetSize.NORMAL,
    val focusIndicators: Boolean = true
)

// Touch target sizes
enum class TouchTargetSize(val multiplier: Float) {
    SMALL(0.75f),
    NORMAL(1.0f),
    LARGE(1.5f),
    EXTRA_LARGE(2.0f)
}

// Accessibility state
val LocalAccessibilityConfig = compositionLocalOf { AccessibilityConfig() }

var accessibilityConfig by mutableStateOf(AccessibilityConfig())

/**
 * Provide accessibility configuration to composables
 */
@Composable
fun ProvideAccessibilityConfig(
    config: AccessibilityConfig,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAccessibilityConfig provides config
    ) {
        content()
    }
}

/**
 * Get current accessibility configuration
 */
@Composable
fun getAccessibilityConfig(): AccessibilityConfig {
    return LocalAccessibilityConfig.current
}

/**
 * High contrast color scheme for accessibility
 */
object HighContrastColors {
    // High contrast primary colors
    val Primary = Color(0xFF0000FF) // Pure blue
    val OnPrimary = Color(0xFFFFFFFF) // White
    val PrimaryVariant = Color(0xFF0000CC) // Darker blue

    // High contrast secondary colors
    val Secondary = Color(0xFFFF6600) // Orange
    val OnSecondary = Color(0xFF000000) // Black
    val SecondaryVariant = Color(0xFFCC3300) // Darker orange

    // High contrast backgrounds
    val Background = Color(0xFFFFFFFF) // White
    val Surface = Color(0xFFFFFFFF) // White
    val Error = Color(0xFFFF0000) // Pure red
    val OnBackground = Color(0xFF000000) // Black
    val OnSurface = Color(0xFF000000) // Black
    val OnError = Color(0xFFFFFFFF) // White

    // High contrast borders and dividers
    val Outline = Color(0xFF000000) // Black
    val SurfaceVariant = Color(0xFFF0F0F0) // Light gray
}

/**
 * Color blind friendly color palette
 */
object ColorBlindFriendlyColors {
    // Use distinct colors that work well for different types of color blindness
    val SafeRed = Color(0xFFE73C3C) // Red
    val SafeGreen = Color(0xFF27AE60) // Green
    val SafeBlue = Color(0xFF3498DB) // Blue
    val SafeYellow = Color(0xFFF1C40F) // Yellow
    val SafePurple = Color(0xFF9B59B6) // Purple
    val SafeOrange = Color(0xFFE67E22) // Orange

    // Patterns for differentiation
    val Solid = Color(0xFF000000) // Black
    val Striped = Color(0xFF666666) // Gray
    val Dotted = Color(0xFF999999) // Light gray
}

/**
 * Accessibility-aware text styles
 */
object AccessibleTypography {

    fun displayLarge(accessibilityConfig: AccessibilityConfig): TextStyle {
        return RoshniGamesTypography.displayLarge.copy(
            fontSize = if (accessibilityConfig.largeText) {
                RoshniGamesTypography.displayLarge.fontSize * 1.2
            } else {
                RoshniGamesTypography.displayLarge.fontSize
            }
        )
    }

    fun bodyLarge(accessibilityConfig: AccessibilityConfig): TextStyle {
        return RoshniGamesTypography.bodyLarge.copy(
            fontSize = if (accessibilityConfig.largeText) {
                RoshniGamesTypography.bodyLarge.fontSize * 1.2
            } else {
                RoshniGamesTypography.bodyLarge.fontSize
            },
            lineHeight = if (accessibilityConfig.largeText) {
                RoshniGamesTypography.bodyLarge.lineHeight * 1.3
            } else {
                RoshniGamesTypography.bodyLarge.lineHeight
            }
        )
    }

    fun labelLarge(accessibilityConfig: AccessibilityConfig): TextStyle {
        return RoshniGamesTypography.labelLarge.copy(
            fontSize = if (accessibilityConfig.largeText) {
                RoshniGamesTypography.labelLarge.fontSize * 1.2
            } else {
                RoshniGamesTypography.labelLarge.fontSize
            }
        )
    }
}

/**
 * Accessibility-aware spacing
 */
object AccessibleSpacing {

    fun getTouchTargetSize(accessibilityConfig: AccessibilityConfig): Dp {
        return when (accessibilityConfig.touchTargetSize) {
            TouchTargetSize.SMALL -> ComponentSizes.ButtonMinHeight * 0.75f
            TouchTargetSize.NORMAL -> ComponentSizes.ButtonMinHeight
            TouchTargetSize.LARGE -> ComponentSizes.ButtonMinHeight * 1.5f
            TouchTargetSize.EXTRA_LARGE -> ComponentSizes.ButtonMinHeight * 2.0f
        }
    }

    fun getMinimumTouchTarget(): Dp {
        // WCAG recommends minimum 44dp touch targets
        return 44.dp
    }

    fun getAccessiblePadding(accessibilityConfig: AccessibilityConfig): Dp {
        return when (accessibilityConfig.touchTargetSize) {
            TouchTargetSize.SMALL -> Spacing.Medium
            TouchTargetSize.NORMAL -> Spacing.Large
            TouchTargetSize.LARGE -> Spacing.ExtraLarge
            TouchTargetSize.EXTRA_LARGE -> Spacing.Huge
        }
    }
}

/**
 * Accessibility utilities
 */
object AccessibilityUtils {

    /**
     * Generate accessible color combinations
     */
    fun getAccessibleColorPair(background: Color, foreground: Color): Pair<Color, Color> {
        // Calculate contrast ratio
        val contrastRatio = calculateContrastRatio(background, foreground)

        // If contrast is insufficient, adjust colors
        return if (contrastRatio < 4.5) { // WCAG AA standard
            val adjustedForeground = if (isLightColor(background)) {
                Color.Black
            } else {
                Color.White
            }
            Pair(background, adjustedForeground)
        } else {
            Pair(background, foreground)
        }
    }

    /**
     * Calculate contrast ratio between two colors
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Float {
        val luminance1 = calculateLuminance(color1)
        val luminance2 = calculateLuminance(color2)

        val lighter = maxOf(luminance1, luminance2)
        val darker = minOf(luminance1, luminance2)

        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Calculate luminance of a color
     */
    private fun calculateLuminance(color: Color): Float {
        val red = color.red * 255
        val green = color.green * 255
        val blue = color.blue * 255

        val rLinear = if (red <= 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4).toFloat()
        val gLinear = if (green <= 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4).toFloat()
        val bLinear = if (blue <= 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4).toFloat()

        return 0.2126f * rLinear + 0.7152f * gLinear + 0.0722f * bLinear
    }

    /**
     * Check if color is light
     */
    private fun isLightColor(color: Color): Boolean {
        val luminance = calculateLuminance(color)
        return luminance > 0.5f
    }

    /**
     * Generate accessible focus indicator color
     */
    fun getFocusIndicatorColor(backgroundColor: Color): Color {
        return if (isLightColor(backgroundColor)) {
            Color(0xFF0066CC) // Blue for light backgrounds
        } else {
            Color(0xFF66CCFF) // Light blue for dark backgrounds
        }
    }

    /**
     * Get minimum readable font size based on accessibility settings
     */
    fun getMinimumFontSize(accessibilityConfig: AccessibilityConfig): androidx.compose.ui.unit.TextUnit {
        return if (accessibilityConfig.largeText) {
            18.sp // Minimum 18sp for large text
        } else {
            14.sp // Minimum 14sp for normal text
        }
    }

    /**
     * Get accessible animation duration
     */
    fun getAccessibleAnimationDuration(
        baseDuration: Int,
        accessibilityConfig: AccessibilityConfig
    ): Int {
        return if (accessibilityConfig.reduceMotion) {
            (baseDuration * 0.5).toInt() // Faster animations for reduced motion
        } else {
            baseDuration
        }
    }
}

/**
 * Accessibility composable utilities
 */
object AccessibleComposables {

    /**
     * Create accessible button with proper touch targets and focus indicators
     */
    @Composable
    fun AccessibleButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
    ) {
        val config = getAccessibilityConfig()
        val minTouchTarget = AccessibleSpacing.getMinimumTouchTarget()
        val currentTouchTarget = AccessibleSpacing.getTouchTargetSize(config)

        androidx.compose.material3.OutlinedButton(
            onClick = onClick,
            modifier = modifier.sizeIn(
                minWidth = minTouchTarget,
                minHeight = minTouchTarget
            ),
            enabled = enabled,
            content = content
        )
    }

    /**
     * Create accessible text with proper sizing
     */
    @Composable
    fun AccessibleText(
        text: String,
        style: TextStyle,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified
    ) {
        val config = getAccessibilityConfig()
        val accessibleStyle = style.copy(
            fontSize = maxOf(style.fontSize, AccessibilityUtils.getMinimumFontSize(config))
        )

        androidx.compose.material3.Text(
            text = text,
            style = accessibleStyle,
            modifier = modifier,
            color = color
        )
    }

    /**
     * Create accessible icon with proper sizing and descriptions
     */
    @Composable
    fun AccessibleIcon(
        imageVector: androidx.compose.ui.graphics.vector.ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified
    ) {
        val config = getAccessibilityConfig()
        val iconSize = when (config.touchTargetSize) {
            TouchTargetSize.SMALL -> IconSizes.Medium
            TouchTargetSize.NORMAL -> IconSizes.Large
            TouchTargetSize.LARGE -> IconSizes.ExtraLarge
            TouchTargetSize.EXTRA_LARGE -> IconSizes.Huge
        }

        androidx.compose.material3.Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier.size(iconSize),
            tint = tint
        )
    }
}