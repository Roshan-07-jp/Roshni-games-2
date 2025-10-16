package com.roshni.games.core.database.converter

import androidx.room.TypeConverter
import com.roshni.games.core.utils.terms.AcceptanceMethod

class AcceptanceMethodConverter {

    @TypeConverter
    fun fromAcceptanceMethod(method: AcceptanceMethod): String {
        return method.name
    }

    @TypeConverter
    fun toAcceptanceMethod(value: String): AcceptanceMethod {
        return try {
            AcceptanceMethod.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AcceptanceMethod.EXPLICIT_CLICK // Default fallback
        }
    }
}