package com.example.data.remote

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.DiscoveredTv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

sealed class DiscoveryState {
    object Idle : DiscoveryState()
    data class Scanning(val message: String, val progress: Float = 0f) : DiscoveryState()
    data class Finished(val discoveredTvs: List<DiscoveredTv>, val summary: String) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

class SamsungTvDiscoveryManager(
    private val context: Context,
    private val restApi: SamsungTvRestApi = SamsungTvRestApi()
) {
    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val discoveredMap = ConcurrentHashMap<String, DiscoveredTv>()

    /**
     * Finds Samsung TVs using both SSDP multicast and local subnet REST probes.
     */
    suspend fun findSamsungTvs(): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        discoveredMap.clear()
        _discoveryState.value = DiscoveryState.Scanning("Starting Samsung TV discovery...", 0.1f)

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("samsung_tv_ssdp_lock")?.apply {
            setReferenceCounted(true)
            try { acquire() } catch (_: Exception) {}
        }

        try {
            // Method 1: SSDP M-SEARCH (Fast multicast discovery)
            _discoveryState.value = DiscoveryState.Scanning("Broadcasting SSDP discovery requests...", 0.3f)
            performSsdpDiscovery()

            // Method 2: Subnet Probe (Overcomes router multicast filtering / AP isolation)
            val phoneIp = getLocalWifiIpAddress(wifiManager)
            if (phoneIp != null && phoneIp.contains(".")) {
                val subnet = phoneIp.substringBeforeLast(".") + "."
                _discoveryState.value = DiscoveryState.Scanning("Probing local Wi-Fi subnet ($subnet*)...", 0.6f)
                probeSubnetForSamsungTvs(subnet)
            }

            val list = discoveredMap.values.toList()
            val summary = if (list.isNotEmpty()) {
                "Found ${list.size} Samsung TV(s) on Wi-Fi"
            } else {
                "No Samsung TVs responded. Ensure TV is turned ON and connected to the same Wi-Fi. If your router has AP Isolation or blocks multicast, check TV Network Status."
            }

            _discoveryState.value = DiscoveryState.Finished(list, summary)
            list
        } catch (e: Exception) {
            Log.e("TvDiscovery", "Discovery error: ${e.message}", e)
            _discoveryState.value = DiscoveryState.Error("Discovery encountered error: ${e.message}")
            discoveredMap.values.toList()
        } finally {
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Broadcasts SSDP M-SEARCH packets and listens for Samsung TV responses.
     */
    private suspend fun performSsdpDiscovery() = withContext(Dispatchers.IO) {
        val targets = listOf(
            "urn:samsung.com:device:RemoteControlReceiver:1",
            "urn:schemas-upnp-org:device:MediaRenderer:1",
            "ssdp:all"
        )

        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 1800
                socket.broadcast = true

                for (target in targets) {
                    val msearch = "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: 239.255.255.250:1900\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 2\r\n" +
                            "ST: $target\r\n\r\n"

                    val bytes = msearch.toByteArray()
                    val packet = DatagramPacket(
                        bytes,
                        bytes.size,
                        InetAddress.getByName("239.255.255.250"),
                        1900
                    )
                    socket.send(packet)
                }

                val buffer = ByteArray(2048)
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 2000) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(receivePacket)
                        val response = String(receivePacket.data, 0, receivePacket.length)
                        val responderIp = receivePacket.address.hostAddress ?: continue

                        // Check if response indicates Samsung TV
                        if (response.contains("Samsung", ignoreCase = true) ||
                            response.contains("SEC", ignoreCase = true) ||
                            response.contains("RemoteControlReceiver", ignoreCase = true)
                        ) {
                            inspectAndAddTv(responderIp, "SSDP Discovery")
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        break
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TvDiscovery", "SSDP multicast exception: ${e.message}")
        }
    }

    /**
     * Probes the local /24 subnet for responsive Samsung REST APIs (port 8001).
     */
    private suspend fun probeSubnetForSamsungTvs(subnet: String) = coroutineScope {
        // Probe in chunks to avoid overwhelming socket table
        val batches = (1..254).chunked(32)
        for (batch in batches) {
            batch.map { host ->
                async(Dispatchers.IO) {
                    val ip = "$subnet$host"
                    if (isPortOpen(ip, 8001, timeoutMs = 250)) {
                        inspectAndAddTv(ip, "Wi-Fi Subnet Probe")
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun inspectAndAddTv(ip: String, method: String) {
        if (discoveredMap.containsKey(ip)) return

        val infoResult = restApi.getDeviceInfo(ip)
        if (infoResult.isSuccess) {
            val info = infoResult.getOrNull()
            val tv = DiscoveredTv(
                ip = ip,
                port = 8002,
                name = info?.name?.ifBlank { "Samsung Smart TV" } ?: "Samsung Smart TV",
                modelName = info?.modelName?.ifBlank { "UA32T4410" } ?: "UA32T4410",
                id = info?.id ?: "",
                mac = info?.wifiMac ?: "",
                discoveryMethod = method
            )
            discoveredMap[ip] = tv
            Log.d("TvDiscovery", "Discovered TV at $ip: ${tv.name} (${tv.modelName}) via $method")
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalWifiIpAddress(wifiManager: WifiManager?): String? {
        return try {
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt == 0) return null
            String.format(
                java.util.Locale.US,
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (_: Exception) {
            null
        }
    }
}
