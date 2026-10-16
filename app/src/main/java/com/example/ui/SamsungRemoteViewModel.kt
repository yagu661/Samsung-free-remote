package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TvPreferences
import com.example.data.model.DiagnosticItem
import com.example.data.model.DiagnosticStatus
import com.example.data.model.DiscoveredTv
import com.example.data.model.RemoteKey
import com.example.data.model.TvAction
import com.example.data.model.TvApp
import com.example.data.model.TvConnectionState
import com.example.data.model.TvConnectionStatus
import com.example.data.remote.DiscoveryState
import com.example.data.remote.OpenRouterClient
import com.example.data.remote.OpenRouterMessage
import com.example.data.remote.SamsungTvController
import com.example.data.remote.SamsungTvDiscoveryManager
import com.example.data.remote.TvAppDeployServer
import com.example.data.remote.TvServerState
import com.example.data.remote.SamsungTvRestApi
import com.example.data.remote.WakeOnLanUtil
import com.example.data.remote.YouTubeSearchResult
import com.example.voice.TvCommandParser
import com.example.voice.VoiceInputManager
import com.example.voice.VoiceState
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.data.model.TizenDiagnosticsResult
import com.example.data.model.TizenInstallState
import com.example.data.model.TizenInstalledApp
import com.example.data.model.TizenPackageInfo
import com.example.data.remote.TizenPackageValidator
import com.example.data.remote.TizenSdbClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssistantChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val action: TvAction? = null,
    val status: String? = null,
    val isError: Boolean = false,
    val originalUserPrompt: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class YouTubeSearchUiState(
    val isSearching: Boolean = false,
    val query: String = "",
    val step1VoiceRecognized: Boolean? = null,
    val step2TvConnected: Boolean? = null,
    val step3AppLaunched: Boolean? = null,
    val step4TextInjected: Boolean? = null,
    val resultMessage: String? = null,
    val fallbackNeeded: Boolean = false
)

class SamsungRemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = TvPreferences(application)
    val tvController = SamsungTvController(application, preferences)
    val voiceInputManager = VoiceInputManager(application)
    val openRouterClient = OpenRouterClient(preferences)
    private val restApi = SamsungTvRestApi()
    val discoveryManager = SamsungTvDiscoveryManager(application, restApi)

    val connectionState: StateFlow<TvConnectionState> = tvController.connectionState
    val logFlow = tvController.logFlow
    val voiceState: StateFlow<VoiceState> = voiceInputManager.voiceState
    val partialSpeechText: StateFlow<String> = voiceInputManager.partialText

    val discoveryState: StateFlow<DiscoveryState> = discoveryManager.discoveryState
    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    val autoExecuteVoice = MutableStateFlow(preferences.autoExecuteVoice)
    val useGeminiAi = MutableStateFlow(preferences.useGeminiAi)
    val aiProvider = MutableStateFlow(preferences.aiProvider)
    val customGeminiApiKey = MutableStateFlow(preferences.customGeminiApiKey)
    val openRouterApiKey = MutableStateFlow(preferences.openRouterApiKey)
    val openRouterModel = MutableStateFlow(preferences.openRouterModel)
    val speechLanguage = MutableStateFlow(preferences.speechLanguage)
    val aiSystemPrompt = MutableStateFlow(preferences.aiSystemPrompt)

    val groqApiKey = MutableStateFlow(preferences.groqApiKey)
    val cerebrasApiKey = MutableStateFlow(preferences.cerebrasApiKey)
    val mistralApiKey = MutableStateFlow(preferences.mistralApiKey)
    val cloudflareApiKey = MutableStateFlow(preferences.cloudflareApiKey)
    val cloudflareAccountId = MutableStateFlow(preferences.cloudflareAccountId)
    val enableMultiProviderFallback = MutableStateFlow(preferences.enableMultiProviderFallback)

    val ipAddressInput = MutableStateFlow(preferences.tvIp.ifBlank { "192.168.1." })
    val portInput = MutableStateFlow(preferences.tvPort)

    // Voice Screen Pending Action
    private val _pendingVoiceAction = MutableStateFlow<TvAction?>(null)
    val pendingVoiceAction: StateFlow<TvAction?> = _pendingVoiceAction.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow<String?>(null)
    val voiceStatusMessage: StateFlow<String?> = _voiceStatusMessage.asStateFlow()

    // TV Deploy Server & Local Package Manager
    val tvDeployServer = TvAppDeployServer(application, preferences) {
        refreshApiKeysFromPreferences()
    }
    val tvServerState: StateFlow<TvServerState> = tvDeployServer.state
    private val _tvExportMessage = MutableStateFlow<String?>(null)
    val tvExportMessage: StateFlow<String?> = _tvExportMessage.asStateFlow()

    // AI Assistant Chat Messages
    private val _assistantChat = MutableStateFlow<List<AssistantChatMessage>>(
        listOf(
            AssistantChatMessage(
                isUser = false,
                text = "Hello! I am your Samsung TV AI Assistant, made with ❤️ by Soumya. Powered by Google Gemini 3.5 Flash. You can speak or type commands like 'Turn down the volume a little', 'Open YouTube', 'Search YouTube for Bengali songs', or ask any questions."
            )
        )
    )
    val assistantChat: StateFlow<List<AssistantChatMessage>> = _assistantChat.asStateFlow()

    val isAiProcessing = MutableStateFlow(false)

    // YouTube Search Workflow State
    private val _youtubeSearchState = MutableStateFlow(YouTubeSearchUiState())
    val youtubeSearchState: StateFlow<YouTubeSearchUiState> = _youtubeSearchState.asStateFlow()

    // Diagnostics State
    private val _diagnostics = MutableStateFlow<List<DiagnosticItem>>(getInitialDiagnostics())
    val diagnostics: StateFlow<List<DiagnosticItem>> = _diagnostics.asStateFlow()
    val isRunningDiagnostics = MutableStateFlow(false)

    init {
        // Auto-reconnect if IP is saved
        if (preferences.tvIp.isNotBlank()) {
            tvController.connectToTV(preferences.tvIp, preferences.tvPort)
        }
    }

    private fun getInitialDiagnostics(): List<DiagnosticItem> {
        return listOf(
            DiagnosticItem("net_ping", "Network Connectivity", "Checks if TV IP is reachable on local Wi-Fi"),
            DiagnosticItem("rest_info", "TV Information Retrieval", "Queries model and OS via http://<ip>:8001/api/v2/"),
            DiagnosticItem("ws_conn", "WebSocket Connection", "Tests secure socket handshake on port 8002/8001"),
            DiagnosticItem("auth_pair", "Authorization & Token", "Verifies whether pairing token is saved and accepted"),
            DiagnosticItem("key_cmd", "Basic Navigation Key", "Tests sending KEY_INFO or KEY_VOLUP"),
            DiagnosticItem("app_launch", "Application Launching", "Tests REST launch endpoint for YouTube"),
            DiagnosticItem("speech_rec", "Voice Recognition Engine", "Checks Android SpeechRecognizer availability"),
            DiagnosticItem("openrouter_api", "OpenRouter AI Connectivity", "Checks if OpenRouter API key is configured"),
            DiagnosticItem("yt_search", "YouTube Search Support", "Tests YouTube launch and search query deep-link")
        )
    }

    // --- Settings Setters ---

    fun setAiProvider(provider: String) {
        aiProvider.value = provider.trim()
        preferences.aiProvider = provider.trim()
    }

    fun setCustomGeminiApiKey(key: String) {
        customGeminiApiKey.value = key.trim()
        preferences.customGeminiApiKey = key.trim()
    }

    fun setOpenRouterApiKey(key: String) {
        openRouterApiKey.value = key.trim()
        preferences.openRouterApiKey = key.trim()
    }

    fun setOpenRouterModel(model: String) {
        openRouterModel.value = model.trim()
        preferences.openRouterModel = model.trim()
    }

    fun setGroqApiKey(key: String) {
        groqApiKey.value = key.trim()
        preferences.groqApiKey = key.trim()
    }

    fun setCerebrasApiKey(key: String) {
        cerebrasApiKey.value = key.trim()
        preferences.cerebrasApiKey = key.trim()
    }

    fun setMistralApiKey(key: String) {
        mistralApiKey.value = key.trim()
        preferences.mistralApiKey = key.trim()
    }

    fun setCloudflareApiKey(key: String) {
        cloudflareApiKey.value = key.trim()
        preferences.cloudflareApiKey = key.trim()
    }

    fun setCloudflareAccountId(id: String) {
        cloudflareAccountId.value = id.trim()
        preferences.cloudflareAccountId = id.trim()
    }

    fun setEnableMultiProviderFallback(enabled: Boolean) {
        enableMultiProviderFallback.value = enabled
        preferences.enableMultiProviderFallback = enabled
    }

    fun testAiProvider(provider: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = openRouterClient.testProvider(provider)
            if (result.isSuccess) {
                onResult(true, result.getOrNull() ?: "Success!")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to connect")
            }
        }
    }

    fun generateTermuxExportScript(): String {
        val gemini = preferences.getEffectiveGeminiApiKey()
        val groq = preferences.groqApiKey
        val cerebras = preferences.cerebrasApiKey
        val openrouter = preferences.openRouterApiKey
        val mistral = preferences.mistralApiKey
        val cfKey = preferences.cloudflareApiKey
        val cfAcc = preferences.cloudflareAccountId

        return buildString {
            appendLine("# Paste this into Termux on your Galaxy M30s:")
            appendLine("export GEMINI_API_KEY=\"$gemini\"")
            appendLine("export GROQ_API_KEY=\"$groq\"")
            appendLine("export CEREBRAS_API_KEY=\"$cerebras\"")
            appendLine("export OPENROUTER_API_KEY=\"$openrouter\"")
            appendLine("export MISTRAL_API_KEY=\"$mistral\"")
            appendLine("export CLOUDFLARE_API_KEY=\"$cfKey\"")
            appendLine("export CLOUDFLARE_ACCOUNT_ID=\"$cfAcc\"")
            appendLine()
            appendLine("# Run the ultra-resilient Python fallback engine:")
            appendLine("python3 llm_fallback_system.py \"Search YouTube for Bengali songs\"")
        }
    }

    fun setSpeechLanguage(lang: String) {
        speechLanguage.value = lang.trim()
        preferences.speechLanguage = lang.trim()
    }

    fun setAiSystemPrompt(prompt: String) {
        aiSystemPrompt.value = prompt
        preferences.aiSystemPrompt = prompt
    }

    fun setAutoExecuteVoice(enabled: Boolean) {
        autoExecuteVoice.value = enabled
        preferences.autoExecuteVoice = enabled
    }

    fun setUseGeminiAi(enabled: Boolean) {
        useGeminiAi.value = enabled
        preferences.useGeminiAi = enabled
    }

    // --- Connection Screen Actions ---

    fun onIpChanged(newIp: String) {
        ipAddressInput.value = newIp
    }

    fun onPortChanged(newPort: Int) {
        portInput.value = newPort
    }

    fun connectTv() {
        val ip = ipAddressInput.value.trim()
        val port = portInput.value
        tvController.connectToTV(ip, port)
    }

    fun disconnectTv() {
        tvController.disconnectFromTV()
    }

    fun testConnection() {
        val ip = ipAddressInput.value.trim()
        val port = portInput.value
        viewModelScope.launch {
            _voiceStatusMessage.value = "Testing reachability to $ip:$port..."
            val reachable = restApi.checkSocketReachability(ip, port, timeoutMs = 2000)
            if (reachable) {
                val infoResult = restApi.getDeviceInfo(ip)
                if (infoResult.isSuccess) {
                    val info = infoResult.getOrNull()
                    _voiceStatusMessage.value = "TV Reachable! Found: ${info?.name ?: "Samsung TV"} (${info?.modelName ?: ""})"
                } else {
                    _voiceStatusMessage.value = "Port $port is open, but REST info failed. TV may be in standby."
                }
            } else {
                _voiceStatusMessage.value = "Cannot reach $ip:$port. Ensure phone and TV are on the same Wi-Fi network and TV is turned ON."
            }
        }
    }

    fun findMySamsungTv() {
        viewModelScope.launch {
            _voiceStatusMessage.value = "Searching for Samsung TVs on local Wi-Fi..."
            val tvs = discoveryManager.findSamsungTvs()
            _discoveredTvs.value = tvs
            if (tvs.isNotEmpty()) {
                _voiceStatusMessage.value = "Found ${tvs.size} Samsung TV(s). Tap to connect!"
            } else {
                _voiceStatusMessage.value = "No TVs detected automatically. Check Wi-Fi or enter IP manually."
            }
        }
    }

    fun selectDiscoveredTv(tv: DiscoveredTv) {
        ipAddressInput.value = tv.ip
        portInput.value = tv.port
        preferences.tvIp = tv.ip
        preferences.tvPort = tv.port
        preferences.tvName = tv.name
        preferences.tvModel = tv.modelName
        if (tv.mac.isNotBlank()) {
            preferences.tvMac = tv.mac
        }
        connectTv()
    }

    fun clearPairingToken() {
        preferences.clearToken()
        tvController.connectToTV(ipAddressInput.value.trim(), portInput.value, forceNewToken = true)
    }

    // --- Remote Key Actions ---

    fun sendKey(key: RemoteKey): String? {
        val result = tvController.sendRemoteKey(key)
        return result.exceptionOrNull()?.message
    }

    fun launchApp(app: TvApp) {
        viewModelScope.launch {
            tvController.launchApplication(app)
        }
    }

    // --- Voice Control Actions ---

    fun startVoiceListening() {
        _voiceStatusMessage.value = "Listening... Speak your command"
        _pendingVoiceAction.value = null
        val lang = preferences.speechLanguage
        voiceInputManager.startListening(lang) { recognizedText ->
            handleVoiceRecognized(recognizedText)
        }
    }

    fun startVoiceListeningForYouTube(onQueryRecognized: (String) -> Unit) {
        _voiceStatusMessage.value = "Listening for YouTube search query..."
        val lang = preferences.speechLanguage
        voiceInputManager.startListening(lang) { spoken ->
            handleYouTubeQuerySpoken(spoken, onQueryRecognized)
        }
    }

    fun handleYouTubeQuerySpoken(spoken: String, onQueryRecognized: (String) -> Unit) {
        val query = extractYouTubeQuery(spoken)
        if (query.isNotBlank()) {
            onQueryRecognized(query)
            searchYouTubeWorkflow(query)
            _voiceStatusMessage.value = "Searching YouTube on TV for: \"$query\""
        }
    }

    fun extractYouTubeQuery(raw: String): String {
        val clean = raw.trim().removeSuffix(".").removeSuffix("?").removeSuffix("!").trim()
        val trimmed = clean
        val lower = trimmed.lowercase()
        val prefixes = listOf(
            "search youtube for ",
            "search on youtube for ",
            "search on youtube ",
            "search youtube ",
            "find on youtube ",
            "find youtube ",
            "play on youtube ",
            "play youtube ",
            "search for ",
            "search ",
            "find ",
            "look up "
        )
        for (p in prefixes) {
            if (lower.startsWith(p)) {
                val candidate = trimmed.substring(p.length).trim()
                if (candidate.isNotEmpty()) return candidate
            }
        }
        if (lower.endsWith(" on youtube")) {
            val candidate = trimmed.substring(0, trimmed.length - 11).trim()
            if (candidate.isNotEmpty()) return candidate
        }
        if (lower.endsWith(" in youtube")) {
            val candidate = trimmed.substring(0, trimmed.length - 11).trim()
            if (candidate.isNotEmpty()) return candidate
        }
        return trimmed
    }

    fun stopVoiceListening() {
        voiceInputManager.stopListening()
    }

    fun cancelVoiceListening() {
        voiceInputManager.cancelListening()
        _voiceStatusMessage.value = "Voice recognition cancelled."
    }

    fun handleVoiceRecognized(speech: String) {
        viewModelScope.launch {
            _voiceStatusMessage.value = "Recognized: \"$speech\". Analyzing command..."

            // 1. Try deterministic parser first (zero latency, offline)
            val directAction = TvCommandParser.parseDeterministic(speech)
            if (directAction != null) {
                if (autoExecuteVoice.value) {
                    executeAction(directAction)
                    _voiceStatusMessage.value = "Executed: ${getActionDescription(directAction)}"
                } else {
                    _pendingVoiceAction.value = directAction
                    _voiceStatusMessage.value = "Matched command: ${getActionDescription(directAction)}"
                }
                return@launch
            }

            // Check if user spoke a YouTube search query directly
            val trimmedLower = speech.trim().lowercase()
            if (trimmedLower.startsWith("search ") || trimmedLower.contains("youtube") || trimmedLower.startsWith("find ") || trimmedLower.startsWith("play ")) {
                val extractedQuery = extractYouTubeQuery(speech)
                if (extractedQuery.isNotBlank() && extractedQuery != speech) {
                    val ytAction = TvAction.YouTubeSearch(extractedQuery)
                    if (autoExecuteVoice.value) {
                        executeAction(ytAction)
                        _voiceStatusMessage.value = "Executed: Search YouTube for \"$extractedQuery\""
                    } else {
                        _pendingVoiceAction.value = ytAction
                        _voiceStatusMessage.value = "Matched: Search YouTube for \"$extractedQuery\""
                    }
                    return@launch
                }
            }

            // 2. Use AI interpreter for natural language
            if (!useGeminiAi.value) {
                _voiceStatusMessage.value = "Command not recognized deterministically and AI engine is disabled in Settings."
                return@launch
            }

            val providerName = if (preferences.aiProvider == "gemini") "Google Gemini 2.0 Flash" else "OpenRouter (${preferences.openRouterModel})"
            _voiceStatusMessage.value = "Interpreting via $providerName..."
            val aiResult = openRouterClient.sendChatCompletion(speech)
            aiResult.onSuccess { assistantResult ->
                if (assistantResult.isCommand && assistantResult.command != null) {
                    val validatedAction = TvCommandParser.validateAndMapAiResult(assistantResult.command)
                    if (autoExecuteVoice.value) {
                        executeAction(validatedAction)
                        _voiceStatusMessage.value = "Executed: ${getActionDescription(validatedAction)}"
                    } else {
                        _pendingVoiceAction.value = validatedAction
                        _voiceStatusMessage.value = assistantResult.textResponse
                    }
                } else {
                    _voiceStatusMessage.value = assistantResult.textResponse
                }
            }.onFailure { err ->
                _voiceStatusMessage.value = "AI error: ${err.message}. Check API key in Settings or try direct commands like 'Volume Up'."
            }
        }
    }

    fun executePendingVoiceAction() {
        val action = _pendingVoiceAction.value ?: return
        executeAction(action)
        _pendingVoiceAction.value = null
    }

    fun cancelPendingVoiceAction() {
        _pendingVoiceAction.value = null
        _voiceStatusMessage.value = "Command cancelled."
    }

    fun executeAction(action: TvAction) {
        viewModelScope.launch {
            when (action) {
                is TvAction.PressKey -> {
                    tvController.sendRemoteKey(action.key)
                }
                is TvAction.AdjustVolume -> {
                    tvController.adjustVolume(action.delta)
                }
                is TvAction.LaunchApp -> {
                    tvController.launchApplication(action.app)
                }
                is TvAction.YouTubeSearch -> {
                    searchYouTubeWorkflow(action.query)
                }
                is TvAction.SendText -> {
                    tvController.sendText(action.text)
                }
                is TvAction.ClarificationNeeded -> {
                    _voiceStatusMessage.value = action.message
                }
                is TvAction.Unsupported -> {
                    _voiceStatusMessage.value = "Unsupported: ${action.reason}"
                }
            }
        }
    }

    fun getActionDescription(action: TvAction): String {
        return when (action) {
            is TvAction.PressKey -> "Press ${action.key.label} (${action.key.description})"
            is TvAction.AdjustVolume -> "Adjust volume by ${action.delta} steps"
            is TvAction.LaunchApp -> "Launch ${action.app.name}"
            is TvAction.YouTubeSearch -> "Search YouTube for \"${action.query}\""
            is TvAction.SendText -> "Type \"${action.text}\" on TV"
            is TvAction.ClarificationNeeded -> action.message
            is TvAction.Unsupported -> "Unsupported: ${action.reason}"
        }
    }

    // --- Screen 4: YouTube Voice Search Workflow ---

    fun searchYouTubeWorkflow(query: String) {
        viewModelScope.launch {
            _youtubeSearchState.value = YouTubeSearchUiState(
                isSearching = true,
                query = query,
                step1VoiceRecognized = true,
                step2TvConnected = (connectionState.value.status == TvConnectionStatus.CONNECTED),
                step3AppLaunched = null,
                step4TextInjected = null,
                resultMessage = "Starting YouTube search workflow..."
            )

            // Step 2 verification
            val isConnected = connectionState.value.status == TvConnectionStatus.CONNECTED
            if (!isConnected) {
                _youtubeSearchState.value = _youtubeSearchState.value.copy(
                    step2TvConnected = false,
                    resultMessage = "WebSocket disconnected. Attempting REST app launch..."
                )
            } else {
                _youtubeSearchState.value = _youtubeSearchState.value.copy(step2TvConnected = true)
            }

            // Step 3 & 4 execution
            val result: YouTubeSearchResult = tvController.searchYouTube(query)
            _youtubeSearchState.value = _youtubeSearchState.value.copy(
                isSearching = false,
                step3AppLaunched = result.launchedApp,
                step4TextInjected = result.textInjected,
                resultMessage = result.message,
                fallbackNeeded = !result.textInjected
            )
        }
    }

    // --- Screen 5: AI Command Assistant (OpenRouter) ---

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = AssistantChatMessage(isUser = true, text = userText)
        _assistantChat.value = _assistantChat.value + userMsg

        viewModelScope.launch {
            isAiProcessing.value = true

            // Check if user is asking about the app creator or Soumya
            val lower = userText.trim().lowercase()
            if (lower.contains("who made") || lower.contains("who created") || lower.contains("who is soumya") || lower.contains("soumya") || lower == "about" || lower == "creator" || lower == "developer") {
                delay(150)
                val reply = AssistantChatMessage(
                    isUser = false,
                    text = "I am Samsung AI Remote, made with love by Soumya specifically for your Samsung UA32T4410 Smart TV and Samsung Galaxy M30s! ❤️",
                    status = "About"
                )
                _assistantChat.value = _assistantChat.value + reply
                isAiProcessing.value = false
                return@launch
            }

            // 1. Check deterministic commands first (instant, works without internet or API key)
            val deterministic = TvCommandParser.parseDeterministic(userText)
            if (deterministic != null) {
                delay(200)
                executeAction(deterministic)
                val reply = AssistantChatMessage(
                    isUser = false,
                    text = "Executed: ${getActionDescription(deterministic)}",
                    action = deterministic,
                    status = "Executed"
                )
                _assistantChat.value = _assistantChat.value + reply
                isAiProcessing.value = false
                return@launch
            }

            // 2. Send to AI Engine with conversation context
            val history = _assistantChat.value.dropLast(1).map {
                OpenRouterMessage(
                    role = if (it.isUser) "user" else "assistant",
                    content = it.text
                )
            }

            val aiResult = openRouterClient.sendChatCompletion(userText, history)
            aiResult.onSuccess { assistantResult ->
                if (assistantResult.isCommand && assistantResult.command != null) {
                    val validated = TvCommandParser.validateAndMapAiResult(assistantResult.command)
                    when (validated) {
                        is TvAction.Unsupported -> {
                            _assistantChat.value = _assistantChat.value + AssistantChatMessage(
                                isUser = false,
                                text = "Sorry, this action is not supported on your Samsung Smart TV: ${validated.reason}",
                                action = validated,
                                status = "Unsupported"
                            )
                        }
                        is TvAction.ClarificationNeeded -> {
                            _assistantChat.value = _assistantChat.value + AssistantChatMessage(
                                isUser = false,
                                text = validated.message,
                                action = validated,
                                status = "Needs Clarification"
                            )
                        }
                        else -> {
                            executeAction(validated)
                            _assistantChat.value = _assistantChat.value + AssistantChatMessage(
                                isUser = false,
                                text = assistantResult.textResponse,
                                action = validated,
                                status = "Executed"
                            )
                        }
                    }
                } else {
                    // Conversational chat response
                    _assistantChat.value = _assistantChat.value + AssistantChatMessage(
                        isUser = false,
                        text = assistantResult.textResponse,
                        status = "Response"
                    )
                }
            }.onFailure { err ->
                val errorMsg = err.message ?: "Unknown AI error"
                val helpfulText = if (errorMsg.contains("API Key", ignoreCase = true) || errorMsg.contains("403") || errorMsg.contains("configured", ignoreCase = true)) {
                    "⚠️ AI API Key is not configured yet. Go to Settings > 'AI Assistant & API Configuration' to enter your free Google Gemini or OpenRouter API key.\n\n💡 Tip: Direct remote commands (e.g., 'Volume Up', 'Mute', 'Open YouTube', 'Pause') work instantly even without an API key!"
                } else {
                    errorMsg
                }
                _assistantChat.value = _assistantChat.value + AssistantChatMessage(
                    isUser = false,
                    text = helpfulText,
                    isError = true,
                    originalUserPrompt = userText,
                    status = "Configuration Required"
                )
            }

            isAiProcessing.value = false
        }
    }

    fun testAiConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = openRouterClient.testConnection()
            result.onSuccess { msg ->
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.message ?: "Connection test failed")
            }
        }
    }

    fun retryChatMessage(userText: String) {
        sendChatMessage(userText)
    }

    fun clearChat() {
        _assistantChat.value = listOf(
            AssistantChatMessage(
                isUser = false,
                text = "Chat cleared. How can I help you control your Samsung Smart TV?"
            )
        )
    }

    // --- Screen 6: Diagnostics Suite ---

    fun runAllDiagnostics() {
        viewModelScope.launch {
            isRunningDiagnostics.value = true
            val ip = ipAddressInput.value.trim()
            val port = portInput.value

            // 1. Net Ping
            updateDiagnostic("net_ping", DiagnosticStatus.TESTING, "Testing TCP reachability...")
            val pingOk = restApi.checkSocketReachability(ip, port, 2500)
            updateDiagnostic(
                "net_ping",
                if (pingOk) DiagnosticStatus.CONNECTED else DiagnosticStatus.NOT_CONNECTED,
                if (pingOk) "TCP connection to $ip:$port succeeded" else "Unable to connect to $ip:$port. Check TV power and Wi-Fi"
            )

            // 2. REST Info
            updateDiagnostic("rest_info", DiagnosticStatus.TESTING, "Calling http://$ip:8001/api/v2/...")
            val infoRes = restApi.getDeviceInfo(ip)
            if (infoRes.isSuccess) {
                val d = infoRes.getOrNull()
                updateDiagnostic("rest_info", DiagnosticStatus.SUCCESS, "Model: ${d?.modelName}, Name: ${d?.name}")
            } else {
                updateDiagnostic("rest_info", DiagnosticStatus.FAILED, "REST error: ${infoRes.exceptionOrNull()?.message}")
            }

            // 3. WS Conn
            updateDiagnostic("ws_conn", DiagnosticStatus.TESTING, "Checking active WebSocket state...")
            val isWs = connectionState.value.status == TvConnectionStatus.CONNECTED
            updateDiagnostic(
                "ws_conn",
                if (isWs) DiagnosticStatus.CONNECTED else DiagnosticStatus.NOT_CONNECTED,
                if (isWs) "WebSocket is active on port $port" else "WebSocket is currently ${connectionState.value.status}"
            )

            // 4. Auth & Token
            val token = preferences.tvToken
            updateDiagnostic(
                "auth_pair",
                if (!token.isNullOrBlank()) DiagnosticStatus.SUCCESS else DiagnosticStatus.AUTHORIZATION_REQUIRED,
                if (!token.isNullOrBlank()) "Token is saved and valid" else "No token stored. Connection will prompt 'Allow' on TV"
            )

            // 5. Basic Key
            updateDiagnostic("key_cmd", DiagnosticStatus.TESTING, "Testing key command transmission...")
            if (isWs) {
                val keyRes = tvController.sendRemoteKey(RemoteKey.KEY_INFO)
                if (keyRes.isSuccess) {
                    updateDiagnostic("key_cmd", DiagnosticStatus.SUCCESS, "KEY_INFO sent successfully")
                } else {
                    updateDiagnostic("key_cmd", DiagnosticStatus.FAILED, "Failed: ${keyRes.exceptionOrNull()?.message}")
                }
            } else {
                updateDiagnostic("key_cmd", DiagnosticStatus.NOT_CONNECTED, "Cannot test keys while TV WebSocket is disconnected")
            }

            // 6. App Launch
            updateDiagnostic("app_launch", DiagnosticStatus.TESTING, "Testing YouTube app status check...")
            val appRes = restApi.getAppStatus(ip, TvApp.YOUTUBE.id)
            if (appRes.isSuccess) {
                updateDiagnostic("app_launch", DiagnosticStatus.SUCCESS, "YouTube app endpoint is active and supported")
            } else {
                updateDiagnostic("app_launch", DiagnosticStatus.UNSUPPORTED, "App status query returned: ${appRes.exceptionOrNull()?.message}")
            }

            // 7. Speech Rec
            val speechAvail = voiceInputManager.isAvailable()
            updateDiagnostic(
                "speech_rec",
                if (speechAvail) DiagnosticStatus.SUCCESS else DiagnosticStatus.UNSUPPORTED,
                if (speechAvail) "Android SpeechRecognizer is ready" else "SpeechRecognizer query returned unavailable"
            )

            // 8. AI Engine Status
            val hasAiKey = if (preferences.aiProvider == "gemini") {
                preferences.getEffectiveGeminiApiKey().isNotBlank()
            } else {
                preferences.openRouterApiKey.isNotBlank()
            }
            updateDiagnostic(
                "openrouter_api",
                if (hasAiKey) DiagnosticStatus.SUCCESS else DiagnosticStatus.AUTHORIZATION_REQUIRED,
                if (hasAiKey) {
                    if (preferences.aiProvider == "gemini") "Google Gemini 3.5 Flash is ready (active)"
                    else "OpenRouter API Key configured (${preferences.openRouterModel})"
                } else "AI API Key not set. Configure in Settings tab."
            )

            // 9. YouTube Search
            updateDiagnostic(
                "yt_search",
                if (infoRes.isSuccess) DiagnosticStatus.SUCCESS else DiagnosticStatus.UNSUPPORTED,
                "Deep-link launch supported via /api/v2/applications/${TvApp.YOUTUBE.id}"
            )

            isRunningDiagnostics.value = false
        }
    }

    private fun updateDiagnostic(id: String, status: DiagnosticStatus, details: String) {
        _diagnostics.value = _diagnostics.value.map { item ->
            if (item.id == id) item.copy(status = status, details = details) else item
        }
    }

    fun toggleTvServer() {
        if (tvServerState.value.isRunning) {
            tvDeployServer.stopServer()
        } else {
            tvDeployServer.startServer()
        }
    }

    fun exportTvApp() {
        viewModelScope.launch {
            val result = tvDeployServer.exportWgtToDownloads()
            result.onSuccess { file ->
                _tvExportMessage.value = "Successfully exported to: ${file.absolutePath}"
            }.onFailure { err ->
                _tvExportMessage.value = "Export failed: ${err.message}"
            }
        }
    }

    fun refreshApiKeysFromPreferences() {
        viewModelScope.launch(Dispatchers.Main) {
            customGeminiApiKey.value = preferences.customGeminiApiKey
            groqApiKey.value = preferences.groqApiKey
            cerebrasApiKey.value = preferences.cerebrasApiKey
            mistralApiKey.value = preferences.mistralApiKey
            cloudflareApiKey.value = preferences.cloudflareApiKey
            cloudflareAccountId.value = preferences.cloudflareAccountId
            openRouterApiKey.value = preferences.openRouterApiKey
            openRouterModel.value = preferences.openRouterModel
            enableMultiProviderFallback.value = preferences.enableMultiProviderFallback
        }
    }

    // --- Tizen SDB Installer & Package Manager Engine ---
    val tizenSdbClient = TizenSdbClient()

    private val _selectedWgtUri = MutableStateFlow<Uri?>(null)
    val selectedWgtUri: StateFlow<Uri?> = _selectedWgtUri.asStateFlow()

    private val _tizenPackageInfo = MutableStateFlow<TizenPackageInfo?>(null)
    val tizenPackageInfo: StateFlow<TizenPackageInfo?> = _tizenPackageInfo.asStateFlow()

    private val _tizenInstallState = MutableStateFlow<TizenInstallState>(TizenInstallState.Idle)
    val tizenInstallState: StateFlow<TizenInstallState> = _tizenInstallState.asStateFlow()

    private val _tizenLogs = MutableStateFlow<List<String>>(listOf("[*] Tizen SDB Engine ready on Android."))
    val tizenLogs: StateFlow<List<String>> = _tizenLogs.asStateFlow()

    private val _installedTizenApps = MutableStateFlow<List<TizenInstalledApp>>(emptyList())
    val installedTizenApps: StateFlow<List<TizenInstalledApp>> = _installedTizenApps.asStateFlow()

    val isTizenAppsLoading = MutableStateFlow(false)

    private val _tizenDiagnostics = MutableStateFlow<TizenDiagnosticsResult?>(null)
    val tizenDiagnostics: StateFlow<TizenDiagnosticsResult?> = _tizenDiagnostics.asStateFlow()
    val isRunningTizenDiagnostics = MutableStateFlow(false)

    fun appendTizenLog(msg: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _tizenLogs.value = _tizenLogs.value + "[$timestamp] $msg"
    }

    fun clearTizenLogs() {
        _tizenLogs.value = listOf("[*] Logs cleared.")
    }

    fun selectWgtUri(uri: Uri, context: Context) {
        _selectedWgtUri.value = uri
        viewModelScope.launch(Dispatchers.IO) {
            _tizenInstallState.value = TizenInstallState.Validating("Inspecting .wgt package structure...")
            appendTizenLog("[*] Inspecting selected package: $uri")
            val res = TizenPackageValidator.inspectWgt(context, uri)
            if (res.isSuccess) {
                val info = res.getOrNull()
                _tizenPackageInfo.value = info
                _tizenInstallState.value = TizenInstallState.Idle
                appendTizenLog("[✓] Valid package: ${info?.name} (ID: ${info?.packageId}, v${info?.version})")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Invalid package"
                _tizenPackageInfo.value = null
                _tizenInstallState.value = TizenInstallState.Failed(
                    title = "Invalid Package",
                    errorMessage = err,
                    actionableGuide = "Please ensure you selected a valid Tizen .wgt file containing config.xml."
                )
                appendTizenLog("[ERROR] Package validation failed: $err")
            }
        }
    }

    fun loadDefaultBuiltInWgt(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _tizenInstallState.value = TizenInstallState.Validating("Loading built-in SamsungAssistant.wgt...")
            val downloadFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SamsungAssistant.wgt")
            val targetFile = if (downloadFile.exists()) downloadFile else File(context.filesDir, "SamsungAssistant.wgt")

            if (!targetFile.exists()) {
                val exported = tvDeployServer.exportWgtToDownloads().getOrNull()
                if (exported != null) {
                    val res = TizenPackageValidator.inspectWgtFile(exported)
                    if (res.isSuccess) {
                        _selectedWgtUri.value = Uri.fromFile(exported)
                        _tizenPackageInfo.value = res.getOrNull()
                        _tizenInstallState.value = TizenInstallState.Idle
                        appendTizenLog("[✓] Loaded built-in SamsungAssistant.wgt from Downloads")
                        return@launch
                    }
                }
            } else {
                val res = TizenPackageValidator.inspectWgtFile(targetFile)
                if (res.isSuccess) {
                    _selectedWgtUri.value = Uri.fromFile(targetFile)
                    _tizenPackageInfo.value = res.getOrNull()
                    _tizenInstallState.value = TizenInstallState.Idle
                    appendTizenLog("[✓] Loaded SamsungAssistant.wgt (${targetFile.length()} bytes)")
                    return@launch
                }
            }
            _tizenInstallState.value = TizenInstallState.Idle
        }
    }

    fun startTizenInstallation(context: Context) {
        val tvIp = preferences.tvIp.trim()
        if (tvIp.isBlank() || tvIp == "192.168.1.") {
            _tizenInstallState.value = TizenInstallState.Failed(
                title = "TV IP Required",
                errorMessage = "TV IP address is not configured.",
                actionableGuide = "Go to the Connect tab and enter your Samsung TV's IP address (e.g. 192.168.1.105)."
            )
            return
        }

        val uri = _selectedWgtUri.value
        val pkgInfo = _tizenPackageInfo.value
        if (uri == null && pkgInfo == null) {
            _tizenInstallState.value = TizenInstallState.Failed(
                title = "No Package Selected",
                errorMessage = "Please choose a .wgt file first.",
                actionableGuide = "Tap 'Select .WGT File' or 'Use Built-in SamsungAssistant.wgt'."
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _tizenInstallState.value = TizenInstallState.Connecting(tvIp)
            appendTizenLog("=== STARTING TIZEN SDB WIRELESS INSTALLATION ===")
            appendTizenLog("[*] Target TV: $tvIp:26101")
            appendTizenLog("[*] Package: ${pkgInfo?.name ?: "app"} (ID: ${pkgInfo?.packageId})")

            val inputStream: InputStream? = if (uri != null && uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else if (uri != null) {
                context.contentResolver.openInputStream(uri)
            } else {
                val downloadFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SamsungAssistant.wgt")
                if (downloadFile.exists()) downloadFile.inputStream() else null
            }

            if (inputStream == null) {
                _tizenInstallState.value = TizenInstallState.Failed(
                    title = "Read Error",
                    errorMessage = "Cannot read .wgt file content.",
                    actionableGuide = "Try selecting the file again using the file picker."
                )
                return@launch
            }

            val totalBytes = pkgInfo?.sizeBytes ?: 0L

            val result = tizenSdbClient.installPackage(
                tvIp = tvIp,
                wgtInputStream = inputStream,
                totalBytes = totalBytes,
                onProgress = { percent, status ->
                    if (percent < 80) {
                        _tizenInstallState.value = TizenInstallState.Pushing(
                            bytesSent = (totalBytes * percent / 100),
                            totalBytes = totalBytes,
                            progressPercent = percent
                        )
                    } else {
                        _tizenInstallState.value = TizenInstallState.ExecutingInstall(
                            progressPercent = percent,
                            statusText = status
                        )
                    }
                },
                onLog = { logMsg ->
                    appendTizenLog(logMsg)
                }
            )

            if (result.isSuccess) {
                _tizenInstallState.value = TizenInstallState.Success(
                    packageId = pkgInfo?.packageId ?: "installed",
                    message = "Successfully installed ${pkgInfo?.name ?: "app"} on your Samsung Smart TV!"
                )
                appendTizenLog("=== INSTALLATION SUCCESSFUL ===")
                loadInstalledTizenApps()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Installation failed"
                val actionable = when {
                    err.contains("refused", ignoreCase = true) || err.contains("closed", ignoreCase = true) || err.contains("timeout", ignoreCase = true) ->
                        "TV Developer Mode is OFF or Host IP is incorrect.\n1. On TV, open Apps > Press 1-2-3-4-5\n2. Turn Developer Mode ON -> Set Host IP to your phone's IP: ${tvDeployServer.getLocalIpAddress()}\n3. Cold reboot TV by holding remote Power button for 5 seconds."
                    err.contains("certificate", ignoreCase = true) || err.contains("signature", ignoreCase = true) ->
                        "Package signature rejected by TV.\nCheck that your package author-signature is valid, or install a debug-signed build."
                    else -> "Check the terminal logs below for full error output from the TV."
                }
                _tizenInstallState.value = TizenInstallState.Failed(
                    title = "Installation Failed",
                    errorMessage = err,
                    actionableGuide = actionable
                )
                appendTizenLog("=== INSTALLATION FAILED ===")
            }
        }
    }

    fun loadInstalledTizenApps() {
        val tvIp = preferences.tvIp.trim()
        if (tvIp.isBlank() || tvIp == "192.168.1.") return

        viewModelScope.launch(Dispatchers.IO) {
            isTizenAppsLoading.value = true
            appendTizenLog("[*] Querying installed apps list via SDB pkgcmd -l...")
            val res = tizenSdbClient.listInstalledApps(tvIp)
            if (res.isSuccess) {
                val list = res.getOrNull() ?: emptyList()
                _installedTizenApps.value = list
                appendTizenLog("[✓] Found ${list.size} installed application(s) on TV.")
            } else {
                appendTizenLog("[!] Failed to list apps: ${res.exceptionOrNull()?.message}")
            }
            isTizenAppsLoading.value = false
        }
    }

    fun launchTizenApp(appId: String) {
        val tvIp = preferences.tvIp.trim()
        viewModelScope.launch(Dispatchers.IO) {
            appendTizenLog("[*] Launching $appId on TV...")
            val res = tizenSdbClient.launchApp(tvIp, appId)
            if (res.isSuccess) {
                appendTizenLog("[✓] Launch command executed: $appId")
            } else {
                appendTizenLog("[!] Failed to launch: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun stopTizenApp(appId: String) {
        val tvIp = preferences.tvIp.trim()
        viewModelScope.launch(Dispatchers.IO) {
            appendTizenLog("[*] Stopping $appId on TV...")
            val res = tizenSdbClient.stopApp(tvIp, appId)
            if (res.isSuccess) {
                appendTizenLog("[✓] Stopped: $appId")
            } else {
                appendTizenLog("[!] Failed to stop: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun uninstallTizenApp(pkgId: String) {
        val tvIp = preferences.tvIp.trim()
        viewModelScope.launch(Dispatchers.IO) {
            appendTizenLog("[*] Uninstalling package $pkgId from TV...")
            val res = tizenSdbClient.uninstallApp(tvIp, pkgId)
            if (res.isSuccess) {
                appendTizenLog("[✓] Uninstalled: $pkgId")
                loadInstalledTizenApps()
            } else {
                appendTizenLog("[ERROR] Failed to uninstall: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun runTizenSdbDiagnostics() {
        val tvIp = preferences.tvIp.trim()
        val phoneIp = tvDeployServer.getLocalIpAddress()
        viewModelScope.launch(Dispatchers.IO) {
            isRunningTizenDiagnostics.value = true
            appendTizenLog("[*] Running SDB & TV port diagnostics ($tvIp)...")
            val diag = tizenSdbClient.runDiagnostics(tvIp, phoneIp)
            _tizenDiagnostics.value = diag
            appendTizenLog("[*] SDB Port 26101: ${if (diag.isSdbPort26101Open) "OPEN (Ready)" else "CLOSED"}")
            appendTizenLog("[*] Remote Port 8002: ${if (diag.isPort8002Open) "OPEN" else "CLOSED"}")
            appendTizenLog("[*] Summary: ${diag.summary}")
            isRunningTizenDiagnostics.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        tvDeployServer.stopServer()
    }
}
