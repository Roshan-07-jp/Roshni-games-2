package com.roshni.games.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.roshni.games.core.designsystem.theme.BorderRadius
import com.roshni.games.core.designsystem.theme.ComponentSizes
import com.roshni.games.core.designsystem.theme.Elevation
import com.roshni.games.core.designsystem.theme.GamingTypography
import com.roshni.games.core.designsystem.theme.IconSizes
import com.roshni.games.core.designsystem.theme.RoshniGamesColors
import com.roshni.games.core.designsystem.theme.Spacing

/**
 * Roshni Games Design System Components
 *
 * Reusable composables that implement the Roshni Games design system.
 */

// Gaming Button Component
@Composable
fun GamingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium
) {
    val buttonColors = when (variant) {
        ButtonVariant.Primary -> ButtonColors(
            background = RoshniGamesColors.Primary.Main,
            content = RoshniGamesColors.Primary.OnMain,
            disabledBackground = RoshniGamesColors.Neutral.Outline,
            disabledContent = RoshniGamesColors.Neutral.OnSurfaceVariant
        )
        ButtonVariant.Secondary -> ButtonColors(
            background = RoshniGamesColors.Secondary.Main,
            content = RoshniGamesColors.Secondary.OnMain,
            disabledBackground = RoshniGamesColors.Neutral.Outline,
            disabledContent = RoshniGamesColors.Neutral.OnSurfaceVariant
        )
        ButtonVariant.Success -> ButtonColors(
            background = RoshniGamesColors.Success.Main,
            content = RoshniGamesColors.Success.OnMain,
            disabledBackground = RoshniGamesColors.Neutral.Outline,
            disabledContent = RoshniGamesColors.Neutral.OnSurfaceVariant
        )
        ButtonVariant.Gaming -> ButtonColors(
            background = Brush.horizontalGradient(
                colors = listOf(
                    RoshniGamesColors.Gaming.PowerUpGlow,
                    RoshniGamesColors.Primary.Main
                )
            ),
            content = Color.White,
            disabledBackground = RoshniGamesColors.Neutral.Outline,
            disabledContent = RoshniGamesColors.Neutral.OnSurfaceVariant
        )
    }

    val buttonHeight = when (size) {
        ButtonSize.Small -> 36.dp
        ButtonSize.Medium -> ComponentSizes.ButtonMinHeight
        ButtonSize.Large -> 56.dp
    }

    Box(
        modifier = modifier
            .height(buttonHeight)
            .clip(RoundedCornerShape(BorderRadius.ButtonRadius))
            .background(
                if (enabled) buttonColors.background else buttonColors.disabledBackground,
                shape = RoundedCornerShape(BorderRadius.ButtonRadius)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = Spacing.Large),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = GamingTypography.GamingButton.copy(
                color = if (enabled) buttonColors.content else buttonColors.disabledContent
            ),
            textAlign = TextAlign.Center
        )
    }
}

// Score Display Component
@Composable
fun ScoreDisplay(
    score: Long,
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    Box(
        modifier = modifier
            .size(ComponentSizes.ScoreDisplaySize)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        RoshniGamesColors.Gaming.ScoreHighlight.copy(alpha = 0.8f),
                        RoshniGamesColors.Primary.Main.copy(alpha = 0.9f)
                    )
                )
            )
            .border(
                width = 3.dp,
                color = RoshniGamesColors.Primary.Shade200,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.toString(),
            style = GamingTypography.ScoreDisplay.copy(
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
    }
}

// Game Card Component
@Composable
fun GameCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSizes.GameCardHeight),
        shape = RoundedCornerShape(BorderRadius.GameCardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevation.GameCardElevation
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.GameCardPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = title,
                    modifier = Modifier.size(IconSizes.GameIcon),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(Spacing.Medium))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            subtitle?.let {
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Achievement Badge Component
@Composable
fun AchievementBadge(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    earned: Boolean = true
) {
    val badgeColors = if (earned) {
        Pair(
            RoshniGamesColors.Gaming.AchievementGold,
            RoshniGamesColors.Tertiary.Shade900
        )
    } else {
        Pair(
            RoshniGamesColors.Neutral.Outline,
            RoshniGamesColors.Neutral.OnSurfaceVariant
        )
    }

    Card(
        modifier = modifier
            .width(280.dp)
            .height(ComponentSizes.AchievementCardHeight),
        shape = RoundedCornerShape(BorderRadius.Large),
        colors = CardDefaults.cardColors(
            containerColor = badgeColors.first,
            contentColor = badgeColors.second
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevation.Medium
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(IconSizes.AchievementIcon),
                tint = badgeColors.second
            )

            Spacer(modifier = Modifier.width(Spacing.Medium))

            Column {
                Text(
                    text = title,
                    style = GamingTypography.AchievementTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = badgeColors.second.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Progress Bar Component
@Composable
fun GamingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = RoshniGamesColors.Gaming.LevelProgress,
    backgroundColor: Color = RoshniGamesColors.Neutral.SurfaceVariant
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSizes.ProgressBarHeight)
            .clip(RoundedCornerShape(BorderRadius.Small))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(ComponentSizes.ProgressBarHeight)
                .clip(RoundedCornerShape(BorderRadius.Small))
                .background(color)
        )
    }
}

// Data classes for component variants
enum class ButtonVariant {
    Primary, Secondary, Success, Gaming
}

enum class ButtonSize {
    Small, Medium, Large
}

data class ButtonColors(
    val background: Any,
    val content: Color,
    val disabledBackground: Color,
    val disabledContent: Color
)