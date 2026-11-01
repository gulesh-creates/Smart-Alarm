package com.zenith.study.domain.repository

import com.zenith.study.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow

interface FocusSessionRepository {
    fun getAllSessionsFlow(): Flow<List<FocusSession>>
    suspend fun getAllSessions(): List<FocusSession>
    suspend fun insertSession(session: FocusSession): Long
    suspend fun clearAll()
}

