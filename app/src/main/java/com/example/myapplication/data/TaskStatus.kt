package com.example.myapplication.data

/**
 * 任务是否仍需展示/提醒：未完成，且未过期（无截止时间视为不限期）。
 */
fun Task.isActive(now: Long): Boolean = !completed && (dueAt <= 0L || dueAt > now)
