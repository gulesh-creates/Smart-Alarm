package com.zenith.study.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zenith.study.data.local.entity.TodoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todo_items WHERE occurrenceDateEpochDay = :epochDay ORDER BY createdAt ASC")
    fun getTodosForDate(epochDay: Long): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_items WHERE alarmId = :alarmId AND occurrenceDateEpochDay = :epochDay ORDER BY createdAt ASC")
    fun getTodosForAlarmAndDate(alarmId: Long, epochDay: Long): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(item: TodoItemEntity): Long

    @Update
    suspend fun updateTodo(item: TodoItemEntity)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteTodo(id: Long)

    @Query("DELETE FROM todo_items")
    suspend fun clearAll()
}

