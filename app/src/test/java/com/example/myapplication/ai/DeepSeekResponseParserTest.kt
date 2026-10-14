package com.example.myapplication.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DeepSeekResponseParserTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun dueAt(json: String): LocalDateTime? =
        DeepSeekResponseParser.parse(json, zone)?.dueAt
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }

    @Test
    fun `parses content and offset due time`() {
        val json = """{"content": "提交周报", "dueAt": "2026-10-15T18:00:00+08:00"}"""
        val task = DeepSeekResponseParser.parse(json, zone)!!
        assertEquals("提交周报", task.content)
        assertEquals(LocalDateTime.of(2026, 10, 15, 18, 0), dueAt(json))
    }

    @Test
    fun `null due time yields null`() {
        val task = DeepSeekResponseParser.parse("""{"content": "买牛奶", "dueAt": null}""", zone)!!
        assertEquals("买牛奶", task.content)
        assertNull(task.dueAt)
    }

    @Test
    fun `string null due time is treated as none`() {
        val task = DeepSeekResponseParser.parse("""{"content": "买牛奶", "dueAt": "null"}""", zone)!!
        assertNull(task.dueAt)
    }

    @Test
    fun `local date time without offset uses given zone`() {
        assertEquals(
            LocalDateTime.of(2026, 10, 15, 9, 0),
            dueAt("""{"content": "开会", "dueAt": "2026-10-15T09:00:00"}"""),
        )
    }

    @Test
    fun `missing content yields null`() {
        assertNull(
            DeepSeekResponseParser.parse("""{"content": null, "dueAt": "2026-10-15T09:00:00"}""", zone),
        )
    }

    @Test
    fun `malformed json yields null`() {
        assertNull(DeepSeekResponseParser.parse("not json", zone))
    }
}
