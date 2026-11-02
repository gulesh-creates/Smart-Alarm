package com.zenith.study.data.repository

import com.zenith.study.data.local.dao.TodoDao
import com.zenith.study.data.local.entity.TodoItemEntity
import com.zenith.study.domain.model.TodoItem
import com.zenith.study.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TodoRepositoryImpl(private val todoDao: TodoDao) : TodoRepository {

    override fun getTodosForDate(epochDay: Long): Flow<List<TodoItem>> {
        return todoDao.getTodosForDate(epochDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTodosForAlarmAndDate(alarmId: Long, epochDay: Long): Flow<List<TodoItem>> {
        return todoDao.getTodosForAlarmAndDate(alarmId, epochDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllTodos(): Flow<List<TodoItem>> {
        return todoDao.getAllTodos().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertTodo(item: TodoItem): Long {
        return todoDao.insertTodo(TodoItemEntity.fromDomain(item))
    }

    override suspend fun updateTodo(item: TodoItem) {
        todoDao.updateTodo(TodoItemEntity.fromDomain(item))
    }

    override suspend fun deleteTodo(id: Long) {
        todoDao.deleteTodo(id)
    }

    override suspend fun clearAll() {
        todoDao.clearAll()
    }
}
