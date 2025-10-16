package com.roshni.games.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Roshni Games Design Tokens
 *
 * Centralized design system values for consistent spacing, sizing, and visual elements.
 */

// Spacing Scale - Consistent spacing throughout the app
object Spacing {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 24.dp
    val Huge: Dp = 32.dp
    val Massive: Dp = 48.dp
    val Epic: Dp = 64.dp

    // Gaming-specific spacing
    val GameCardPadding: Dp = 16.dp
    val ScoreDisplayMargin: Dp = 8.dp
    val ButtonTouchTarget: Dp = 48.dp
}

// Component Sizes - Standard sizes for UI components
object ComponentSizes {
    // Button sizes
    val ButtonMinHeight: Dp = 40.dp
    val ButtonMinWidth: Dp = 88.dp
    val IconButtonSize: Dp = 48.dp

    // Card sizes
    val CardMinHeight: Dp = 72.dp
    val GameCardHeight: Dp = 200.dp
    val AchievementCardHeight: Dp = 120.dp

    // Text field sizes
    val TextFieldMinHeight: Dp = 56.dp

    // App bar height
    val AppBarHeight: Dp = 64.dp

    // Bottom navigation height
    val BottomNavHeight: Dp = 80.dp

    // Gaming-specific sizes
    val ScoreDisplaySize: Dp = 64.dp
    val LevelIndicatorSize: Dp = 32.dp
    val ProgressBarHeight: Dp = 8.dp
    val EnergyBarHeight: Dp = 12.dp
}

// Border Radius - Consistent corner radius values
object BorderRadius {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 20.dp
    val Full: Dp = 1000.dp // Effectively infinite for pill shapes

    // Gaming-specific radius
    val GameCardRadius: Dp = 16.dp
    val ButtonRadius: Dp = 12.dp
    val AvatarRadius: Dp = 20.dp
}

// Elevation - Material Design elevation values
object Elevation {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 1.dp
    val Small: Dp = 3.dp
    val Medium: Dp = 6.dp
    val Large: Dp = 8.dp
    val ExtraLarge: Dp = 12.dp

    // Gaming-specific elevation
    val GameCardElevation: Dp = 4.dp
    val FloatingButtonElevation: Dp = 8.dp
    val ModalElevation: Dp = 16.dp
}

// Animation Durations - Standard animation timing
object AnimationDuration {
    const val Instant: Int = 0
    const val Fast: Int = 150
    const val Normal: Int = 300
    const val Slow: Int = 500
    const val VerySlow: Int = 1000

    // Gaming-specific durations
    const val GameTransition: Int = 200
    const val ScoreAnimation: Int = 400
    const val AchievementPopup: Int = 600
}

// Icon Sizes - Standard icon dimensions
object IconSizes {
    val ExtraSmall: Dp = 12.dp
    val Small: Dp = 16.dp
    val Medium: Dp = 20.dp
    val Large: Dp = 24.dp
    val ExtraLarge: Dp = 32.dp
    val Huge: Dp = 48.dp

    // Gaming-specific icons
    val GameIcon: Dp = 64.dp
    val AchievementIcon: Dp = 40.dp
    val ScoreIcon: Dp = 24.dp
}

// Stroke Widths - For borders and outlines
object StrokeWidth {
    val None: Dp = 0.dp
    val Thin: Dp = 1.dp
    val Medium: Dp = 2.dp
    val Thick: Dp = 4.dp

    // Gaming-specific strokes
    val ProgressRing: Dp = 3.dp
    val ButtonBorder: Dp = 2.dp
    val CardBorder: Dp = 1.dp
}

// Gaming-specific design tokens
object GamingTokens {
    // Score and points
    val MaxScoreDigits: Int = 9
    val ScoreAnimationDuration: Int = 300
    val ScoreIncrementDelay: Int = 50

    // Game timing
    val GameRoundDuration: Long = 30000L // 30 seconds
    val PowerUpDuration: Long = 10000L // 10 seconds
    val ComboWindow: Long = 2000L // 2 seconds

    // Achievement thresholds
    val BronzeScore: Long = 1000
    val SilverScore: Long = 5000
    val GoldScore: Long = 10000
    val PlatinumScore: Long = 50000

    // Streak multipliers
    val StreakMultiplierBase: Float = 1.0f
    val StreakMultiplierIncrement: Float = 0.1f
    val MaxStreakMultiplier: Float = 3.0f
}

// Layout breakpoints for responsive design
object Breakpoints {
    val Compact: Dp = 600.dp
    val Medium: Dp = 840.dp
    val Expanded: Dp = 1200.dp
    val Large: Dp = 1600.dp
}

// Z-index layers for proper stacking
object ZIndex {
    const val Background: Float = 0f
    const val Surface: Float = 1f
    const val Content: Float = 2f
    const val FloatingButton: Float = 3f
    const val Modal: Float = 4f
    const val Tooltip: Float = 5f
    const val Toast: Float = 6f

    // Gaming-specific layers
    const val GameOverlay: Float = 7f
    const val AchievementPopup: Float = 8f
    const val LoadingSpinner: Float = 9f
}