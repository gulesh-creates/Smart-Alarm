package com.zenith.study.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateMachineTest {

    @Test
    fun initialState_isReadyWithFullDuration() {
        val duration = 25 * 60 * 1000L
        val sm = TimerStateMachine(duration)

        assertTrue(sm.state is TimerStatus.Ready)
        assertEquals(duration, sm.state.totalDurationMillis)
        assertEquals(duration, sm.state.remainingMillis)
    }

    @Test
    fun startOrResume_transitionsToRunning() {
        val sm = TimerStateMachine(25 * 60 * 1000L)
        val started = sm.startOrResume(nowMonotonic = 1000L)

        assertTrue(started)
        assertTrue(sm.state is TimerStatus.Running)
        val running = sm.state as TimerStatus.Running
        assertEquals(1000L, running.resumeMonotonicMillis)
    }

    @Test
    fun pause_preservesRemainingTime() {
        val duration = 60 * 1000L // 60s
        val sm = TimerStateMachine(duration)
        sm.startOrResume(nowMonotonic = 0L)

        // 20s elapsed
        val paused = sm.pause(nowMonotonic = 20_000L)
        assertTrue(paused)
        assertTrue(sm.state is TimerStatus.Paused)
        assertEquals(40_000L, sm.state.remainingMillis)

        // Resumed at monotonic 50_000L
        sm.startOrResume(nowMonotonic = 50_000L)
        // 10s elapsed since resume
        assertEquals(30_000L, sm.getRemainingMillis(nowMonotonic = 60_000L))
    }

    @Test
    fun tick_detectsCompletionAndPreventsDuplicateCompletion() {
        val duration = 10 * 1000L // 10s
        val sm = TimerStateMachine(duration)
        sm.startOrResume(nowMonotonic = 0L)

        // 5s elapsed
        val completedHalfway = sm.tick(nowMonotonic = 5_000L)
        assertFalse(completedHalfway)
        assertTrue(sm.state is TimerStatus.Running)

        // 10s elapsed -> exact finish
        val completedAtEnd = sm.tick(nowMonotonic = 10_000L)
        assertTrue(completedAtEnd)
        assertTrue(sm.state is TimerStatus.Completed)
        assertEquals(0L, sm.state.remainingMillis)

        // Subsequent tick must NOT report completed again
        val subsequentTick = sm.tick(nowMonotonic = 15_000L)
        assertFalse(subsequentTick)
        assertTrue(sm.state is TimerStatus.Completed)
    }

    @Test
    fun reset_restoresOriginalDuration() {
        val duration = 45 * 60 * 1000L
        val sm = TimerStateMachine(duration)
        sm.startOrResume(0L)
        sm.pause(10_000L)

        sm.reset()
        assertTrue(sm.state is TimerStatus.Ready)
        assertEquals(duration, sm.state.remainingMillis)
    }
}

