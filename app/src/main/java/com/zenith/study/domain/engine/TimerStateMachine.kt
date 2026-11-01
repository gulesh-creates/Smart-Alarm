package com.zenith.study.domain.engine

sealed interface TimerStatus {
    val totalDurationMillis: Long
    val remainingMillis: Long

    data class Ready(
        override val totalDurationMillis: Long
    ) : TimerStatus {
        override val remainingMillis: Long = totalDurationMillis
    }

    data class Running(
        override val totalDurationMillis: Long,
        val runningRemainingBaseMillis: Long,
        val resumeMonotonicMillis: Long
    ) : TimerStatus {
        override val remainingMillis: Long
            get() = (runningRemainingBaseMillis).coerceAtLeast(0L)
    }

    data class Paused(
        override val totalDurationMillis: Long,
        override val remainingMillis: Long
    ) : TimerStatus

    data class Completed(
        override val totalDurationMillis: Long
    ) : TimerStatus {
        override val remainingMillis: Long = 0L
    }
}

/**
 * Pure state machine tracking study countdown via monotonic clock.
 * Immune to drift from suspended intervals or system wall-clock adjustments.
 */
class TimerStateMachine(initialDurationMillis: Long = 25 * 60 * 1000L) {

    var state: TimerStatus = TimerStatus.Ready(initialDurationMillis)
        private set

    fun setDuration(durationMillis: Long) {
        require(durationMillis > 0) { "Duration must be positive" }
        state = TimerStatus.Ready(durationMillis)
    }

    fun startOrResume(nowMonotonic: Long): Boolean {
        return when (val current = state) {
            is TimerStatus.Ready -> {
                state = TimerStatus.Running(
                    totalDurationMillis = current.totalDurationMillis,
                    runningRemainingBaseMillis = current.totalDurationMillis,
                    resumeMonotonicMillis = nowMonotonic
                )
                true
            }
            is TimerStatus.Paused -> {
                state = TimerStatus.Running(
                    totalDurationMillis = current.totalDurationMillis,
                    runningRemainingBaseMillis = current.remainingMillis,
                    resumeMonotonicMillis = nowMonotonic
                )
                true
            }
            is TimerStatus.Running, is TimerStatus.Completed -> false
        }
    }

    fun pause(nowMonotonic: Long): Boolean {
        return when (val current = state) {
            is TimerStatus.Running -> {
                val elapsedSinceResume = (nowMonotonic - current.resumeMonotonicMillis).coerceAtLeast(0L)
                val remaining = (current.runningRemainingBaseMillis - elapsedSinceResume).coerceAtLeast(0L)
                state = if (remaining <= 0L) {
                    TimerStatus.Completed(current.totalDurationMillis)
                } else {
                    TimerStatus.Paused(current.totalDurationMillis, remaining)
                }
                true
            }
            is TimerStatus.Ready, is TimerStatus.Paused, is TimerStatus.Completed -> false
        }
    }

    fun reset() {
        state = TimerStatus.Ready(state.totalDurationMillis)
    }

    /**
     * Updates the remaining time and transitions to Completed if 0 is reached.
     * @return true if the timer transitioned to Completed on this tick.
     */
    fun tick(nowMonotonic: Long): Boolean {
        val current = state
        if (current is TimerStatus.Running) {
            val elapsed = (nowMonotonic - current.resumeMonotonicMillis).coerceAtLeast(0L)
            val remaining = (current.runningRemainingBaseMillis - elapsed).coerceAtLeast(0L)
            if (remaining <= 0L) {
                state = TimerStatus.Completed(current.totalDurationMillis)
                return true
            }
        }
        return false
    }

    fun getRemainingMillis(nowMonotonic: Long): Long {
        return when (val current = state) {
            is TimerStatus.Ready -> current.totalDurationMillis
            is TimerStatus.Paused -> current.remainingMillis
            is TimerStatus.Completed -> 0L
            is TimerStatus.Running -> {
                val elapsed = (nowMonotonic - current.resumeMonotonicMillis).coerceAtLeast(0L)
                (current.runningRemainingBaseMillis - elapsed).coerceAtLeast(0L)
            }
        }
    }
}

