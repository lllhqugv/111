package com.example.myapplication.ai

import com.example.myapplication.parse.ParsedTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** DeepSeek 调用失败；message 为可直接展示给用户的中文说明。 */
class DeepSeekException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 调用 DeepSeek（OpenAI 兼容接口）从 OCR 文本中提取任务内容与截止时间。
 * 需要联网；任何失败都会抛出 [DeepSeekException]。
 */
object DeepSeekClient {

    private const val ENDPOINT = "https://api.deepseek.com/chat/completions"
    private const val MODEL = "deepseek-chat"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MAX_TOKENS = 512

    private val SYSTEM_PROMPT = """
        你是任务提取助手。用户会给你一段从手机屏幕截图中 OCR 出来的文本，以及当前时间。
        请从中提取最有可能的一个待办任务，并以 json 输出，格式示例：
        {"content": "提交周报", "dueAt": "2026-10-15T18:00:00+08:00"}
        字段说明：content 为任务名（简短标题、去掉时间词，例如"发周报"）；dueAt 为截止时间的 ISO8601 字符串（含时区偏移，如上例），
        若文本中没有明确或可推断的时间则为 null。若文本中完全没有可执行的任务，content 返回 null。
        只输出 json，不要输出任何解释。
    """.trimIndent()

    suspend fun parseTask(
        apiKey: String,
        ocrText: String,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): ParsedTask? = withContext(Dispatchers.IO) {
        val responseBody = post(apiKey, buildRequestBody(ocrText, now))
        DeepSeekResponseParser.parse(extractMessageContent(responseBody), now.zone)
    }

    private fun buildRequestBody(ocrText: String, now: ZonedDateTime): String {
        val stamp = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val userMessage = "当前时间：$stamp（时区 ${now.zone.id}）\n文本：\n$ocrText"
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            .put(JSONObject().put("role", "user").put("content", userMessage))
        return JSONObject()
            .put("model", MODEL)
            .put("messages", messages)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("max_tokens", MAX_TOKENS)
            .put("stream", false)
            .toString()
    }

    private fun post(apiKey: String, requestBody: String): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                throw DeepSeekException("接口返回 HTTP $code：${summarizeError(text)}")
            }
            return text
        } catch (e: DeepSeekException) {
            throw e
        } catch (t: Throwable) {
            throw DeepSeekException("网络请求失败：${t.message ?: t::class.java.simpleName}", t)
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractMessageContent(responseBody: String): String {
        val content = runCatching {
            JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "")
        }.getOrNull()
        if (content.isNullOrBlank()) throw DeepSeekException("AI 返回内容为空")
        return content
    }

    private fun summarizeError(body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return message?.takeIf { it.isNotBlank() } ?: body.take(200)
    }
}
