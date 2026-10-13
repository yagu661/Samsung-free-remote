package com.example.data.model

enum class TvConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    AUTHORIZATION_REQUIRED,
    CONNECTED,
    ERROR
}

data class TvDeviceInfo(
    val id: String = "",
    val name: String = "",
    val modelName: String = "",
    val networkType: String = "",
    val wifiMac: String = "",
    val powerState: String = "",
    val rawJson: String = ""
)

data class TvConnectionState(
    val status: TvConnectionStatus = TvConnectionStatus.DISCONNECTED,
    val ipAddress: String = "",
    val port: Int = 8002,
    val isSecure: Boolean = true,
    val token: String? = null,
    val deviceInfo: TvDeviceInfo? = null,
    val errorMessage: String? = null,
    val lastPingMs: Long = 0L
)

enum class RemoteKey(val keyName: String, val label: String, val description: String) {
    KEY_POWER("KEY_POWER", "Power", "Toggle Power (Active only when TV is On)"),
    KEY_VOLUP("KEY_VOLUP", "Vol +", "Volume Up"),
    KEY_VOLDOWN("KEY_VOLDOWN", "Vol -", "Volume Down"),
    KEY_MUTE("KEY_MUTE", "Mute", "Mute / Unmute Audio"),
    KEY_UP("KEY_UP", "Up", "Navigate Up"),
    KEY_DOWN("KEY_DOWN", "Down", "Navigate Down"),
    KEY_LEFT("KEY_LEFT", "Left", "Navigate Left"),
    KEY_RIGHT("KEY_RIGHT", "Right", "Navigate Right"),
    KEY_ENTER("KEY_ENTER", "OK", "Select / Enter"),
    KEY_RETURN("KEY_RETURN", "Back", "Back / Return"),
    KEY_HOME("KEY_HOME", "Home", "Home Screen / Smart Hub"),
    KEY_MENU("KEY_MENU", "Menu", "TV Menu"),
    KEY_CHUP("KEY_CHUP", "CH +", "Channel Up"),
    KEY_CHDOWN("KEY_CHDOWN", "CH -", "Channel Down"),
    KEY_PLAY("KEY_PLAY", "Play", "Play Media"),
    KEY_PAUSE("KEY_PAUSE", "Pause", "Pause Media"),
    KEY_STOP("KEY_STOP", "Stop", "Stop Media"),
    KEY_SETTINGS("KEY_TOOLS", "Settings", "Settings Shortcut"),
    KEY_SOURCE("KEY_SOURCE", "Source", "Input Source (HDMI/TV)"),
    KEY_INFO("KEY_INFO", "Info", "Display Current Program Info"),
    KEY_1("KEY_1", "1", "Number 1"),
    KEY_2("KEY_2", "2", "Number 2"),
    KEY_3("KEY_3", "3", "Number 3"),
    KEY_4("KEY_4", "4", "Number 4"),
    KEY_5("KEY_5", "5", "Number 5"),
    KEY_6("KEY_6", "6", "Number 6"),
    KEY_7("KEY_7", "7", "Number 7"),
    KEY_8("KEY_8", "8", "Number 8"),
    KEY_9("KEY_9", "9", "Number 9"),
    KEY_0("KEY_0", "0", "Number 0"),
    KEY_REWIND("KEY_REWIND", "Rewind", "Rewind Media"),
    KEY_FF("KEY_FF", "Fast Fwd", "Fast Forward Media"),
    KEY_EXIT("KEY_EXIT", "Exit", "Exit current screen or menu"),
    KEY_TOOLS("KEY_TOOLS", "Tools", "TV Tools Menu")
}

data class DiscoveredTv(
    val ip: String,
    val port: Int = 8002,
    val name: String = "Samsung Smart TV",
    val modelName: String = "UA32T4410",
    val id: String = "",
    val mac: String = "",
    val discoveryMethod: String = "SSDP" // "SSDP" or "Subnet Probe"
)

data class TvApp(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String
) {
    companion object {
        val YOUTUBE = TvApp("111299001912", "YouTube", "YouTube Video Player", "youtube")
        val NETFLIX = TvApp("11101200001", "Netflix", "Netflix Streaming", "netflix")
        val PRIME_VIDEO = TvApp("3201512006785", "Prime Video", "Amazon Prime Video", "prime")
        val SPOTIFY = TvApp("3201606009684", "Spotify", "Spotify Music", "spotify")
        val BROWSER = TvApp("org.tizen.browser", "Internet", "Samsung Web Browser", "browser")

        val KNOWN_APPS = listOf(YOUTUBE, NETFLIX, PRIME_VIDEO, SPOTIFY, BROWSER)
    }
}

sealed class TvAction {
    data class PressKey(val key: RemoteKey) : TvAction()
    data class AdjustVolume(val delta: Int) : TvAction() // e.g. +3 or -2
    data class LaunchApp(val app: TvApp) : TvAction()
    data class YouTubeSearch(val query: String) : TvAction()
    data class SendText(val text: String) : TvAction()
    data class ClarificationNeeded(val message: String) : TvAction()
    data class Unsupported(val reason: String) : TvAction()
}

enum class DiagnosticStatus {
    NOT_TESTED,
    TESTING,
    CONNECTED,
    NOT_CONNECTED,
    AUTHORIZATION_REQUIRED,
    UNSUPPORTED,
    SUCCESS,
    FAILED
}

data class DiagnosticItem(
    val id: String,
    val title: String,
    val description: String,
    val status: DiagnosticStatus = DiagnosticStatus.NOT_TESTED,
    val details: String? = null
)
