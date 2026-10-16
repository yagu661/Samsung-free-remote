package com.example.data.remote

import com.example.data.model.TizenDiagnosticsResult
import com.example.data.model.TizenInstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin native implementation of Samsung Tizen Smart Development Bridge (SDB / ADB wire protocol)
 * over TCP port 26101.
 *
 * Runs directly on Android ARM64 without external binaries, root, or PC dependencies.
 */
class TizenSdbClient(
    private val connectTimeoutMs: Int = 6000,
    private val readTimeoutMs: Int = 15000
) {

    companion object {
        const val SDB_PORT = 26101
        const val REMOTE_SOCKET_PORT = 8002
        const val REST_INFO_PORT = 8001

        // SDB / ADB Wire Protocol Constants (Little-Endian)
        private const val A_CNXN = 0x4e584e43 // "CNXN"
        private const val A_AUTH = 0x48545541 // "AUTH"
        private const val A_OPEN = 0x4e45504f // "OPEN"
        private const val A_OKAY = 0x59414b4f // "OKAY"
        private const val A_CLSE = 0x45534c43 // "CLSE"
        private const val A_WRTE = 0x45545257 // "WRTE"

        private const val A_VERSION = 0x01000000
        private const val MAX_PAYLOAD = 4096

        // Sync Protocol Constants
        private val SYNC_SEND = "SEND".toByteArray(Charsets.US_ASCII)
        private val SYNC_DATA = "DATA".toByteArray(Charsets.US_ASCII)
        private val SYNC_DONE = "DONE".toByteArray(Charsets.US_ASCII)
        private val SYNC_OKAY = "OKAY".toByteArray(Charsets.US_ASCII)
        private val SYNC_FAIL = "FAIL".toByteArray(Charsets.US_ASCII)
    }

    private data class SdbMessage(
        val command: Int,
        val arg0: Int,
        val arg1: Int,
        val dataLength: Int,
        val dataCrc: Int,
        val magic: Int,
        val payload: ByteArray = ByteArray(0)
    )

    /**
     * Installs a .wgt package on the Samsung Smart TV via SDB protocol.
     */
    suspend fun installPackage(
        tvIp: String,
        wgtInputStream: InputStream,
        totalBytes: Long,
        onProgress: (percent: Int, status: String) -> Unit,
        onLog: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            onLog("[*] Connecting to Samsung Smart TV SDB daemon at $tvIp:$SDB_PORT...")
            socket = Socket()
            socket.connect(InetSocketAddress(tvIp, SDB_PORT), connectTimeoutMs)
            socket.soTimeout = readTimeoutMs

            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            // 1. Handshake
            onLog("[*] Initializing SDB handshake (CNXN)...")
            onProgress(10, "SDB Handshake...")
            val handshakePayload = "host::SamsungTizenInstaller\u0000".toByteArray(Charsets.UTF_8)
            sendMessage(outputStream, A_CNXN, A_VERSION, MAX_PAYLOAD, handshakePayload)

            val handshakeResponse = readMessage(inputStream)
            if (handshakeResponse.command != A_CNXN) {
                if (handshakeResponse.command == A_AUTH) {
                    return@withContext Result.failure(
                        SecurityException("TV requested RSA key authentication. Please ensure Developer Mode is ON with Host IP set to your phone IP.")
                    )
                }
                return@withContext Result.failure(
                    IllegalStateException("SDB handshake failed: unexpected response 0x${Integer.toHexString(handshakeResponse.command)}")
                )
            }
            onLog("[✓] SDB handshake verified! Connected to Samsung Tizen OS.")

            // 2. Open 'sync:' service for file transfer
            val localSyncId = 1
            onLog("[*] Opening SDB sync stream for file transfer...")
            onProgress(20, "Opening file transfer stream...")
            sendMessage(outputStream, A_OPEN, localSyncId, 0, "sync:\u0000".toByteArray(Charsets.UTF_8))

            val syncOpenResponse = readMessage(inputStream)
            if (syncOpenResponse.command != A_OKAY) {
                return@withContext Result.failure(
                    IllegalStateException("Failed to open SDB sync stream: response 0x${Integer.toHexString(syncOpenResponse.command)}")
                )
            }
            val remoteSyncId = syncOpenResponse.arg0
            onLog("[✓] Sync stream established (Remote ID: $remoteSyncId).")

            // 3. Push .wgt file into /tmp/samsung_app.wgt
            val targetRemotePath = "/tmp/samsung_app.wgt"
            onLog("[*] Pushing package to TV storage ($targetRemotePath)...")
            onProgress(25, "Pushing .wgt to TV storage...")

            // Send SYNC SEND header
            val sendModeStr = "$targetRemotePath,0664"
            val sendHeader = ByteBuffer.allocate(8 + sendModeStr.length).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put(SYNC_SEND)
                putInt(sendModeStr.length)
                put(sendModeStr.toByteArray(Charsets.UTF_8))
            }.array()
            sendWrite(outputStream, localSyncId, remoteSyncId, sendHeader)

            // Stream chunks
            val buffer = ByteArray(2048)
            var bytesSent = 0L
            var readLen: Int
            while (wgtInputStream.read(buffer).also { readLen = it } != -1) {
                val chunkHeader = ByteBuffer.allocate(8 + readLen).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                    put(SYNC_DATA)
                    putInt(readLen)
                    put(buffer, 0, readLen)
                }.array()
                sendWrite(outputStream, localSyncId, remoteSyncId, chunkHeader)
                bytesSent += readLen

                val percent = if (totalBytes > 0) {
                    25 + ((bytesSent.toDouble() / totalBytes) * 50).toInt().coerceIn(0, 50)
                } else {
                    50
                }
                onProgress(percent, "Transferring: ${(bytesSent / 1024)} KB / ${(totalBytes / 1024)} KB")
            }

            // Send SYNC DONE
            val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
            val donePacket = ByteBuffer.allocate(8).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put(SYNC_DONE)
                putInt(nowSeconds)
            }.array()
            sendWrite(outputStream, localSyncId, remoteSyncId, donePacket)

            // Wait for sync response (OKAY or FAIL)
            val syncAck = readMessage(inputStream)
            onLog("[✓] File transfer completed ($bytesSent bytes). Closing sync stream.")
            sendMessage(outputStream, A_CLSE, localSyncId, remoteSyncId, ByteArray(0))

            // 4. Open 'shell:' service and execute pkgcmd
            onLog("[*] Triggering Tizen package installer (pkgcmd)...")
            onProgress(80, "Executing pkgcmd -i on TV...")

            val localShellId = 2
            val installCmd = "pkgcmd -i -t wgt -p $targetRemotePath\u0000"
            sendMessage(outputStream, A_OPEN, localShellId, 0, "shell:$installCmd".toByteArray(Charsets.UTF_8))

            val shellOpenResponse = readMessage(inputStream)
            if (shellOpenResponse.command != A_OKAY) {
                return@withContext Result.failure(
                    IllegalStateException("Failed to open SDB shell for pkgcmd execution.")
                )
            }
            val remoteShellId = shellOpenResponse.arg0

            // Read command execution output
            val logAccumulator = StringBuilder()
            var isInstallSuccess = false
            var isFinished = false

            while (!isFinished) {
                val shellMsg = readMessage(inputStream)
                when (shellMsg.command) {
                    A_WRTE -> {
                        val text = String(shellMsg.payload, Charsets.UTF_8)
                        logAccumulator.append(text)
                        onLog("[TV] " + text.trim())
                        if (text.contains("100%") || text.contains("install") || text.contains("spend time")) {
                            onProgress(95, "Registering package...")
                        }
                        if (text.contains("[install] ok", ignoreCase = true) ||
                            text.contains("val[ok]", ignoreCase = true) ||
                            text.contains("spend time for pkgcmd", ignoreCase = true)) {
                            isInstallSuccess = true
                        }
                        // Acknowledge write
                        sendMessage(outputStream, A_OKAY, localShellId, remoteShellId, ByteArray(0))
                    }
                    A_CLSE -> {
                        isFinished = true
                    }
                    else -> {}
                }
            }

            // Cleanup remote temp file
            try {
                executeSingleShellCommand(outputStream, inputStream, "rm -f $targetRemotePath")
            } catch (_: Exception) {}

            onProgress(100, if (isInstallSuccess) "Installation Complete!" else "Installation finished")

            if (isInstallSuccess || logAccumulator.contains("ok", ignoreCase = true)) {
                onLog("[✓] SUCCESS! Application installed on Samsung Smart TV.")
                Result.success(logAccumulator.toString().ifBlank { "Application installed successfully." })
            } else {
                val output = logAccumulator.toString()
                val errorMsg = if (output.contains("author signature", ignoreCase = true) || output.contains("certificate", ignoreCase = true)) {
                    "Certificate error: Package signature was rejected by TV. Ensure author signature matches or Developer Mode is enabled."
                } else if (output.isNotBlank()) {
                    output
                } else {
                    "TV closed the installer stream without reporting success. Check Developer Mode on TV."
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            onLog("[ERROR] Installation failed: ${e.message}")
            Result.failure(e)
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Lists installed applications on the TV using `pkgcmd -l`.
     */
    suspend fun listInstalledApps(tvIp: String): Result<List<TizenInstalledApp>> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(tvIp, SDB_PORT), connectTimeoutMs)
            socket.soTimeout = readTimeoutMs

            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            // Handshake
            val handshakePayload = "host::SamsungTizenInstaller\u0000".toByteArray(Charsets.UTF_8)
            sendMessage(outputStream, A_CNXN, A_VERSION, MAX_PAYLOAD, handshakePayload)
            val cnxn = readMessage(inputStream)
            if (cnxn.command != A_CNXN) {
                return@withContext Result.failure(IllegalStateException("SDB handshake failed"))
            }

            // Run `pkgcmd -l`
            val output = executeSingleShellCommand(outputStream, inputStream, "pkgcmd -l")
            val apps = parseInstalledApps(output)
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Launches an installed application via SDB `app_launcher -s <appId>`.
     */
    suspend fun launchApp(tvIp: String, appId: String): Result<String> = withContext(Dispatchers.IO) {
        runCommand(tvIp, "app_launcher -s $appId")
    }

    /**
     * Stops/kills a running application via SDB `app_launcher -k <appId>`.
     */
    suspend fun stopApp(tvIp: String, appId: String): Result<String> = withContext(Dispatchers.IO) {
        runCommand(tvIp, "app_launcher -k $appId")
    }

    /**
     * Uninstalls an application via SDB `pkgcmd -u -n <pkgId>`.
     */
    suspend fun uninstallApp(tvIp: String, pkgId: String): Result<String> = withContext(Dispatchers.IO) {
        runCommand(tvIp, "pkgcmd -u -n $pkgId")
    }

    /**
     * Runs full port diagnostics on the target Samsung Smart TV.
     */
    suspend fun runDiagnostics(tvIp: String, phoneIp: String): TizenDiagnosticsResult = withContext(Dispatchers.IO) {
        val isPort8002 = checkPort(tvIp, REMOTE_SOCKET_PORT, 2000)
        val isSdbPort26101 = checkPort(tvIp, SDB_PORT, 2500)
        val isRestPort8001 = checkPort(tvIp, REST_INFO_PORT, 2000)

        val isDevMode = isSdbPort26101

        val summary = when {
            isSdbPort26101 -> "✓ Developer Mode SDB is active! TV is ready for direct installation."
            isPort8002 && !isSdbPort26101 -> "⚠️ TV is online, but SDB port 26101 is closed. Developer Mode is OFF or Host IP does not match your phone IP."
            else -> "✗ Cannot connect to TV. Check Wi-Fi or TV IP."
        }

        val recommendation = when {
            isSdbPort26101 -> "Ready! Select any .wgt file and tap 'Install to TV'."
            isPort8002 && !isSdbPort26101 -> "To enable Developer Mode:\n1. On TV, open 'APPS'\n2. Press 1, 2, 3, 4, 5 on remote\n3. Turn Developer Mode ON -> Set Host IP to: $phoneIp\n4. Hold remote Power button for 5 sec to reboot TV."
            else -> "Ensure TV is turned ON and both TV and Galaxy M30s are connected to the same Wi-Fi network."
        }

        TizenDiagnosticsResult(
            tvIp = tvIp,
            phoneIp = phoneIp,
            isPort8002Open = isPort8002,
            isSdbPort26101Open = isSdbPort26101,
            isRestPort8001Open = isRestPort8001,
            isDeveloperModeActive = isDevMode,
            summary = summary,
            recommendation = recommendation
        )
    }

    private fun checkPort(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun runCommand(tvIp: String, command: String): Result<String> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(tvIp, SDB_PORT), connectTimeoutMs)
            socket.soTimeout = readTimeoutMs

            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            val handshakePayload = "host::SamsungTizenInstaller\u0000".toByteArray(Charsets.UTF_8)
            sendMessage(outputStream, A_CNXN, A_VERSION, MAX_PAYLOAD, handshakePayload)
            val cnxn = readMessage(inputStream)
            if (cnxn.command != A_CNXN) {
                return@withContext Result.failure(IllegalStateException("SDB handshake failed"))
            }

            val output = executeSingleShellCommand(outputStream, inputStream, command)
            Result.success(output)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun executeSingleShellCommand(outputStream: OutputStream, inputStream: InputStream, command: String): String {
        val localId = 3
        sendMessage(outputStream, A_OPEN, localId, 0, "shell:$command\u0000".toByteArray(Charsets.UTF_8))
        val openAck = readMessage(inputStream)
        if (openAck.command != A_OKAY) {
            return ""
        }
        val remoteId = openAck.arg0

        val result = StringBuilder()
        var finished = false
        while (!finished) {
            val msg = readMessage(inputStream)
            when (msg.command) {
                A_WRTE -> {
                    result.append(String(msg.payload, Charsets.UTF_8))
                    sendMessage(outputStream, A_OKAY, localId, remoteId, ByteArray(0))
                }
                A_CLSE -> {
                    finished = true
                }
                else -> {}
            }
        }
        return result.toString()
    }

    private fun parseInstalledApps(output: String): List<TizenInstalledApp> {
        val apps = mutableListOf<TizenInstalledApp>()
        val lines = output.lines()
        var currentPkg = ""
        var currentType = "wgt"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("pkgid : [", ignoreCase = true)) {
                val pkg = trimmed.substringAfter("pkgid : [").substringBefore("]")
                if (pkg.isNotBlank()) currentPkg = pkg
            } else if (trimmed.contains("appid : [", ignoreCase = true)) {
                val app = trimmed.substringAfter("appid : [").substringBefore("]")
                if (currentPkg.isNotBlank()) {
                    apps.add(TizenInstalledApp(packageId = currentPkg, appType = currentType, label = app))
                    currentPkg = ""
                }
            } else if (trimmed.startsWith("pkg_name :") || trimmed.startsWith("package :")) {
                val pkg = trimmed.substringAfter(":").trim()
                if (pkg.isNotBlank()) {
                    apps.add(TizenInstalledApp(packageId = pkg, appType = "wgt", label = pkg))
                }
            }
        }

        // If standard list parsing did not catch, extract word tokens
        if (apps.isEmpty() && output.isNotBlank()) {
            output.split("\\s+".toRegex()).forEach { token ->
                val clean = token.trim().removeSurrounding("[", "]")
                if (clean.contains(".") && clean.length > 5 && !clean.contains(":") && !clean.contains("/")) {
                    apps.add(TizenInstalledApp(packageId = clean, appType = "wgt", label = clean))
                }
            }
        }

        return apps.distinctBy { it.packageId }
    }

    private fun sendMessage(out: OutputStream, command: Int, arg0: Int, arg1: Int, payload: ByteArray) {
        val length = payload.size
        var crc = 0
        for (b in payload) {
            crc += (b.toInt() and 0xFF)
        }
        val magic = command xor -0x1 // command ^ 0xFFFFFFFF

        val header = ByteBuffer.allocate(24).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            putInt(command)
            putInt(arg0)
            putInt(arg1)
            putInt(length)
            putInt(crc)
            putInt(magic)
        }.array()

        out.write(header)
        if (length > 0) {
            out.write(payload)
        }
        out.flush()
    }

    private fun sendWrite(out: OutputStream, localId: Int, remoteId: Int, payload: ByteArray) {
        sendMessage(out, A_WRTE, localId, remoteId, payload)
    }

    private fun readMessage(input: InputStream): SdbMessage {
        val headerBuf = ByteArray(24)
        readFully(input, headerBuf)

        val bb = ByteBuffer.wrap(headerBuf).order(ByteOrder.LITTLE_ENDIAN)
        val command = bb.int
        val arg0 = bb.int
        val arg1 = bb.int
        val length = bb.int
        val crc = bb.int
        val magic = bb.int

        val payload = if (length > 0) {
            val p = ByteArray(length)
            readFully(input, p)
            p
        } else {
            ByteArray(0)
        }

        return SdbMessage(command, arg0, arg1, length, crc, magic, payload)
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        val target = buffer.size
        while (offset < target) {
            val count = input.read(buffer, offset, target - offset)
            if (count < 0) {
                throw java.io.EOFException("Unexpected end of SDB stream (read $offset / $target bytes)")
            }
            offset += count
        }
    }
}
