package com.zenith.study.data.repository

import com.zenith.study.data.local.dao.FocusSessionDao
import com.zenith.study.data.local.entity.FocusSessionEntity
import com.zenith.study.domain.model.FocusSession
import com.zenith.study.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FocusSessionRepositoryImpl(
    private val focusSessionDao: FocusSessionDao
) : FocusSessionRepository {

    override fun getAllSessionsFlow(): Flow<List<FocusSession>> =
        focusSessionDao.getAllSessionsFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSessions(): List<FocusSession> =
        focusSessionDao.getAllSessions().map { it.toDomain() }

    override suspend fun insertSession(session: FocusSession): Long =
        focusSessionDao.insertSession(FocusSessionEntity.fromDomain(session))

    override suspend fun clearAll() =
        focusSessionDao.clearAll()
}

