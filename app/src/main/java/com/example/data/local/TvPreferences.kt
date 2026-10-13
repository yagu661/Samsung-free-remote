package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class TvPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("samsung_remote_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TV_IP = "pref_tv_ip"
        private const val KEY_TV_PORT = "pref_tv_port"
        private const val KEY_TV_TOKEN = "pref_tv_token"
        private const val KEY_TV_NAME = "pref_tv_name"
        private const val KEY_TV_MODEL = "pref_tv_model"
        private const val KEY_TV_MAC = "pref_tv_mac"
        private const val KEY_LAST_CONNECTED = "pref_last_connected"
    }

    var tvIp: String
        get() = prefs.getString(KEY_TV_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TV_IP, value.trim()).apply()

    var tvPort: Int
        get() = prefs.getInt(KEY_TV_PORT, 8002)
        set(value) = prefs.edit().putInt(KEY_TV_PORT, value).apply()

    var tvToken: String?
        get() = prefs.getString(KEY_TV_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TV_TOKEN, value).apply()

    var tvName: String
        get() = prefs.getString(KEY_TV_NAME, "Samsung UA32T4410") ?: "Samsung UA32T4410"
        set(value) = prefs.edit().putString(KEY_TV_NAME, value).apply()

    var tvModel: String
        get() = prefs.getString(KEY_TV_MODEL, "UA32T4410AKXXL") ?: "UA32T4410AKXXL"
        set(value) = prefs.edit().putString(KEY_TV_MODEL, value).apply()

    var tvMac: String?
        get() = prefs.getString(KEY_TV_MAC, null)
        set(value) = prefs.edit().putString(KEY_TV_MAC, value).apply()

    var autoExecuteVoice: Boolean
        get() = prefs.getBoolean("pref_auto_execute_voice", false)
        set(value) = prefs.edit().putBoolean("pref_auto_execute_voice", value).apply()

    var useGeminiAi: Boolean
        get() = prefs.getBoolean("pref_use_gemini_ai", true)
        set(value) = prefs.edit().putBoolean("pref_use_gemini_ai", value).apply()

    fun clearToken() {
        prefs.edit().remove(KEY_TV_TOKEN).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
