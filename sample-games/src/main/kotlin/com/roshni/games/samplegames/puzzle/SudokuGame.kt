package com.roshni.games.samplegames.puzzle

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roshni.games.gametemplates.puzzle.PuzzleGameTemplate
import com.roshni.games.gametemplates.puzzle.PuzzleGameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

/**
 * Fully playable Sudoku game implementation
 */
class SudokuGame(
    context: Context,
    gameView: View
) : PuzzleGameTemplate(context, "sudoku-classic", gameView) {

    private val _sudokuBoard = MutableStateFlow<List<List<Int>>>(emptyList())
    val sudokuBoard: StateFlow<List<List<Int>>> = _sudokuBoard.asStateFlow()

    private val _selectedCell = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedCell: StateFlow<Pair<Int, Int>?> = _selectedCell.asStateFlow()

    private val _notes = MutableStateFlow<Map<Pair<Int, Int>, Set<Int>>>(emptyMap())
    val notes: StateFlow<Map<Pair<Int, Int>, Set<Int>>> = _notes.asStateFlow()

    private val _isNotesMode = MutableStateFlow(false)
    val isNotesMode: StateFlow<Boolean> = _isNotesMode.asStateFlow()

    private val _difficulty = MutableStateFlow(SudokuDifficulty.MEDIUM)
    val difficulty: StateFlow<SudokuDifficulty> = _difficulty.asStateFlow()

    override fun registerGameSystems() {
        // Sudoku doesn't need physics
    }

    override fun initializeGameState() {
        super.initializeGameState()
        _sudokuBoard.value = List(9) { List(9) { 0 } }
        _selectedCell.value = null
        _notes.value = emptyMap()
        _isNotesMode.value = false
    }

    override fun loadGameAssets() {
        super.loadGameAssets()
        // Load Sudoku-specific assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "sudoku/cell_background.png",
                    "sudoku/cell_selected.png",
                    "sudoku/number_font.ttf"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load Sudoku assets")
            }
        }
    }

    override fun setupInputHandling() {
        super.setupInputHandling()
        // Sudoku-specific input handling is implemented in onPuzzleTouch
    }

    override fun setupAudio() {
        super.setupAudio()
        // Sudoku-specific audio setup
    }

    override fun initializeGame() {
        super.initializeGame()
        generateNewPuzzle()
    }

    override fun updateGame(deltaTime: Float) {
        super.updateGame(deltaTime)
        // Sudoku-specific game updates
    }

    override fun updateGameProgress() {
        super.updateGameProgress()
        // Check for puzzle completion
        if (_sudokuBoard.value.isNotEmpty() && isPuzzleComplete()) {
            completePuzzle()
        }
    }

    override fun generatePuzzle() {
        generateNewPuzzle()
    }

    override fun onPuzzleTouch(x: Float, y: Float) {
        val cellSize = 360.dp / 9 // Assuming 360dp board
        val cellSizePx = cellSize.value * context.resources.displayMetrics.density

        val col = (x / cellSizePx).toInt().coerceIn(0, 8)
        val row = (y / cellSizePx).toInt().coerceIn(0, 8)

        selectCell(row, col)
    }

    override fun onPuzzleDrag(x: Float, y: Float) {
        // Sudoku doesn't use drag gestures
    }

    override fun onPuzzleRelease(x: Float, y: Float) {
        // Sudoku handles touch in onPuzzleTouch
    }

    override fun updatePuzzle(deltaTime: Float) {
        // Sudoku is turn-based, no continuous updates needed
    }

    override fun onHintUsed() {
        _selectedCell.value?.let { (row, col) ->
            if (_sudokuBoard.value[row][col] == 0) {
                val solution = solveSudoku(_sudokuBoard.value)
                if (solution.isNotEmpty()) {
                    val correctNumber = solution[row][col]
                    addNoteToCell(row, col, correctNumber)
                }
            }
        }
    }

    override fun onPuzzleCompleted() {
        Timber.d("Sudoku puzzle completed!")
    }

    override fun calculateTimeBonus(): Long {
        val baseTime = 300 // 5 minutes in seconds
        val actualTime = _timeElapsed.value
        return if (actualTime < baseTime) {
            ((baseTime - actualTime) * 10).toLong() // 10 points per second saved
        } else {
            0L
        }
    }

    override fun calculateMoveBonus(): Long {
        val baseMoves = 100
        val actualMoves = _moves.value
        return if (actualMoves < baseMoves) {
            ((baseMoves - actualMoves) * 5).toLong() // 5 points per move saved
        } else {
            0L
        }
    }

    override fun getGameViewModel(): PuzzleGameViewModel {
        return SudokuGameViewModel()
    }

    /**
     * Generate a new Sudoku puzzle
     */
    private fun generateNewPuzzle() {
        // Create empty board
        val emptyBoard = List(9) { List(9) { 0 } }

        // Generate complete valid solution first
        val solution = generateCompleteSudoku()

        // Remove numbers based on difficulty
        val puzzle = removeNumbers(solution, _difficulty.value)

        _sudokuBoard.value = puzzle
        _selectedCell.value = null
        _notes.value = emptyMap()

        Timber.d("Generated new Sudoku puzzle with difficulty: ${_difficulty.value}")
    }

    /**
     * Generate a complete valid Sudoku solution
     */
    private fun generateCompleteSudoku(): List<List<Int>> {
        val board = List(9) { MutableList(9) { 0 } }

        fun solve(): Boolean {
            for (row in 0..8) {
                for (col in 0..8) {
                    if (board[row][col] == 0) {
                        val numbers = (1..9).shuffled()
                        for (num in numbers) {
                            if (isValidMove(board, row, col, num)) {
                                board[row][col] = num
                                if (solve()) return true
                                board[row][col] = 0
                            }
                        }
                        return false
                    }
                }
            }
            return true
        }

        solve()
        return board
    }

    /**
     * Remove numbers from complete solution based on difficulty
     */
    private fun removeNumbers(solution: List<List<Int>>, difficulty: SudokuDifficulty): List<List<Int>> {
        val puzzle = solution.map { it.toMutableList() }
        val cellsToRemove = when (difficulty) {
            SudokuDifficulty.EASY -> 40
            SudokuDifficulty.MEDIUM -> 50
            SudokuDifficulty.HARD -> 60
            SudokuDifficulty.EXPERT -> 65
        }

        val positions = mutableListOf<Pair<Int, Int>>()
        for (row in 0..8) {
            for (col in 0..8) {
                positions.add(row to col)
            }
        }

        positions.shuffle()

        for (i in 0 until cellsToRemove) {
            val (row, col) = positions[i]
            puzzle[row][col] = 0
        }

        return puzzle
    }

    /**
     * Check if a number can be placed at the given position
     */
    private fun isValidMove(board: List<List<Int>>, row: Int, col: Int, num: Int): Boolean {
        // Check row
        for (c in 0..8) {
            if (board[row][c] == num) return false
        }

        // Check column
        for (r in 0..8) {
            if (board[r][col] == num) return false
        }

        // Check 3x3 box
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (board[r][c] == num) return false
            }
        }

        return true
    }

    /**
     * Solve the Sudoku puzzle (for hints and validation)
     */
    private fun solveSudoku(board: List<List<Int>>): List<List<Int>> {
        val puzzle = board.map { it.toMutableList() }

        fun solve(): Boolean {
            for (row in 0..8) {
                for (col in 0..8) {
                    if (puzzle[row][col] == 0) {
                        for (num in 1..9) {
                            if (isValidMove(puzzle, row, col, num)) {
                                puzzle[row][col] = num
                                if (solve()) return true
                                puzzle[row][col] = 0
                            }
                        }
                        return false
                    }
                }
            }
            return true
        }

        solve()
        return puzzle
    }

    /**
     * Select a cell
     */
    private fun selectCell(row: Int, col: Int) {
        if (row in 0..8 && col in 0..8) {
            _selectedCell.value = row to col
            recordMove()
        }
    }

    /**
     * Place a number in the selected cell
     */
    fun placeNumber(number: Int) {
        _selectedCell.value?.let { (row, col) ->
            if (_sudokuBoard.value[row][col] == 0) { // Only allow placing in empty cells
                val newBoard = _sudokuBoard.value.map { it.toMutableList() }
                newBoard[row][col] = number
                _sudokuBoard.value = newBoard

                recordMove()
            }
        }
    }

    /**
     * Add note to cell
     */
    private fun addNoteToCell(row: Int, col: Int, number: Int) {
        val cellKey = row to col
        val currentNotes = _notes.value[cellKey] ?: emptySet()
        if (number !in currentNotes) {
            _notes.value = _notes.value + (cellKey to (currentNotes + number))
        }
    }

    /**
     * Toggle notes mode
     */
    fun toggleNotesMode() {
        _isNotesMode.value = !_isNotesMode.value
    }

    /**
     * Check if puzzle is complete
     */
    private fun isPuzzleComplete(): Boolean {
        // Check if all cells are filled
        if (_sudokuBoard.value.any { row -> row.any { it == 0 } }) {
            return false
        }

        // Check if solution is valid
        val solution = solveSudoku(_sudokuBoard.value)
        return solution.all { row -> row.all { it != 0 } }
    }

    @Composable
    override fun PuzzleGameHeader(
        moves: Int,
        hintsUsed: Int,
        timeElapsed: Long,
        isComplete: Boolean,
        onHintClick: () -> Unit,
        onResetClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game stats
            Column {
                Text(
                    text = "Moves: $moves",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hints: $hintsUsed",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Time: ${formatTime(timeElapsed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Control buttons
            Row {
                Button(
                    onClick = onHintClick,
                    enabled = hintsUsed < puzzleConfig.maxHints
                ) {
                    Text("Hint")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onResetClick) {
                    Text("Reset")
                }
            }
        }
    }

    @Composable
    override fun PuzzleGameContent() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sudoku board
            SudokuBoard(
                board = _sudokuBoard.collectAsState().value,
                selectedCell = _selectedCell.collectAsState().value,
                notes = _notes.collectAsState().value,
                onCellClick = { row, col -> selectCell(row, col) },
                onNumberInput = { number -> placeNumber(number) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Number input buttons
            NumberInputPad(
                onNumberClick = { number -> placeNumber(number) },
                onNotesToggle = { toggleNotesMode() },
                isNotesMode = _isNotesMode.collectAsState().value
            )
        }
    }

    @Composable
    override fun PuzzleCompletionOverlay(
        score: Long,
        timeElapsed: Long,
        moves: Int,
        onPlayAgain: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 Puzzle Complete! 🎉",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Final Score: $score",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Time: ${formatTime(timeElapsed)}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Moves: $moves",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Play Again")
                    }
                }
            }
        }
    }

    @Composable
    private fun SudokuBoard(
        board: List<List<Int>>,
        selectedCell: Pair<Int, Int>?,
        notes: Map<Pair<Int, Int>, Set<Int>>,
        onCellClick: (Int, Int) -> Unit,
        onNumberInput: (Int) -> Unit
    ) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .border(2.dp, Color.Black)
        ) {
            // Draw 9x9 grid with thicker lines for 3x3 boxes
            for (i in 0..9) {
                val isThick = i % 3 == 0
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    // Horizontal lines
                    drawLine(
                        color = if (isThick) Color.Black else Color.Gray,
                        start = androidx.compose.ui.geometry.Offset(0f, i * size.height / 9),
                        end = androidx.compose.ui.geometry.Offset(size.width, i * size.height / 9),
                        strokeWidth = if (isThick) 3f else 1f
                    )

                    // Vertical lines
                    drawLine(
                        color = if (isThick) Color.Black else Color.Gray,
                        start = androidx.compose.ui.geometry.Offset(i * size.width / 9, 0f),
                        end = androidx.compose.ui.geometry.Offset(i * size.width / 9, size.height),
                        strokeWidth = if (isThick) 3f else 1f
                    )
                }
            }

            // Draw cells
            for (row in 0..8) {
                for (col in 0..8) {
                    val cellKey = row to col
                    val isSelected = selectedCell == cellKey
                    val cellValue = board[row][col]
                    val cellNotes = notes[cellKey] ?: emptySet()

                    SudokuCell(
                        value = cellValue,
                        notes = cellNotes,
                        isSelected = isSelected,
                        isOriginal = false, // Would need to track original puzzle state
                        modifier = Modifier
                            .offset(
                                x = (col * 40).dp,
                                y = (row * 40).dp
                            )
                            .size(40.dp)
                            .clickable { onCellClick(row, col) }
                    )
                }
            }
        }
    }

    @Composable
    private fun SudokuCell(
        value: Int,
        notes: Set<Int>,
        isSelected: Boolean,
        isOriginal: Boolean,
        modifier: Modifier
    ) {
        Box(
            modifier = modifier
                .background(
                    if (isSelected) Color.LightGray.copy(alpha = 0.5f)
                    else Color.Transparent
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color.Blue else Color.Gray
                ),
            contentAlignment = Alignment.Center
        ) {
            if (value != 0) {
                Text(
                    text = value.toString(),
                    fontSize = 18.sp,
                    fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Normal,
                    color = if (isOriginal) Color.Black else Color.Blue
                )
            } else if (notes.isNotEmpty()) {
                // Draw notes (small numbers)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (j in 0..2) {
                                val noteValue = i * 3 + j + 1
                                if (noteValue in notes) {
                                    Text(
                                        text = noteValue.toString(),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NumberInputPad(
        onNumberClick: (Int) -> Unit,
        onNotesToggle: () -> Unit,
        isNotesMode: Boolean
    ) {
        Column {
            // Notes toggle button
            Button(
                onClick = onNotesToggle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNotesMode) "Numbers Mode" else "Notes Mode")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Number buttons 1-9
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(9) { index ->
                    val number = index + 1
                    Button(
                        onClick = { onNumberClick(number) },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Text(
                            text = number.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}

/**
 * Sudoku game view model
 */
class SudokuGameViewModel : PuzzleGameViewModel() {

    private val _sudokuBoard = MutableStateFlow(List(9) { List(9) { 0 } })
    val sudokuBoard: StateFlow<List<List<Int>>> = _sudokuBoard.asStateFlow()

    private val _selectedCell = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedCell: StateFlow<Pair<Int, Int>?> = _selectedCell.asStateFlow()

    fun updateBoard(board: List<List<Int>>) {
        _sudokuBoard.value = board
    }

    fun selectCell(row: Int, col: Int) {
        _selectedCell.value = row to col
    }
}

/**
 * Sudoku difficulty levels
 */
enum class SudokuDifficulty(val displayName: String, val cellsToRemove: Int) {
    EASY("Easy", 40),
    MEDIUM("Medium", 50),
    HARD("Hard", 60),
    EXPERT("Expert", 65)
}