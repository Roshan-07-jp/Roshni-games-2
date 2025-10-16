package com.roshni.games.feature.profile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roshni.games.feature.profile.domain.model.Achievement

@Composable
fun AchievementCard(
    achievement: Achievement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Achievement Icon and Lock Status
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (achievement.unlockedAt != null) {
                                when (achievement.rarity) {
                                    com.roshni.games.feature.profile.domain.model.AchievementRarity.COMMON -> Color.Yellow.copy(alpha = 0.2f)
                                    com.roshni.games.feature.profile.domain.model.AchievementRarity.RARE -> Color.Blue.copy(alpha = 0.2f)
                                    com.roshni.games.feature.profile.domain.model.AchievementRarity.EPIC -> Color.Magenta.copy(alpha = 0.2f)
                                    com.roshni.games.feature.profile.domain.model.AchievementRarity.LEGENDARY -> Color.Cyan.copy(alpha = 0.2f)
                                }
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (achievement.unlockedAt != null) {
                        Text(
                            text = "🏆",
                            fontSize = 24.sp
                        )
                    } else {
                        Text(
                            text = "🔒",
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Achievement Title
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Achievement Description
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )

            // Progress (if not completed)
            if (achievement.unlockedAt == null) {
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    // Progress Text
                    Text(
                        text = "${(achievement.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = achievement.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Points and Rarity
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Points
                Text(
                    text = "${achievement.points} pts",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )

                // Rarity Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (achievement.rarity) {
                                com.roshni.games.feature.profile.domain.model.AchievementRarity.COMMON -> Color.Yellow.copy(alpha = 0.2f)
                                com.roshni.games.feature.profile.domain.model.AchievementRarity.RARE -> Color.Blue.copy(alpha = 0.2f)
                                com.roshni.games.feature.profile.domain.model.AchievementRarity.EPIC -> Color.Magenta.copy(alpha = 0.2f)
                                com.roshni.games.feature.profile.domain.model.AchievementRarity.LEGENDARY -> Color.Cyan.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = achievement.rarity.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = when (achievement.rarity) {
                            com.roshni.games.feature.profile.domain.model.AchievementRarity.COMMON -> Color.Yellow.copy(alpha = 0.8f)
                            com.roshni.games.feature.profile.domain.model.AchievementRarity.RARE -> Color.Blue.copy(alpha = 0.8f)
                            com.roshni.games.feature.profile.domain.model.AchievementRarity.EPIC -> Color.Magenta.copy(alpha = 0.8f)
                            com.roshni.games.feature.profile.domain.model.AchievementRarity.LEGENDARY -> Color.Cyan.copy(alpha = 0.8f)
                        },
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}