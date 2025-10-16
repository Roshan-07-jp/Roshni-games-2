package com.roshni.games.feature.profile.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roshni.games.core.designsystem.theme.RoshniGamesTheme
import com.roshni.games.feature.profile.domain.model.ProfileNavigationEvent
import com.roshni.games.feature.profile.presentation.components.AchievementCard
import com.roshni.games.feature.profile.presentation.components.ProfileHeader
import com.roshni.games.feature.profile.presentation.components.StatsCard
import com.roshni.games.feature.profile.presentation.viewmodel.ProfileAction
import com.roshni.games.feature.profile.presentation.viewmodel.ProfileTab
import com.roshni.games.feature.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToGame: (String) -> Unit
) {
    RoshniGamesTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                ProfileTopBar(
                    onSettingsClick = onNavigateToSettings,
                    onRefreshClick = { viewModel.onAction(ProfileAction.RefreshProfile) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // Profile Header
                uiState.profileState.userProfile?.let { profile ->
                    ProfileHeader(
                        profile = profile,
                        onEditClick = { viewModel.onAction(ProfileAction.EditProfile) },
                        onCustomizeClick = { viewModel.onAction(ProfileAction.CustomizeProfile) }
                    )
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileTab.values().forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.onAction(ProfileAction.SelectTab(tab)) },
                            text = {
                                Text(
                                    text = tab.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.selectedTab) {
                        ProfileTab.OVERVIEW -> OverviewTab(uiState = uiState, onAction = viewModel::onAction)
                        ProfileTab.ACHIEVEMENTS -> AchievementsTab(uiState = uiState, onAction = viewModel::onAction)
                        ProfileTab.STATISTICS -> StatisticsTab(uiState = uiState)
                        ProfileTab.ACTIVITY -> ActivityTab(uiState = uiState, onAction = viewModel::onAction)
                    }
                }
            }
        }

        // Handle navigation events
        LaunchedEffect(viewModel.navigationEvent) {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    ProfileNavigationEvent.NavigateToSettings -> onNavigateToSettings()
                    is ProfileNavigationEvent.NavigateToGame -> onNavigateToGame(event.gameId)
                    else -> { /* Handle other navigation events */ }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun OverviewTab(
    uiState: com.roshni.games.feature.profile.presentation.viewmodel.ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.profileState.gameStatistics.let { stats ->
                    StatsCard(
                        title = "Games Played",
                        value = stats.totalGamesPlayed.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "Play Time",
                        value = "${stats.totalPlayTime / 60}h",
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "Achievements",
                        value = uiState.profileState.achievements.count { it.unlockedAt != null }.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Achievements
        if (uiState.profileState.achievements.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    val unlockedAchievements = uiState.profileState.achievements
                        .filter { it.unlockedAt != null }
                        .take(5)

                    items(unlockedAchievements) { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            onClick = { onAction(ProfileAction.ViewAchievement(achievement)) }
                        )
                    }
                }
            }
        }

        // Recent Activity
        if (uiState.profileState.recentActivity.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
            }

            items(uiState.profileState.recentActivity.take(5)) { activity ->
                ActivityItem(
                    activity = activity,
                    onGameClick = { gameId ->
                        gameId?.let { onAction(ProfileAction.NavigateToGame(it)) }
                    }
                )
            }
        }

        // Spacer for bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AchievementsTab(
    uiState: com.roshni.games.feature.profile.presentation.viewmodel.ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.profileState.achievements) { achievement ->
            AchievementCard(
                achievement = achievement,
                onClick = { onAction(ProfileAction.ViewAchievement(achievement)) }
            )
        }

        // Spacer for bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatisticsTab(
    uiState: com.roshni.games.feature.profile.presentation.viewmodel.ProfileUiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Stats
        item {
            Text(
                text = "Overall Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        uiState.profileState.gameStatistics.let { stats ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsCard(
                        title = "Total Score",
                        value = stats.highestScore.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "Current Streak",
                        value = "${stats.currentStreak} days",
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "Best Streak",
                        value = "${stats.longestStreak} days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Category Stats
        if (uiState.profileState.gameStatistics.categoryStats.isNotEmpty()) {
            item {
                Text(
                    text = "By Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
            }

            uiState.profileState.gameStatistics.categoryStats.values.forEach { categoryStats ->
                item {
                    CategoryStatsCard(categoryStats = categoryStats)
                }
            }
        }

        // Spacer for bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActivityTab(
    uiState: com.roshni.games.feature.profile.presentation.viewmodel.ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.profileState.recentActivity.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent activity",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(uiState.profileState.recentActivity) { activity ->
                ActivityItem(
                    activity = activity,
                    onGameClick = { gameId ->
                        gameId?.let { onAction(ProfileAction.NavigateToGame(it)) }
                    }
                )
            }
        }

        // Spacer for bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActivityItem(
    activity: com.roshni.games.feature.profile.domain.model.ProfileActivity,
    onGameClick: (String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGameClick(activity.gameId) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Activity Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (activity.type) {
                            com.roshni.games.feature.profile.domain.model.ActivityType.ACHIEVEMENT_UNLOCKED -> Color.Yellow.copy(alpha = 0.2f)
                            com.roshni.games.feature.profile.domain.model.ActivityType.HIGH_SCORE -> Color.Green.copy(alpha = 0.2f)
                            com.roshni.games.feature.profile.domain.model.ActivityType.GAME_COMPLETED -> Color.Blue.copy(alpha = 0.2f)
                            com.roshni.games.feature.profile.domain.model.ActivityType.LEVEL_UP -> Color.Purple.copy(alpha = 0.2f)
                            com.roshni.games.feature.profile.domain.model.ActivityType.STREAK_MILESTONE -> Color.Orange.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (activity.type) {
                        com.roshni.games.feature.profile.domain.model.ActivityType.ACHIEVEMENT_UNLOCKED -> "🏆"
                        com.roshni.games.feature.profile.domain.model.ActivityType.HIGH_SCORE -> "⭐"
                        com.roshni.games.feature.profile.domain.model.ActivityType.GAME_COMPLETED -> "🎯"
                        com.roshni.games.feature.profile.domain.model.ActivityType.LEVEL_UP -> "⬆️"
                        com.roshni.games.feature.profile.domain.model.ActivityType.STREAK_MILESTONE -> "🔥"
                    },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Activity Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = formatTimeAgo(activity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Score (if available)
            activity.score?.let { score ->
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CategoryStatsCard(
    categoryStats: com.roshni.games.feature.profile.domain.model.CategoryStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryStats.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${categoryStats.gamesPlayed} games • ${categoryStats.totalPlayTime / 60}h played",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Best: ${categoryStats.bestScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Avg: ${categoryStats.averageScore.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatTimeAgo(dateTime: kotlinx.datetime.LocalDateTime): String {
    val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val diff = now.toEpochDays() - dateTime.toEpochDays()

    return when {
        diff == 0L -> "Today"
        diff == 1L -> "Yesterday"
        diff < 7L -> "$diff days ago"
        else -> "A while ago"
    }
}