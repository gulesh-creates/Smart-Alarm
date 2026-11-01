package com.zenith.study.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zenith.study.domain.model.FocusSession

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val isCompleted: Boolean
) {
    fun toDomain(): FocusSession = FocusSession(
        id = id,
        subject = subject,
        plannedMinutes = plannedMinutes,
        actualMinutes = actualMinutes,
        startedAtEpochMillis = startedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        isCompleted = isCompleted
    )

    companion object {
        fun fromDomain(session: FocusSession): FocusSessionEntity = FocusSessionEntity(
            id = session.id,
            subject = session.subject,
            plannedMinutes = session.plannedMinutes,
            actualMinutes = session.actualMinutes,
            startedAtEpochMillis = session.startedAtEpochMillis,
            completedAtEpochMillis = session.completedAtEpochMillis,
            isCompleted = session.isCompleted
        )
    }
}

