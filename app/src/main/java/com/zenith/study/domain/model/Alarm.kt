package com.zenith.study.domain.model

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Domain representation of an Alarm.
 *
 * @param id Stable unique identifier (primary key in persistence).
 * @param hour 24-hour representation [0..23].
 * @param minute Minute representation [0..59].
 * @param daysOfWeek Set of repeat days. When empty, represents a one-time alarm.
 * @param isEnabled Whether the alarm is active and scheduled with AlarmManager.
 * @param label User-facing title for the alarm.
 * @param tone Tone name or content URI for the alarm.
 * @param walkToDismiss When true, requires physical steps to silence the ringing alarm.
 * @param stepTarget Number of steps required before dismiss is unlocked [5..500].
 * @param isVibrationEnabled Whether vibration pattern runs during alarm ring.
 * @param lastTriggeredTimeMillis Epoch timestamp of when this alarm last started ringing.
 */
data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val isEnabled: Boolean = true,
    val label: String = "",
    val tone: String = "Default",
    val walkToDismiss: Boolean = true,
    val stepTarget: Int = 30,
    val isVibrationEnabled: Boolean = true,
    val lastTriggeredTimeMillis: Long = 0L
) {
    init {
        require(hour in 0..23) { "Hour must be within 0..23, was: $hour" }
        require(minute in 0..59) { "Minute must be within 0..59, was: $minute" }
        if (walkToDismiss) {
            require(stepTarget in 5..500) { "Step target must be between 5 and 500, was: $stepTarget" }
        }
    }

    val isRepeating: Boolean
        get() = daysOfWeek.isNotEmpty()

    fun formatTime(is24Hour: Boolean): String {
        return if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when (val h = hour % 12) {
                0 -> 12
                else -> h
            }
            String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
        }
    }

    fun getDaysSummary(): String {
        if (!isRepeating) return "Once"
        if (daysOfWeek.size == 7) return "Every day"
        if (daysOfWeek == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            return "Mon - Fri"
        }
        if (daysOfWeek == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            return "Weekends"
        }
        // Ordered days
        val ordered = DayOfWeek.values().filter { it in daysOfWeek }
        return ordered.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
}

