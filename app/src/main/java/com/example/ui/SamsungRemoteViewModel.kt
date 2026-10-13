package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TvPreferences
import com.example.data.model.DiagnosticItem
import com.example.data.model.DiagnosticStatus
import com.example.data.model.RemoteKey
import com.example.data.model.TvAction
import com.example.data.model.TvApp
import com.example.data.model.TvConnectionState
import com.example.data.model.TvConnectionStatus
import com.example.data.model.DiscoveredTv
import com.example.data.remote.DiscoveryState
import com.example.data.remote.GeminiClient
import com.example.data.remote.SamsungTvController
import com.example.data.remote.SamsungTvDiscoveryManager
import com.example.data.remote.SamsungTvRestApi
import com.example.data.remote.WakeOnLanUtil
import com.example.data.remote.YouTubeSearchResult
import com.example.voice.TvCommandParser
import com.example.voice.VoiceInputManager
import com.example.voice.VoiceState
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
    private val geminiClient = GeminiClient()
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

    val ipAddressInput = MutableStateFlow(preferences.tvIp.ifBlank { "192.168.1." })
    val portInput = MutableStateFlow(preferences.tvPort)

    // Voice Screen Pending Action
    private val _pendingVoiceAction = MutableStateFlow<TvAction?>(null)
    val pendingVoiceAction: StateFlow<TvAction?> = _pendingVoiceAction.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow<String?>(null)
    val voiceStatusMessage: StateFlow<String?> = _voiceStatusMessage.asStateFlow()

    // AI Assistant Chat Messages
    private val _assistantChat = MutableStateFlow<List<AssistantChatMessage>>(
        listOf(
            AssistantChatMessage(
                isUser = false,
                text = "Hello! I am your Samsung TV AI Assistant. You can speak or type commands like 'Turn down the volume a little', 'Open YouTube', or 'Find cat videos on YouTube'."
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
            DiagnosticItem("yt_search", "YouTube Search Support", "Tests YouTube launch and search query deep-link")
        )
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

    fun setAutoExecuteVoice(enabled: Boolean) {
        autoExecuteVoice.value = enabled
        preferences.autoExecuteVoice = enabled
    }

    fun setUseGeminiAi(enabled: Boolean) {
        useGeminiAi.value = enabled
        preferences.useGeminiAi = enabled
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
        voiceInputManager.startListening { recognizedText ->
            handleVoiceRecognized(recognizedText)
        }
    }

    fun stopVoiceListening() {
        voiceInputManager.stopListening()
    }

    fun handleVoiceRecognized(speech: String) {
        viewModelScope.launch {
            _voiceStatusMessage.value = "Recognized: \"$speech\". Analyzing command..."

            // 1. Try deterministic parser first
            val directAction = TvCommandParser.parseDeterministic(speech)
            if (directAction != null) {
                _pendingVoiceAction.value = directAction
                _voiceStatusMessage.value = "Matched command: ${getActionDescription(directAction)}"
                return@launch
            }

            // 2. Use Gemini AI interpreter for natural language
            _voiceStatusMessage.value = "Using Gemini AI to interpret natural language..."
            val aiResult = geminiClient.interpretVoiceCommand(speech)
            aiResult.onSuccess { commandAi ->
                val validatedAction = TvCommandParser.validateAndMapAiResult(commandAi)
                _pendingVoiceAction.value = validatedAction
                _voiceStatusMessage.value = "AI: ${commandAi.explanation}"
            }.onFailure { err ->
                _voiceStatusMessage.value = "AI interpretation failed: ${err.message}. Try exact phrases like 'Volume Up' or 'Open YouTube'."
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
                // TV not connected, but REST might still work if on Wi-Fi
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

    // --- Screen 5: AI Command Assistant ---

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = AssistantChatMessage(isUser = true, text = userText)
        _assistantChat.value = _assistantChat.value + userMsg

        viewModelScope.launch {
            isAiProcessing.value = true

            // Check deterministic first
            val deterministic = TvCommandParser.parseDeterministic(userText)
            if (deterministic != null) {
                delay(300)
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

            // AI interpretation
            val aiResult = geminiClient.interpretVoiceCommand(userText)
            aiResult.onSuccess { commandAi ->
                val validated = TvCommandParser.validateAndMapAiResult(commandAi)
                when (validated) {
                    is TvAction.Unsupported -> {
                        _assistantChat.value = _assistantChat.value + AssistantChatMessage(
                            isUser = false,
                            text = "Sorry, this action is unsupported on your Samsung Smart TV: ${validated.reason}",
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
                            text = "Understood: ${commandAi.explanation}. Executing action...",
                            action = validated,
                            status = "Executed"
                        )
                    }
                }
            }.onFailure { err ->
                _assistantChat.value = _assistantChat.value + AssistantChatMessage(
                    isUser = false,
                    text = "Could not process request via Gemini: ${err.message}. Please try again or use direct remote controls.",
                    status = "Failed"
                )
            }

            isAiProcessing.value = false
        }
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
                if (speechAvail) "Android SpeechRecognizer is ready" else "SpeechRecognizer not found on system"
            )

            // 8. YouTube Search
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
}
