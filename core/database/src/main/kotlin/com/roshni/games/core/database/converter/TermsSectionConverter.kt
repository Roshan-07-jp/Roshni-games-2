package com.roshni.games.core.database.converter

import androidx.room.TypeConverter
import com.roshni.games.core.utils.terms.TermsSection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TermsSectionConverter {

    @TypeConverter
    fun fromTermsSectionList(sections: List<TermsSection>): String {
        return Json.encodeToString(sections)
    }

    @TypeConverter
    fun toTermsSectionList(value: String): List<TermsSection> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList() // Default fallback
        }
    }
}