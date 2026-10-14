package com.example.data.remote

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

data class TvServerState(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val serverUrl: String = "",
    val downloadCount: Int = 0,
    val lastEvent: String = "Server stopped"
)

class TvAppDeployServer(private val context: Context) {

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
                val reader = client.getInputStream().bufferedReader()
                val output = BufferedOutputStream(client.getOutputStream())

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0]
                val path = parts[1]

                Log.d(tag, "Incoming HTTP request: $method $path from ${client.inetAddress.hostAddress}")

                when {
                    path == "/widgetlist.xml" || path == "/widgetlist" -> {
                        serveWidgetList(output, hostIp, port)
                        _state.value = _state.value.copy(
                            lastEvent = "TV requested widgetlist.xml from ${client.inetAddress.hostAddress}"
                        )
                    }
                    path == "/SamsungAssistant.wgt" || path.endsWith(".wgt") -> {
                        val bytesSent = serveWgtFile(output)
                        _state.value = _state.value.copy(
                            downloadCount = _state.value.downloadCount + 1,
                            lastEvent = "Delivered SamsungAssistant.wgt ($bytesSent bytes) to TV!"
                        )
                    }
                    else -> {
                        serveStatusPage(output, hostIp, port)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling client", e)
        }
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

    private fun serveStatusPage(output: BufferedOutputStream, hostIp: String, port: Int) {
        val html = """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Samsung AI TV Assistant Deployment Server</title>
    <style>
        body { font-family: sans-serif; background: #0a0a0c; color: #fff; padding: 30px; text-align: center; }
        .card { background: #16161a; border: 1px solid #00E676; border-radius: 16px; padding: 24px; max-width: 600px; margin: 0 auto; }
        h1 { color: #00E676; }
        a { color: #00E676; text-decoration: none; font-weight: bold; }
        .btn { display: inline-block; background: #00E676; color: #000; padding: 12px 24px; border-radius: 8px; margin-top: 15px; font-weight: bold; }
    </style>
</head>
<body>
    <div class="card">
        <h1>Samsung AI TV Assistant Server</h1>
        <p>Deployment server is active on <strong>$hostIp:$port</strong></p>
        <p>For Samsung UA32T4410 Developer Mode User App Sync:</p>
        <p><a href="/widgetlist.xml">View widgetlist.xml</a></p>
        <a class="btn" href="/SamsungAssistant.wgt">Download SamsungAssistant.wgt</a>
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
