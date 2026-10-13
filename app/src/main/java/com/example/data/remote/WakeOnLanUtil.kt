package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLanUtil {

    /**
     * Sends a Wake-on-LAN Magic Packet to the TV MAC address via UDP broadcast on port 9.
     */
    suspend fun sendWakeOnLan(macAddress: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.replace(":", "").replace("-", "")
            if (cleanMac.length != 12) {
                return@withContext Result.failure(IllegalArgumentException("Invalid MAC address: $macAddress"))
            }

            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                macBytes[i] = cleanMac.substring(i * 2, (i + 1) * 2).toInt(16).toByte()
            }

            // Magic packet = 6 bytes of 0xFF followed by 16 repetitions of target MAC
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0 until 6) {
                bytes[i] = 0xFF.toByte()
            }
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, bytes, 6 + i * macBytes.size, macBytes.size)
            }

            val address = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(bytes, bytes.size, address, 9)

            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(packet)
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
