package com.roshni.games.gametemplates.casual

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
 * Template for casual games (Match-3, Bubble Poppers, Time Management, Hidden Object, etc.)
 */
abstract class CasualGameTemplate(
    context: Context,
    gameId: String,
    gameView: View
) : BaseGameTemplate(context, gameId, gameView) {

    // Casual-specific state
    private val _gameBoard = MutableStateFlow<List<List<GamePiece>>>(emptyList())
    val gameBoard: StateFlow<List<List<GamePiece>>> = _gameBoard.asStateFlow()

    private val _selectedPieces = MutableStateFlow<List<GamePiece>>(emptyList())
    val selectedPieces: StateFlow<List<GamePiece>> = _selectedPieces.asStateFlow()

    private val _powerUps = MutableStateFlow<List<CasualPowerUp>>(emptyList())
    val powerUps: StateFlow<List<CasualPowerUp>> = _powerUps.asStateFlow()

    private val _objectives = MutableStateFlow<List<GameObjective>>(emptyList())
    val objectives: StateFlow<List<GameObjective>> = _objectives.asStateFlow()

    private val _objectiveProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val objectiveProgress: StateFlow<Map<String, Int>> = _objectiveProgress.asStateFlow()

    // Casual configuration
    protected val casualConfig = CasualConfig()

    override fun initializeGameState() {
        // Initialize casual-specific state
        _gameBoard.value = emptyList()
        _selectedPieces.value = emptyList()
        _powerUps.value = emptyList()
        _objectives.value = createObjectives()
        _objectiveProgress.value = _objectives.value.associate { it.id to 0 }
    }

    override fun loadGameAssets() {
        // Load common casual game assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "casual/match_sound.mp3",
                    "casual/pop_sound.mp3",
                    "casual/powerup_sound.mp3",
                    "casual/level_complete.mp3"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load casual assets")
            }
        }
    }

    override fun setupInputHandling() {
        // Setup casual-specific input handling
        viewModelScope.launch {
            inputSystem.touchEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.TouchEvent.Down -> {
                        onCasualTouch(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Move -> {
                        onCasualDrag(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Up -> {
                        onCasualRelease(event.x, event.y)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setupAudio() {
        // Setup casual-specific audio
        if (gameConfig.enableAudio) {
            audioSystem.setMusicVolume(0.5f) // Medium volume for casual games
            audioSystem.setSfxVolume(0.7f)
        }
    }

    override fun initializeGame() {
        // Initialize casual-specific game state
        initializeGameBoard()
        spawnInitialPowerUps()
        startCasualGame()
    }

    override fun updateGame(deltaTime: Float) {
        // Update casual-specific game logic
        updateGameBoard(deltaTime)
        updatePowerUps(deltaTime)
        updateObjectives(deltaTime)
        updateCasualGame(deltaTime)
    }

    override fun updateGameProgress() {
        // Update casual game progress
        _gameState.value?.let { state ->
            val updatedState = state.copy(
                score = _score.value,
                level = _level.value,
                customData = state.customData + mapOf(
                    "boardSize" to _gameBoard.value.size.toString(),
                    "selectedPieces" to _selectedPieces.value.size.toString(),
                    "powerUps" to _powerUps.value.size.toString(),
                    "objectives" to _objectives.value.size.toString()
                )
            )
            _gameState.value = updatedState
        }
    }

    override fun onScoreChanged(newScore: Long) {
        // Casual-specific score handling
        if (newScore > 0) {
            playSoundEffect("match_sound")
        }
    }

    override fun onGameOver() {
        // Casual-specific game over handling
        if (_score.value > 0) {
            playSoundEffect("level_complete")
        }
    }

    /**
     * Initialize the game board
     */
    protected abstract fun initializeGameBoard()

    /**
     * Create game objectives
     */
    protected abstract fun createObjectives(): List<GameObjective>

    /**
     * Spawn initial power-ups
     */
    protected fun spawnInitialPowerUps() {
        _powerUps.value = generateInitialPowerUps()
    }

    /**
     * Generate initial power-ups
     */
    protected abstract fun generateInitialPowerUps(): List<CasualPowerUp>

    /**
     * Select game piece
     */
    protected fun selectPiece(piece: GamePiece) {
        if (casualConfig.allowMultiSelect) {
            if (_selectedPieces.value.contains(piece)) {
                _selectedPieces.value = _selectedPieces.value - piece
            } else {
                _selectedPieces.value = _selectedPieces.value + piece
            }
        } else {
            _selectedPieces.value = listOf(piece)
        }
    }

    /**
     * Clear selection
     */
    protected fun clearSelection() {
        _selectedPieces.value = emptyList()
    }

    /**
     * Process selected pieces (match, remove, etc.)
     */
    protected fun processSelection() {
        if (_selectedPieces.value.isNotEmpty()) {
            val points = calculatePoints(_selectedPieces.value)
            addScore(points)

            // Update objectives
            updateObjectiveProgress(_selectedPieces.value)

            // Remove processed pieces
            removePieces(_selectedPieces.value)

            // Apply power-up effects
            applyPowerUpEffects()

            // Clear selection
            clearSelection()

            // Check for level completion
            checkLevelCompletion()

            playSoundEffect("pop_sound")
        }
    }

    /**
     * Calculate points for selected pieces
     */
    protected abstract fun calculatePoints(pieces: List<GamePiece>): Long

    /**
     * Remove pieces from board
     */
    protected abstract fun removePieces(pieces: List<GamePiece>)

    /**
     * Update objective progress
     */
    protected fun updateObjectiveProgress(pieces: List<GamePiece>) {
        val currentProgress = _objectiveProgress.value.toMutableMap()

        _objectives.value.forEach { objective ->
            val progress = calculateObjectiveProgress(objective, pieces)
            currentProgress[objective.id] = (currentProgress[objective.id] ?: 0) + progress
        }

        _objectiveProgress.value = currentProgress
    }

    /**
     * Calculate progress for specific objective
     */
    protected abstract fun calculateObjectiveProgress(objective: GameObjective, pieces: List<GamePiece>): Int

    /**
     * Apply power-up effects
     */
    protected abstract fun applyPowerUpEffects()

    /**
     * Check if level is completed
     */
    protected fun checkLevelCompletion(): Boolean {
        val completedObjectives = _objectives.value.count { objective ->
            (_objectiveProgress.value[objective.id] ?: 0) >= objective.targetValue
        }

        if (completedObjectives >= _objectives.value.size) {
            completeLevel()
            return true
        }

        return false
    }

    /**
     * Complete current level
     */
    protected fun completeLevel() {
        playSoundEffect("level_complete")
        nextLevel()
        onLevelCompleted()
    }

    /**
     * Add power-up
     */
    protected fun addPowerUp(powerUp: CasualPowerUp) {
        _powerUps.value = _powerUps.value + powerUp
    }

    /**
     * Use power-up
     */
    protected fun usePowerUp(powerUp: CasualPowerUp) {
        _powerUps.value = _powerUps.value - powerUp
        applyPowerUp(powerUp)
        playSoundEffect("powerup_sound")
    }

    /**
     * Apply power-up effect
     */
    protected abstract fun applyPowerUp(powerUp: CasualPowerUp)

    /**
     * Abstract methods for casual-specific implementation
     */
    protected abstract fun onCasualTouch(x: Float, y: Float)
    protected abstract fun onCasualDrag(x: Float, y: Float)
    protected abstract fun onCasualRelease(x: Float, y: Float)
    protected abstract fun updateGameBoard(deltaTime: Float)
    protected abstract fun updatePowerUps(deltaTime: Float)
    protected abstract fun updateObjectives(deltaTime: Float)
    protected abstract fun updateCasualGame(deltaTime: Float)
    protected abstract fun startCasualGame()
    protected abstract fun onLevelCompleted()

    /**
     * Casual configuration
     */
    protected data class CasualConfig(
        val boardWidth: Int = 8,
        val boardHeight: Int = 8,
        val allowMultiSelect: Boolean = false,
        val enableGravity: Boolean = true,
        val enablePowerUps: Boolean = true,
        val enableObjectives: Boolean = true,
        val pieceTypes: List<PieceType> = listOf(
            PieceType.RED, PieceType.BLUE, PieceType.GREEN, PieceType.YELLOW
        )
    )

    /**
     * Game piece
     */
    data class GamePiece(
        val id: String,
        val type: PieceType,
        val x: Int,
        val y: Int,
        val special: Boolean = false,
        val power: Int = 1
    )

    /**
     * Piece types
     */
    enum class PieceType(val color: androidx.compose.ui.graphics.Color, val points: Int) {
        RED(androidx.compose.ui.graphics.Color.Red, 10),
        BLUE(androidx.compose.ui.graphics.Color.Blue, 10),
        GREEN(androidx.compose.ui.graphics.Color.Green, 10),
        YELLOW(androidx.compose.ui.graphics.Color.Yellow, 10),
        PURPLE(androidx.compose.ui.graphics.Color.Magenta, 15),
        ORANGE(androidx.compose.ui.graphics.Color(0xFFFF9800), 15)
    }

    /**
     * Power-up for casual games
     */
    data class CasualPowerUp(
        val id: String,
        val type: PowerUpType,
        val x: Float,
        val y: Float,
        val effect: String,
        val duration: Float? = null
    )

    /**
     * Power-up types
     */
    enum class PowerUpType {
        BOMB, ROCKET, COLOR_BURST, TIME_FREEZE, MULTIPLIER
    }

    /**
     * Game objective
     */
    data class GameObjective(
        val id: String,
        val type: ObjectiveType,
        val targetValue: Int,
        val description: String,
        val reward: Long = 100
    )

    /**
     * Objective types
     */
    enum class ObjectiveType {
        SCORE_TARGET, PIECES_DESTROYED, COMBO_COUNT, TIME_LIMIT
    }

    /**
     * Casual game view model
     */
    abstract class CasualGameViewModel : GameViewModel() {

        private val _gameBoard = MutableStateFlow<List<List<GamePiece>>>(emptyList())
        val gameBoard: StateFlow<List<List<GamePiece>>> = _gameBoard.asStateFlow()

        private val _selectedPieces = MutableStateFlow<List<GamePiece>>(emptyList())
        val selectedPieces: StateFlow<List<GamePiece>> = _selectedPieces.asStateFlow()

        private val _powerUps = MutableStateFlow<List<CasualPowerUp>>(emptyList())
        val powerUps: StateFlow<List<CasualPowerUp>> = _powerUps.asStateFlow()

        private val _objectives = MutableStateFlow<List<GameObjective>>(emptyList())
        val objectives: StateFlow<List<GameObjective>> = _objectives.asStateFlow()

        private val _objectiveProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
        val objectiveProgress: StateFlow<Map<String, Int>> = _objectiveProgress.asStateFlow()

        fun setGameBoard(board: List<List<GamePiece>>) {
            _gameBoard.value = board
        }

        fun setSelectedPieces(pieces: List<GamePiece>) {
            _selectedPieces.value = pieces
        }

        fun addSelectedPiece(piece: GamePiece) {
            _selectedPieces.value = _selectedPieces.value + piece
        }

        fun removeSelectedPiece(piece: GamePiece) {
            _selectedPieces.value = _selectedPieces.value - piece
        }

        fun clearSelection() {
            _selectedPieces.value = emptyList()
        }

        fun addPowerUp(powerUp: CasualPowerUp) {
            _powerUps.value = _powerUps.value + powerUp
        }

        fun removePowerUp(powerUp: CasualPowerUp) {
            _powerUps.value = _powerUps.value - powerUp
        }

        fun setObjectives(objectives: List<GameObjective>) {
            _objectives.value = objectives
        }

        fun updateObjectiveProgress(progress: Map<String, Int>) {
            _objectiveProgress.value = progress
        }
    }

    @Composable
    override fun GameContent() {
        val gameViewModel = getGameViewModel() as CasualGameViewModel
        val currentBoard by gameViewModel.gameBoard.collectAsState()
        val currentSelected by gameViewModel.selectedPieces.collectAsState()
        val currentPowerUps by gameViewModel.powerUps.collectAsState()
        val currentObjectives by gameViewModel.objectives.collectAsState()
        val currentProgress by gameViewModel.objectiveProgress.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            // Casual game header
            CasualGameHeader(
                score = _score.collectAsState().value,
                level = _level.collectAsState().value,
                objectives = currentObjectives,
                objectiveProgress = currentProgress
            )

            // Main casual game content
            CasualGameContent(
                gameBoard = currentBoard,
                selectedPieces = currentSelected,
                powerUps = currentPowerUps
            )
        }
    }

    @Composable
    protected abstract fun CasualGameHeader(
        score: Long,
        level: Int,
        objectives: List<GameObjective>,
        objectiveProgress: Map<String, Int>
    )

    @Composable
    protected abstract fun CasualGameContent(
        gameBoard: List<List<GamePiece>>,
        selectedPieces: List<GamePiece>,
        powerUps: List<CasualPowerUp>
    )
}