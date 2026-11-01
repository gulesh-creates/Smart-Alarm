package com.zenith.study.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zenith.study.domain.model.Alarm
import java.time.DayOfWeek

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<DayOfWeek>,
    val isEnabled: Boolean,
    val label: String,
    val tone: String,
    val walkToDismiss: Boolean,
    val stepTarget: Int,
    val isVibrationEnabled: Boolean,
    val lastTriggeredTimeMillis: Long
) {
    fun toDomain(): Alarm = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        daysOfWeek = daysOfWeek,
        isEnabled = isEnabled,
        label = label,
        tone = tone,
        walkToDismiss = walkToDismiss,
        stepTarget = stepTarget,
        isVibrationEnabled = isVibrationEnabled,
        lastTriggeredTimeMillis = lastTriggeredTimeMillis
    )

    companion object {
        fun fromDomain(alarm: Alarm): AlarmEntity = AlarmEntity(
            id = alarm.id,
            hour = alarm.hour,
            minute = alarm.minute,
            daysOfWeek = alarm.daysOfWeek,
            isEnabled = alarm.isEnabled,
            label = alarm.label,
            tone = alarm.tone,
            walkToDismiss = alarm.walkToDismiss,
            stepTarget = alarm.stepTarget,
            isVibrationEnabled = alarm.isVibrationEnabled,
            lastTriggeredTimeMillis = alarm.lastTriggeredTimeMillis
        )
    }
}

