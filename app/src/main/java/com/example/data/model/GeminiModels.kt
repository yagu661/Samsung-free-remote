package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Float? = 0.2f
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

/**
 * Structured output schema that Gemini returns for TV commands
 */
@JsonClass(generateAdapter = true)
data class TvCommandAiResult(
    val intent: String, // e.g. "PRESS_KEY", "ADJUST_VOLUME", "LAUNCH_APP", "SEARCH_YOUTUBE", "CLARIFY", "UNSUPPORTED"
    val keyName: String? = null, // e.g. "KEY_VOLUP", "KEY_HOME", etc.
    val volumeDelta: Int? = null, // e.g. 2 or -3
    val appId: String? = null, // e.g. "111299001912"
    val appName: String? = null, // e.g. "YouTube"
    val searchQuery: String? = null, // e.g. "funny cat videos"
    val explanation: String = "",
    val confidence: Float = 1.0f
)
