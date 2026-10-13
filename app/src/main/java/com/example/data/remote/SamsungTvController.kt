package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.TvPreferences
import com.example.data.model.RemoteKey
import com.example.data.model.TvApp
import com.example.data.model.TvConnectionState
import com.example.data.model.TvConnectionStatus
import com.example.data.model.TvDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class SamsungTvController(
    private val context: Context,
    private val preferences: TvPreferences = TvPreferences(context)
) : SamsungTvSocketManager.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val socketManager = SamsungTvSocketManager()
    private val restApi = SamsungTvRestApi()

    private val _connectionState = MutableStateFlow(
        TvConnectionState(
            ipAddress = preferences.tvIp,
            port = preferences.tvPort,
            token = preferences.tvToken
        )
    )
    val connectionState: StateFlow<TvConnectionState> = _connectionState.asStateFlow()

    private val _logFlow = MutableSharedFlow<String>(replay = 20)
    val logFlow: SharedFlow<String> = _logFlow.asSharedFlow()

    private var lastKeyTimestamp = 0L
    private val debounceThresholdMs = 120L

    init {
        socketManager.setListener(this)
    }

    private fun addLog(msg: String) {
        Log.d("SamsungTvController", msg)
        scope.launch { _logFlow.emit(msg) }
    }

    /**
     * Connects to the Samsung TV using specified or stored parameters.
     */
    fun connectToTV(
        ip: String = preferences.tvIp,
        port: Int = preferences.tvPort,
        forceNewToken: Boolean = false
    ) {
        if (ip.isBlank()) {
            _connectionState.value = _connectionState.value.copy(
                status = TvConnectionStatus.ERROR,
                errorMessage = "Please enter TV IP address first"
            )
            return
        }

        preferences.tvIp = ip
        preferences.tvPort = port
        val token = if (forceNewToken) null else preferences.tvToken

        _connectionState.value = _connectionState.value.copy(
            status = TvConnectionStatus.CONNECTING,
            ipAddress = ip,
            port = port,
            isSecure = (port == 8002),
            errorMessage = null
        )

        addLog("Connecting to Samsung TV at $ip:$port (token: ${if (token != null) "saved" else "none"})...")

        // First check REST API in parallel to query model info
        scope.launch {
            val infoResult = restApi.getDeviceInfo(ip)
            infoResult.onSuccess { info ->
                addLog("TV REST info received: ${info.name} (${info.modelName})")
                preferences.tvName = info.name
                preferences.tvModel = info.modelName
                if (info.wifiMac.isNotBlank()) {
                    preferences.tvMac = info.wifiMac
                }
                _connectionState.value = _connectionState.value.copy(deviceInfo = info)
            }.onFailure { e ->
                addLog("TV REST info check skipped: ${e.message}")
            }
        }

        socketManager.connect(ip, port, token)
    }

    /**
     * Disconnects from the TV.
     */
    fun disconnectFromTV() {
        addLog("Disconnecting from TV...")
        socketManager.disconnect()
        _connectionState.value = _connectionState.value.copy(
            status = TvConnectionStatus.DISCONNECTED,
            errorMessage = null
        )
    }

    /**
     * Reconnects to the TV using saved settings.
     */
    fun reconnectToTV() {
        val ip = preferences.tvIp
        if (ip.isNotBlank()) {
            connectToTV(ip, preferences.tvPort)
        }
    }

    /**
     * Queries device info via REST API.
     */
    suspend fun getTVInformation(ip: String): Result<TvDeviceInfo> {
        val result = restApi.getDeviceInfo(ip)
        result.onSuccess { info ->
            _connectionState.value = _connectionState.value.copy(deviceInfo = info)
        }
        return result
    }

    /**
     * Sends a remote key command with rapid-tap debouncing.
     */
    fun sendRemoteKey(key: RemoteKey): Result<Boolean> {
        val now = System.currentTimeMillis()
        if (now - lastKeyTimestamp < debounceThresholdMs) {
            return Result.failure(Exception("Debounced: rapid key presses prevented"))
        }
        lastKeyTimestamp = now

        if (_connectionState.value.status != TvConnectionStatus.CONNECTED) {
            // Special case for KEY_POWER: if TV is in standby, attempt Wake-on-LAN if MAC known
            if (key == RemoteKey.KEY_POWER) {
                val mac = preferences.tvMac
                if (!mac.isNullOrBlank()) {
                    scope.launch {
                        val wolResult = WakeOnLanUtil.sendWakeOnLan(mac)
                        if (wolResult.isSuccess) {
                            addLog("Sent Wake-on-LAN magic packet to TV MAC $mac")
                        }
                    }
                    return Result.failure(Exception("TV is disconnected. Sent Wake-on-LAN packet. Power button via WebSocket only works when TV is On."))
                }
            }
            return Result.failure(Exception("TV is not connected. Connect on the Connection screen first."))
        }

        val success = socketManager.sendKey(key)
        if (success) {
            addLog("Sent key: ${key.keyName} (${key.label})")
            return Result.success(true)
        } else {
            addLog("Failed to send key: ${key.keyName}")
            return Result.failure(Exception("WebSocket send failed. Check connection."))
        }
    }

    /**
     * Adjusts volume by delta steps (e.g. +3 or -2).
     */
    suspend fun adjustVolume(delta: Int): Result<Int> = withContext(Dispatchers.IO) {
        if (_connectionState.value.status != TvConnectionStatus.CONNECTED) {
            return@withContext Result.failure(Exception("TV is not connected"))
        }

        val key = if (delta > 0) RemoteKey.KEY_VOLUP else RemoteKey.KEY_VOLDOWN
        val steps = kotlin.math.abs(delta).coerceIn(1, 15)

        for (i in 0 until steps) {
            socketManager.sendKey(key)
            delay(150)
        }
        addLog("Adjusted volume by $delta steps")
        Result.success(steps)
    }

    /**
     * Navigates directional keys (UP, DOWN, LEFT, RIGHT).
     */
    fun navigate(direction: RemoteKey): Result<Boolean> {
        return when (direction) {
            RemoteKey.KEY_UP, RemoteKey.KEY_DOWN, RemoteKey.KEY_LEFT, RemoteKey.KEY_RIGHT,
            RemoteKey.KEY_ENTER, RemoteKey.KEY_RETURN, RemoteKey.KEY_HOME -> sendRemoteKey(direction)
            else -> Result.failure(IllegalArgumentException("Invalid navigation key: $direction"))
        }
    }

    /**
     * Launches an application on the TV.
     */
    suspend fun launchApplication(app: TvApp, payloadUrl: String? = null): Result<Boolean> {
        val ip = preferences.tvIp
        if (ip.isBlank()) {
            return Result.failure(Exception("No TV IP configured"))
        }

        addLog("Launching app: ${app.name} (${app.id})...")
        val result = restApi.launchApplication(ip, app.id, payloadUrl)
        result.onSuccess {
            addLog("App launch request succeeded for ${app.name}")
        }.onFailure { e ->
            addLog("App launch request failed: ${e.message}")
        }
        return result
    }

    /**
     * Sends plain text using SendRemoteText.
     */
    fun sendText(text: String): Result<Boolean> {
        if (_connectionState.value.status != TvConnectionStatus.CONNECTED) {
            return Result.failure(Exception("TV is not connected"))
        }
        val sent = socketManager.sendText(text)
        return if (sent) Result.success(true) else Result.failure(Exception("Failed to send text"))
    }

    /**
     * YouTube Voice Search workflow implementation:
     * 1. Launches YouTube on TV via REST API deep link or App ID.
     * 2. Attempts text entry into YouTube search.
     */
    suspend fun searchYouTube(query: String): YouTubeSearchResult = withContext(Dispatchers.IO) {
        val ip = preferences.tvIp
        if (ip.isBlank()) {
            return@withContext YouTubeSearchResult(
                success = false,
                query = query,
                launchedApp = false,
                textInjected = false,
                message = "TV IP address is missing. Connect to TV first."
            )
        }

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val youtubeUrl = "https://www.youtube.com/results?search_query=$encodedQuery"

        addLog("Executing YouTube Voice Search: '$query'")

        // Step 1: Launch YouTube
        val launchResult = restApi.launchApplication(ip, TvApp.YOUTUBE.id, youtubeUrl)
        val appLaunched = launchResult.isSuccess

        if (!appLaunched) {
            return@withContext YouTubeSearchResult(
                success = false,
                query = query,
                launchedApp = false,
                textInjected = false,
                message = "Could not launch YouTube on TV (${launchResult.exceptionOrNull()?.message ?: "error"}). Ensure TV is on."
            )
        }

        // Wait for YouTube app to open on TV
        delay(2500)

        // Step 2: Attempt direct SendRemoteText
        val textSent = if (_connectionState.value.status == TvConnectionStatus.CONNECTED) {
            socketManager.sendText(query)
        } else false

        YouTubeSearchResult(
            success = true,
            query = query,
            launchedApp = true,
            textInjected = textSent,
            message = if (textSent) {
                "Launched YouTube and injected search query '$query'."
            } else {
                "Launched YouTube on TV with query '$query'. If search box is not focused, use the D-Pad to select Search."
            }
        )
    }

    // --- SocketManager Listener Callbacks ---

    override fun onStatusChanged(status: TvConnectionStatus, message: String?) {
        _connectionState.value = _connectionState.value.copy(
            status = status,
            errorMessage = message
        )
        addLog("Status changed: $status ${message?.let { "($it)" } ?: ""}")
    }

    override fun onTokenReceived(token: String) {
        preferences.tvToken = token
        _connectionState.value = _connectionState.value.copy(token = token)
        addLog("New pairing token saved securely")
    }

    override fun onMessageReceived(raw: String) {
        // Event logged internally
    }

    override fun onError(error: String) {
        addLog("Error: $error")
    }
}

data class YouTubeSearchResult(
    val success: Boolean,
    val query: String,
    val launchedApp: Boolean,
    val textInjected: Boolean,
    val message: String
)
