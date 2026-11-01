package com.zenith.study.domain.repository

import com.zenith.study.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarmsFlow(): Flow<List<Alarm>>
    fun getEnabledAlarmsFlow(): Flow<List<Alarm>>
    suspend fun getEnabledAlarms(): List<Alarm>
    suspend fun getAlarmById(id: Long): Alarm?
    suspend fun insertAlarm(alarm: Alarm): Long
    suspend fun updateAlarm(alarm: Alarm)
    suspend fun deleteAlarm(alarmId: Long)
    suspend fun clearAll()
}

