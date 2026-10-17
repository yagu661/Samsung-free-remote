package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.data.model.RemoteKey
import com.example.data.model.TvConnectionStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SamsungTvSocketManager {

    interface Listener {
        fun onStatusChanged(status: TvConnectionStatus, message: String? = null)
        fun onTokenReceived(token: String)
        fun onMessageReceived(raw: String)
        fun onError(error: String)
    }

    private var currentWebSocket: WebSocket? = null
    private var listener: Listener? = null
    private val appDisplayName = "Samsung AI Remote"

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    /**
     * Builds an OkHttpClient configured to allow local self-signed certificates on port 8002.
     */
    private fun createSocketClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // Keep alive
            .writeTimeout(5, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Connects to the Samsung TV WebSocket endpoint on port 8002 (WSS) or 8001 (WS).
     */
    fun connect(ip: String, port: Int = 8002, token: String? = null) {
        disconnect()

        listener?.onStatusChanged(TvConnectionStatus.CONNECTING, "Connecting to $ip:$port...")

        val base64Name = Base64.encodeToString(appDisplayName.toByteArray(), Base64.NO_WRAP)
        val scheme = if (port == 8002) "wss" else "ws"

        val urlBuilder = StringBuilder("$scheme://$ip:$port/api/v2/channels/samsung.remote.control?name=$base64Name")
        if (!token.isNullOrBlank()) {
            urlBuilder.append("&token=$token")
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .build()

        val client = createSocketClient()

        currentWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SamsungRemote", "WebSocket handshake open on $ip:$port")
                // Still waiting for ms.channel.connect event from TV
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SamsungRemote", "WS RX: $text")
                listener?.onMessageReceived(text)

                try {
                    val json = JSONObject(text)
                    val event = json.optString("event")
                    when (event) {
                        "ms.channel.connect" -> {
                            val data = json.optJSONObject("data")
                            val newToken = data?.optString("token")
                            if (!newToken.isNullOrBlank()) {
                                listener?.onTokenReceived(newToken)
                            }
                            listener?.onStatusChanged(TvConnectionStatus.CONNECTED, "Connected and paired with TV")
                        }
                        "ms.channel.clientConnect" -> {
                            listener?.onStatusChanged(TvConnectionStatus.CONNECTED, "TV client ready")
                        }
                        "ms.channel.unauthorized" -> {
                            listener?.onStatusChanged(
                                TvConnectionStatus.AUTHORIZATION_REQUIRED,
                                "TV access denied. Please click 'Allow' on your Samsung TV screen."
                            )
                        }
                        "ms.channel.timeOut" -> {
                            listener?.onStatusChanged(
                                TvConnectionStatus.AUTHORIZATION_REQUIRED,
                                "Pairing request timed out. Please retry and approve on TV."
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SamsungRemote", "Failed to parse TV message: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SamsungRemote", "WS Closing: $code / $reason")
                webSocket.close(1000, null)
                listener?.onStatusChanged(TvConnectionStatus.DISCONNECTED, "Connection closed ($reason)")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SamsungRemote", "WS Closed: $code / $reason")
                listener?.onStatusChanged(TvConnectionStatus.DISCONNECTED, "Disconnected")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SamsungRemote", "WS Failure on $ip:$port: ${t.message}", t)
                val isAuthRequired = t.message?.contains("unauthorized", ignoreCase = true) == true ||
                                     response?.code == 401 || response?.code == 403
                if (isAuthRequired) {
                    listener?.onStatusChanged(
                        TvConnectionStatus.AUTHORIZATION_REQUIRED,
                        "Samsung TV Pairing: Please look at your TV screen and select 'Allow' using your remote."
                    )
                } else if (port == 8002) {
                    // Automatically fallback to port 8001 (unencrypted WS)
                    Log.d("SamsungRemote", "Port 8002 (WSS) failed, attempting automatic fallback to Port 8001 (WS)...")
                    listener?.onStatusChanged(
                        TvConnectionStatus.CONNECTING,
                        "Retrying on TV port 8001 (WS fallback)..."
                    )
                    connect(ip, port = 8001, token = token)
                    return
                } else {
                    listener?.onStatusChanged(
                        TvConnectionStatus.ERROR,
                        t.message ?: "Connection failed. Ensure TV is ON and connected to the same Wi-Fi."
                    )
                }
                listener?.onError(t.localizedMessage ?: "WebSocket connection error")
            }
        })
    }

    /**
     * Sends a remote control key command to the TV.
     */
    fun sendKey(key: RemoteKey): Boolean {
        val ws = currentWebSocket ?: return false
        val payload = JSONObject().apply {
            put("method", "ms.remote.control")
            put("params", JSONObject().apply {
                put("Cmd", "Click")
                put("DataOfCmd", key.keyName)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            })
        }.toString()

        return ws.send(payload)
    }

    /**
     * Sends text to the TV active input field.
     */
    fun sendText(text: String): Boolean {
        val ws = currentWebSocket ?: return false
        val base64Text = Base64.encodeToString(text.toByteArray(), Base64.NO_WRAP)
        val payload = JSONObject().apply {
            put("method", "ms.remote.control")
            put("params", JSONObject().apply {
                put("Cmd", base64Text)
                put("DataOfCmd", "base64")
                put("TypeOfRemote", "SendRemoteText")
            })
        }.toString()

        return ws.send(payload)
    }

    /**
     * Disconnects the active socket.
     */
    fun disconnect() {
        try {
            currentWebSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        currentWebSocket = null
        listener?.onStatusChanged(TvConnectionStatus.DISCONNECTED, "Disconnected")
    }

    fun isConnected(): Boolean {
        return currentWebSocket != null
    }
}
