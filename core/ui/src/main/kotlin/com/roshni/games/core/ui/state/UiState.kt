package com.roshni.games.core.ui.state

/**
 * Sealed class representing different states of UI data loading
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val throwable: Throwable,
        val retry: (() -> Unit)? = null
    ) : UiState<Nothing>()
}

/**
 * Extension functions for UiState
 */
fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading

fun <T> UiState<T>.isSuccess(): Boolean = this is UiState.Success

fun <T> UiState<T>.isError(): Boolean = this is UiState.Error

fun <T> UiState<T>.getDataOrNull(): T? = (this as? UiState.Success)?.data

fun <T> UiState<T>.getErrorOrNull(): Throwable? = (this as? UiState.Error)?.throwable

/**
 * Convert a nullable value to UiState
 */
fun <T> T?.toUiState(): UiState<T> {
    return this?.let { UiState.Success(it) } ?: UiState.Error(NullPointerException("Data is null"))
}

/**
 * Convert Result to UiState
 */
fun <T> Result<T>.toUiState(): UiState<T> {
    return fold(
        onSuccess = { UiState.Success(it) },
        onFailure = { UiState.Error(it) }
    )
}