package com.roshni.games.core.database.converter

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime

class LocalDateTimeConverter {

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): String {
        return dateTime.toString()
    }

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime {
        return value.toLocalDateTime()
    }
}