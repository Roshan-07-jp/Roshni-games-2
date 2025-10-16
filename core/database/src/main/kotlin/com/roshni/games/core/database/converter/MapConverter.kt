package com.roshni.games.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MapConverter {

    @TypeConverter
    fun fromStringMap(map: Map<String, Any>): String {
        return Json.encodeToString(map)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, Any> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap() // Default fallback
        }
    }
}