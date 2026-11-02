package com.zenith.study.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class AlarmValidationTest {

    @Test
    fun validAlarm_createsSuccessfully() {
        val alarm = Alarm(
            id = 1,
            hour = 6,
            minute = 30,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            walkToDismiss = true,
            stepTarget = 30,
            volume = 0.8f,
            showEmergencyDismiss = true,
            tone = "Custom Chime",
            toneUri = "content://media/audio/123"
        )
        assertEquals(6, alarm.hour)
        assertEquals(30, alarm.minute)
        assertEquals(true, alarm.isRepeating)
        assertEquals(0.8f, alarm.volume, 0.001f)
        assertTrue(alarm.showEmergencyDismiss)
        assertEquals("content://media/audio/123", alarm.toneUri)
        assertEquals("06:30 AM", alarm.formatTime(is24Hour = false))
        assertEquals("06:30", alarm.formatTime(is24Hour = true))
    }

    @Test
    fun invalidHour_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            Alarm(hour = 24, minute = 0)
        }
    }

    @Test
    fun invalidMinute_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            Alarm(hour = 12, minute = 60)
        }
    }

    @Test
    fun invalidStepTarget_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            Alarm(hour = 7, minute = 0, walkToDismiss = true, stepTarget = 2) // Minimum is 5
        }
    }

    @Test
    fun invalidVolumeNegative_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            Alarm(hour = 7, minute = 0, volume = -0.1f)
        }
    }

    @Test
    fun invalidVolumeExcess_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            Alarm(hour = 7, minute = 0, volume = 1.05f)
        }
    }

    @Test
    fun emergencyDismiss_defaultsToTrue() {
        val alarm = Alarm(hour = 8, minute = 0)
        assertTrue(alarm.showEmergencyDismiss)
        assertEquals(1.0f, alarm.volume, 0.001f)
    }

    @Test
    fun emergencyDismiss_canBeDisabledForStrictMode() {
        val alarm = Alarm(hour = 8, minute = 0, walkToDismiss = true, stepTarget = 50, showEmergencyDismiss = false)
        assertFalse(alarm.showEmergencyDismiss)
    }
}
