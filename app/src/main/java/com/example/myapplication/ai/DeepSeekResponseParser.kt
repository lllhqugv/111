package com.example.myapplication.ai

import com.example.myapplication.parse.ParsedTask
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 解析 DeepSeek 返回的 json 内容（形如 `{"content": "...", "dueAt": "..."}`）。
 * 纯函数，便于单元测试；模型偶尔会把“无”写成字符串 "null"、空串或 JSON null，这里统一按无处理。
 */
object DeepSeekResponseParser {

    fun parse(modelContent: String, zone: ZoneId = ZoneId.systemDefault()): ParsedTask? {
        val obj = runCatching { JSONObject(modelContent) }.getOrNull() ?: return null
        val content = obj.optNullableString("content") ?: return null
        val dueAt = obj.optNullableString("dueAt")?.let { parseInstant(it, zone) }
        return ParsedTask(content = content, dueAt = dueAt?.toEpochMilli())
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (isNull(key)) return null
        return optString(key, "").trim().takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun parseInstant(raw: String, zone: ZoneId): Instant? {
        runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()?.let { return it }
        runCatching { ZonedDateTime.parse(raw).toInstant() }.getOrNull()?.let { return it }
        runCatching { Instant.parse(raw) }.getOrNull()?.let { return it }
        runCatching { LocalDateTime.parse(raw).atZone(zone).toInstant() }.getOrNull()?.let { return it }
        return null
    }
}
