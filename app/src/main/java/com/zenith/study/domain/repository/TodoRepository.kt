package com.zenith.study.domain.repository

import com.zenith.study.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodosForDate(epochDay: Long): Flow<List<TodoItem>>
    fun getTodosForAlarmAndDate(alarmId: Long, epochDay: Long): Flow<List<TodoItem>>
    fun getAllTodos(): Flow<List<TodoItem>>
    suspend fun insertTodo(item: TodoItem): Long
    suspend fun updateTodo(item: TodoItem)
    suspend fun deleteTodo(id: Long)
    suspend fun clearAll()
}

