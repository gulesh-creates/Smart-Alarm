package com.zenith.study.domain.engine

import com.zenith.study.domain.model.FocusSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

data class DayActivity(
    val dayOfWeek: DayOfWeek,
    val shortLabel: String,
    val totalMinutes: Int,
    val isToday: Boolean
)

data class StatisticsReport(
    val todayMinutes: Int,
    val todaySessionsCount: Int,
    val weekMinutes: Int,
    val monthMinutes: Int,
    val longestBlockMinutes: Int,
    val streakDays: Int,
    val goalProgressFraction: Float,
    val weeklyDistribution: List<DayActivity>
)

/**
 * Pure engine for calculating study statistics from session timestamps.
 * Respects local midnight boundaries and user timezones.
 */
object StatisticsEngine {

    fun computeReport(
        sessions: List<FocusSession>,
        now: ZonedDateTime,
        dailyGoalMinutes: Int
    ): StatisticsReport {
        val zone = now.zone
        val today = now.toLocalDate()
        val currentYearMonth = today.year to today.month

        // Filter sessions by local dates
        var todayMins = 0
        var todayCount = 0
        var monthMins = 0
        var longestBlock = 0

        // Map for active dates to compute streak
        val dateMinutesMap = mutableMapOf<LocalDate, Int>()

        for (session in sessions) {
            val sessionDate = Instant.ofEpochMilli(session.startedAtEpochMillis)
                .atZone(zone)
                .toLocalDate()

            val minutes = session.actualMinutes
            dateMinutesMap[sessionDate] = (dateMinutesMap[sessionDate] ?: 0) + minutes

            if (sessionDate == today) {
                todayMins += minutes
                todayCount++
            }

            if (sessionDate.year == currentYearMonth.first && sessionDate.month == currentYearMonth.second) {
                monthMins += minutes
            }

            if (minutes > longestBlock) {
                longestBlock = minutes
            }
        }

        // Compute 7-day Monday -> Sunday activity
        // Find Monday of the current week
        val mondayThisWeek = today.with(DayOfWeek.MONDAY)
        val weeklyDistribution = mutableListOf<DayActivity>()
        var weekMins = 0

        for (i in 0L..6L) {
            val dayDate = mondayThisWeek.plusDays(i)
            val dayMins = dateMinutesMap[dayDate] ?: 0
            weekMins += dayMins

            weeklyDistribution.add(
                DayActivity(
                    dayOfWeek = dayDate.dayOfWeek,
                    shortLabel = dayDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    totalMinutes = dayMins,
                    isToday = (dayDate == today)
                )
            )
        }

        // Calculate consecutive active days streak ending today or yesterday
        var streak = 0
        var checkDate = if ((dateMinutesMap[today] ?: 0) > 0) today else today.minusDays(1)
        while ((dateMinutesMap[checkDate] ?: 0) > 0) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        val goalFraction = if (dailyGoalMinutes > 0) {
            (todayMins.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
        } else 0f

        return StatisticsReport(
            todayMinutes = todayMins,
            todaySessionsCount = todayCount,
            weekMinutes = weekMins,
            monthMinutes = monthMins,
            longestBlockMinutes = longestBlock,
            streakDays = streak,
            goalProgressFraction = goalFraction,
            weeklyDistribution = weeklyDistribution
        )
    }
}

