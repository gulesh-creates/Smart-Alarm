package com.zenith.study.data.repository

import com.zenith.study.data.local.dao.AlarmDao
import com.zenith.study.data.local.entity.AlarmEntity
import com.zenith.study.domain.model.Alarm
import com.zenith.study.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepositoryImpl(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarmsFlow(): Flow<List<Alarm>> =
        alarmDao.getAllAlarmsFlow().map { list -> list.map { it.toDomain() } }

    override fun getEnabledAlarmsFlow(): Flow<List<Alarm>> =
        alarmDao.getEnabledAlarmsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getEnabledAlarms(): List<Alarm> =
        alarmDao.getEnabledAlarms().map { it.toDomain() }

    override suspend fun getAlarmById(id: Long): Alarm? =
        alarmDao.getAlarmById(id)?.toDomain()

    override suspend fun insertAlarm(alarm: Alarm): Long =
        alarmDao.insertAlarm(AlarmEntity.fromDomain(alarm))

    override suspend fun updateAlarm(alarm: Alarm) =
        alarmDao.updateAlarm(AlarmEntity.fromDomain(alarm))

    override suspend fun deleteAlarm(alarmId: Long) =
        alarmDao.deleteAlarmById(alarmId)

    override suspend fun clearAll() =
        alarmDao.clearAll()
}

