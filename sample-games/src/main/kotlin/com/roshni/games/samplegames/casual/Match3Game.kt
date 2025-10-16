package com.roshni.games.samplegames.casual

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.gametemplates.casual.CasualGameTemplate
import com.roshni.games.gametemplates.casual.CasualGameViewModel
import com.roshni.games.gametemplates.casual.CasualPowerUp
import com.roshni.games.gametemplates.casual.GameObjective
import com.roshni.games.gametemplates.casual.GamePiece
import com.roshni.games.gametemplates.casual.PowerUpType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

/**
 * Fully playable Match-3 game implementation
 */
class Match3Game(
    context: Context,
    gameView: View
) : CasualGameTemplate(context, "match3-jewels", gameView) {

    private val _gameBoard = MutableStateFlow<List<List<GamePiece>>>(emptyList())
    val gameBoard: StateFlow<List<List<GamePiece>>> = _gameBoard.asStateFlow()

    private val _selectedPieces = MutableStateFlow<List<GamePiece>>(emptyList())
    val selectedPieces: StateFlow<List<GamePiece>> = _selectedPieces.asStateFlow()

    private val _animationQueue = MutableStateFlow<List<GameAnimation>>(emptyList())
    val animationQueue: StateFlow<List<GameAnimation>> = _animationQueue.asStateFlow()

    override fun initializeGameState() {
        super.initializeGameState()
        _gameBoard.value = emptyList()
        _selectedPieces.value = emptyList()
        _animationQueue.value = emptyList()
    }

    override fun loadGameAssets() {
        super.loadGameAssets()
        // Load Match-3 specific assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "match3/gem_blue.png",
                    "match3/gem_red.png",
                    "match3/gem_green.png",
                    "match3/gem_yellow.png",
                    "match3/gem_purple.png",
                    "match3/gem_orange.png",
                    "match3/explosion.png"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load Match-3 assets")
            }
        }
    }

    override fun setupInputHandling() {
        super.setupInputHandling()
        // Match-3 specific input handling is implemented in onCasualTouch
    }

    override fun setupAudio() {
        super.setupAudio()
        // Match-3 specific audio setup
    }

    override fun initializeGame() {
        super.initializeGame()
        initializeGameBoard()
    }

    override fun updateGame(deltaTime: Float) {
        super.updateGame(deltaTime)
        // Update animations
        updateAnimations(deltaTime)
    }

    override fun updateGameProgress() {
        super.updateGameProgress()
        // Check for matches and update board
        findAndProcessMatches()
    }

    override fun initializeGameBoard() {
        val board = mutableListOf<List<GamePiece>>()

        for (row in 0 until casualConfig.boardHeight) {
            val boardRow = mutableListOf<GamePiece>()
            for (col in 0 until casualConfig.boardWidth) {
                var piece: GamePiece
                do {
                    val type = casualConfig.pieceTypes.random()
                    piece = GamePiece(
                        id = "piece_${row}_${col}",
                        type = type,
                        x = col,
                        y = row
                    )
                } while (wouldCreateMatch(board, boardRow, piece, row, col))

                boardRow.add(piece)
            }
            board.add(boardRow)
        }

        _gameBoard.value = board
    }

    override fun createObjectives(): List<GameObjective> {
        return listOf(
            GameObjective(
                id = "score_target",
                type = com.roshni.games.gametemplates.casual.ObjectiveType.SCORE_TARGET,
                targetValue = 1000,
                description = "Score 1000 points",
                reward = 100
            ),
            GameObjective(
                id = "destroy_pieces",
                type = com.roshni.games.gametemplates.casual.ObjectiveType.PIECES_DESTROYED,
                targetValue = 50,
                description = "Destroy 50 pieces",
                reward = 150
            )
        )
    }

    override fun generateInitialPowerUps(): List<CasualPowerUp> {
        return listOf(
            CasualPowerUp(
                id = "bomb_1",
                type = PowerUpType.BOMB,
                x = 2f,
                y = 8f,
                effect = "Explode surrounding pieces"
            )
        )
    }

    override fun calculatePoints(pieces: List<GamePiece>): Long {
        return pieces.sumOf { it.type.points * it.power }.toLong()
    }

    override fun removePieces(pieces: List<GamePiece>) {
        // Add explosion animation for each removed piece
        pieces.forEach { piece ->
            _animationQueue.value = _animationQueue.value + GameAnimation.Explosion(
                x = (piece.x * 50).dp,
                y = (piece.y * 50).dp,
                duration = 500f
            )
        }

        // Remove pieces from board
        val newBoard = _gameBoard.value.map { row ->
            row.map { piece ->
                if (pieces.contains(piece)) {
                    null // Mark for removal
                } else {
                    piece
                }
            }.filterNotNull()
        }.toMutableList()

        // Apply gravity (make pieces fall down)
        applyGravity(newBoard)

        // Fill empty spaces
        fillEmptySpaces(newBoard)

        _gameBoard.value = newBoard
    }

    override fun calculateObjectiveProgress(objective: GameObjective, pieces: List<GamePiece>): Int {
        return when (objective.type) {
            com.roshni.games.gametemplates.casual.ObjectiveType.SCORE_TARGET -> {
                pieces.sumOf { it.type.points * it.power }
            }
            com.roshni.games.gametemplates.casual.ObjectiveType.PIECES_DESTROYED -> {
                pieces.size
            }
            else -> 0
        }
    }

    override fun applyPowerUpEffects() {
        // Apply gravity and fill empty spaces after power-up effects
        val newBoard = _gameBoard.value.map { row -> row.toMutableList() }.toMutableList()
        applyGravity(newBoard)
        fillEmptySpaces(newBoard)
        _gameBoard.value = newBoard
    }

    override fun onCasualTouch(x: Float, y: Float) {
        val cellSize = 50.dp
        val cellSizePx = cellSize.value * context.resources.displayMetrics.density

        val col = (x / cellSizePx).toInt().coerceIn(0, casualConfig.boardWidth - 1)
        val row = (y / cellSizePx).toInt().coerceIn(0, casualConfig.boardHeight - 1)

        selectPieceAt(row, col)
    }

    override fun onCasualDrag(x: Float, y: Float) {
        // Match-3 doesn't use drag gestures for selection
    }

    override fun onCasualRelease(x: Float, y: Float) {
        // Process selection when touch is released
        if (_selectedPieces.value.isNotEmpty()) {
            processSelection()
        }
    }

    override fun updateGameBoard(deltaTime: Float) {
        // Update board animations and effects
        val updatedAnimations = _animationQueue.value.filter { animation ->
            animation.update(deltaTime)
            animation.isActive
        }
        _animationQueue.value = updatedAnimations
    }

    override fun updatePowerUps(deltaTime: Float) {
        // Update power-up positions and effects
    }

    override fun updateObjectives(deltaTime: Float) {
        // Update objective progress over time
    }

    override fun updateCasualGame(deltaTime: Float) {
        // Main game loop updates
    }

    override fun startCasualGame() {
        Timber.d("Starting Match-3 game")
    }

    override fun onLevelCompleted() {
        Timber.d("Match-3 level completed!")
    }

    override fun applyPowerUp(powerUp: CasualPowerUp) {
        when (powerUp.type) {
            PowerUpType.BOMB -> {
                // Explode pieces in 3x3 area around power-up position
                val centerX = powerUp.x.toInt()
                val centerY = powerUp.y.toInt()

                val piecesToRemove = mutableListOf<GamePiece>()
                for (row in (centerY - 1).coerceAtLeast(0)..(centerY + 1).coerceAtMost(casualConfig.boardHeight - 1)) {
                    for (col in (centerX - 1).coerceAtLeast(0)..(centerX + 1).coerceAtMost(casualConfig.boardWidth - 1)) {
                        _gameBoard.value.getOrNull(row)?.getOrNull(col)?.let { piece ->
                            piecesToRemove.add(piece)
                        }
                    }
                }

                if (piecesToRemove.isNotEmpty()) {
                    removePieces(piecesToRemove)
                }
            }
            else -> {
                // Handle other power-up types
            }
        }
    }

    override fun getGameViewModel(): CasualGameViewModel {
        return Match3GameViewModel()
    }

    /**
     * Select piece at given position
     */
    private fun selectPieceAt(row: Int, col: Int) {
        val piece = _gameBoard.value.getOrNull(row)?.getOrNull(col)
        if (piece != null) {
            selectPiece(piece)
        }
    }

    /**
     * Check if placing a piece would create a match (for initial board generation)
     */
    private fun wouldCreateMatch(
        board: List<List<GamePiece>>,
        currentRow: List<GamePiece>,
        piece: GamePiece,
        row: Int,
        col: Int
    ): Boolean {
        // Check horizontal matches
        if (col >= 2) {
            val left1 = currentRow.getOrNull(col - 1)
            val left2 = currentRow.getOrNull(col - 2)
            if (left1 != null && left2 != null &&
                left1.type == piece.type && left2.type == piece.type) {
                return true
            }
        }

        // Check vertical matches
        if (row >= 2) {
            val up1 = board.getOrNull(row - 1)?.getOrNull(col)
            val up2 = board.getOrNull(row - 2)?.getOrNull(col)
            if (up1 != null && up2 != null &&
                up1.type == piece.type && up2.type == piece.type) {
                return true
            }
        }

        return false
    }

    /**
     * Find and process all matches on the board
     */
    private fun findAndProcessMatches() {
        val matches = findAllMatches()

        if (matches.isNotEmpty()) {
            val piecesToRemove = matches.flatten().distinct()
            removePieces(piecesToRemove)
        }
    }

    /**
     * Find all matches on the board
     */
    private fun findAllMatches(): List<List<GamePiece>> {
        val matches = mutableListOf<List<GamePiece>>()

        // Find horizontal matches
        for (row in _gameBoard.value.indices) {
            for (col in 0.._gameBoard.value[row].size - 3) {
                val piece1 = _gameBoard.value[row][col]
                val piece2 = _gameBoard.value[row][col + 1]
                val piece3 = _gameBoard.value[row][col + 2]

                if (piece1.type == piece2.type && piece2.type == piece3.type) {
                    matches.add(listOf(piece1, piece2, piece3))
                }
            }
        }

        // Find vertical matches
        for (col in _gameBoard.value[0].indices) {
            for (row in 0.._gameBoard.value.size - 3) {
                val piece1 = _gameBoard.value[row][col]
                val piece2 = _gameBoard.value[row + 1][col]
                val piece3 = _gameBoard.value[row + 2][col]

                if (piece1.type == piece2.type && piece2.type == piece3.type) {
                    matches.add(listOf(piece1, piece2, piece3))
                }
            }
        }

        return matches
    }

    /**
     * Apply gravity to make pieces fall down
     */
    private fun applyGravity(board: MutableList<MutableList<GamePiece>>) {
        for (col in 0 until casualConfig.boardWidth) {
            val column = mutableListOf<GamePiece>()

            // Collect non-null pieces from this column
            for (row in 0 until casualConfig.boardHeight) {
                board.getOrNull(row)?.getOrNull(col)?.let { piece ->
                    column.add(piece)
                }
            }

            // Clear the column in the board
            for (row in 0 until casualConfig.boardHeight) {
                if (board.size > row && board[row].size > col) {
                    board[row][col] = GamePiece(
                        id = "empty_${row}_${col}",
                        type = casualConfig.pieceTypes[0], // Default empty type
                        x = col,
                        y = row
                    )
                }
            }

            // Place pieces back from the bottom
            for (i in column.indices) {
                val piece = column[i]
                val newRow = casualConfig.boardHeight - column.size + i
                if (board.size > newRow && board[newRow].size > col) {
                    board[newRow][col] = piece.copy(y = newRow)
                }
            }
        }
    }

    /**
     * Fill empty spaces with new pieces
     */
    private fun fillEmptySpaces(board: MutableList<MutableList<GamePiece>>) {
        for (row in 0 until casualConfig.boardHeight) {
            for (col in 0 until casualConfig.boardWidth) {
                if (board.getOrNull(row)?.getOrNull(col)?.type == casualConfig.pieceTypes[0]) {
                    // This is an empty space, fill it
                    val type = casualConfig.pieceTypes.filter { it != casualConfig.pieceTypes[0] }.random()
                    board[row][col] = GamePiece(
                        id = "new_${row}_${col}_${System.currentTimeMillis()}",
                        type = type,
                        x = col,
                        y = row
                    )
                }
            }
        }
    }

    /**
     * Update animations
     */
    private fun updateAnimations(deltaTime: Float) {
        _animationQueue.value = _animationQueue.value.mapNotNull { animation ->
            animation.update(deltaTime)
            if (animation.isActive) animation else null
        }
    }

    @Composable
    override fun CasualGameHeader(
        score: Long,
        level: Int,
        objectives: List<GameObjective>,
        objectiveProgress: Map<String, Int>
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Score and level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Level: $level",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Objectives
            objectives.forEach { objective ->
                val progress = objectiveProgress[objective.id] ?: 0
                val percentage = (progress.toFloat() / objective.targetValue * 100).coerceIn(0f, 100f)

                LinearProgressIndicator(
                    progress = percentage / 100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${objective.description}: $progress/${objective.targetValue}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    @Composable
    override fun CasualGameContent(
        gameBoard: List<List<GamePiece>>,
        selectedPieces: List<GamePiece>,
        powerUps: List<CasualPowerUp>
    ) {
        Box(
            modifier = Modifier
                .size((casualConfig.boardWidth * 50).dp, (casualConfig.boardHeight * 50).dp)
                .border(2.dp, Color.Black)
        ) {
            // Draw game board
            LazyVerticalGrid(
                columns = GridCells.Fixed(casualConfig.boardWidth),
                content = {
                    items(gameBoard.size * casualConfig.boardWidth) { index ->
                        val row = index / casualConfig.boardWidth
                        val col = index % casualConfig.boardWidth
                        val piece = gameBoard.getOrNull(row)?.getOrNull(col)

                        if (piece != null) {
                            Match3Cell(
                                piece = piece,
                                isSelected = selectedPieces.contains(piece),
                                modifier = Modifier
                                    .size(50.dp)
                                    .clickable {
                                        selectPieceAt(row, col)
                                    }
                            )
                        }
                    }
                }
            )

            // Draw power-ups
            powerUps.forEach { powerUp ->
                PowerUpIcon(
                    powerUp = powerUp,
                    modifier = Modifier
                        .offset(
                            x = (powerUp.x * 50).dp,
                            y = (powerUp.y * 50).dp
                        )
                        .size(50.dp)
                        .clickable {
                            usePowerUp(powerUp)
                        }
                )
            }

            // Draw animations
            _animationQueue.collectAsState().value.forEach { animation ->
                when (animation) {
                    is GameAnimation.Explosion -> {
                        ExplosionEffect(
                            x = animation.x,
                            y = animation.y,
                            progress = animation.progress
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Match3Cell(
        piece: GamePiece,
        isSelected: Boolean,
        modifier: Modifier
    ) {
        Box(
            modifier = modifier
                .background(
                    if (isSelected) Color.Yellow.copy(alpha = 0.3f)
                    else Color.Transparent
                )
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color.Yellow else Color.Gray
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getPieceEmoji(piece.type),
                fontSize = 24.sp
            )
        }
    }

    @Composable
    private fun PowerUpIcon(
        powerUp: CasualPowerUp,
        modifier: Modifier
    ) {
        Box(
            modifier = modifier
                .background(Color.Red.copy(alpha = 0.7f))
                .border(2.dp, Color.Yellow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💣",
                fontSize = 20.sp
            )
        }
    }

    @Composable
    private fun ExplosionEffect(
        x: androidx.compose.ui.unit.Dp,
        y: androidx.compose.ui.unit.Dp,
        progress: Float
    ) {
        Box(
            modifier = Modifier
                .offset(x, y)
                .size(50.dp)
                .background(Color.Red.copy(alpha = 1f - progress)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💥",
                fontSize = 20.sp
            )
        }
    }

    private fun getPieceEmoji(type: com.roshni.games.gametemplates.casual.PieceType): String {
        return when (type) {
            com.roshni.games.gametemplates.casual.PieceType.RED -> "🔴"
            com.roshni.games.gametemplates.casual.PieceType.BLUE -> "🔵"
            com.roshni.games.gametemplates.casual.PieceType.GREEN -> "🟢"
            com.roshni.games.gametemplates.casual.PieceType.YELLOW -> "🟡"
            com.roshni.games.gametemplates.casual.PieceType.PURPLE -> "🟣"
            com.roshni.games.gametemplates.casual.PieceType.ORANGE -> "🟠"
        }
    }
}

/**
 * Match-3 game view model
 */
class Match3GameViewModel : CasualGameViewModel() {

    private val _gameBoard = MutableStateFlow<List<List<GamePiece>>>(emptyList())
    val gameBoard: StateFlow<List<List<GamePiece>>> = _gameBoard.asStateFlow()

    fun updateGameBoard(board: List<List<GamePiece>>) {
        _gameBoard.value = board
    }
}

/**
 * Game animation base class
 */
sealed class GameAnimation {
    abstract val isActive: Boolean
    abstract val progress: Float
    abstract fun update(deltaTime: Float): Boolean

    data class Explosion(
        val x: androidx.compose.ui.unit.Dp,
        val y: androidx.compose.ui.unit.Dp,
        val duration: Float = 500f,
        private var currentTime: Float = 0f
    ) : GameAnimation() {
        override val isActive: Boolean
            get() = currentTime < duration

        override val progress: Float
            get() = (currentTime / duration).coerceIn(0f, 1f)

        override fun update(deltaTime: Float): Boolean {
            currentTime += deltaTime
            return isActive
        }
    }
}