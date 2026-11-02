package com.zenith.study.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartTimerPolicyTest {

    @Test
    fun smartTimer_screenOff_startsOrResumes() {
        val sm = TimerStateMachine(25 * 60 * 1000L)
        assertTrue(sm.state is TimerStatus.Ready)

        // Screen turned OFF at monotonic 1000
        val started = sm.startOrResume(nowMonotonic = 1000L)
        assertTrue(started)
        assertTrue(sm.state is TimerStatus.Running)

        // Duplicate screen OFF event must be idempotent
        val duplicateOff = sm.startOrResume(nowMonotonic = 1500L)
        assertFalse(duplicateOff)
        assertTrue(sm.state is TimerStatus.Running)
    }

    @Test
    fun smartTimer_screenOn_pausesCountdown() {
        val sm = TimerStateMachine(25 * 60 * 1000L)
        sm.startOrResume(nowMonotonic = 0L)

        // 5 minutes (300,000 ms) elapsed, then screen turned ON
        val paused = sm.pause(nowMonotonic = 300_000L)
        assertTrue(paused)
        assertTrue(sm.state is TimerStatus.Paused)
        assertEquals(20 * 60 * 1000L, sm.state.remainingMillis)

        // Duplicate screen ON event must be idempotent
        val duplicateOn = sm.pause(nowMonotonic = 310_000L)
        assertFalse(duplicateOn)
        assertTrue(sm.state is TimerStatus.Paused)
    }

    @Test
    fun smartTimer_resumesFromExactPausedRemaining() {
        val duration = 45 * 60 * 1000L // 45m
        val sm = TimerStateMachine(duration)
        sm.startOrResume(nowMonotonic = 0L)

        // Paused after 10m
        sm.pause(nowMonotonic = 10 * 60 * 1000L)
        assertEquals(35 * 60 * 1000L, sm.state.remainingMillis)

        // Resumed at monotonic 1,000,000L (screen turned off again)
        sm.startOrResume(nowMonotonic = 1_000_000L)
        assertTrue(sm.state is TimerStatus.Running)

        // 5m elapsed since resume
        val remaining = sm.getRemainingMillis(nowMonotonic = 1_000_000L + 5 * 60 * 1000L)
        assertEquals(30 * 60 * 1000L, remaining)
    }

    @Test
    fun partialSessionCalculation_recordsAccurateMinutesAndIncompleteState() {
        val totalDurationMillis = 60 * 60 * 1000L // 60 minutes
        val sm = TimerStateMachine(totalDurationMillis)
        sm.startOrResume(nowMonotonic = 0L)

        // Ended early after 22 minutes (1,320,000 ms)
        val nowMonotonic = 22 * 60 * 1000L
        val remainingMillis = sm.getRemainingMillis(nowMonotonic)
        val elapsedMillis = (totalDurationMillis - remainingMillis).coerceAtLeast(0L)
        val actualMinutes = (elapsedMillis / 60000L).toInt()

        assertEquals(22, actualMinutes)
        val isCompleted = remainingMillis <= 0L
        assertFalse("Early finish must NOT be recorded as completed", isCompleted)
    }

    @Test
    fun naturalCompletion_recordsFullMinutesAndCompletedState() {
        val totalDurationMillis = 25 * 60 * 1000L
        val sm = TimerStateMachine(totalDurationMillis)
        sm.startOrResume(nowMonotonic = 0L)

        val completed = sm.tick(nowMonotonic = 25 * 60 * 1000L)
        assertTrue(completed)
        assertTrue(sm.state is TimerStatus.Completed)
        assertEquals(0L, sm.state.remainingMillis)

        val plannedMinutes = (totalDurationMillis / 60000L).toInt()
        assertEquals(25, plannedMinutes)
    }

    @Test
    fun completedSession_doesNotAutoRestartOnScreenOff() {
        val totalDurationMillis = 5 * 1000L
        val sm = TimerStateMachine(totalDurationMillis)
        sm.startOrResume(nowMonotonic = 0L)
        sm.tick(nowMonotonic = 5 * 1000L)
        assertTrue(sm.state is TimerStatus.Completed)

        // Attempt screen OFF start
        val started = sm.startOrResume(nowMonotonic = 10_000L)
        assertFalse("Completed session must not auto-restart on screen off", started)
        assertTrue(sm.state is TimerStatus.Completed)
    }
}

