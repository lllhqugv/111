package com.example.myapplication.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskStatusTest {

    private val now = 1_000_000L

    private fun task(dueAt: Long, completed: Boolean = false) = Task(
        id = 1L,
        text = "测试任务",
        createdAt = 0L,
        dueAt = dueAt,
        completed = completed,
    )

    @Test
    fun `future due task is active`() {
        assertTrue(task(dueAt = now + 60_000L).isActive(now))
    }

    @Test
    fun `task without due time is active`() {
        assertTrue(task(dueAt = 0L).isActive(now))
    }

    @Test
    fun `expired task is not active`() {
        assertFalse(task(dueAt = now - 1L).isActive(now))
    }

    @Test
    fun `completed task is not active even in the future`() {
        assertFalse(task(dueAt = now + 60_000L, completed = true).isActive(now))
    }

    @Test
    fun `completed undated task is not active`() {
        assertFalse(task(dueAt = 0L, completed = true).isActive(now))
    }
}
