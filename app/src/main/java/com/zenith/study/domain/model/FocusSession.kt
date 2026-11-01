package com.zenith.study.domain.model

/**
 * Domain representation of a completed or ended study focus session.
 */
data class FocusSession(
    val id: Long = 0,
    val subject: String = "Deep Work",
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val isCompleted: Boolean
) {
    init {
        require(plannedMinutes > 0) { "Planned minutes must be positive" }
        require(actualMinutes >= 0) { "Actual minutes cannot be negative" }
        require(completedAtEpochMillis >= startedAtEpochMillis) { "Completion cannot precede start" }
    }
}

