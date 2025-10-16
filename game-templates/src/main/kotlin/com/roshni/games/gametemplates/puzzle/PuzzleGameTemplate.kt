package com.roshni.games.gametemplates.puzzle

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.roshni.games.gametemplates.base.BaseGameTemplate
import com.roshni.games.gametemplates.base.GameUiState
import com.roshni.games.gametemplates.base.GameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Template for puzzle games (Sudoku, Crosswords, Jigsaw, Match-3, etc.)
 */
abstract class PuzzleGameTemplate(
    context: Context,
    gameId: String,
    gameView: View
) : BaseGameTemplate(context, gameId, gameView) {

    // Puzzle-specific state
    private val _moves = MutableStateFlow(0)
    val moves: StateFlow<Int> = _moves.asStateFlow()

    private val _hintsUsed = MutableStateFlow(0)
    val hintsUsed: StateFlow<Int> = _hintsUsed.asStateFlow()

    private val _timeElapsed = MutableStateFlow(0L)
    val timeElapsed: StateFlow<Long> = _timeElapsed.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    // Puzzle configuration
    protected val puzzleConfig = PuzzleConfig()

    override fun initializeGameState() {
        // Initialize puzzle-specific state
        _moves.value = 0
        _hintsUsed.value = 0
        _timeElapsed.value = 0L
        _isComplete.value = false
    }

    override fun loadGameAssets() {
        // Load common puzzle assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "puzzle/hint_button.png",
                    "puzzle/reset_button.png",
                    "puzzle/complete_sound.mp3",
                    "puzzle/hint_sound.mp3"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load puzzle assets")
            }
        }
    }

    override fun setupInputHandling() {
        // Setup puzzle-specific input handling
        viewModelScope.launch {
            inputSystem.touchEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.TouchEvent.Down -> {
                        onPuzzleTouch(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Move -> {
                        onPuzzleDrag(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Up -> {
                        onPuzzleRelease(event.x, event.y)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setupAudio() {
        // Setup puzzle-specific audio
        if (gameConfig.enableAudio) {
            audioSystem.setMusicVolume(0.3f) // Lower volume for puzzle games
            audioSystem.setSfxVolume(0.7f)
        }
    }

    override fun initializeGame() {
        // Initialize puzzle-specific game state
        generatePuzzle()
        startTimer()
    }

    override fun updateGame(deltaTime: Float) {
        // Update puzzle-specific game logic
        updatePuzzle(deltaTime)
    }

    override fun updateGameProgress() {
        // Update puzzle progress
        _gameState.value?.let { state ->
            val updatedState = state.copy(
                score = _score.value,
                level = _level.value,
                customData = state.customData + mapOf(
                    "moves" to _moves.value.toString(),
                    "hintsUsed" to _hintsUsed.value.toString(),
                    "timeElapsed" to _timeElapsed.value.toString(),
                    "isComplete" to _isComplete.value.toString()
                )
            )
            _gameState.value = updatedState
        }
    }

    override fun onScoreChanged(newScore: Long) {
        // Puzzle-specific score handling
        if (newScore > 0 && newScore % 1000 == 0L) {
            playSoundEffect("milestone_sound")
        }
    }

    override fun onGameOver() {
        // Puzzle-specific game over handling
        playSoundEffect("game_over_sound")
        stopTimer()
    }

    /**
     * Record a move
     */
    protected fun recordMove() {
        _moves.value++
        playSoundEffect("move_sound")
    }

    /**
     * Use a hint
     */
    protected fun useHint() {
        if (_hintsUsed.value < puzzleConfig.maxHints) {
            _hintsUsed.value++
            playSoundEffect("hint_sound")
            onHintUsed()
        }
    }

    /**
     * Complete the puzzle
     */
    protected fun completePuzzle() {
        _isComplete.value = true
        stopTimer()
        playSoundEffect("complete_sound")

        // Bonus points for completion
        val timeBonus = calculateTimeBonus()
        val moveBonus = calculateMoveBonus()
        addScore(timeBonus + moveBonus)

        onPuzzleCompleted()
    }

    /**
     * Reset the puzzle
     */
    protected fun resetPuzzle() {
        _moves.value = 0
        _hintsUsed.value = 0
        _timeElapsed.value = 0L
        _isComplete.value = false

        generatePuzzle()
        startTimer()
        playSoundEffect("reset_sound")
    }

    /**
     * Start the game timer
     */
    private fun startTimer() {
        viewModelScope.launch {
            while (_isGameActive.value && !_isComplete.value) {
                kotlinx.coroutines.delay(1000)
                _timeElapsed.value++
            }
        }
    }

    /**
     * Stop the game timer
     */
    private fun stopTimer() {
        // Timer will stop automatically when coroutine is cancelled
    }

    /**
     * Abstract methods for puzzle-specific implementation
     */
    protected abstract fun generatePuzzle()
    protected abstract fun onPuzzleTouch(x: Float, y: Float)
    protected abstract fun onPuzzleDrag(x: Float, y: Float)
    protected abstract fun onPuzzleRelease(x: Float, y: Float)
    protected abstract fun updatePuzzle(deltaTime: Float)
    protected abstract fun onHintUsed()
    protected abstract fun onPuzzleCompleted()
    protected abstract fun calculateTimeBonus(): Long
    protected abstract fun calculateMoveBonus(): Long

    /**
     * Puzzle configuration
     */
    protected data class PuzzleConfig(
        val maxHints: Int = 3,
        val timeLimit: Long? = null, // null for no time limit
        val allowUndo: Boolean = true,
        val showTimer: Boolean = true,
        val showMoveCounter: Boolean = true,
        val autoCheckCompletion: Boolean = true
    )

    /**
     * Puzzle game view model
     */
    abstract class PuzzleGameViewModel : GameViewModel() {

        private val _moves = MutableStateFlow(0)
        val moves: StateFlow<Int> = _moves.asStateFlow()

        private val _hintsUsed = MutableStateFlow(0)
        val hintsUsed: StateFlow<Int> = _hintsUsed.asStateFlow()

        private val _timeElapsed = MutableStateFlow(0L)
        val timeElapsed: StateFlow<Long> = _timeElapsed.asStateFlow()

        private val _isComplete = MutableStateFlow(false)
        val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

        fun recordMove() {
            _moves.value++
        }

        fun useHint() {
            _hintsUsed.value++
        }

        fun updateTime() {
            _timeElapsed.value++
        }

        fun completePuzzle() {
            _isComplete.value = true
        }

        fun resetPuzzle() {
            _moves.value = 0
            _hintsUsed.value = 0
            _timeElapsed.value = 0L
            _isComplete.value = false
        }
    }

    @Composable
    override fun GameContent() {
        val gameViewModel = getGameViewModel() as PuzzleGameViewModel
        val currentMoves by gameViewModel.moves.collectAsState()
        val currentHints by gameViewModel.hintsUsed.collectAsState()
        val currentTime by gameViewModel.timeElapsed.collectAsState()
        val completed by gameViewModel.isComplete.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game header with stats
            PuzzleGameHeader(
                moves = currentMoves,
                hintsUsed = currentHints,
                timeElapsed = currentTime,
                isComplete = completed,
                onHintClick = { useHint() },
                onResetClick = { resetPuzzle() }
            )

            // Main puzzle content
            PuzzleGameContent()

            // Game completion overlay
            if (completed) {
                PuzzleCompletionOverlay(
                    score = _score.collectAsState().value,
                    timeElapsed = currentTime,
                    moves = currentMoves,
                    onPlayAgain = { startNewGame() }
                )
            }
        }
    }

    @Composable
    protected abstract fun PuzzleGameHeader(
        moves: Int,
        hintsUsed: Int,
        timeElapsed: Long,
        isComplete: Boolean,
        onHintClick: () -> Unit,
        onResetClick: () -> Unit
    )

    @Composable
    protected abstract fun PuzzleGameContent()

    @Composable
    protected abstract fun PuzzleCompletionOverlay(
        score: Long,
        timeElapsed: Long,
        moves: Int,
        onPlayAgain: () -> Unit
    )
}

/**
 * Helper function to play sound effect
 */
fun BaseGameTemplate.playSoundEffect(soundId: String) {
    if (gameConfig.enableAudio) {
        audioSystem.playSoundEffect(soundId)
    }
}