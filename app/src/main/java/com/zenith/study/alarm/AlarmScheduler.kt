package com.zenith.study.alarm

import com.zenith.study.domain.model.Alarm

interface AlarmScheduler {
    fun schedule(alarm: Alarm): Boolean
    fun cancel(alarmId: Long)
    fun canScheduleExactAlarms(): Boolean
}

