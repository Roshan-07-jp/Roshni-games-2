package com.roshni.games.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Roshni Games Color System
 *
 * A vibrant, playful color palette designed for gaming experiences.
 * Follows Material 3 color system guidelines with custom gaming-focused colors.
 */

// Primary Colors - Main brand colors for Roshni Games
object RoshniGamesColors {
    object Primary {
        val Main = Color(0xFF6B46C1) // Purple - main brand color
        val OnMain = Color(0xFFFFFFFF) // White text on primary
        val Container = Color(0xFFE9D5FF) // Light purple container
        val OnContainer = Color(0xFF1E1B4B) // Dark text on container
        val Inverse = Color(0xFFD4BBFF) // Inverse variant
        val Shade50 = Color(0xFFFAF5FF)
        val Shade100 = Color(0xFFF3E8FF)
        val Shade200 = Color(0xFFE9D5FF)
        val Shade300 = Color(0xFFD4BBFF)
        val Shade400 = Color(0xFFBD98FF)
        val Shade500 = Color(0xFF6B46C1)
        val Shade600 = Color(0xFF5B21B6)
        val Shade700 = Color(0xFF4C1D95)
        val Shade800 = Color(0xFF3B0764)
        val Shade900 = Color(0xFF2D1B69)
    }

    // Secondary Colors - Accent colors for highlights and interactions
    object Secondary {
        val Main = Color(0xFF10B981) // Emerald green
        val OnMain = Color(0xFFFFFFFF)
        val Container = Color(0xFFD1FAE5)
        val OnContainer = Color(0xFF064E3B)
        val Shade50 = Color(0xFFECFDF5)
        val Shade100 = Color(0xFFD1FAE5)
        val Shade200 = Color(0xFFBBF7D0)
        val Shade300 = Color(0xFF8CE99A)
        val Shade400 = Color(0xFF4ADE80)
        val Shade500 = Color(0xFF10B981)
        val Shade600 = Color(0xFF059669)
        val Shade700 = Color(0xFF047857)
        val Shade800 = Color(0xFF065F46)
        val Shade900 = Color(0xFF064E3B)
    }

    // Tertiary Colors - Special elements and gaming highlights
    object Tertiary {
        val Main = Color(0xFFF59E0B) // Amber for achievements/highlights
        val OnMain = Color(0xFFFFFFFF)
        val Container = Color(0xFFFFF3C4)
        val OnContainer = Color(0xFF7C2D12)
        val Shade50 = Color(0xFFFFFBEB)
        val Shade100 = Color(0xFFFEF3C7)
        val Shade200 = Color(0xFFFDCF6B)
        val Shade300 = Color(0xFFF59E0B)
        val Shade400 = Color(0xFFD97706)
        val Shade500 = Color(0xFFB45309)
        val Shade600 = Color(0xFF92400E)
        val Shade700 = Color(0xFF78350F)
        val Shade800 = Color(0xFF451A03)
        val Shade900 = Color(0xFF7C2D12)
    }

    // Error Colors - For errors and warnings
    object Error {
        val Main = Color(0xFFEF4444) // Red
        val OnMain = Color(0xFFFFFFFF)
        val Container = Color(0xFFFEE2E2)
        val OnContainer = Color(0xFF991B1B)
        val Shade50 = Color(0xFFFEF2F2)
        val Shade100 = Color(0xFFFEE2E2)
        val Shade200 = Color(0xFFFECACA)
        val Shade300 = Color(0xFFFCA5A5)
        val Shade400 = Color(0xFFF87171)
        val Shade500 = Color(0xFFEF4444)
        val Shade600 = Color(0xFFDC2626)
        val Shade700 = Color(0xFFB91C1C)
        val Shade800 = Color(0xFF991B1B)
        val Shade900 = Color(0xFF7F1D1D)
    }

    // Success Colors - For positive feedback
    object Success {
        val Main = Color(0xFF10B981)
        val OnMain = Color(0xFFFFFFFF)
        val Container = Color(0xFFD1FAE5)
        val OnContainer = Color(0xFF064E3B)
    }

    // Warning Colors - For cautions and alerts
    object Warning {
        val Main = Color(0xFFF59E0B)
        val OnMain = Color(0xFFFFFFFF)
        val Container = Color(0xFFFFF3C4)
        val OnContainer = Color(0xFF7C2D12)
    }

    // Info Colors - For informational content
    object Info {
        val Main = Color(0xFF3B82F6)
        val OnMain = Color(0xFFFFFFFF)
        val Container = Color(0xFFDBEAFE)
        val OnContainer = Color(0xFF1E3A8A)
    }

    // Neutral Colors - Backgrounds, surfaces, and text
    object Neutral {
        val Background = Color(0xFFFAFAFA)
        val OnBackground = Color(0xFF1F2937)
        val Surface = Color(0xFFFFFFFF)
        val OnSurface = Color(0xFF374151)
        val SurfaceVariant = Color(0xFFF3F4F6)
        val OnSurfaceVariant = Color(0xFF6B7280)
        val Outline = Color(0xFFD1D5DB)
        val OutlineVariant = Color(0xFFE5E7EB)
        val Scrim = Color(0xFF000000).copy(alpha = 0.32f)
        val InverseSurface = Color(0xFF111827)
        val InverseOnSurface = Color(0xFFF9FAFB)

        // Text colors
        val TextPrimary = Color(0xFF111827)
        val TextSecondary = Color(0xFF6B7280)
        val TextTertiary = Color(0xFF9CA3AF)
        val TextInverse = Color(0xFFF9FAFB)

        // Surface colors for different elevations
        val Surface1 = Color(0xFFFFFFFF)
        val Surface2 = Color(0xFFF9FAFB)
        val Surface3 = Color(0xFFF3F4F6)
        val Surface4 = Color(0xFFEEEEEE)
        val Surface5 = Color(0xFFE5E7EB)
    }

    // Gaming-specific colors
    object Gaming {
        val ScoreHighlight = Color(0xFFFF6B6B)
        val AchievementGold = Color(0xFFFFD700)
        val LevelProgress = Color(0xFF4ECDC4)
        val EnergyBar = Color(0xFF45B7D1)
        val StreakFire = Color(0xFFFF4500)
        val PowerUpGlow = Color(0xFF9932CC)
        val GameOver = Color(0xFFDC143C)
        val Victory = Color(0xFF32CD32)
    }
}

// Extended color palette for gradients and special effects
object RoshniGamesGradients {
    val PrimaryGradient = listOf(
        RoshniGamesColors.Primary.Shade500,
        RoshniGamesColors.Primary.Shade700
    )

    val SecondaryGradient = listOf(
        RoshniGamesColors.Secondary.Shade400,
        RoshniGamesColors.Secondary.Shade600
    )

    val GamingEnergyGradient = listOf(
        RoshniGamesColors.Gaming.EnergyBar,
        RoshniGamesColors.Primary.Shade500
    )

    val AchievementGradient = listOf(
        RoshniGamesColors.Gaming.AchievementGold,
        RoshniGamesColors.Tertiary.Shade400
    )
}