package com.roshni.games.feature.splash.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roshni.games.core.designsystem.theme.RoshniGamesTheme
import com.roshni.games.feature.splash.presentation.components.UpdateDialog
import com.roshni.games.feature.splash.presentation.viewmodel.SplashNavigationEvent
import com.roshni.games.feature.splash.presentation.viewmodel.SplashViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToUpdate: (String) -> Unit,
    onShowError: (String) -> Unit
) {
    RoshniGamesTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            SplashContent(uiState = uiState)

            // Handle update dialog
            if (uiState.showUpdateDialog && uiState.updateInfo != null) {
                UpdateDialog(
                    updateInfo = uiState.updateInfo,
                    onUpdate = { viewModel.onUpdateConfirmed() },
                    onDismiss = { viewModel.onUpdateDialogDismissed() }
                )
            }

            // Handle error state
            if (uiState.error != null) {
                ErrorContent(
                    error = uiState.error,
                    onRetry = { viewModel.retryInitialization() }
                )
            }
        }

        // Handle navigation events
        LaunchedEffect(viewModel.navigationEvent) {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    SplashNavigationEvent.NavigateToHome -> onNavigateToHome()
                    SplashNavigationEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                    is SplashNavigationEvent.NavigateToUpdate -> onNavigateToUpdate(event.updateUrl)
                    is SplashNavigationEvent.ShowError -> onShowError(event.message)
                }
            }
        }
    }
}

@Composable
private fun SplashContent(uiState: com.roshni.games.feature.splash.presentation.viewmodel.SplashUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        // Logo/Brand Section
        BrandSection()

        // Progress Section
        ProgressSection(uiState = uiState)
    }
}

@Composable
private fun BrandSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        // Placeholder for logo - in real app this would be a drawable resource
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎮",
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Text(
            text = "Roshni Games",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Your Ultimate Gaming Experience",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ProgressSection(uiState: com.roshni.games.feature.splash.presentation.viewmodel.SplashUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 48.dp)
    ) {
        // Animated progress indicator
        val animatedProgress by animateFloatAsState(
            targetValue = uiState.progress,
            animationSpec = tween(durationMillis = 500),
            label = "progress"
        )

        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(
            text = uiState.currentStep.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 16.dp)
        )

        // Circular progress for loading state
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        androidx.compose.material3.Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Try Again")
        }
    }
}