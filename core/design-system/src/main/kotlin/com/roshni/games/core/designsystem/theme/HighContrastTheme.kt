package com.roshni.games.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * High contrast color schemes for accessibility
 */
object HighContrastColorScheme {

    // High contrast light theme colors
    val highContrastLightColors = lightColorScheme(
        primary = Color.Black,
        onPrimary = Color.White,
        primaryContainer = Color.Black,
        onPrimaryContainer = Color.White,
        secondary = Color.Black,
        onSecondary = Color.White,
        secondaryContainer = Color.Black,
        onSecondaryContainer = Color.White,
        tertiary = Color.Black,
        onTertiary = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color.White,
        onSurfaceVariant = Color.Black,
        surfaceTint = Color.Black,
        inverseOnSurface = Color.White,
        inverseSurface = Color.Black,
        error = Color.Black,
        onError = Color.White,
        errorContainer = Color.Black,
        onErrorContainer = Color.White,
        outline = Color.Black,
        outlineVariant = Color.Black,
        scrim = Color.Black.copy(alpha = 0.5f)
    )

    // High contrast dark theme colors
    val highContrastDarkColors = darkColorScheme(
        primary = Color.White,
        onPrimary = Color.Black,
        primaryContainer = Color.White,
        onPrimaryContainer = Color.Black,
        secondary = Color.White,
        onSecondary = Color.Black,
        secondaryContainer = Color.White,
        onSecondaryContainer = Color.Black,
        tertiary = Color.White,
        onTertiary = Color.Black,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceVariant = Color.Black,
        onSurfaceVariant = Color.White,
        surfaceTint = Color.White,
        inverseOnSurface = Color.Black,
        inverseSurface = Color.White,
        error = Color.White,
        onError = Color.Black,
        errorContainer = Color.White,
        onErrorContainer = Color.Black,
        outline = Color.White,
        outlineVariant = Color.White,
        scrim = Color.White.copy(alpha = 0.5f)
    )

    // Gaming-specific high contrast colors
    val gamingHighContrastLight = highContrastLightColors.copy(
        primary = Color(0xFF000000), // Pure black for maximum contrast
        secondary = Color(0xFF000000),
        error = Color(0xFFCC0000), // High contrast red for errors
        surface = Color(0xFFFFFFF0) // Slightly off-white for better game visibility
    )

    val gamingHighContrastDark = highContrastDarkColors.copy(
        primary = Color(0xFFFFFFFF), // Pure white for maximum contrast
        secondary = Color(0xFFFFFFFF),
        error = Color(0xFFFF4444), // High contrast red for errors
        surface = Color(0xFF0A0A0A) // Slightly off-black for better game visibility
    )
}

/**
 * High contrast theme configuration
 */
data class HighContrastThemeConfig(
    val isHighContrast: Boolean = false,
    val contrastLevel: ContrastLevel = ContrastLevel.NORMAL,
    val colorScheme: ColorScheme = highContrastLightColors,
    val borderWidth: BorderWidth = BorderWidth.NORMAL,
    val fontSize: FontSize = FontSize.NORMAL,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL
)

enum class ContrastLevel {
    NORMAL,
    HIGH,
    MAXIMUM
}

enum class BorderWidth(
    val width: androidx.compose.ui.unit.Dp
) {
    NORMAL(androidx.compose.ui.unit.dp),
    THICK(2.dp),
    EXTRA_THICK(4.dp)
}

enum class FontSize(
    val scale: Float
) {
    NORMAL(1.0f),
    LARGE(1.2f),
    EXTRA_LARGE(1.5f)
}

enum class AnimationSpeed(
    val multiplier: Float
) {
    NORMAL(1.0f),
    SLOW(0.5f),
    NONE(0.0f)
}

/**
 * Composition local for high contrast theme configuration
 */
val LocalHighContrastConfig = staticCompositionLocalOf { HighContrastThemeConfig() }

/**
 * Get high contrast color scheme based on dark theme setting
 */
@Composable
fun getHighContrastColorScheme(
    darkTheme: Boolean = false,
    isGamingTheme: Boolean = false
): ColorScheme {
    return when {
        darkTheme && isGamingTheme -> HighContrastColorScheme.gamingHighContrastDark
        darkTheme -> HighContrastColorScheme.highContrastDarkColors
        isGamingTheme -> HighContrastColorScheme.gamingHighContrastLight
        else -> HighContrastColorScheme.highContrastLightColors
    }
}

/**
 * Calculate contrast ratio between two colors
 */
fun calculateContrastRatio(color1: Color, color2: Color): Float {
    fun Color.toLuminance(): Float {
        val red = this.red
        val green = this.green
        val blue = this.blue

        val rLinear = if (red <= 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4).toFloat()
        val gLinear = if (green <= 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4).toFloat()
        val bLinear = if (blue <= 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4).toFloat()

        return 0.2126f * rLinear + 0.7152f * gLinear + 0.0722f * bLinear
    }

    val luminance1 = color1.toLuminance()
    val luminance2 = color2.toLuminance()

    val lighter = maxOf(luminance1, luminance2)
    val darker = minOf(luminance1, luminance2)

    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * Check if color combination meets WCAG contrast requirements
 */
fun isAccessibleColorCombination(
    backgroundColor: Color,
    foregroundColor: Color,
    level: AccessibilityLevel = AccessibilityLevel.AA
): Boolean {
    val contrastRatio = calculateContrastRatio(backgroundColor, foregroundColor)

    return when (level) {
        AccessibilityLevel.A -> contrastRatio >= 7.0f // Enhanced contrast
        AccessibilityLevel.AA -> contrastRatio >= 4.5f // Normal contrast
        AccessibilityLevel.AAA -> contrastRatio >= 7.0f // Enhanced contrast
    }
}

enum class AccessibilityLevel {
    A,   // 7:1 contrast ratio
    AA,  // 4.5:1 contrast ratio
    AAA  // 7:1 contrast ratio
}

/**
 * Get accessible color alternative if current combination doesn't meet requirements
 */
fun getAccessibleColorAlternative(
    backgroundColor: Color,
    foregroundColor: Color,
    targetContrast: Float = 4.5f
): Color {
    val currentContrast = calculateContrastRatio(backgroundColor, foregroundColor)

    return if (currentContrast >= targetContrast) {
        foregroundColor
    } else {
        // Return high contrast alternative
        if (isLightColor(backgroundColor)) {
            Color.Black
        } else {
            Color.White
        }
    }
}

/**
 * Check if color is light (luminance > 0.5)
 */
private fun isLightColor(color: Color): Boolean {
    val luminance = color.toLuminance()
    return luminance > 0.5f
}

/**
 * Extension function to get luminance of a color
 */
private fun Color.toLuminance(): Float {
    val red = this.red
    val green = this.green
    val blue = this.blue

    val rLinear = if (red <= 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4).toFloat()
    val gLinear = if (green <= 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4).toFloat()
    val bLinear = if (blue <= 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4).toFloat()

    return 0.2126f * rLinear + 0.7152f * gLinear + 0.0722f * bLinear
}

/**
 * High contrast theme composable
 */
@Composable
fun HighContrastTheme(
    darkTheme: Boolean = false,
    isGamingTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = getHighContrastColorScheme(darkTheme, isGamingTheme)

    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Use existing typography
        content = content
    )
}

/**
 * Preview composable for high contrast theme
 */
@Composable
fun HighContrastThemePreview() {
    HighContrastTheme {
        // Preview content would go here
    }
}