package com.roshni.games.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.roshni.games.core.ui.state.UiState

@Composable
fun <T> StateHandler(
    state: UiState<T>,
    onLoading: @Composable (Modifier) -> Unit = { LoadingScreen(modifier = it) },
    onError: @Composable (Modifier, Throwable, (() -> Unit)?) -> Unit = { modifier, error, retry ->
        ErrorScreen(
            title = "Error",
            message = error.message ?: "Unknown error occurred",
            onRetry = retry,
            modifier = modifier
        )
    },
    onSuccess: @Composable (Modifier, T) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is UiState.Loading -> {
            onLoading(modifier)
        }
        is UiState.Error -> {
            onError(modifier, state.throwable, state.retry)
        }
        is UiState.Success -> {
            onSuccess(modifier, state.data)
        }
    }
}