package com.roshni.games.gametemplates.card

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
 * Template for card games (Solitaire, Poker, Blackjack, Memory Cards, etc.)
 */
abstract class CardGameTemplate(
    context: Context,
    gameId: String,
    gameView: View
) : BaseGameTemplate(context, gameId, gameView) {

    // Card-specific state
    private val _deck = MutableStateFlow<List<PlayingCard>>(emptyList())
    val deck: StateFlow<List<PlayingCard>> = _deck.asStateFlow()

    private val _hand = MutableStateFlow<List<PlayingCard>>(emptyList())
    val hand: StateFlow<List<PlayingCard>> = _hand.asStateFlow()

    private val _tableau = MutableStateFlow<List<List<PlayingCard>>>(emptyList())
    val tableau: StateFlow<List<List<PlayingCard>>> = _tableau.asStateFlow()

    private val _foundation = MutableStateFlow<List<List<PlayingCard>>>(emptyList())
    val foundation: StateFlow<List<List<PlayingCard>>> = _foundation.asStateFlow()

    private val _discardPile = MutableStateFlow<List<PlayingCard>>(emptyList())
    val discardPile: StateFlow<List<PlayingCard>> = _discardPile.asStateFlow()

    // Card game configuration
    protected val cardConfig = CardConfig()

    override fun initializeGameState() {
        // Initialize card-specific state
        _deck.value = createDeck()
        _hand.value = emptyList()
        _tableau.value = emptyList()
        _foundation.value = emptyList()
        _discardPile.value = emptyList()
    }

    override fun loadGameAssets() {
        // Load common card game assets
        viewModelScope.launch {
            try {
                assetManager.preloadAssets(listOf(
                    "cards/card_back.png",
                    "cards/card_flip.mp3",
                    "cards/card_place.mp3",
                    "cards/win_sound.mp3"
                ))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load card assets")
            }
        }
    }

    override fun setupInputHandling() {
        // Setup card-specific input handling
        viewModelScope.launch {
            inputSystem.touchEvents.collect { event ->
                when (event) {
                    is com.roshni.games.gameengine.systems.TouchEvent.Down -> {
                        onCardTouch(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Move -> {
                        onCardDrag(event.x, event.y)
                    }
                    is com.roshni.games.gameengine.systems.TouchEvent.Up -> {
                        onCardRelease(event.x, event.y)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setupAudio() {
        // Setup card-specific audio
        if (gameConfig.enableAudio) {
            audioSystem.setMusicVolume(0.3f) // Low volume for card games
            audioSystem.setSfxVolume(0.5f)
        }
    }

    override fun initializeGame() {
        // Initialize card-specific game state
        shuffleDeck()
        dealCards()
        startCardGame()
    }

    override fun updateGame(deltaTime: Float) {
        // Update card-specific game logic
        updateCardGame(deltaTime)
    }

    override fun updateGameProgress() {
        // Update card game progress
        _gameState.value?.let { state ->
            val updatedState = state.copy(
                score = _score.value,
                level = _level.value,
                customData = state.customData + mapOf(
                    "deckSize" to _deck.value.size.toString(),
                    "handSize" to _hand.value.size.toString(),
                    "tableauSize" to _tableau.value.sumOf { it.size }.toString(),
                    "foundationSize" to _foundation.value.sumOf { it.size }.toString()
                )
            )
            _gameState.value = updatedState
        }
    }

    override fun onScoreChanged(newScore: Long) {
        // Card-specific score handling
        if (newScore > 0) {
            playSoundEffect("card_place")
        }
    }

    override fun onGameOver() {
        // Card-specific game over handling
        if (_score.value > 0) {
            playSoundEffect("win_sound")
        }
    }

    /**
     * Create a standard deck of cards
     */
    protected fun createDeck(): List<PlayingCard> {
        val suits = listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS, Suit.SPADES)
        val ranks = listOf(
            Rank.ACE, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE,
            Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN,
            Rank.JACK, Rank.QUEEN, Rank.KING
        )

        return suits.flatMap { suit ->
            ranks.map { rank ->
                PlayingCard(rank, suit)
            }
        }
    }

    /**
     * Shuffle the deck
     */
    protected fun shuffleDeck() {
        _deck.value = _deck.value.shuffled()
        playSoundEffect("card_flip")
    }

    /**
     * Draw card from deck
     */
    protected fun drawCard(): PlayingCard? {
        return if (_deck.value.isNotEmpty()) {
            val card = _deck.value.first()
            _deck.value = _deck.value.drop(1)
            card
        } else {
            null
        }
    }

    /**
     * Add card to hand
     */
    protected fun addCardToHand(card: PlayingCard) {
        _hand.value = _hand.value + card
    }

    /**
     * Remove card from hand
     */
    protected fun removeCardFromHand(card: PlayingCard) {
        _hand.value = _hand.value - card
    }

    /**
     * Add card to tableau pile
     */
    protected fun addCardToTableau(pileIndex: Int, card: PlayingCard) {
        val currentTableau = _tableau.value.toMutableList()
        if (currentTableau.size > pileIndex) {
            currentTableau[pileIndex] = currentTableau[pileIndex] + card
        } else {
            currentTableau.add(listOf(card))
        }
        _tableau.value = currentTableau
    }

    /**
     * Remove card from tableau pile
     */
    protected fun removeCardFromTableau(pileIndex: Int, card: PlayingCard): Boolean {
        val currentTableau = _tableau.value.toMutableList()
        if (currentTableau.size > pileIndex && currentTableau[pileIndex].isNotEmpty()) {
            currentTableau[pileIndex] = currentTableau[pileIndex].dropLast(1)
            _tableau.value = currentTableau
            return true
        }
        return false
    }

    /**
     * Add card to foundation pile
     */
    protected fun addCardToFoundation(pileIndex: Int, card: PlayingCard) {
        val currentFoundation = _foundation.value.toMutableList()
        if (currentFoundation.size > pileIndex) {
            currentFoundation[pileIndex] = currentFoundation[pileIndex] + card
        } else {
            currentFoundation.add(listOf(card))
        }
        _foundation.value = currentFoundation
    }

    /**
     * Add card to discard pile
     */
    protected fun addCardToDiscard(card: PlayingCard) {
        _discardPile.value = _discardPile.value + card
    }

    /**
     * Get top card from discard pile
     */
    protected fun getTopDiscardCard(): PlayingCard? {
        return _discardPile.value.lastOrNull()
    }

    /**
     * Check if move is valid (can be overridden for specific game rules)
     */
    protected open fun isValidMove(fromCard: PlayingCard, toCard: PlayingCard): Boolean {
        return (fromCard.suit == toCard.suit && fromCard.rank.value == toCard.rank.value - 1) ||
               (fromCard.color != toCard.color && fromCard.rank.value == toCard.rank.value + 1)
    }

    /**
     * Check if game is won
     */
    protected open fun isGameWon(): Boolean {
        return _foundation.value.all { pile ->
            pile.size == 13 // Full suit in each foundation pile
        }
    }

    /**
     * Abstract methods for card-specific implementation
     */
    protected abstract fun dealCards()
    protected abstract fun onCardTouch(x: Float, y: Float)
    protected abstract fun onCardDrag(x: Float, y: Float)
    protected abstract fun onCardRelease(x: Float, y: Float)
    protected abstract fun updateCardGame(deltaTime: Float)
    protected abstract fun startCardGame()

    /**
     * Card configuration
     */
    protected data class CardConfig(
        val numDecks: Int = 1,
        val numTableauPiles: Int = 7,
        val numFoundationPiles: Int = 4,
        val allowMultipleCards: Boolean = true,
        val autoMoveToFoundation: Boolean = true,
        val showCardHints: Boolean = true
    )

    /**
     * Playing card data class
     */
    data class PlayingCard(
        val rank: Rank,
        val suit: Suit,
        val isFaceUp: Boolean = false,
        val id: String = "${rank.name}_${suit.name}"
    ) {
        val color: CardColor
            get() = when (suit) {
                Suit.HEARTS, Suit.DIAMONDS -> CardColor.RED
                Suit.CLUBS, Suit.SPADES -> CardColor.BLACK
            }

        val value: Int
            get() = rank.value
    }

    /**
     * Card ranks
     */
    enum class Rank(val displayName: String, val value: Int, val shortName: String) {
        ACE("Ace", 1, "A"),
        TWO("Two", 2, "2"),
        THREE("Three", 3, "3"),
        FOUR("Four", 4, "4"),
        FIVE("Five", 5, "5"),
        SIX("Six", 6, "6"),
        SEVEN("Seven", 7, "7"),
        EIGHT("Eight", 8, "8"),
        NINE("Nine", 9, "9"),
        TEN("Ten", 10, "10"),
        JACK("Jack", 11, "J"),
        QUEEN("Queen", 12, "Q"),
        KING("King", 13, "K")
    }

    /**
     * Card suits
     */
    enum class Suit(val displayName: String, val symbol: String) {
        HEARTS("Hearts", "♥"),
        DIAMONDS("Diamonds", "♦"),
        CLUBS("Clubs", "♣"),
        SPADES("Spades", "♠")
    }

    /**
     * Card colors
     */
    enum class CardColor {
        RED, BLACK
    }

    /**
     * Card game view model
     */
    abstract class CardGameViewModel : GameViewModel() {

        private val _deck = MutableStateFlow<List<PlayingCard>>(emptyList())
        val deck: StateFlow<List<PlayingCard>> = _deck.asStateFlow()

        private val _hand = MutableStateFlow<List<PlayingCard>>(emptyList())
        val hand: StateFlow<List<PlayingCard>> = _hand.asStateFlow()

        private val _tableau = MutableStateFlow<List<List<PlayingCard>>>(emptyList())
        val tableau: StateFlow<List<List<PlayingCard>>> = _tableau.asStateFlow()

        private val _foundation = MutableStateFlow<List<List<PlayingCard>>>(emptyList())
        val foundation: StateFlow<List<List<PlayingCard>>> = _foundation.asStateFlow()

        private val _discardPile = MutableStateFlow<List<PlayingCard>>(emptyList())
        val discardPile: StateFlow<List<PlayingCard>> = _discardPile.asStateFlow()

        fun setDeck(deck: List<PlayingCard>) {
            _deck.value = deck
        }

        fun setHand(hand: List<PlayingCard>) {
            _hand.value = hand
        }

        fun setTableau(tableau: List<List<PlayingCard>>) {
            _tableau.value = tableau
        }

        fun setFoundation(foundation: List<List<PlayingCard>>) {
            _foundation.value = foundation
        }

        fun setDiscardPile(discardPile: List<PlayingCard>) {
            _discardPile.value = discardPile
        }

        fun addCardToHand(card: PlayingCard) {
            _hand.value = _hand.value + card
        }

        fun removeCardFromHand(card: PlayingCard) {
            _hand.value = _hand.value - card
        }

        fun flipCardInTableau(pileIndex: Int, cardIndex: Int) {
            val currentTableau = _tableau.value.toMutableList()
            if (currentTableau.size > pileIndex && currentTableau[pileIndex].size > cardIndex) {
                val pile = currentTableau[pileIndex].toMutableList()
                val card = pile[cardIndex]
                pile[cardIndex] = card.copy(isFaceUp = !card.isFaceUp)
                currentTableau[pileIndex] = pile
                _tableau.value = currentTableau
            }
        }
    }

    @Composable
    override fun GameContent() {
        val gameViewModel = getGameViewModel() as CardGameViewModel
        val currentDeck by gameViewModel.deck.collectAsState()
        val currentHand by gameViewModel.hand.collectAsState()
        val currentTableau by gameViewModel.tableau.collectAsState()
        val currentFoundation by gameViewModel.foundation.collectAsState()
        val currentDiscard by gameViewModel.discardPile.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            // Card game header
            CardGameHeader(
                score = _score.collectAsState().value,
                deckSize = currentDeck.size,
                handSize = currentHand.size
            )

            // Main card game content
            CardGameContent(
                deck = currentDeck,
                hand = currentHand,
                tableau = currentTableau,
                foundation = currentFoundation,
                discardPile = currentDiscard
            )
        }
    }

    @Composable
    protected abstract fun CardGameHeader(
        score: Long,
        deckSize: Int,
        handSize: Int
    )

    @Composable
    protected abstract fun CardGameContent(
        deck: List<PlayingCard>,
        hand: List<PlayingCard>,
        tableau: List<List<PlayingCard>>,
        foundation: List<List<PlayingCard>>,
        discardPile: List<PlayingCard>
    )
}