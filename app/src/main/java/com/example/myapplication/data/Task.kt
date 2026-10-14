package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 本地持久化的任务。createdAt / dueAt / notifiedAt 均为 epoch 毫秒。 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** 任务名（标题）。 */
    val text: String,
    val createdAt: Long,
    val dueAt: Long,
    /** 识别出该任务的屏幕原文；手动创建或旧数据为 null。 */
    val originalText: String? = null,
    /** 最近一次到期提醒的推送时间；null 表示尚未提醒。 */
    val notifiedAt: Long? = null,
    /** 是否已完成。 */
    val completed: Boolean = false,
)
