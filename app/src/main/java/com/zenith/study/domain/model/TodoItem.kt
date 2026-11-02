package com.zenith.study.domain.model

import java.time.LocalDate

/**
 * Domain representation of a To-Do task associated with a morning study routine
 * or post-alarm challenge.
 *
 * @param id Stable primary key.
 * @param alarmId The alarm that initiated this to-do entry (-1 if general).
 * @param occurrenceDateEpochDay LocalDate.toEpochDay() representing the day of occurrence.
 * @param taskText Description of the task.
 * @param isCompleted Whether the task is checked off.
 * @param createdAt Epoch timestamp when created.
 */
data class TodoItem(
    val id: Long = 0,
    val alarmId: Long = -1L,
    val occurrenceDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val taskText: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

