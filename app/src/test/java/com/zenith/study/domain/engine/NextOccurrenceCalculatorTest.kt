package com.zenith.study.domain.engine

import com.zenith.study.domain.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class NextOccurrenceCalculatorTest {

    private val zone = ZoneId.of("America/New_York")

    @Test
    fun oneTimeAlarm_scheduledLaterToday_firesToday() {
        val now = ZonedDateTime.of(2026, 10, 31, 8, 0, 0, 0, zone)
        val alarm = Alarm(
            id = 1,
            hour = 9,
            minute = 30,
            daysOfWeek = emptySet(),
            isEnabled = true
        )

        val next = NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)
        assertNotNull(next)
        assertEquals(2026, next!!.year)
        assertEquals(10, next.monthValue)
        assertEquals(31, next.dayOfMonth)
        assertEquals(9, next.hour)
        assertEquals(30, next.minute)
    }

    @Test
    fun oneTimeAlarm_scheduledEarlierToday_firesTomorrow() {
        val now = ZonedDateTime.of(2026, 10, 31, 10, 0, 0, 0, zone)
        val alarm = Alarm(
            id = 1,
            hour = 8,
            minute = 15,
            daysOfWeek = emptySet(),
            isEnabled = true
        )

        val next = NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)
        assertNotNull(next)
        assertEquals(2026, next!!.year)
        assertEquals(11, next.monthValue) // Nov 1
        assertEquals(1, next.dayOfMonth)
        assertEquals(8, next.hour)
        assertEquals(15, next.minute)
    }

    @Test
    fun repeatingAlarm_selectsNextMatchingWeekday() {
        // 2026-10-31 is a Saturday
        val now = ZonedDateTime.of(2026, 10, 31, 14, 0, 0, 0, zone)
        assertEquals(DayOfWeek.SATURDAY, now.dayOfWeek)

        // Repeat Mon, Wed, Fri at 07:00
        val alarm = Alarm(
            id = 2,
            hour = 7,
            minute = 0,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            isEnabled = true
        )

        val next = NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)
        assertNotNull(next)
        assertEquals(DayOfWeek.MONDAY, next!!.dayOfWeek)
        assertEquals(2026, next.year)
        assertEquals(11, next.monthValue)
        assertEquals(2, next.dayOfMonth) // Monday Nov 2
        assertEquals(7, next.hour)
        assertEquals(0, next.minute)
    }

    @Test
    fun repeatingAlarm_todayMatchesAndLater_firesToday() {
        val now = ZonedDateTime.of(2026, 10, 31, 6, 0, 0, 0, zone) // Saturday
        val alarm = Alarm(
            id = 3,
            hour = 8,
            minute = 0,
            daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            isEnabled = true
        )

        val next = NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)
        assertNotNull(next)
        assertEquals(DayOfWeek.SATURDAY, next!!.dayOfWeek)
        assertEquals(31, next.dayOfMonth)
        assertEquals(8, next.hour)
    }

    @Test
    fun repeatingAlarm_todayMatchesButTimePassed_firesNextMatchingDay() {
        val now = ZonedDateTime.of(2026, 10, 31, 9, 0, 0, 0, zone) // Saturday
        val alarm = Alarm(
            id = 4,
            hour = 8,
            minute = 0,
            daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            isEnabled = true
        )

        val next = NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)
        assertNotNull(next)
        assertEquals(DayOfWeek.SUNDAY, next!!.dayOfWeek)
        assertEquals(1, next.dayOfMonth)
        assertEquals(8, next.hour)
    }

    @Test
    fun disabledAlarm_returnsNull() {
        val now = ZonedDateTime.of(2026, 10, 31, 8, 0, 0, 0, zone)
        val alarm = Alarm(
            id = 5,
            hour = 10,
            minute = 0,
            isEnabled = false
        )

        val next = NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)
        assertNull(next)
    }

    @Test
    fun midnightBoundary_schedulesAccurately() {
        val now = ZonedDateTime.of(2026, 10, 31, 23, 59, 30, 0, zone)
        val alarm = Alarm(
            id = 6,
            hour = 0,
            minute = 0,
            isEnabled = true
        )

        val next = NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)
        assertNotNull(next)
        assertEquals(1, next!!.dayOfMonth)
        assertEquals(11, next.monthValue)
        assertEquals(0, next.hour)
        assertEquals(0, next.minute)
    }

    @Test
    fun daylightSavingTime_fallBack_handlesOverlap() {
        // US Daylight Saving Time ends first Sunday in November (Nov 1, 2026)
        val now = ZonedDateTime.of(2026, 10, 31, 20, 0, 0, 0, zone)
        val alarm = Alarm(
            id = 7,
            hour = 6,
            minute = 0,
            isEnabled = true
        )

        val next = NextOccurrenceCalculator.calculateNextOccurrence(alarm, now)
        assertNotNull(next)
        assertTrue(next!!.isAfter(now))
        assertEquals(6, next.hour)
        assertEquals(0, next.minute)
    }
}

