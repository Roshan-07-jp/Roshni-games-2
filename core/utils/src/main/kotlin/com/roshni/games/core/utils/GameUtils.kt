package com.roshni.games.core.utils

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Gaming utility functions for Roshni Games
 */

object GameUtils {

    /**
     * Calculate experience points based on score and difficulty
     */
    fun calculateExperience(score: Long, difficulty: String, timeBonus: Boolean = false): Long {
        val baseXP = when (difficulty.uppercase()) {
            "EASY" -> score / 100
            "MEDIUM" -> score / 50
            "HARD" -> score / 25
            "EXPERT" -> score / 10
            else -> score / 50
        }

        return if (timeBonus) (baseXP * 1.5).toLong() else baseXP
    }

    /**
     * Calculate level based on experience points
     */
    fun calculateLevel(experience: Long): Int {
        return when {
            experience < 1000 -> 1
            experience < 5000 -> 2
            experience < 12000 -> 3
            experience < 22000 -> 4
            experience < 35000 -> 5
            experience < 52000 -> 6
            experience < 73000 -> 7
            experience < 98000 -> 8
            experience < 127000 -> 9
            experience < 160000 -> 10
            else -> 10 + ((experience - 160000) / 50000).toInt()
        }
    }

    /**
     * Calculate experience required for next level
     */
    fun experienceForNextLevel(currentLevel: Int): Long {
        return when (currentLevel) {
            1 -> 1000
            2 -> 4000
            3 -> 7000
            4 -> 10000
            5 -> 13000
            6 -> 17000
            7 -> 21000
            8 -> 25000
            9 -> 29000
            10 -> 33000
            else -> 33000 + (currentLevel - 10) * 50000
        }
    }

    /**
     * Calculate score multiplier based on streak
     */
    fun calculateStreakMultiplier(streak: Int): Float {
        return 1.0f + min(streak, 10) * 0.1f
    }

    /**
     * Generate a random game seed
     */
    fun generateGameSeed(): Long {
        return Random.nextLong(Long.MAX_VALUE)
    }

    /**
     * Format score with appropriate suffix (K, M, B)
     */
    fun formatScore(score: Long): String {
        return when {
            score >= 1_000_000_000 -> String.format("%.1fB", score / 1_000_000_000.0)
            score >= 1_000_000 -> String.format("%.1fM", score / 1_000_000.0)
            score >= 1_000 -> String.format("%.1fK", score / 1_000.0)
            else -> score.toString()
        }
    }

    /**
     * Calculate percentage of completion
     */
    fun calculatePercentage(current: Long, total: Long): Float {
        return if (total > 0) min((current.toFloat() / total.toFloat()) * 100, 100f) else 0f
    }

    /**
     * Generate achievement progress text
     */
    fun getAchievementProgressText(current: Long, target: Long): String {
        val percentage = calculatePercentage(current, target)
        return "Progress: ${formatScore(current)} / ${formatScore(target)} (${percentage.toInt()}%)"
    }

    /**
     * Validate game score
     */
    fun isValidScore(score: Long): Boolean {
        return score >= 0 && score <= Long.MAX_VALUE
    }

    /**
     * Calculate combo multiplier
     */
    fun calculateComboMultiplier(combo: Int): Float {
        return 1.0f + (combo - 1) * 0.05f
    }

    /**
     * Generate random number within range
     */
    fun randomInt(min: Int, max: Int): Int {
        return Random.nextInt(min, max + 1)
    }

    /**
     * Generate random float within range
     */
    fun randomFloat(min: Float, max: Float): Float {
        return Random.nextFloat() * (max - min) + min
    }

    /**
     * Clamp value between min and max
     */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return max(min, min(value, max))
    }

    /**
     * Clamp value between min and max (Int version)
     */
    fun clamp(value: Int, min: Int, max: Int): Int {
        return max(min, min(value, max))
    }

    /**
     * Linear interpolation between two values
     */
    fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * clamp(fraction, 0f, 1f)
    }

    /**
     * Calculate distance between two points
     */
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}