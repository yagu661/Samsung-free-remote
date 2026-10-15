package com.example.data.remote

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.util.Log
import com.example.data.local.TvPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class TvServerState(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val serverUrl: String = "",
    val downloadCount: Int = 0,
    val apiSyncCount: Int = 0,
    val lastEvent: String = "Server stopped"
)

class TvAppDeployServer(
    private val context: Context,
    private val preferences: TvPreferences = TvPreferences(context),
    var onKeysUpdated: (() -> Unit)? = null
) {

    private val tag = "TvAppDeployServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(TvServerState())
    val state: StateFlow<TvServerState> = _state.asStateFlow()

    fun getLocalIpAddress(): String {
        try {
            // First attempt: WifiManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.ipAddress?.let { ipInt ->
                if (ipInt != 0) {
                    val ip = String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                    if (ip != "0.0.0.0") return ip
                }
            }

            // Second attempt: Network interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val host = address.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve local IP", e)
        }
        return "192.168.1.xxx"
    }

    fun startServer(port: Int = 8080): Boolean {
        if (_state.value.isRunning) return true

        return try {
            serverSocket = ServerSocket(port).apply {
                reuseAddress = true
            }
            val ip = getLocalIpAddress()
            val url = "http://$ip:$port"

            _state.value = TvServerState(
                isRunning = true,
                port = port,
                serverUrl = url,
                downloadCount = 0,
                apiSyncCount = 0,
                lastEvent = "Server running on $url"
            )

            serverJob = scope.launch {
                while (isActive && serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch { handleClient(clientSocket, ip, port) }
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(tag, "Accept error", e)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start server on port $port", e)
            _state.value = _state.value.copy(
                isRunning = false,
                lastEvent = "Failed to start on port $port: ${e.message}"
            )
            false
        }
    }

    fun stopServer() {
        try {
            serverJob?.cancel()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing server socket", e)
        } finally {
            serverSocket = null
            serverJob = null
            _state.value = _state.value.copy(
                isRunning = false,
                lastEvent = "Server stopped"
            )
        }
    }

    private fun handleClient(socket: Socket, hostIp: String, port: Int) {
        try {
            socket.use { client ->
                val reader = client.getInputStream().bufferedReader(StandardCharsets.UTF_8)
                val output = BufferedOutputStream(client.getOutputStream())

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0].uppercase()
                val path = parts[1]
                val clientIp = client.inetAddress?.hostAddress ?: "unknown"

                Log.d(tag, "Incoming HTTP request: $method $path from $clientIp")

                // Read HTTP Headers
                val headers = mutableMapOf<String, String>()
                var headerLine: String?
                while (reader.readLine().also { headerLine = it } != null) {
                    if (headerLine.isNullOrBlank()) break
                    val colonIdx = headerLine!!.indexOf(':')
                    if (colonIdx > 0) {
                        val k = headerLine!!.substring(0, colonIdx).trim().lowercase()
                        val v = headerLine!!.substring(colonIdx + 1).trim()
                        headers[k] = v
                    }
                }

                // Handle CORS preflight
                if (method == "OPTIONS") {
                    serveCorsOptions(output)
                    return
                }

                // Read Request Body if Content-Length specified
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                val body = if (contentLength in 1..65536) {
                    val charArray = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val r = reader.read(charArray, readTotal, contentLength - readTotal)
                        if (r == -1) break
                        readTotal += r
                    }
                    String(charArray, 0, readTotal)
                } else ""

                val cleanPath = path.substringBefore('?')

                when {
                    // API Keys JSON Endpoint
                    cleanPath == "/api/keys" || cleanPath == "/api/config" || cleanPath == "/apis" && method == "GET" -> {
                        serveApiKeysJson(output)
                        _state.value = _state.value.copy(
                            apiSyncCount = _state.value.apiSyncCount + 1,
                            lastEvent = "Device ($clientIp) fetched API keys via /api/keys"
                        )
                    }

                    // Shell Environment Export Script for Termux/Linux
                    cleanPath == "/env.sh" || cleanPath == "/export.sh" || cleanPath == "/api/env" -> {
                        serveEnvScript(output)
                        _state.value = _state.value.copy(
                            apiSyncCount = _state.value.apiSyncCount + 1,
                            lastEvent = "Device ($clientIp) downloaded env.sh script"
                        )
                    }

                    // Save/Update API Keys via POST (From Web UI form or JSON payload)
                    (cleanPath == "/api/keys" || cleanPath == "/api/save") && method == "POST" -> {
                        val contentType = headers["content-type"] ?: ""
                        val success = handleSaveApiKeys(contentType, body)
                        if (contentType.contains("application/json")) {
                            serveJsonResponse(output, success)
                        } else {
                            serveStatusPage(output, hostIp, port, saveMessage = if (success) "✅ API keys updated and synchronized with Phone!" else "⚠️ Failed to save API keys.")
                        }
                        _state.value = _state.value.copy(
                            apiSyncCount = _state.value.apiSyncCount + 1,
                            lastEvent = "API keys updated remotely from $clientIp"
                        )
                    }

                    // Tizen Widget List
                    cleanPath == "/widgetlist.xml" || cleanPath == "/widgetlist" -> {
                        serveWidgetList(output, hostIp, port)
                        _state.value = _state.value.copy(
                            lastEvent = "TV requested widgetlist.xml from $clientIp"
                        )
                    }

                    // Tizen WGT Application Package Download
                    cleanPath == "/SamsungAssistant.wgt" || cleanPath.endsWith(".wgt") -> {
                        val bytesSent = serveWgtFile(output)
                        _state.value = _state.value.copy(
                            downloadCount = _state.value.downloadCount + 1,
                            lastEvent = "Delivered SamsungAssistant.wgt ($bytesSent bytes) to $clientIp!"
                        )
                    }

                    // Default Web Dashboard
                    else -> {
                        serveStatusPage(output, hostIp, port)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling client", e)
        }
    }

    private fun handleSaveApiKeys(contentType: String, body: String): Boolean {
        try {
            if (contentType.contains("application/json")) {
                val json = JSONObject(body)
                if (json.has("geminiApiKey")) preferences.customGeminiApiKey = json.optString("geminiApiKey")
                if (json.has("GEMINI_API_KEY")) preferences.customGeminiApiKey = json.optString("GEMINI_API_KEY")

                if (json.has("groqApiKey")) preferences.groqApiKey = json.optString("groqApiKey")
                if (json.has("GROQ_API_KEY")) preferences.groqApiKey = json.optString("GROQ_API_KEY")

                if (json.has("cerebrasApiKey")) preferences.cerebrasApiKey = json.optString("cerebrasApiKey")
                if (json.has("CEREBRAS_API_KEY")) preferences.cerebrasApiKey = json.optString("CEREBRAS_API_KEY")

                if (json.has("openRouterApiKey")) preferences.openRouterApiKey = json.optString("openRouterApiKey")
                if (json.has("OPENROUTER_API_KEY")) preferences.openRouterApiKey = json.optString("OPENROUTER_API_KEY")

                if (json.has("openRouterModel")) preferences.openRouterModel = json.optString("openRouterModel")

                if (json.has("mistralApiKey")) preferences.mistralApiKey = json.optString("mistralApiKey")
                if (json.has("MISTRAL_API_KEY")) preferences.mistralApiKey = json.optString("MISTRAL_API_KEY")

                if (json.has("cloudflareApiKey")) preferences.cloudflareApiKey = json.optString("cloudflareApiKey")
                if (json.has("CLOUDFLARE_API_KEY")) preferences.cloudflareApiKey = json.optString("CLOUDFLARE_API_KEY")

                if (json.has("cloudflareAccountId")) preferences.cloudflareAccountId = json.optString("cloudflareAccountId")
                if (json.has("CLOUDFLARE_ACCOUNT_ID")) preferences.cloudflareAccountId = json.optString("CLOUDFLARE_ACCOUNT_ID")

                if (json.has("enableMultiProviderFallback")) {
                    preferences.enableMultiProviderFallback = json.optBoolean("enableMultiProviderFallback", true)
                }
            } else {
                // Form URL Encoded
                val params = parseFormBody(body)
                params["gemini_key"]?.let { if (it.isNotBlank()) preferences.customGeminiApiKey = it }
                params["groq_key"]?.let { preferences.groqApiKey = it }
                params["cerebras_key"]?.let { preferences.cerebrasApiKey = it }
                params["openrouter_key"]?.let { preferences.openRouterApiKey = it }
                params["openrouter_model"]?.let { preferences.openRouterModel = it }
                params["mistral_key"]?.let { preferences.mistralApiKey = it }
                params["cf_key"]?.let { preferences.cloudflareApiKey = it }
                params["cf_account"]?.let { preferences.cloudflareAccountId = it }
                params["fallback_toggle"]?.let {
                    preferences.enableMultiProviderFallback = (it == "on" || it == "true" || it == "1")
                }
            }
            onKeysUpdated?.invoke()
            return true
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse and save API keys", e)
            return false
        }
    }

    private fun parseFormBody(body: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        body.split("&").forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value.trim()
            }
        }
        return map
    }

    private fun serveApiKeysJson(output: BufferedOutputStream) {
        val effectiveGemini = preferences.getEffectiveGeminiApiKey()
        val groq = preferences.groqApiKey
        val cerebras = preferences.cerebrasApiKey
        val openRouter = preferences.openRouterApiKey
        val openRouterModel = preferences.openRouterModel
        val mistral = preferences.mistralApiKey
        val cfKey = preferences.cloudflareApiKey
        val cfAccount = preferences.cloudflareAccountId
        val fallback = preferences.enableMultiProviderFallback

        val json = JSONObject().apply {
            put("status", "ok")
            put("timestamp", System.currentTimeMillis())
            put("device", "Samsung TV Companion & AI Controller")
            put("enableMultiProviderFallback", fallback)
            put("keys", JSONObject().apply {
                put("GEMINI_API_KEY", effectiveGemini)
                put("GROQ_API_KEY", groq)
                put("CEREBRAS_API_KEY", cerebras)
                put("OPENROUTER_API_KEY", openRouter)
                put("OPENROUTER_MODEL", openRouterModel)
                put("MISTRAL_API_KEY", mistral)
                put("CLOUDFLARE_API_KEY", cfKey)
                put("CLOUDFLARE_ACCOUNT_ID", cfAccount)
            })
            put("providers", JSONObject().apply {
                put("gemini", JSONObject().apply {
                    put("tier", 1)
                    put("hasKey", effectiveGemini.isNotBlank())
                    put("models", org.json.JSONArray(listOf("gemini-2.0-flash", "gemini-1.5-flash")))
                })
                put("groq", JSONObject().apply {
                    put("tier", 2)
                    put("hasKey", groq.isNotBlank())
                    put("models", org.json.JSONArray(listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")))
                })
                put("cerebras", JSONObject().apply {
                    put("tier", 3)
                    put("hasKey", cerebras.isNotBlank())
                    put("models", org.json.JSONArray(listOf("llama3.1-8b", "llama3.3-70b")))
                })
                put("openrouter", JSONObject().apply {
                    put("tier", 4)
                    put("hasKey", openRouter.isNotBlank())
                    put("selectedModel", openRouterModel)
                    put("models", org.json.JSONArray(listOf("openrouter/free", "qwen/qwen3.8-27b:free", "nvidia/nemotron-3.5-lightning:free")))
                })
                put("mistral", JSONObject().apply {
                    put("tier", 5)
                    put("hasKey", mistral.isNotBlank())
                    put("models", org.json.JSONArray(listOf("mistral-small-latest", "open-mistral-7b")))
                })
                put("cloudflare", JSONObject().apply {
                    put("tier", 6)
                    put("hasKey", cfKey.isNotBlank() && cfAccount.isNotBlank())
                    put("models", org.json.JSONArray(listOf("@cf/meta/llama-3.1-8b-instruct")))
                })
            })
        }

        val bytes = json.toString(2).toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveEnvScript(output: BufferedOutputStream) {
        val script = buildString {
            appendLine("#!/bin/sh")
            appendLine("# Exported from Samsung TV Companion Local Server")
            appendLine("# Run: eval \$(curl -s http://${getLocalIpAddress()}:${_state.value.port}/env.sh)")
            appendLine()
            appendLine("export GEMINI_API_KEY=\"${preferences.getEffectiveGeminiApiKey()}\"")
            appendLine("export GROQ_API_KEY=\"${preferences.groqApiKey}\"")
            appendLine("export CEREBRAS_API_KEY=\"${preferences.cerebrasApiKey}\"")
            appendLine("export OPENROUTER_API_KEY=\"${preferences.openRouterApiKey}\"")
            appendLine("export OPENROUTER_MODEL=\"${preferences.openRouterModel}\"")
            appendLine("export MISTRAL_API_KEY=\"${preferences.mistralApiKey}\"")
            appendLine("export CLOUDFLARE_API_KEY=\"${preferences.cloudflareApiKey}\"")
            appendLine("export CLOUDFLARE_ACCOUNT_ID=\"${preferences.cloudflareAccountId}\"")
            appendLine("export PHONE_SERVER_URL=\"http://${getLocalIpAddress()}:${_state.value.port}\"")
            appendLine()
            appendLine("echo \"[SUCCESS] Loaded 6-tier AI provider keys from Phone Local Server!\"")
        }

        val bytes = script.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveJsonResponse(output: BufferedOutputStream, success: Boolean) {
        val json = JSONObject().apply {
            put("status", if (success) "ok" else "error")
            put("message", if (success) "API keys saved successfully to phone" else "Failed to parse API keys")
        }
        val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveCorsOptions(output: BufferedOutputStream) {
        val header = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization, x-api-key\r\n" +
                "Access-Control-Max-Age: 86400\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.flush()
    }

    private fun serveWidgetList(output: BufferedOutputStream, hostIp: String, port: Int) {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<rsp stat="ok">
    <list>
        <widget id="aistudio01.samsungassistant">
            <title>Samsung AI TV Assistant</title>
            <compression size="26296" type="zip"/>
            <description>Samsung Smart TV AI Assistant with YouTube &amp; Voice</description>
            <download>http://$hostIp:$port/SamsungAssistant.wgt</download>
        </widget>
    </list>
</rsp>
""".trimIndent()

        val bytes = xml.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/xml; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveWgtFile(output: BufferedOutputStream): Int {
        var totalBytes = 0
        try {
            val assetStream: InputStream = context.assets.open("SamsungAssistant.wgt")
            val buffer = ByteArray(8192)
            val byteOut = ByteArrayOutputStream()
            var read: Int
            while (assetStream.read(buffer).also { read = it } != -1) {
                byteOut.write(buffer, 0, read)
            }
            assetStream.close()
            val wgtBytes = byteOut.toByteArray()
            totalBytes = wgtBytes.size

            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/vnd.tizen.wgt\r\n" +
                    "Content-Disposition: attachment; filename=\"SamsungAssistant.wgt\"\r\n" +
                    "Content-Length: ${wgtBytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"

            output.write(header.toByteArray(StandardCharsets.UTF_8))
            output.write(wgtBytes)
            output.flush()
        } catch (e: Exception) {
            Log.e(tag, "Error reading SamsungAssistant.wgt from assets", e)
            val errHeader = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
            output.write(errHeader.toByteArray(StandardCharsets.UTF_8))
            output.flush()
        }
        return totalBytes
    }

    private fun serveStatusPage(output: BufferedOutputStream, hostIp: String, port: Int, saveMessage: String? = null) {
        val geminiKey = preferences.getEffectiveGeminiApiKey()
        val groqKey = preferences.groqApiKey
        val cerebrasKey = preferences.cerebrasApiKey
        val openRouterKey = preferences.openRouterApiKey
        val openRouterModel = preferences.openRouterModel
        val mistralKey = preferences.mistralApiKey
        val cfKey = preferences.cloudflareApiKey
        val cfAccount = preferences.cloudflareAccountId
        val fallbackChecked = if (preferences.enableMultiProviderFallback) "checked" else ""

        fun maskKey(k: String): String {
            if (k.isBlank()) return "Not configured"
            return if (k.length > 8) k.take(4) + "••••••••" + k.takeLast(4) else "••••••••"
        }

        val html = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Samsung AI TV Assistant • Local Wi-Fi Sync Hub</title>
    <style>
        :root {
            --bg: #090a0f;
            --surface: #14161f;
            --surface-elevated: #1e212e;
            --accent: #00E676;
            --accent-dim: rgba(0, 230, 118, 0.15);
            --text: #f0f2f5;
            --muted: #8b92a5;
            --border: #2a2e3f;
            --code-bg: #0d0e14;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
        body { background: var(--bg); color: var(--text); padding: 24px; line-height: 1.5; }
        .container { max-width: 760px; margin: 0 auto; }
        .header { text-align: center; margin-bottom: 24px; }
        .badge { display: inline-block; background: var(--accent-dim); color: var(--accent); padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; border: 1px solid var(--accent); margin-bottom: 12px; }
        h1 { font-size: 26px; color: var(--text); font-weight: 800; letter-spacing: -0.5px; }
        p.subtitle { color: var(--muted); font-size: 14px; margin-top: 6px; }
        
        .alert { background: rgba(0, 230, 118, 0.2); border: 1px solid var(--accent); color: var(--text); padding: 14px 18px; border-radius: 12px; font-weight: 600; margin-bottom: 20px; text-align: center; }
        
        .card { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 22px; margin-bottom: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
        .card h2 { font-size: 18px; color: var(--accent); margin-bottom: 14px; display: flex; align-items: center; gap: 8px; }
        
        .grid-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 14px; }
        @media (max-width: 600px) { .grid-row { grid-template-columns: 1fr; } }
        
        label { display: block; font-size: 13px; font-weight: 600; color: var(--text); margin-bottom: 6px; }
        label span { color: var(--muted); font-weight: 400; font-size: 11px; }
        input[type="text"], input[type="password"], select {
            width: 100%; background: var(--surface-elevated); border: 1px solid var(--border); border-radius: 8px; padding: 10px 14px; color: var(--text); font-size: 13px; font-family: monospace; outline: none; transition: border-color 0.2s;
        }
        input:focus, select:focus { border-color: var(--accent); }
        
        .checkbox-row { display: flex; align-items: center; gap: 10px; margin: 16px 0; font-size: 14px; }
        .checkbox-row input { width: 18px; height: 18px; accent-color: var(--accent); }
        
        .btn-submit {
            background: var(--accent); color: #000; border: none; border-radius: 10px; padding: 12px 24px; font-size: 15px; font-weight: 800; cursor: pointer; width: 100%; transition: opacity 0.2s;
        }
        .btn-submit:hover { opacity: 0.9; }
        
        .code-box {
            background: var(--code-bg); border: 1px solid var(--border); border-radius: 8px; padding: 12px; font-family: monospace; font-size: 12px; color: #a5d6a7; overflow-x: auto; margin-top: 8px; position: relative;
        }
        .btn-copy {
            display: inline-block; background: var(--surface-elevated); color: var(--text); border: 1px solid var(--border); border-radius: 6px; padding: 4px 10px; font-size: 11px; cursor: pointer; font-weight: 600; margin-top: 6px;
        }
        .btn-copy:hover { border-color: var(--accent); color: var(--accent); }
        
        .links-row { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 10px; }
        .links-row a {
            background: var(--surface-elevated); color: var(--accent); text-decoration: none; padding: 8px 14px; border-radius: 8px; font-size: 13px; font-weight: 600; border: 1px solid var(--border);
        }
        .links-row a:hover { border-color: var(--accent); }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <span class="badge">● LOCAL WI-FI ACTIVE SERVER</span>
            <h1>Samsung TV &amp; AI Companion Hub</h1>
            <p class="subtitle">Broadcasted from Android Phone on <strong>$hostIp:$port</strong></p>
        </div>

        ${if (saveMessage != null) "<div class=\"alert\">$saveMessage</div>" else ""}

        <!-- Section 1: BYOK API Remote Sync Form -->
        <div class="card">
            <h2>🔑 Send &amp; Sync AI Keys to Phone</h2>
            <p style="color: var(--muted); font-size: 13px; margin-bottom: 16px;">
                Type or paste your AI API keys on your PC/laptop keyboard, click Save, and they will immediately sync to the Android App!
            </p>

            <form action="/api/keys" method="POST">
                <div class="grid-row">
                    <div>
                        <label>Google Gemini API Key <span>(${maskKey(geminiKey)})</span></label>
                        <input type="password" name="gemini_key" placeholder="AIzaSy..." value="$geminiKey">
                    </div>
                    <div>
                        <label>GroqCloud API Key <span>(${maskKey(groqKey)})</span></label>
                        <input type="password" name="groq_key" placeholder="gsk_..." value="$groqKey">
                    </div>
                </div>

                <div class="grid-row">
                    <div>
                        <label>Cerebras Cloud API Key <span>(${maskKey(cerebrasKey)})</span></label>
                        <input type="password" name="cerebras_key" placeholder="csk_..." value="$cerebrasKey">
                    </div>
                    <div>
                        <label>OpenRouter API Key <span>(${maskKey(openRouterKey)})</span></label>
                        <input type="password" name="openrouter_key" placeholder="sk-or-..." value="$openRouterKey">
                    </div>
                </div>

                <div class="grid-row">
                    <div>
                        <label>OpenRouter Model Selection</label>
                        <select name="openrouter_model">
                            <option value="openrouter/free" ${if (openRouterModel == "openrouter/free") "selected" else ""}>openrouter/free (Auto-Router)</option>
                            <option value="qwen/qwen3.8-27b:free" ${if (openRouterModel == "qwen/qwen3.8-27b:free") "selected" else ""}>qwen/qwen3.8-27b:free</option>
                            <option value="nvidia/nemotron-3.5-lightning:free" ${if (openRouterModel == "nvidia/nemotron-3.5-lightning:free") "selected" else ""}>nvidia/nemotron-3.5-lightning:free</option>
                        </select>
                    </div>
                    <div>
                        <label>Mistral AI API Key <span>(${maskKey(mistralKey)})</span></label>
                        <input type="password" name="mistral_key" placeholder="Mistral key" value="$mistralKey">
                    </div>
                </div>

                <div class="grid-row">
                    <div>
                        <label>Cloudflare Workers AI Key <span>(${maskKey(cfKey)})</span></label>
                        <input type="password" name="cf_key" placeholder="Cloudflare API token" value="$cfKey">
                    </div>
                    <div>
                        <label>Cloudflare Account ID <span>(${maskKey(cfAccount)})</span></label>
                        <input type="text" name="cf_account" placeholder="32-char hex account id" value="$cfAccount">
                    </div>
                </div>

                <div class="checkbox-row">
                    <input type="checkbox" id="fallback_toggle" name="fallback_toggle" $fallbackChecked>
                    <label for="fallback_toggle" style="margin: 0; cursor: pointer;">Enable Multi-Provider Auto Fallback (Gemini → Groq → Cerebras → OpenRouter → Mistral → Cloudflare)</label>
                </div>

                <button type="submit" class="btn-submit">💾 Save &amp; Sync to Android App</button>
            </form>
        </div>

        <!-- Section 2: Developer CLI & Termux Quick Sync -->
        <div class="card">
            <h2>⚡ Termux &amp; CLI Quick-Sync (1-Liner)</h2>
            <p style="color: var(--muted); font-size: 13px;">
                Fetch all keys automatically into your Termux terminal on Galaxy M30s or on your PC:
            </p>
            <div class="code-box" id="termuxCode">eval $(curl -s http://$hostIp:$port/env.sh)</div>
            <button class="btn-copy" onclick="navigator.clipboard.writeText(document.getElementById('termuxCode').innerText); alert('Copied Termux 1-liner!');">📋 Copy Termux Command</button>

            <div style="margin-top: 14px;">
                <label>Run Python Multi-Provider Fallback with Auto-Sync:</label>
                <div class="code-box" id="pyCode">python3 llm_fallback_system.py --sync http://$hostIp:$port</div>
                <button class="btn-copy" onclick="navigator.clipboard.writeText(document.getElementById('pyCode').innerText); alert('Copied Python command!');">📋 Copy Python Command</button>
            </div>

            <div style="margin-top: 14px;">
                <label>JSON API Endpoint for TV or Custom Apps:</label>
                <div class="links-row">
                    <a href="/api/keys" target="_blank">View /api/keys (JSON) ↗</a>
                    <a href="/env.sh" target="_blank">View /env.sh (Bash) ↗</a>
                </div>
            </div>
        </div>

        <!-- Section 3: Samsung TV App Deployment -->
        <div class="card">
            <h2>📺 Samsung Smart TV App Sync (Tizen 5.5)</h2>
            <p style="color: var(--muted); font-size: 13px;">
                For Samsung UA32T4410 Developer Mode User App Sync:
            </p>
            <div class="links-row">
                <a href="/widgetlist.xml" target="_blank">📄 View widgetlist.xml</a>
                <a href="/SamsungAssistant.wgt" download>⬇️ Download SamsungAssistant.wgt</a>
            </div>
        </div>
    </div>
</body>
</html>
""".trimIndent()

        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    fun exportWgtToDownloads(): Result<File> {
        return try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val targetFile = File(downloadDir, "SamsungAssistant.wgt")

            val inStream = context.assets.open("SamsungAssistant.wgt")
            val outStream = FileOutputStream(targetFile)

            inStream.copyTo(outStream)
            inStream.close()
            outStream.flush()
            outStream.close()

            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(tag, "Failed to export SamsungAssistant.wgt to Downloads", e)
            Result.failure(e)
        }
    }
}
