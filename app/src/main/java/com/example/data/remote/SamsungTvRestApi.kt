package com.example.data.remote

import com.example.data.model.TvDeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class SamsungTvRestApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Checks if the TV's IP address and port are reachable at the TCP socket layer.
     */
    suspend fun checkSocketReachability(ip: String, port: Int, timeoutMs: Int = 2000): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Retrieves TV device metadata from http://{ip}:8001/api/v2/
     */
    suspend fun getDeviceInfo(ip: String): Result<TvDeviceInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$ip:8001/api/v2/"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("HTTP ${response.code}: ${response.message}")
                )
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val deviceObj = json.optJSONObject("device") ?: JSONObject()

            val info = TvDeviceInfo(
                id = json.optString("id", deviceObj.optString("id", "")),
                name = json.optString("name", deviceObj.optString("name", "Samsung Smart TV")),
                modelName = deviceObj.optString("modelName", "UA32T4410"),
                networkType = deviceObj.optString("networkType", "wireless"),
                wifiMac = deviceObj.optString("wifiMac", ""),
                powerState = deviceObj.optString("PowerState", "on"),
                rawJson = body
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches an application on the TV via POST http://{ip}:8001/api/v2/applications/{appId}
     */
    suspend fun launchApplication(ip: String, appId: String, payloadUrl: String? = null): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ip:8001/api/v2/applications/$appId"
                val requestBuilder = Request.Builder().url(url)

                if (payloadUrl != null) {
                    val jsonPayload = JSONObject().apply {
                        put("action", "load")
                        put("url", payloadUrl)
                    }.toString()
                    val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
                    requestBuilder.post(requestBody)
                } else {
                    val emptyBody = "".toRequestBody(null)
                    requestBuilder.post(emptyBody)
                }

                val response = client.newCall(requestBuilder.build()).execute()
                if (response.isSuccessful || response.code == 204 || response.code == 200) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: Failed to launch app $appId"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Queries application status from GET http://{ip}:8001/api/v2/applications/{appId}
     */
    suspend fun getAppStatus(ip: String, appId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$ip:8001/api/v2/applications/$appId"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val visible = json.optBoolean("visible", true)
                Result.success(visible)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
