package com.roshni.games.core.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Date and Time utility functions for Roshni Games
 */

object DateTimeUtils {

    fun getCurrentDateTime(): LocalDateTime {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatTimeAgo(dateTime: LocalDateTime): String {
        val now = getCurrentDateTime()
        val duration = now.toInstant(TimeZone.currentSystemDefault()) -
                dateTime.toInstant(TimeZone.currentSystemDefault())

        val minutes = duration.inWholeMinutes
        val hours = duration.inWholeHours
        val days = duration.inWholeDays

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            days < 7 -> "$days days ago"
            else -> {
                val weeks = days / 7
                if (weeks < 4) "$weeks weeks ago" else "A long time ago"
            }
        }
    }

    fun isToday(dateTime: LocalDateTime): Boolean {
        val today = getCurrentDateTime().date
        return dateTime.date == today
    }

    fun isYesterday(dateTime: LocalDateTime): Boolean {
        val yesterday = getCurrentDateTime().date.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        return dateTime.date == yesterday
    }

    fun isWithinLastDays(dateTime: LocalDateTime, days: Int): Boolean {
        val cutoff = getCurrentDateTime().date.minus(days, kotlinx.datetime.DateTimeUnit.DAY)
        return dateTime.date >= cutoff
    }
}