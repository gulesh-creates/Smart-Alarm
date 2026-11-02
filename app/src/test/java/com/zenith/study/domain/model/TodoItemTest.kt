package com.zenith.study.domain.model

import com.zenith.study.data.local.entity.TodoItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodoItemTest {

    @Test
    fun todoItem_creationAndDomainMapping() {
        val todayEpochDay = LocalDate.now().toEpochDay()
        val domain = TodoItem(
            id = 5,
            alarmId = 10,
            occurrenceDateEpochDay = todayEpochDay,
            taskText = "Review Operating Systems Chapter 4",
            isCompleted = false,
            createdAt = 1700000000000L
        )

        val entity = TodoItemEntity.fromDomain(domain)
        assertEquals(5L, entity.id)
        assertEquals(10L, entity.alarmId)
        assertEquals(todayEpochDay, entity.occurrenceDateEpochDay)
        assertEquals("Review Operating Systems Chapter 4", entity.taskText)
        assertFalse(entity.isCompleted)
        assertEquals(1700000000000L, entity.createdAt)

        val roundtrip = entity.toDomain()
        assertEquals(domain, roundtrip)
    }

    @Test
    fun todoItem_toggleCompletion() {
        val domain = TodoItem(
            id = 1,
            taskText = "Practice Kotlin Coroutines",
            isCompleted = false
        )
        val toggled = domain.copy(isCompleted = !domain.isCompleted)
        assertTrue(toggled.isCompleted)
    }
}

