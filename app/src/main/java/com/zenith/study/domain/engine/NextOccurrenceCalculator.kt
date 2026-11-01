package com.zenith.study.domain.engine

import com.zenith.study.domain.model.Alarm
import java.time.ZonedDateTime

/**
 * Pure engine for calculating exact chronological next alarm occurrences.
 * Handles one-time alarms, recurring weekday patterns, midnight rollovers,
 * daylight saving transitions, and timezone changes.
 */
object NextOccurrenceCalculator {

    /**
     * Calculates the exact next [ZonedDateTime] when the given [alarm] should fire.
     *
     * @param alarm The alarm definition.
     * @param now The reference point in time (usually ZonedDateTime.now(ZoneId.systemDefault())).
     * @return The next ZonedDateTime strictly in the future, or null if the alarm is disabled.
     */
    fun calculateNextOccurrence(alarm: Alarm, now: ZonedDateTime): ZonedDateTime? {
        if (!alarm.isEnabled) return null

        val targetToday = now
            .withHour(alarm.hour)
            .withMinute(alarm.minute)
            .withSecond(0)
            .withNano(0)

        // Case 1: One-time alarm (no weekdays selected)
        if (!alarm.isRepeating) {
            return if (targetToday.isAfter(now)) {
                targetToday
            } else {
                targetToday.plusDays(1)
            }
        }

        // Case 2: Repeating alarm on specific days of the week
        // Check today first
        if (alarm.daysOfWeek.contains(now.dayOfWeek) && targetToday.isAfter(now)) {
            return targetToday
        }

        // Search the next 1 to 7 days
        for (offset in 1L..7L) {
            val candidateDate = now.plusDays(offset)
            if (alarm.daysOfWeek.contains(candidateDate.dayOfWeek)) {
                return candidateDate
                    .withHour(alarm.hour)
                    .withMinute(alarm.minute)
                    .withSecond(0)
                    .withNano(0)
            }
        }

        return null
    }

    /**
     * Convenience method returning epoch millis for AlarmManager.
     */
    fun calculateNextEpochMillis(alarm: Alarm, now: ZonedDateTime): Long? {
        return calculateNextOccurrence(alarm, now)?.toInstant()?.toEpochMilli()
    }
}

