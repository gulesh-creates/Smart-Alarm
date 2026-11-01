package com.zenith.study.data.local

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>?): String {
        if (days.isNullOrEmpty()) return ""
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(data: String?): Set<DayOfWeek> {
        if (data.isNullOrBlank()) return emptySet()
        return data.split(",")
            .mapNotNull {
                try {
                    DayOfWeek.valueOf(it.trim())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .toSet()
    }
}

