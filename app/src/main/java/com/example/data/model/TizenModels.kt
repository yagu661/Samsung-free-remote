package com.example.data.model

import android.graphics.Bitmap

/**
 * Metadata extracted from a Tizen .wgt package's config.xml and contents.
 */
data class TizenPackageInfo(
    val packageId: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val appId: String,
    val sizeBytes: Long,
    val fileName: String,
    val hasSignature: Boolean,
    val hasAuthorSignature: Boolean,
    val iconBitmap: Bitmap? = null
)

/**
 * Representation of an application installed on the Samsung Tizen TV.
 */
data class TizenInstalledApp(
    val packageId: String,
    val appType: String = "wgt",
    val label: String = ""
)

/**
 * State machine for the SDB installation process.
 */
sealed class TizenInstallState {
    object Idle : TizenInstallState()
    data class Validating(val message: String = "Inspecting .wgt package structure...") : TizenInstallState()
    data class Connecting(val tvIp: String) : TizenInstallState()
    data class Pushing(val bytesSent: Long, val totalBytes: Long, val progressPercent: Int) : TizenInstallState()
    data class ExecutingInstall(val progressPercent: Int = 90, val statusText: String = "Executing pkgcmd on TV...") : TizenInstallState()
    data class Success(val packageId: String, val message: String) : TizenInstallState()
    data class Failed(val title: String, val errorMessage: String, val actionableGuide: String) : TizenInstallState()
}

/**
 * Diagnostics results for TV ports and Developer Mode.
 */
data class TizenDiagnosticsResult(
    val tvIp: String,
    val phoneIp: String,
    val isPort8002Open: Boolean,     // WebSocket Remote Port
    val isSdbPort26101Open: Boolean, // Tizen Developer Mode SDB Port
    val isRestPort8001Open: Boolean, // REST System Info Port
    val isDeveloperModeActive: Boolean,
    val summary: String,
    val recommendation: String
)
