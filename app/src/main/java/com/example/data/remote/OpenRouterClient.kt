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
    val isCommand: Boolean,
    val providerUsed: String = "Google Gemini"
)

/**
 * Multi-Provider Ultra-Resilient AI Client for Samsung Smart TV Remote.
 * Supports:
 *   Tier 1: Google Gemini (gemini-2.0-flash, gemini-1.5-flash)
 *   Tier 2: GroqCloud (llama-3.3-70b-versatile, llama-3.1-8b-instant)
 *   Tier 3: Cerebras Cloud (llama3.1-8b, llama3.3-70b)
 *   Tier 4: OpenRouter (openrouter/free, qwen/qwen3.8-27b:free)
 *   Tier 5: Mistral AI (mistral-small-latest, open-mistral-7b)
 *   Tier 6: Cloudflare Workers AI (@cf/meta/llama-3.1-8b-instruct)
 */
class OpenRouterClient(
    private val preferences: TvPreferences,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "OpenRouterClient"

        // Base URLs
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val CEREBRAS_URL = "https://api.cerebras.ai/v1/chat/completions"
        private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions"

        val POPULAR_MODELS = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemini-2.0-flash-exp:free",
            "google/gemini-2.0-flash-thinking-exp:free",
            "qwen/qwen-2.5-72b-instruct:free",
            "mistralai/mistral-small-24b-instruct-2501:free",
            "google/gemini-2.0-flash-001",
            "meta-llama/llama-3.3-70b-instruct",
            "deepseek/deepseek-chat"
        )
    }

    /**
     * Executes completion with automatic fallback chain across all configured providers.
     */
    suspend fun sendChatCompletion(
        userPrompt: String,
        conversationHistory: List<OpenRouterMessage> = emptyList()
    ): Result<OpenRouterAssistantResult> = withContext(Dispatchers.IO) {
        val preferred = preferences.aiProvider.lowercase().trim()
        val fallbackEnabled = preferences.enableMultiProviderFallback

        // Build priority execution chain based on user preference and fallback
        val executionOrder = mutableListOf<String>()

        if (preferred.isNotBlank() && preferred != "auto" && preferred != "fallback") {
            executionOrder.add(preferred)
        }

        if (fallbackEnabled || executionOrder.isEmpty()) {
            val fullChain = listOf("gemini", "groq", "cerebras", "openrouter", "mistral", "cloudflare")
            fullChain.forEach { p ->
                if (!executionOrder.contains(p)) {
                    executionOrder.add(p)
                }
            }
        }

        val errors = mutableListOf<String>()

        for (provider in executionOrder) {
            when (provider) {
                "gemini" -> {
                    val key = preferences.getEffectiveGeminiApiKey()
                    if (key.isBlank()) continue
                    val res = sendGeminiCompletion(userPrompt, conversationHistory)
                    if (res.isSuccess) return@withContext res
                    val err = res.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "[WARN] Model gemini-2.0-flash on Google AI Studio failed ($err). Trying next target...")
                    errors.add("Google Gemini: $err")
                }
                "groq" -> {
                    val key = preferences.groqApiKey.trim()
                    if (key.isBlank()) continue
                    val res = sendOpenAiCompatibleCompletion(
                        baseUrl = GROQ_URL,
                        apiKey = key,
                        model = "llama-3.3-70b-versatile",
                        providerLabel = "GroqCloud",
                        userPrompt = userPrompt,
                        conversationHistory = conversationHistory
                    )
                    if (res.isSuccess) return@withContext res
                    val err = res.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "[WARN] Model llama-3.3-70b-versatile on GroqCloud failed ($err). Trying next target...")
                    errors.add("GroqCloud: $err")
                }
                "cerebras" -> {
                    val key = preferences.cerebrasApiKey.trim()
                    if (key.isBlank()) continue
                    val res = sendOpenAiCompatibleCompletion(
                        baseUrl = CEREBRAS_URL,
                        apiKey = key,
                        model = "llama3.1-8b",
                        providerLabel = "Cerebras Cloud",
                        userPrompt = userPrompt,
                        conversationHistory = conversationHistory
                    )
                    if (res.isSuccess) return@withContext res
                    val err = res.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "[WARN] Model llama3.1-8b on Cerebras Cloud failed ($err). Trying next target...")
                    errors.add("Cerebras Cloud: $err")
                }
                "openrouter" -> {
                    val key = preferences.openRouterApiKey.trim()
                    if (key.isBlank()) continue
                    val model = preferences.openRouterModel.ifBlank { "google/gemini-2.0-flash-001" }
                    val res = sendOpenAiCompatibleCompletion(
                        baseUrl = OPENROUTER_URL,
                        apiKey = key,
                        model = model,
                        providerLabel = "OpenRouter",
                        userPrompt = userPrompt,
                        conversationHistory = conversationHistory,
                        extraHeaders = mapOf(
                            "HTTP-Referer" to "https://github.com/soumyarana77789/samsung-ai-remote",
                            "X-Title" to "Samsung AI Remote"
                        )
                    )
                    if (res.isSuccess) return@withContext res
                    val err = res.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "[WARN] Model $model on OpenRouter failed ($err). Trying next target...")
                    errors.add("OpenRouter: $err")
                }
                "mistral" -> {
                    val key = preferences.mistralApiKey.trim()
                    if (key.isBlank()) continue
                    val res = sendOpenAiCompatibleCompletion(
                        baseUrl = MISTRAL_URL,
                        apiKey = key,
                        model = "mistral-small-latest",
                        providerLabel = "Mistral AI",
                        userPrompt = userPrompt,
                        conversationHistory = conversationHistory
                    )
                    if (res.isSuccess) return@withContext res
                    val err = res.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "[WARN] Model mistral-small-latest on Mistral AI failed ($err). Trying next target...")
                    errors.add("Mistral AI: $err")
                }
                "cloudflare" -> {
                    val key = preferences.cloudflareApiKey.trim()
                    val accountId = preferences.cloudflareAccountId.trim()
                    if (key.isBlank() || accountId.isBlank()) continue
                    val cfUrl = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/v1/chat/completions"
                    val res = sendOpenAiCompatibleCompletion(
                        baseUrl = cfUrl,
                        apiKey = key,
                        model = "@cf/meta/llama-3.1-8b-instruct",
                        providerLabel = "Cloudflare Workers AI",
                        userPrompt = userPrompt,
                        conversationHistory = conversationHistory
                    )
                    if (res.isSuccess) return@withContext res
                    val err = res.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "[WARN] Model @cf/meta/llama-3.1-8b-instruct on Cloudflare Workers AI failed ($err). Trying next target...")
                    errors.add("Cloudflare AI: $err")
                }
            }
        }

        if (errors.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No AI API keys configured. Please add your free Google Gemini, Groq, or OpenRouter API key in the 'APIs' tab.")
            )
        }

        Result.failure(
            RuntimeException("All configured LLM providers failed:\n" + errors.joinToString("\n"))
        )
    }

    private suspend fun sendGeminiCompletion(
        userPrompt: String,
        conversationHistory: List<OpenRouterMessage>
    ): Result<OpenRouterAssistantResult> {
        val apiKey = preferences.getEffectiveGeminiApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Gemini API Key is not configured."))
        }

        val systemPrompt = preferences.aiSystemPrompt.ifBlank { TvPreferences.DEFAULT_SYSTEM_PROMPT }

        try {
            val contentsArray = JSONArray().apply {
                conversationHistory.takeLast(6).forEach { msg ->
                    put(JSONObject().apply {
                        put("role", if (msg.role == "user") "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                })
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val url = "$GEMINI_BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = when (code) {
                    400 -> "Invalid Gemini request ($code): ${parseErrorMessage(responseBody)}"
                    401, 403 -> "Gemini API key is invalid or lacks permissions ($code)."
                    429 -> "Gemini rate limit reached (429)."
                    else -> "Gemini API Error ($code): ${parseErrorMessage(responseBody)}"
                }
                return Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val rawContent = parts?.optJSONObject(0)?.optString("text", "")?.trim() ?: ""

            if (rawContent.isBlank()) {
                return Result.failure(Exception("Received empty response from Gemini."))
            }

            val parseResult = parseCommandOrChat(rawContent, "Google Gemini")
            return Result.success(parseResult)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun sendOpenAiCompatibleCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        providerLabel: String,
        userPrompt: String,
        conversationHistory: List<OpenRouterMessage>,
        extraHeaders: Map<String, String> = emptyMap()
    ): Result<OpenRouterAssistantResult> {
        val systemPrompt = preferences.aiSystemPrompt.ifBlank { TvPreferences.DEFAULT_SYSTEM_PROMPT }

        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                conversationHistory.takeLast(6).forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }

            val requestBodyJson = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("temperature", 0.2)
            }

            val reqBuilder = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))

            extraHeaders.forEach { (k, v) ->
                reqBuilder.addHeader(k, v)
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val code = response.code
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = when (code) {
                    401 -> "$providerLabel Invalid API Key (401)."
                    402 -> "$providerLabel Insufficient credits (402)."
                    429 -> "$providerLabel Rate limit exceeded (429)."
                    404 -> "$providerLabel Model '$model' not found (404)."
                    500, 502, 503 -> "$providerLabel service temporarily unavailable ($code)."
                    else -> "$providerLabel Error ($code): ${parseErrorMessage(responseBody)}"
                }
                return Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val messageObj = firstChoice?.optJSONObject("message")
            val rawContent = messageObj?.optString("content", "")?.trim() ?: ""

            if (rawContent.isBlank()) {
                return Result.failure(Exception("Received empty response from $providerLabel."))
            }

            val parseResult = parseCommandOrChat(rawContent, providerLabel)
            return Result.success(parseResult)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun parseCommandOrChat(raw: String, providerLabel: String): OpenRouterAssistantResult {
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
                    isCommand = false,
                    providerUsed = providerLabel
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
                isCommand = true,
                providerUsed = providerLabel
            )
        } catch (_: Exception) {
            return OpenRouterAssistantResult(
                command = null,
                textResponse = raw,
                isCommand = false,
                providerUsed = providerLabel
            )
        }
    }

    suspend fun testProvider(provider: String): Result<String> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val p = provider.lowercase().trim()

        when (p) {
            "gemini" -> {
                val key = preferences.getEffectiveGeminiApiKey()
                if (key.isBlank()) return@withContext Result.failure(Exception("Gemini API key is not set."))
                val r = sendGeminiCompletion("Say 'OK' if working.", emptyList())
                r.map { "Google Gemini is active! Latency: ${System.currentTimeMillis() - start}ms" }
            }
            "groq" -> {
                val key = preferences.groqApiKey.trim()
                if (key.isBlank()) return@withContext Result.failure(Exception("Groq API key is not set."))
                val r = sendOpenAiCompatibleCompletion(GROQ_URL, key, "llama-3.1-8b-instant", "GroqCloud", "Say 'OK' if working.", emptyList())
                r.map { "GroqCloud is active! (LPU inference, latency: ${System.currentTimeMillis() - start}ms)" }
            }
            "cerebras" -> {
                val key = preferences.cerebrasApiKey.trim()
                if (key.isBlank()) return@withContext Result.failure(Exception("Cerebras API key is not set."))
                val r = sendOpenAiCompatibleCompletion(CEREBRAS_URL, key, "llama3.1-8b", "Cerebras Cloud", "Say 'OK' if working.", emptyList())
                r.map { "Cerebras Cloud is active! (Wafer scale, latency: ${System.currentTimeMillis() - start}ms)" }
            }
            "openrouter" -> {
                val key = preferences.openRouterApiKey.trim()
                if (key.isBlank()) return@withContext Result.failure(Exception("OpenRouter API key is not set."))
                val model = preferences.openRouterModel.ifBlank { "google/gemini-2.0-flash-001" }
                val r = sendOpenAiCompatibleCompletion(
                    OPENROUTER_URL, key, model, "OpenRouter", "Say 'OK' if working.", emptyList(),
                    mapOf("HTTP-Referer" to "https://github.com/soumyarana77789/samsung-ai-remote")
                )
                r.map { "OpenRouter is active! (Model: $model, latency: ${System.currentTimeMillis() - start}ms)" }
            }
            "mistral" -> {
                val key = preferences.mistralApiKey.trim()
                if (key.isBlank()) return@withContext Result.failure(Exception("Mistral API key is not set."))
                val r = sendOpenAiCompatibleCompletion(MISTRAL_URL, key, "mistral-small-latest", "Mistral AI", "Say 'OK' if working.", emptyList())
                r.map { "Mistral AI is active! Latency: ${System.currentTimeMillis() - start}ms" }
            }
            "cloudflare" -> {
                val key = preferences.cloudflareApiKey.trim()
                val acc = preferences.cloudflareAccountId.trim()
                if (key.isBlank() || acc.isBlank()) return@withContext Result.failure(Exception("Cloudflare API Key and Account ID must both be set."))
                val cfUrl = "https://api.cloudflare.com/client/v4/accounts/$acc/ai/v1/chat/completions"
                val r = sendOpenAiCompatibleCompletion(cfUrl, key, "@cf/meta/llama-3.1-8b-instruct", "Cloudflare AI", "Say 'OK' if working.", emptyList())
                r.map { "Cloudflare Workers AI is active! Latency: ${System.currentTimeMillis() - start}ms" }
            }
            else -> testConnection()
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val r = sendChatCompletion("Say 'OK' if working.", emptyList())
        r.map { "AI Connection Successful via ${it.providerUsed}! Response: ${it.textResponse.take(60)}" }
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
