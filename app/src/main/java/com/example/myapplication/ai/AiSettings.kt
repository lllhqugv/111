package com.example.myapplication.ai

import android.content.Context

/** DeepSeek 接入配置，Key 仅保存在本机 SharedPreferences。 */
object AiSettings {

    private const val PREFS = "ai_prefs"
    private const val KEY_API_KEY = "deepseek_api_key"

    fun apiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "").orEmpty().trim()

    fun setApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_API_KEY, key).apply()
    }

    fun isConfigured(context: Context): Boolean = apiKey(context).isNotEmpty()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
