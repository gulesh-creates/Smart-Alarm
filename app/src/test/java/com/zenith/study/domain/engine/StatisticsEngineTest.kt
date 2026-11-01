package com.zenith.study.domain.engine

import com.zenith.study.domain.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StatisticsEngineTest {

    private val zone = ZoneId.of("America/New_York")
    private val now = ZonedDateTime.of(2026, 10, 31, 15, 0, 0, 0, zone)

    @Test
    fun freshInstall_hasZeroSessionsAndZeroStreak() {
        val report = StatisticsEngine.computeReport(
            sessions = emptyList(),
            now = now,
            dailyGoalMinutes = 180
        )

        assertEquals(0, report.todayMinutes)
        assertEquals(0, report.todaySessionsCount)
        assertEquals(0, report.weekMinutes)
        assertEquals(0, report.monthMinutes)
        assertEquals(0, report.streakDays)
        assertEquals(0, report.longestBlockMinutes)
        assertEquals(0f, report.goalProgressFraction)
        assertEquals(7, report.weeklyDistribution.size)
    }

    @Test
    fun sessionsToday_sumAccurately() {
        val s1 = FocusSession(
            id = 1,
            plannedMinutes = 25,
            actualMinutes = 25,
            startedAtEpochMillis = now.minusHours(2).toInstant().toEpochMilli(),
            completedAtEpochMillis = now.minusHours(2).plusMinutes(25).toInstant().toEpochMilli(),
            isCompleted = true
        )
        val s2 = FocusSession(
            id = 2,
            plannedMinutes = 45,
            actualMinutes = 45,
            startedAtEpochMillis = now.minusHours(1).toInstant().toEpochMilli(),
            completedAtEpochMillis = now.minusHours(1).plusMinutes(45).toInstant().toEpochMilli(),
            isCompleted = true
        )

        val report = StatisticsEngine.computeReport(
            sessions = listOf(s1, s2),
            now = now,
            dailyGoalMinutes = 100
        )

        assertEquals(70, report.todayMinutes)
        assertEquals(2, report.todaySessionsCount)
        assertEquals(45, report.longestBlockMinutes)
        assertEquals(0.7f, report.goalProgressFraction, 0.001f)
        assertEquals(1, report.streakDays)
    }

    @Test
    fun sessionsFromYesterday_doNotPolluteToday() {
        val yesterdaySession = FocusSession(
            id = 1,
            plannedMinutes = 60,
            actualMinutes = 60,
            startedAtEpochMillis = now.minusDays(1).toInstant().toEpochMilli(),
            completedAtEpochMillis = now.minusDays(1).plusMinutes(60).toInstant().toEpochMilli(),
            isCompleted = true
        )

        val report = StatisticsEngine.computeReport(
            sessions = listOf(yesterdaySession),
            now = now,
            dailyGoalMinutes = 180
        )

        assertEquals(0, report.todayMinutes)
        assertEquals(0, report.todaySessionsCount)
        assertEquals(60, report.weekMinutes) // Still within current week
        assertEquals(1, report.streakDays) // Streak ending yesterday counts as 1 day active!
    }

    @Test
    fun multiDayStreak_computesConsecutiveDays() {
        val sToday = FocusSession(
            id = 1, plannedMinutes = 30, actualMinutes = 30,
            startedAtEpochMillis = now.toInstant().toEpochMilli(),
            completedAtEpochMillis = now.plusMinutes(30).toInstant().toEpochMilli(),
            isCompleted = true
        )
        val sYesterday = FocusSession(
            id = 2, plannedMinutes = 30, actualMinutes = 30,
            startedAtEpochMillis = now.minusDays(1).toInstant().toEpochMilli(),
            completedAtEpochMillis = now.minusDays(1).plusMinutes(30).toInstant().toEpochMilli(),
            isCompleted = true
        )
        val sTwoDaysAgo = FocusSession(
            id = 3, plannedMinutes = 30, actualMinutes = 30,
            startedAtEpochMillis = now.minusDays(2).toInstant().toEpochMilli(),
            completedAtEpochMillis = now.minusDays(2).plusMinutes(30).toInstant().toEpochMilli(),
            isCompleted = true
        )

        val report = StatisticsEngine.computeReport(
            sessions = listOf(sToday, sYesterday, sTwoDaysAgo),
            now = now,
            dailyGoalMinutes = 180
        )

        assertEquals(3, report.streakDays)
    }
}

