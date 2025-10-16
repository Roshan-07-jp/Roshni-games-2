package com.roshni.games.feature.gamelibrary.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roshni.games.core.designsystem.theme.RoshniGamesTheme
import com.roshni.games.feature.gamelibrary.domain.model.GameLibraryNavigationEvent
import com.roshni.games.feature.gamelibrary.domain.model.ViewMode
import com.roshni.games.feature.gamelibrary.presentation.components.CategoryChip
import com.roshni.games.feature.gamelibrary.presentation.components.GameGridItem
import com.roshni.games.feature.gamelibrary.presentation.components.GameListItem
import com.roshni.games.feature.gamelibrary.presentation.viewmodel.GameLibraryAction
import com.roshni.games.feature.gamelibrary.presentation.viewmodel.GameLibraryViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun GameLibraryScreen(
    viewModel: GameLibraryViewModel = hiltViewModel(),
    onNavigateToGame: (String) -> Unit,
    onNavigateToCategory: (String) -> Unit
) {
    RoshniGamesTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                GameLibraryTopBar(
                    uiState = uiState,
                    onAction = viewModel::onAction
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // Search Bar
                if (uiState.showSearchBar) {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.onAction(GameLibraryAction.SearchQueryChanged(it)) },
                        onClose = { viewModel.onAction(GameLibraryAction.ToggleSearch) }
                    )
                }

                // Category Filter Chips
                if (uiState.selectedCategories.isNotEmpty() || uiState.selectedDifficulties.isNotEmpty()) {
                    FilterChips(
                        selectedCategories = uiState.selectedCategories,
                        selectedDifficulties = uiState.selectedDifficulties,
                        onClearFilters = { viewModel.onAction(GameLibraryAction.ClearFilters) }
                    )
                }

                // Content Area
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.gameLibraryState.isLoading && uiState.gameLibraryState.games.isEmpty() -> {
                            LoadingContent()
                        }
                        uiState.gameLibraryState.error != null -> {
                            ErrorContent(
                                error = uiState.gameLibraryState.error,
                                onRetry = { viewModel.onAction(GameLibraryAction.RefreshLibrary) },
                                onDismiss = { viewModel.onAction(GameLibraryAction.DismissError) }
                            )
                        }
                        uiState.gameLibraryState.games.isEmpty() -> {
                            EmptyContent()
                        }
                        else -> {
                            GameContent(
                                uiState = uiState,
                                onAction = viewModel::onAction
                            )
                        }
                    }
                }
            }
        }

        // Handle navigation events
        LaunchedEffect(viewModel.navigationEvent) {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is GameLibraryNavigationEvent.NavigateToGame -> onNavigateToGame(event.gameId)
                    is GameLibraryNavigationEvent.NavigateToCategory -> onNavigateToCategory(event.categoryId)
                    else -> { /* Handle other navigation events */ }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameLibraryTopBar(
    uiState: com.roshni.games.feature.gamelibrary.presentation.viewmodel.GameLibraryUiState,
    onAction: (GameLibraryAction) -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Game Library",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            // Search Button
            IconButton(onClick = { onAction(GameLibraryAction.ToggleSearch) }) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Filter Button
            IconButton(onClick = { onAction(GameLibraryAction.ToggleFilters) }) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // View Mode Toggle
            IconButton(
                onClick = { onAction(GameLibraryAction.ToggleViewMode) }
            ) {
                Icon(
                    if (uiState.gameLibraryState.viewMode == ViewMode.GRID) {
                        Icons.Default.List
                    } else {
                        Icons.Default.GridView
                    },
                    contentDescription = "Toggle View Mode",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Refresh Button
            IconButton(onClick = { onAction(GameLibraryAction.RefreshLibrary) }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search games...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close Search")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FilterChips(
    selectedCategories: List<String>,
    selectedDifficulties: List<String>,
    onClearFilters: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(selectedCategories) { category ->
            CategoryChip(
                text = category,
                onRemove = { /* Handle category removal */ }
            )
        }

        items(selectedDifficulties) { difficulty ->
            CategoryChip(
                text = difficulty,
                onRemove = { /* Handle difficulty removal */ }
            )
        }

        item {
            Text(
                text = "Clear All",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClearFilters)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun GameContent(
    uiState: com.roshni.games.feature.gamelibrary.presentation.viewmodel.GameLibraryUiState,
    onAction: (GameLibraryAction) -> Unit
) {
    AnimatedContent(
        targetState = uiState.gameLibraryState.viewMode,
        transitionSpec = { fadeIn() + scaleIn() }
    ) { viewMode ->
        when (viewMode) {
            ViewMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.gameLibraryState.games) { game ->
                        GameGridItem(
                            game = game,
                            onClick = { onAction(GameLibraryAction.GameClicked(game.id)) },
                            onFavoriteClick = { onAction(GameLibraryAction.ToggleFavorite(game.id)) }
                        )
                    }
                }
            }
            ViewMode.LIST -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.gameLibraryState.games) { game ->
                        GameListItem(
                            game = game,
                            onClick = { onAction(GameLibraryAction.GameClicked(game.id)) },
                            onFavoriteClick = { onAction(GameLibraryAction.ToggleFavorite(game.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading games...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }

            androidx.compose.material3.Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎮",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "No games found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Try adjusting your search or filters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}