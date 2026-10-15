package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class TvPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("samsung_remote_prefs", Context.MODE_PRIVATE)

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

    var aiProvider: String
        get() = prefs.getString("pref_ai_provider", "gemini") ?: "gemini"
        set(value) = prefs.edit().putString("pref_ai_provider", value.trim()).apply()

    var customGeminiApiKey: String
        get() = prefs.getString("pref_custom_gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("pref_custom_gemini_api_key", value.trim()).apply()

    fun getEffectiveGeminiApiKey(): String {
        val custom = customGeminiApiKey.trim()
        if (custom.isNotBlank()) return custom
        return com.example.BuildConfig.GEMINI_API_KEY.trim()
    }

    var openRouterApiKey: String
        get() = prefs.getString("pref_openrouter_api_key", "") ?: ""
        set(value) = prefs.edit().putString("pref_openrouter_api_key", value.trim()).apply()

    var openRouterModel: String
        get() = prefs.getString("pref_openrouter_model", "google/gemini-2.0-flash-001") ?: "google/gemini-2.0-flash-001"
        set(value) = prefs.edit().putString("pref_openrouter_model", value.trim()).apply()

    var groqApiKey: String
        get() = prefs.getString("pref_groq_api_key", "") ?: ""
        set(value) = prefs.edit().putString("pref_groq_api_key", value.trim()).apply()

    var cerebrasApiKey: String
        get() = prefs.getString("pref_cerebras_api_key", "") ?: ""
        set(value) = prefs.edit().putString("pref_cerebras_api_key", value.trim()).apply()

    var mistralApiKey: String
        get() = prefs.getString("pref_mistral_api_key", "") ?: ""
        set(value) = prefs.edit().putString("pref_mistral_api_key", value.trim()).apply()

    var cloudflareApiKey: String
        get() = prefs.getString("pref_cloudflare_api_key", "") ?: ""
        set(value) = prefs.edit().putString("pref_cloudflare_api_key", value.trim()).apply()

    var cloudflareAccountId: String
        get() = prefs.getString("pref_cloudflare_account_id", "") ?: ""
        set(value) = prefs.edit().putString("pref_cloudflare_account_id", value.trim()).apply()

    var enableMultiProviderFallback: Boolean
        get() = prefs.getBoolean("pref_enable_multi_provider_fallback", true)
        set(value) = prefs.edit().putBoolean("pref_enable_multi_provider_fallback", value).apply()

    var speechLanguage: String
        get() = prefs.getString("pref_speech_language", "en-US") ?: "en-US"
        set(value) = prefs.edit().putString("pref_speech_language", value.trim()).apply()

    var aiSystemPrompt: String
        get() = prefs.getString("pref_ai_system_prompt", DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString("pref_ai_system_prompt", value).apply()

    fun clearToken() {
        prefs.edit().remove(KEY_TV_TOKEN).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TV_IP = "pref_tv_ip"
        private const val KEY_TV_PORT = "pref_tv_port"
        private const val KEY_TV_TOKEN = "pref_tv_token"
        private const val KEY_TV_NAME = "pref_tv_name"
        private const val KEY_TV_MODEL = "pref_tv_model"
        private const val KEY_TV_MAC = "pref_tv_mac"
        private const val KEY_LAST_CONNECTED = "pref_last_connected"

        const val DEFAULT_SYSTEM_PROMPT = """You are the AI assistant for a Samsung Smart TV remote (Model UA32T4410AKXXL).
You help control the TV and answer user questions.
When the user asks you to control the TV, change volume/channel, navigate, open apps, or search YouTube, return a strict JSON action object without markdown formatting.
If the user asks an ordinary question (like "What can you do?" or "Tell me a joke"), answer conversationally with intent "CHAT".

Schema for JSON actions:
{
  "intent": "PRESS_KEY | ADJUST_VOLUME | LAUNCH_APP | SEARCH_YOUTUBE | CHAT | CLARIFY | UNSUPPORTED",
  "keyName": "KEY_VOLUP | KEY_VOLDOWN | KEY_MUTE | KEY_HOME | KEY_RETURN | KEY_UP | KEY_DOWN | KEY_LEFT | KEY_RIGHT | KEY_ENTER | KEY_MENU | KEY_CHUP | KEY_CHDOWN | KEY_PLAY | KEY_PAUSE | KEY_STOP | KEY_REWIND | KEY_FF | KEY_EXIT | KEY_SETTINGS | KEY_SOURCE | KEY_INFO",
  "volumeDelta": integer,
  "appId": "111299001912 | 11101200001 | 3201512006785 | 3201606009684 | org.tizen.browser",
  "appName": "YouTube | Netflix | Prime Video | Spotify | Browser",
  "searchQuery": string,
  "responseText": "Short natural response to display to the user"
}"""
    }
}
