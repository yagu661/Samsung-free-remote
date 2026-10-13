package com.example.data.remote

import android.util.Log
import com.example.data.local.TvPreferences
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

data class OpenRouterMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class OpenRouterAssistantResult(
    val command: TvCommandAiResult?,
    val textResponse: String,
    val isCommand: Boolean
)

class OpenRouterClient(
    private val preferences: TvPreferences,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

        val POPULAR_MODELS = listOf(
            "google/gemini-2.0-flash-001",
            "meta-llama/llama-3.3-70b-instruct",
            "deepseek/deepseek-chat",
            "mistralai/mistral-small-24b-instruct-2501",
            "anthropic/claude-3.5-haiku"
        )
    }

    suspend fun sendChatCompletion(
        userPrompt: String,
        conversationHistory: List<OpenRouterMessage> = emptyList()
    ): Result<OpenRouterAssistantResult> = withContext(Dispatchers.IO) {
        val apiKey = preferences.openRouterApiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("OpenRouter API Key is not configured. Please enter your API key in Settings > OpenRouter Configuration.")
            )
        }

        val modelId = preferences.openRouterModel.ifBlank { "google/gemini-2.0-flash-001" }
        val systemPrompt = preferences.aiSystemPrompt.ifBlank { TvPreferences.DEFAULT_SYSTEM_PROMPT }

        try {
            val messagesArray = JSONArray().apply {
                // System Instruction
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                // Recent conversation history (keep last 6 turns to manage context and tokens)
                conversationHistory.takeLast(6).forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }

                // Current User Input
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }

            val requestBodyJson = JSONObject().apply {
                put("model", modelId)
                put("messages", messagesArray)
                put("temperature", 0.2)
            }

            val request = Request.Builder()
                .url(OPENROUTER_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://github.com/soumyarana77789/samsung-ai-remote")
                .addHeader("X-Title", "Samsung AI Remote")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = when (code) {
                    401 -> "Invalid OpenRouter API Key (401). Please check your key in Settings."
                    402 -> "Insufficient OpenRouter credits (402). Please add credits to your account at openrouter.ai/credits."
                    429 -> "OpenRouter Rate Limit Exceeded (429). Please wait a few seconds and tap Retry."
                    404 -> "Model '$modelId' was not found on OpenRouter (404). Please select a different model in Settings."
                    503 -> "OpenRouter service or model is temporarily unavailable (503). Try again or switch models."
                    else -> "OpenRouter Error ($code): ${parseErrorMessage(responseBody)}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val messageObj = firstChoice?.optJSONObject("message")
            val rawContent = messageObj?.optString("content", "")?.trim() ?: ""

            if (rawContent.isBlank()) {
                return@withContext Result.failure(Exception("Received empty response from OpenRouter."))
            }

            Log.d("OpenRouterClient", "Model response: $rawContent")

            // Attempt to parse structured TV action
            val parseResult = parseCommandOrChat(rawContent)
            Result.success(parseResult)
        } catch (e: Exception) {
            Log.e("OpenRouterClient", "Request failed", e)
            Result.failure(e)
        }
    }

    private fun parseCommandOrChat(raw: String): OpenRouterAssistantResult {
        // Strip markdown code fences if present (```json ... ``` or ``` ...)
        val cleanJson = if (raw.contains("```")) {
            raw.substringAfter("```json")
                .substringAfter("```")
                .substringBefore("```")
                .trim()
        } else {
            raw.trim()
        }

        try {
            val json = JSONObject(cleanJson)
            val intent = json.optString("intent", "CHAT").uppercase()
            val responseText = json.optString("responseText", json.optString("explanation", ""))

            if (intent == "CHAT" || intent.isBlank()) {
                val finalMsg = if (responseText.isNotBlank()) responseText else raw
                return OpenRouterAssistantResult(
                    command = null,
                    textResponse = finalMsg,
                    isCommand = false
                )
            }

            val command = TvCommandAiResult(
                intent = intent,
                keyName = if (json.has("keyName") && !json.isNull("keyName")) json.getString("keyName").takeIf { it != "null" && it.isNotBlank() } else null,
                volumeDelta = if (json.has("volumeDelta") && !json.isNull("volumeDelta")) json.getInt("volumeDelta") else null,
                appId = if (json.has("appId") && !json.isNull("appId")) json.getString("appId").takeIf { it != "null" && it.isNotBlank() } else null,
                appName = if (json.has("appName") && !json.isNull("appName")) json.getString("appName").takeIf { it != "null" && it.isNotBlank() } else null,
                searchQuery = if (json.has("searchQuery") && !json.isNull("searchQuery")) json.getString("searchQuery").takeIf { it != "null" && it.isNotBlank() } else null,
                explanation = if (responseText.isNotBlank()) responseText else "Action: $intent",
                confidence = json.optDouble("confidence", 1.0).toFloat()
            )

            return OpenRouterAssistantResult(
                command = command,
                textResponse = if (responseText.isNotBlank()) responseText else "Executing $intent command...",
                isCommand = true
            )
        } catch (_: Exception) {
            // Not a JSON action - treat as conversational chat answer
            return OpenRouterAssistantResult(
                command = null,
                textResponse = raw,
                isCommand = false
            )
        }
    }

    private fun parseErrorMessage(rawJson: String): String {
        return try {
            val json = JSONObject(rawJson)
            val errObj = json.optJSONObject("error")
            errObj?.optString("message", rawJson) ?: rawJson
        } catch (_: Exception) {
            rawJson
        }
    }
}
