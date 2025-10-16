package com.roshni.games.feature.search.domain

import com.roshni.games.feature.search.data.model.DateRange
import com.roshni.games.feature.search.data.model.GlobalSearchConfig
import com.roshni.games.feature.search.data.model.SearchFilters
import com.roshni.games.feature.search.data.model.SearchHistoryEntry
import com.roshni.games.feature.search.data.model.SearchQuery
import com.roshni.games.feature.search.data.model.SearchResult
import com.roshni.games.feature.search.data.model.SearchResultType
import com.roshni.games.feature.search.data.model.SearchSort
import com.roshni.games.feature.search.data.model.SearchStats
import com.roshni.games.feature.search.data.model.SearchSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID

/**
 * Global search service for searching across games, achievements, and content
 */
class GlobalSearchService {

    private val _searchConfig = MutableStateFlow(
        GlobalSearchConfig(
            enabled = true,
            maxResults = 100,
            searchDelay = 300,
            enableSuggestions = true,
            maxSuggestions = 10,
            enableHistory = true,
            maxHistoryItems = 50,
            fuzzySearchEnabled = true,
            fuzzyThreshold = 0.6f
        )
    )

    private val _searchHistory = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())
    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    private val _lastSearchStats = MutableStateFlow<SearchStats?>(null)

    // Public flows
    val searchConfig: StateFlow<GlobalSearchConfig> = _searchConfig.asStateFlow()
    val searchHistory: StateFlow<List<SearchHistoryEntry>> = _searchHistory.asStateFlow()
    val searchSuggestions: StateFlow<List<SearchSuggestion>> = _searchSuggestions.asStateFlow()
    val lastSearchStats: StateFlow<SearchStats?> = _lastSearchStats.asStateFlow()

    // Sample data for demonstration
    private val sampleGames = listOf(
        SearchResult(
            id = "game-1",
            type = SearchResultType.GAME,
            title = "Mind Bender Puzzle",
            subtitle = "Logic puzzle game",
            description = "Challenge your mind with increasingly complex puzzles",
            metadata = mapOf(
                "category" to "Puzzle",
                "difficulty" to "Medium",
                "rating" to 4.5f,
                "players" to 10000
            ),
            relevanceScore = 0.9f
        ),
        SearchResult(
            id = "game-2",
            type = SearchResultType.GAME,
            title = "Space Adventure",
            subtitle = "Action adventure game",
            description = "Explore the galaxy in this epic space adventure",
            metadata = mapOf(
                "category" to "Adventure",
                "difficulty" to "Easy",
                "rating" to 4.2f,
                "players" to 25000
            ),
            relevanceScore = 0.8f
        ),
        SearchResult(
            id = "achievement-1",
            type = SearchResultType.ACHIEVEMENT,
            title = "Puzzle Master",
            subtitle = "Complete 100 puzzles",
            description = "Awarded for completing 100 challenging puzzles",
            metadata = mapOf(
                "points" to 500,
                "rarity" to "Rare",
                "category" to "Puzzle"
            ),
            relevanceScore = 0.7f
        ),
        SearchResult(
            id = "player-1",
            type = SearchResultType.PLAYER,
            title = "GameMaster2024",
            subtitle = "Level 45 Player",
            description = "Experienced gamer with high scores across multiple games",
            metadata = mapOf(
                "level" to 45,
                "gamesPlayed" to 150,
                "achievements" to 89
            ),
            relevanceScore = 0.6f
        )
    )

    private val sampleSuggestions = listOf(
        SearchSuggestion("puzzle", com.roshni.games.feature.search.data.model.SuggestionType.QUERY, 150),
        SearchSuggestion("Mind Bender", com.roshni.games.feature.search.data.model.SuggestionType.GAME, 1),
        SearchSuggestion("Space Adventure", com.roshni.games.feature.search.data.model.SuggestionType.GAME, 1),
        SearchSuggestion("Puzzle", com.roshni.games.feature.search.data.model.SuggestionType.CATEGORY, 25),
        SearchSuggestion("Adventure", com.roshni.games.feature.search.data.model.SuggestionType.CATEGORY, 18)
    )

    /**
     * Initialize the search service
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.d("Initializing GlobalSearchService")

            // Load search history and suggestions
            loadSearchHistory()
            loadSearchSuggestions()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize GlobalSearchService")
            Result.failure(e)
        }
    }

    /**
     * Perform global search
     */
    fun search(query: SearchQuery): Flow<List<SearchResult>> {
        val startTime = System.currentTimeMillis()

        return kotlinx.coroutines.flow.flow {
            try {
                // Apply filters and search across different data sources
                val results = performSearch(query)

                // Sort results
                val sortedResults = sortResults(results, query.sortBy)

                // Apply limit and offset
                val paginatedResults = sortedResults
                    .drop(query.offset)
                    .take(query.limit)

                // Update search stats
                val searchTime = System.currentTimeMillis() - startTime
                _lastSearchStats.value = SearchStats(
                    totalResults = results.size,
                    searchTime = searchTime,
                    filtersApplied = query.filters.let { filters ->
                        (if (filters.types.isNotEmpty()) 1 else 0) +
                        (if (filters.categories.isNotEmpty()) 1 else 0) +
                        (if (filters.difficulty.isNotEmpty()) 1 else 0) +
                        (if (filters.tags.isNotEmpty()) 1 else 0) +
                        (if (filters.dateRange != null) 1 else 0) +
                        (if (filters.rating != null) 1 else 0)
                    }
                )

                // Add to search history if enabled
                if (_searchConfig.value.enableHistory && query.text.isNotBlank()) {
                    addToSearchHistory(query, results.size)
                }

                emit(paginatedResults)

                Timber.d("Search completed: '${query.text}' -> ${results.size} results in ${searchTime}ms")

            } catch (e: Exception) {
                Timber.e(e, "Search failed for query: ${query.text}")
                emit(emptyList())
            }
        }
    }

    /**
     * Get search suggestions for a query
     */
    fun getSearchSuggestions(partialQuery: String): Flow<List<SearchSuggestion>> {
        return kotlinx.coroutines.flow.flow {
            if (!_searchConfig.value.enableSuggestions || partialQuery.length < 2) {
                emit(emptyList())
                return@flow
            }

            try {
                val suggestions = sampleSuggestions.filter { suggestion ->
                    suggestion.text.contains(partialQuery, ignoreCase = true)
                }.take(_searchConfig.value.maxSuggestions)

                _searchSuggestions.value = suggestions
                emit(suggestions)

                Timber.d("Generated ${suggestions.size} suggestions for: $partialQuery")

            } catch (e: Exception) {
                Timber.e(e, "Failed to get search suggestions")
                emit(emptyList())
            }
        }
    }

    /**
     * Update search configuration
     */
    suspend fun updateSearchConfig(config: GlobalSearchConfig): Result<Unit> {
        return try {
            _searchConfig.value = config
            Timber.d("Updated search config: $config")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update search config")
            Result.failure(e)
        }
    }

    /**
     * Clear search history
     */
    suspend fun clearSearchHistory(): Result<Unit> {
        return try {
            _searchHistory.value = emptyList()
            Timber.d("Cleared search history")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear search history")
            Result.failure(e)
        }
    }

    /**
     * Remove specific search history entry
     */
    suspend fun removeSearchHistoryEntry(entryId: String): Result<Unit> {
        return try {
            val currentHistory = _searchHistory.value.toMutableList()
            currentHistory.removeAll { it.id == entryId }
            _searchHistory.value = currentHistory
            Timber.d("Removed search history entry: $entryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove search history entry")
            Result.failure(e)
        }
    }

    /**
     * Get popular searches
     */
    fun getPopularSearches(): Flow<List<SearchHistoryEntry>> {
        return _searchHistory.asStateFlow().map { history ->
            history.sortedByDescending { it.resultCount }.take(10)
        }
    }

    /**
     * Get trending searches (simulated)
     */
    fun getTrendingSearches(): Flow<List<String>> {
        return kotlinx.coroutines.flow.flow {
            val trending = listOf(
                "puzzle games",
                "adventure",
                "space shooter",
                "brain training",
                "multiplayer"
            )
            emit(trending)
        }
    }

    /**
     * Perform the actual search across different data sources
     */
    private fun performSearch(query: SearchQuery): List<SearchResult> {
        var results = sampleGames.toMutableList()

        // Filter by text query
        if (query.text.isNotBlank()) {
            results = results.filter { result ->
                result.title.contains(query.text, ignoreCase = true) ||
                result.subtitle?.contains(query.text, ignoreCase = true) == true ||
                result.description?.contains(query.text, ignoreCase = true) == true ||
                result.metadata.any { it.value.toString().contains(query.text, ignoreCase = true) }
            }.toMutableList()
        }

        // Apply filters
        results = applyFilters(results, query.filters)

        // Calculate relevance scores
        results.forEach { result ->
            result.relevanceScore = calculateRelevanceScore(result, query)
        }

        return results
    }

    /**
     * Apply search filters to results
     */
    private fun applyFilters(results: List<SearchResult>, filters: SearchFilters): List<SearchResult> {
        var filteredResults = results

        // Filter by type
        if (filters.types.isNotEmpty()) {
            filteredResults = filteredResults.filter { result ->
                filters.types.contains(result.type)
            }
        }

        // Filter by categories
        if (filters.categories.isNotEmpty()) {
            filteredResults = filteredResults.filter { result ->
                val category = result.metadata["category"]?.toString()
                category != null && filters.categories.any { cat ->
                    cat.equals(category, ignoreCase = true)
                }
            }
        }

        // Filter by difficulty
        if (filters.difficulty.isNotEmpty()) {
            filteredResults = filteredResults.filter { result ->
                val difficulty = result.metadata["difficulty"]?.toString()
                difficulty != null && filters.difficulty.any { diff ->
                    diff.equals(difficulty, ignoreCase = true)
                }
            }
        }

        // Filter by tags
        if (filters.tags.isNotEmpty()) {
            filteredResults = filteredResults.filter { result ->
                // In real implementation, results would have tags in metadata
                true // Simplified for this example
            }
        }

        // Filter by date range
        if (filters.dateRange != null) {
            filteredResults = filteredResults.filter { result ->
                result.lastUpdated in filters.dateRange.startDate..filters.dateRange.endDate
            }
        }

        // Filter by rating
        if (filters.rating != null) {
            filteredResults = filteredResults.filter { result ->
                val rating = result.metadata["rating"] as? Float
                rating != null && rating in filters.rating!!
            }
        }

        return filteredResults
    }

    /**
     * Sort search results
     */
    private fun sortResults(results: List<SearchResult>, sortBy: SearchSort): List<SearchResult> {
        return when (sortBy) {
            SearchSort.RELEVANCE -> results.sortedByDescending { it.relevanceScore }
            SearchSort.NAME -> results.sortedBy { it.title }
            SearchSort.DATE -> results.sortedByDescending { it.lastUpdated }
            SearchSort.POPULARITY -> results.sortedByDescending {
                (it.metadata["players"] as? Int) ?: 0
            }
            SearchSort.RATING -> results.sortedByDescending {
                (it.metadata["rating"] as? Float) ?: 0f
            }
        }
    }

    /**
     * Calculate relevance score for a search result
     */
    private fun calculateRelevanceScore(result: SearchResult, query: SearchQuery): Float {
        if (query.text.isBlank()) return 0.5f

        var score = 0f
        val queryLower = query.text.lowercase()

        // Title match gets highest score
        if (result.title.lowercase().contains(queryLower)) {
            score += 0.4f
        }

        // Subtitle match
        if (result.subtitle?.lowercase()?.contains(queryLower) == true) {
            score += 0.3f
        }

        // Description match
        if (result.description?.lowercase()?.contains(queryLower) == true) {
            score += 0.2f
        }

        // Metadata match
        result.metadata.forEach { (_, value) ->
            if (value.toString().lowercase().contains(queryLower)) {
                score += 0.1f
            }
        }

        return score.coerceIn(0f, 1f)
    }

    /**
     * Add search to history
     */
    private fun addToSearchHistory(query: SearchQuery, resultCount: Int) {
        val entry = SearchHistoryEntry(
            id = UUID.randomUUID().toString(),
            query = query.text,
            timestamp = System.currentTimeMillis(),
            resultCount = resultCount,
            filters = query.filters
        )

        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.add(0, entry) // Add to beginning

        // Keep only max history items
        if (currentHistory.size > _searchConfig.value.maxHistoryItems) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }

        _searchHistory.value = currentHistory
    }

    /**
     * Load search history (simulated)
     */
    private suspend fun loadSearchHistory() {
        // In real implementation, this would load from database
        // For now, we'll start with empty history
    }

    /**
     * Load search suggestions (simulated)
     */
    private suspend fun loadSearchSuggestions() {
        _searchSuggestions.value = sampleSuggestions
    }
}