package com.roshni.games.core.database.converter

import androidx.room.TypeConverter
import com.roshni.games.core.utils.terms.TermsType

class TermsTypeConverter {

    @TypeConverter
    fun fromTermsType(type: TermsType): String {
        return type.name
    }

    @TypeConverter
    fun toTermsType(value: String): TermsType {
        return try {
            TermsType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TermsType.CUSTOM // Default fallback
        }
    }
}