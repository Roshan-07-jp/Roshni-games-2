package com.roshni.games.gametemplates.trivia

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
 * Template for trivia & quiz games (General Knowledge, Science, History, Sports, etc.)
 */
abstract class TriviaGameTemplate(
    context: Context,
    gameId: String,
    gameView: View
) : BaseGameTemplate(context, gameId, gameView) {

    // Trivia-specific state
    private val _currentQuestion = MutableStateFlow<TriviaQuestion?>(null)
    val currentQuestion: StateFlow<TriviaQuestion?> = _currentQuestion.asStateFlow()

    private val _questionIndex = MutableStateFlow(0)
    val questionIndex: StateFlow<Int> = _questionIndex.asStateFlow()

    private val _correctAnswers = MutableStateFlow(0)
    val correctAnswers: StateFlow<Int> = _correctAnswers.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _timePerQuestion = MutableStateFlow(30)
    val timePerQuestion: StateFlow<Int> = _timePerQuestion.asStateFlow()

    private val _questions = MutableStateFlow<List<TriviaQuestion>>(emptyList())
    val questions: StateFlow<List<TriviaQuestion>> = _questions.asStateFlow()

    // Trivia configuration
    protected val triviaConfig = TriviaConfig()

    override fun initializeGameState() {
        // Initialize trivia-specific state
        _currentQuestion.value = null
        _questionIndex.value = 0
        _correctAnswers.value = 0
        _streak.value = 0
        _questions.value = loadQuestions()
    }

    override fun loadGameAssets() {
        // Load common trivia game assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "trivia/correct_answer.mp3",
                    "trivia/wrong_answer.mp3",
                    "trivia/time_up.mp3",
                    "trivia/complete_quiz.mp3"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load trivia assets")
            }
        }
    }

    override fun setupInputHandling() {
        // Setup trivia-specific input handling
        viewModelScope.launch {
            inputSystem.touchEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.TouchEvent.Down -> {
                        onTriviaTouch(event.x, event.y)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setupAudio() {
        // Setup trivia-specific audio
        if (gameConfig.enableAudio) {
            audioSystem.setMusicVolume(0.2f) // Very low volume for trivia games
            audioSystem.setSfxVolume(0.4f)
        }
    }

    override fun initializeGame() {
        // Initialize trivia-specific game state
        startTriviaGame()
    }

    override fun updateGame(deltaTime: Float) {
        // Update trivia-specific game logic
        updateTriviaGame(deltaTime)
    }

    override fun updateGameProgress() {
        // Update trivia game progress
        _gameState.value?.let { state ->
            val updatedState = state.copy(
                score = _score.value,
                level = _level.value,
                customData = state.customData + mapOf(
                    "questionIndex" to _questionIndex.value.toString(),
                    "correctAnswers" to _correctAnswers.value.toString(),
                    "streak" to _streak.value.toString(),
                    "totalQuestions" to _questions.value.size.toString()
                )
            )
            _gameState.value = updatedState
        }
    }

    override fun onScoreChanged(newScore: Long) {
        // Trivia-specific score handling
        if (newScore > 0) {
            playSoundEffect("correct_answer")
        }
    }

    override fun onGameOver() {
        // Trivia-specific game over handling
        playSoundEffect("complete_quiz")
    }

    /**
     * Load questions for the trivia game
     */
    protected abstract fun loadQuestions(): List<TriviaQuestion>

    /**
     * Start the trivia game
     */
    protected fun startTriviaGame() {
        if (_questions.value.isNotEmpty()) {
            _questionIndex.value = 0
            _correctAnswers.value = 0
            _streak.value = 0
            showNextQuestion()
        }
    }

    /**
     * Show next question
     */
    protected fun showNextQuestion() {
        if (_questionIndex.value < _questions.value.size) {
            _currentQuestion.value = _questions.value[_questionIndex.value]
            _questionIndex.value++
        } else {
            // Quiz completed
            completeQuiz()
        }
    }

    /**
     * Answer current question
     */
    protected fun answerQuestion(answerIndex: Int) {
        _currentQuestion.value?.let { question ->
            val isCorrect = question.correctAnswerIndex == answerIndex

            if (isCorrect) {
                _correctAnswers.value++
                _streak.value++

                // Bonus points for streak
                val streakBonus = calculateStreakBonus()
                addScore(question.points + streakBonus)
            } else {
                _streak.value = 0
                playSoundEffect("wrong_answer")
            }

            // Show next question after delay
            viewModelScope.launch {
                kotlinx.coroutines.delay(triviaConfig.answerDelay)
                showNextQuestion()
            }
        }
    }

    /**
     * Skip current question
     */
    protected fun skipQuestion() {
        _streak.value = 0
        playSoundEffect("wrong_answer")

        viewModelScope.launch {
            kotlinx.coroutines.delay(triviaConfig.answerDelay)
            showNextQuestion()
        }
    }

    /**
     * Complete the quiz
     */
    private fun completeQuiz() {
        _currentQuestion.value = null

        // Calculate final score
        val accuracy = _correctAnswers.value.toFloat() / _questions.value.size
        val finalScore = (_score.value * accuracy).toLong()
        setScore(finalScore)

        onQuizCompleted()
    }

    /**
     * Calculate streak bonus
     */
    private fun calculateStreakBonus(): Long {
        return when {
            _streak.value >= 10 -> 50L
            _streak.value >= 5 -> 25L
            _streak.value >= 3 -> 10L
            else -> 0L
        }
    }

    /**
     * Abstract methods for trivia-specific implementation
     */
    protected abstract fun onTriviaTouch(x: Float, y: Float)
    protected abstract fun updateTriviaGame(deltaTime: Float)
    protected abstract fun onQuizCompleted()

    /**
     * Trivia configuration
     */
    protected data class TriviaConfig(
        val questionsPerGame: Int = 10,
        val timePerQuestion: Int = 30,
        val showHints: Boolean = true,
        val allowSkip: Boolean = true,
        val answerDelay: Long = 1500L, // Delay between questions
        val categories: List<QuestionCategory> = listOf(
            QuestionCategory.GENERAL, QuestionCategory.SCIENCE, QuestionCategory.HISTORY
        )
    )

    /**
     * Question categories
     */
    enum class QuestionCategory {
        GENERAL, SCIENCE, HISTORY, SPORTS, ENTERTAINMENT, GEOGRAPHY
    }

    /**
     * Trivia question data class
     */
    data class TriviaQuestion(
        val id: String,
        val category: QuestionCategory,
        val difficulty: com.roshni.games.gamecatalog.data.model.GameDifficulty,
        val question: String,
        val answers: List<String>,
        val correctAnswerIndex: Int,
        val points: Long = 100,
        val timeLimit: Int = 30,
        val hint: String? = null
    )

    /**
     * Trivia game view model
     */
    abstract class TriviaGameViewModel : GameViewModel() {

        private val _currentQuestion = MutableStateFlow<TriviaQuestion?>(null)
        val currentQuestion: StateFlow<TriviaQuestion?> = _currentQuestion.asStateFlow()

        private val _questionIndex = MutableStateFlow(0)
        val questionIndex: StateFlow<Int> = _questionIndex.asStateFlow()

        private val _correctAnswers = MutableStateFlow(0)
        val correctAnswers: StateFlow<Int> = _correctAnswers.asStateFlow()

        private val _streak = MutableStateFlow(0)
        val streak: StateFlow<Int> = _streak.asStateFlow()

        fun setCurrentQuestion(question: TriviaQuestion?) {
            _currentQuestion.value = question
        }

        fun setQuestionIndex(index: Int) {
            _questionIndex.value = index
        }

        fun incrementCorrectAnswers() {
            _correctAnswers.value++
        }

        fun incrementStreak() {
            _streak.value++
        }

        fun resetStreak() {
            _streak.value = 0
        }
    }

    @Composable
    override fun GameContent() {
        val gameViewModel = getGameViewModel() as TriviaGameViewModel
        val currentQuestion by gameViewModel.currentQuestion.collectAsState()
        val currentIndex by gameViewModel.questionIndex.collectAsState()
        val currentCorrect by gameViewModel.correctAnswers.collectAsState()
        val currentStreak by gameViewModel.streak.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trivia game header
            TriviaGameHeader(
                questionIndex = currentIndex,
                totalQuestions = _questions.collectAsState().value.size,
                correctAnswers = currentCorrect,
                streak = currentStreak,
                score = _score.collectAsState().value
            )

            // Main trivia content
            if (currentQuestion != null) {
                TriviaQuestionContent(
                    question = currentQuestion!!,
                    onAnswerSelected = { answerIndex ->
                        answerQuestion(answerIndex)
                    },
                    onSkipQuestion = { skipQuestion() }
                )
            } else {
                // Quiz completed
                TriviaCompletionScreen(
                    score = _score.collectAsState().value,
                    correctAnswers = currentCorrect,
                    totalQuestions = _questions.collectAsState().value.size,
                    onPlayAgain = { startNewGame() }
                )
            }
        }
    }

    @Composable
    protected abstract fun TriviaGameHeader(
        questionIndex: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        streak: Int,
        score: Long
    )

    @Composable
    protected abstract fun TriviaQuestionContent(
        question: TriviaQuestion,
        onAnswerSelected: (Int) -> Unit,
        onSkipQuestion: () -> Unit
    )

    @Composable
    protected abstract fun TriviaCompletionScreen(
        score: Long,
        correctAnswers: Int,
        totalQuestions: Int,
        onPlayAgain: () -> Unit
    )
}