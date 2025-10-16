package com.roshni.games.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = RoshniGamesColors.Primary.Main,
    onPrimary = RoshniGamesColors.Primary.OnMain,
    primaryContainer = RoshniGamesColors.Primary.Container,
    onPrimaryContainer = RoshniGamesColors.Primary.OnContainer,
    secondary = RoshniGamesColors.Secondary.Main,
    onSecondary = RoshniGamesColors.Secondary.OnMain,
    secondaryContainer = RoshniGamesColors.Secondary.Container,
    onSecondaryContainer = RoshniGamesColors.Secondary.OnContainer,
    tertiary = RoshniGamesColors.Tertiary.Main,
    onTertiary = RoshniGamesColors.Tertiary.OnMain,
    tertiaryContainer = RoshniGamesColors.Tertiary.Container,
    onTertiaryContainer = RoshniGamesColors.Tertiary.OnContainer,
    error = RoshniGamesColors.Error.Main,
    onError = RoshniGamesColors.Error.OnMain,
    errorContainer = RoshniGamesColors.Error.Container,
    onErrorContainer = RoshniGamesColors.Error.OnContainer,
    background = RoshniGamesColors.Neutral.Background,
    onBackground = RoshniGamesColors.Neutral.OnBackground,
    surface = RoshniGamesColors.Neutral.Surface,
    onSurface = RoshniGamesColors.Neutral.OnSurface,
    surfaceVariant = RoshniGamesColors.Neutral.SurfaceVariant,
    onSurfaceVariant = RoshniGamesColors.Neutral.OnSurfaceVariant,
    outline = RoshniGamesColors.Neutral.Outline,
    outlineVariant = RoshniGamesColors.Neutral.OutlineVariant,
    scrim = RoshniGamesColors.Neutral.Scrim,
    inverseSurface = RoshniGamesColors.Neutral.InverseSurface,
    inverseOnSurface = RoshniGamesColors.Neutral.InverseOnSurface,
    inversePrimary = RoshniGamesColors.Primary.Inverse,
    surfaceTint = RoshniGamesColors.Primary.Main,
)

private val DarkColorScheme = darkColorScheme(
    primary = RoshniGamesColors.Primary.Main,
    onPrimary = RoshniGamesColors.Primary.OnMain,
    primaryContainer = RoshniGamesColors.Primary.Container,
    onPrimaryContainer = RoshniGamesColors.Primary.OnContainer,
    secondary = RoshniGamesColors.Secondary.Main,
    onSecondary = RoshniGamesColors.Secondary.OnMain,
    secondaryContainer = RoshniGamesColors.Secondary.Container,
    onSecondaryContainer = RoshniGamesColors.Secondary.OnContainer,
    tertiary = RoshniGamesColors.Tertiary.Main,
    onTertiary = RoshniGamesColors.Tertiary.OnMain,
    tertiaryContainer = RoshniGamesColors.Tertiary.Container,
    onTertiaryContainer = RoshniGamesColors.Tertiary.OnContainer,
    error = RoshniGamesColors.Error.Main,
    onError = RoshniGamesColors.Error.OnMain,
    errorContainer = RoshniGamesColors.Error.Container,
    onErrorContainer = RoshniGamesColors.Error.OnContainer,
    background = RoshniGamesColors.Neutral.Background,
    onBackground = RoshniGamesColors.Neutral.OnBackground,
    surface = RoshniGamesColors.Neutral.Surface,
    onSurface = RoshniGamesColors.Neutral.OnSurface,
    surfaceVariant = RoshniGamesColors.Neutral.SurfaceVariant,
    onSurfaceVariant = RoshniGamesColors.Neutral.OnSurfaceVariant,
    outline = RoshniGamesColors.Neutral.Outline,
    outlineVariant = RoshniGamesColors.Neutral.OutlineVariant,
    scrim = RoshniGamesColors.Neutral.Scrim,
    inverseSurface = RoshniGamesColors.Neutral.InverseSurface,
    inverseOnSurface = RoshniGamesColors.Neutral.InverseOnSurface,
    inversePrimary = RoshniGamesColors.Primary.Inverse,
    surfaceTint = RoshniGamesColors.Primary.Main,
)

@Composable
fun RoshniGamesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RoshniGamesTypography,
        content = content
    )
}