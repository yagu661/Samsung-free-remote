package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.TvCommandAiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val systemPrompt = """
        You are the voice command interpreter for a Samsung Smart TV remote (Model UA32T4410AKXXL).
        Convert user speech/requests into structured JSON.
        You MUST NEVER invent unsupported actions.
        Output ONLY valid JSON without markdown fences matching this schema:
        {
          "intent": "PRESS_KEY | ADJUST_VOLUME | LAUNCH_APP | SEARCH_YOUTUBE | CLARIFY | UNSUPPORTED",
          "keyName": "KEY_POWER | KEY_VOLUP | KEY_VOLDOWN | KEY_MUTE | KEY_HOME | KEY_RETURN | KEY_UP | KEY_DOWN | KEY_LEFT | KEY_RIGHT | KEY_ENTER | KEY_MENU | KEY_CHUP | KEY_CHDOWN | KEY_PLAY | KEY_PAUSE | KEY_STOP | KEY_SETTINGS | KEY_SOURCE | KEY_INFO",
          "volumeDelta": integer,
          "appId": "111299001912 | 11101200001 | 3201512006785 | 3201606009684 | org.tizen.browser",
          "appName": "YouTube | Netflix | Prime Video | Spotify | Browser",
          "searchQuery": string,
          "explanation": string,
          "confidence": float
        }
        
        Strict Guidelines:
        - "Volume up a little" -> intent: "ADJUST_VOLUME", volumeDelta: 2, explanation: "Turn volume up by 2 steps"
        - "Turn it down a lot" -> intent: "ADJUST_VOLUME", volumeDelta: -5, explanation: "Turn volume down by 5 steps"
        - "Mute the TV" -> intent: "PRESS_KEY", keyName: "KEY_MUTE", explanation: "Mute audio"
        - "Go home" or "Dashboard" -> intent: "PRESS_KEY", keyName: "KEY_HOME", explanation: "Open Smart Hub Home"
        - "Search YouTube for funny cat videos" -> intent: "SEARCH_YOUTUBE", searchQuery: "funny cat videos", explanation: "Search YouTube for funny cat videos"
        - "Open YouTube" -> intent: "LAUNCH_APP", appId: "111299001912", appName: "YouTube", explanation: "Launch YouTube app"
        - "Open Netflix" -> intent: "LAUNCH_APP", appId: "11101200001", appName: "Netflix", explanation: "Launch Netflix app"
        - If ambiguous (e.g. "change it"), set intent to "CLARIFY" with explanation asking what to change.
        - If impossible/unsupported (e.g. "turn the AC on", "make coffee"), set intent to "UNSUPPORTED" with explanation.
    """.trimIndent()

    suspend fun interpretVoiceCommand(userInput: String): Result<TvCommandAiResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured in Secrets panel / .env")
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userInput)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemPrompt)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                return@withContext Result.failure(Exception("Gemini API Error ${response.code}: $errBody"))
            }

            val responseText = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseText)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

            Log.d("GeminiClient", "Gemini output: $rawOutput")

            // Parse structured output
            val outputJson = JSONObject(rawOutput.trim())
            val result = TvCommandAiResult(
                intent = outputJson.optString("intent", "UNSUPPORTED"),
                keyName = if (outputJson.has("keyName") && !outputJson.isNull("keyName")) outputJson.getString("keyName").takeIf { it != "null" && it.isNotBlank() } else null,
                volumeDelta = if (outputJson.has("volumeDelta") && !outputJson.isNull("volumeDelta")) outputJson.getInt("volumeDelta") else null,
                appId = if (outputJson.has("appId") && !outputJson.isNull("appId")) outputJson.getString("appId").takeIf { it != "null" && it.isNotBlank() } else null,
                appName = if (outputJson.has("appName") && !outputJson.isNull("appName")) outputJson.getString("appName").takeIf { it != "null" && it.isNotBlank() } else null,
                searchQuery = if (outputJson.has("searchQuery") && !outputJson.isNull("searchQuery")) outputJson.getString("searchQuery").takeIf { it != "null" && it.isNotBlank() } else null,
                explanation = outputJson.optString("explanation", "Interpreted command"),
                confidence = outputJson.optDouble("confidence", 1.0).toFloat()
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e("GeminiClient", "Failed to interpret command", e)
            Result.failure(e)
        }
    }
}
