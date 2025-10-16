package com.roshni.games.feature.search.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for search functionality
 */

/**
 * Search result item
 */
@Serializable
data class SearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val relevanceScore: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Types of search results
 */
@Serializable
enum class SearchResultType {
    GAME,
    ACHIEVEMENT,
    PLAYER,
    LEADERBOARD_ENTRY,
    CONTENT,
    CATEGORY
}

/**
 * Search query
 */
@Serializable
data class SearchQuery(
    val text: String,
    val filters: SearchFilters = SearchFilters(),
    val sortBy: SearchSort = SearchSort.RELEVANCE,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Search filters
 */
@Serializable
data class SearchFilters(
    val types: List<SearchResultType> = emptyList(),
    val categories: List<String> = emptyList(),
    val difficulty: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val dateRange: DateRange? = null,
    val rating: IntRange? = null,
    val isOnline: Boolean? = null
)

/**
 * Date range for filtering
 */
@Serializable
data class DateRange(
    val startDate: Long,
    val endDate: Long
)

/**
 * Search sort options
 */
@Serializable
enum class SearchSort {
    RELEVANCE,
    NAME,
    DATE,
    POPULARITY,
    RATING
}

/**
 * Search suggestions
 */
@Serializable
data class SearchSuggestion(
    val text: String,
    val type: SuggestionType,
    val count: Int = 0
)

/**
 * Types of search suggestions
 */
@Serializable
enum class SuggestionType {
    QUERY,
    GAME,
    CATEGORY,
    TAG,
    PLAYER
}

/**
 * Search statistics
 */
@Serializable
data class SearchStats(
    val totalResults: Int = 0,
    val searchTime: Long = 0, // in milliseconds
    val filtersApplied: Int = 0,
    val suggestionsShown: Int = 0
)

/**
 * Search history entry
 */
@Serializable
data class SearchHistoryEntry(
    val id: String,
    val query: String,
    val timestamp: Long,
    val resultCount: Int,
    val filters: SearchFilters
)

/**
 * Global search configuration
 */
@Serializable
data class GlobalSearchConfig(
    val enabled: Boolean = true,
    val maxResults: Int = 100,
    val searchDelay: Long = 300, // debounce delay in milliseconds
    val enableSuggestions: Boolean = true,
    val maxSuggestions: Int = 10,
    val enableHistory: Boolean = true,
    val maxHistoryItems: Int = 50,
    val fuzzySearchEnabled: Boolean = true,
    val fuzzyThreshold: Float = 0.6f
)