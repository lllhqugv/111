package com.example.myapplication.parse

/** 解析结果：任务内容 + 截止时间（epoch 毫秒；未识别到时间时为 null）。 */
data class ParsedTask(
    val content: String,
    val dueAt: Long?,
)
