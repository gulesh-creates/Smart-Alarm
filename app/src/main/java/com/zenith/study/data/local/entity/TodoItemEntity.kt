package com.zenith.study.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zenith.study.domain.model.TodoItem

@Entity(tableName = "todo_items")
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val occurrenceDateEpochDay: Long,
    val taskText: String,
    val isCompleted: Boolean,
    val createdAt: Long
) {
    fun toDomain(): TodoItem = TodoItem(
        id = id,
        alarmId = alarmId,
        occurrenceDateEpochDay = occurrenceDateEpochDay,
        taskText = taskText,
        isCompleted = isCompleted,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(todo: TodoItem): TodoItemEntity = TodoItemEntity(
            id = todo.id,
            alarmId = todo.alarmId,
            occurrenceDateEpochDay = todo.occurrenceDateEpochDay,
            taskText = todo.taskText,
            isCompleted = todo.isCompleted,
            createdAt = todo.createdAt
        )
    }
}

