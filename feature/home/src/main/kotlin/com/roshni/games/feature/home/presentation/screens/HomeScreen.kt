package com.roshni.games.feature.home.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roshni.games.core.designsystem.theme.RoshniGamesTheme
import com.roshni.games.feature.home.domain.model.HomeNavigationEvent
import com.roshni.games.feature.home.presentation.components.GameCard
import com.roshni.games.feature.home.presentation.components.WelcomeHeader
import com.roshni.games.feature.home.presentation.viewmodel.HomeAction
import com.roshni.games.feature.home.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToGame: (String) -> Unit,
    onNavigateToGameLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    RoshniGamesTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                HomeTopBar(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.onAction(HomeAction.RefreshData) },
                    onSettingsClick = { viewModel.onAction(HomeAction.NavigateToSettings) }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Header
                if (uiState.showWelcomeMessage) {
                    item {
                        WelcomeHeader(
                            userStats = uiState.homeState.userStats,
                            onDismiss = { viewModel.onAction(HomeAction.DismissWelcomeMessage) }
                        )
                    }
                }

                // Recently Played Games
                if (uiState.homeState.recentlyPlayedGames.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Continue Playing",
                            onViewAll = { viewModel.onAction(HomeAction.NavigateToGameLibrary) }
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(uiState.homeState.recentlyPlayedGames) { game ->
                                GameCard(
                                    game = game,
                                    onClick = { viewModel.onAction(HomeAction.NavigateToGame(game.id)) }
                                )
                            }
                        }
                    }
                }

                // Game Recommendations
                if (uiState.homeState.recommendedGames.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recommended for You",
                            onViewAll = { viewModel.onAction(HomeAction.NavigateToGameLibrary) }
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(uiState.homeState.recommendedGames) { recommendation ->
                                GameCard(
                                    game = recommendation.game,
                                    subtitle = recommendation.reason,
                                    onClick = { viewModel.onAction(HomeAction.NavigateToGame(recommendation.game.id)) }
                                )
                            }
                        }
                    }
                }

                // Popular Categories
                if (uiState.homeState.popularCategories.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Popular Categories",
                            onViewAll = { viewModel.onAction(HomeAction.NavigateToGameLibrary) }
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(uiState.homeState.popularCategories) { category ->
                                CategoryCard(
                                    category = category,
                                    onClick = { viewModel.onAction(HomeAction.NavigateToCategory(category.id)) }
                                )
                            }
                        }
                    }
                }

                // Achievement Highlights
                if (uiState.homeState.achievementHighlights.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recent Achievements",
                            onViewAll = { viewModel.onAction(HomeAction.NavigateToProfile) }
                        )
                    }

                    items(uiState.homeState.achievementHighlights) { achievement ->
                        AchievementCard(achievement = achievement)
                    }
                }

                // Spacer for bottom padding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Handle navigation events
        LaunchedEffect(viewModel.navigationEvent) {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    HomeNavigationEvent.NavigateToGameLibrary -> onNavigateToGameLibrary()
                    HomeNavigationEvent.NavigateToProfile -> onNavigateToProfile()
                    HomeNavigationEvent.NavigateToSettings -> onNavigateToSettings()
                    is HomeNavigationEvent.NavigateToGame -> onNavigateToGame(event.gameId)
                    is HomeNavigationEvent.NavigateToCategory -> {
                        // For now, navigate to game library with category filter
                        onNavigateToGameLibrary()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Roshni Games",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Welcome back!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(androidx.core.R.drawable.ic_call_answer),
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
private fun SectionHeader(
    title: String,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "View All",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onViewAll)
        )
    }
}

@Composable
private fun CategoryCard(
    category: com.roshni.games.feature.home.domain.model.CategorySummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎮",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${category.gameCount} games",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: com.roshni.games.feature.home.domain.model.AchievementSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (achievement.rarity) {
                            com.roshni.games.feature.home.domain.model.AchievementRarity.COMMON -> Color.Yellow
                            com.roshni.games.feature.home.domain.model.AchievementRarity.RARE -> Color.Blue
                            com.roshni.games.feature.home.domain.model.AchievementRarity.EPIC -> Color.Magenta
                            com.roshni.games.feature.home.domain.model.AchievementRarity.LEGENDARY -> Color.Cyan
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏆",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = achievement.rarity.name,
                style = MaterialTheme.typography.bodySmall,
                color = when (achievement.rarity) {
                    com.roshni.games.feature.home.domain.model.AchievementRarity.COMMON -> Color.Yellow
                    com.roshni.games.feature.home.domain.model.AchievementRarity.RARE -> Color.Blue
                    com.roshni.games.feature.home.domain.model.AchievementRarity.EPIC -> Color.Magenta
                    com.roshni.games.feature.home.domain.model.AchievementRarity.LEGENDARY -> Color.Cyan
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}