package com.example.voice

import com.example.data.model.RemoteKey
import com.example.data.model.TvAction
import com.example.data.model.TvApp
import com.example.data.model.TvCommandAiResult

object TvCommandParser {

    /**
     * Deterministic, zero-latency parsing for exact or canonical voice commands.
     */
    fun parseDeterministic(rawInput: String): TvAction? {
        val cleanRaw = rawInput.trim().removeSuffix(".").removeSuffix("?").removeSuffix("!").trim()
        val text = cleanRaw.lowercase()

        // Volume Commands
        if (text == "volume up" || text == "vol up" || text == "increase volume" || text == "louder" ||
            text == "turn up volume" || text == "turn up the volume" || text == "turn volume up") {
            return TvAction.PressKey(RemoteKey.KEY_VOLUP)
        }
        if (text == "volume down" || text == "vol down" || text == "decrease volume" || text == "lower volume" ||
            text == "quieter" || text == "turn down volume" || text == "turn down the volume" || text == "turn volume down") {
            return TvAction.PressKey(RemoteKey.KEY_VOLDOWN)
        }
        if (text == "mute" || text == "unmute" || text == "mute tv" || text == "mute the tv" ||
            text == "silence" || text == "unmute tv" || text == "unmute the tv") {
            return TvAction.PressKey(RemoteKey.KEY_MUTE)
        }

        // Navigation Commands
        if (text == "go home" || text == "home" || text == "open home" || text == "smart hub" ||
            text == "go to home" || text == "home screen") {
            return TvAction.PressKey(RemoteKey.KEY_HOME)
        }
        if (text == "go back" || text == "back" || text == "return") {
            return TvAction.PressKey(RemoteKey.KEY_RETURN)
        }
        if (text == "move up" || text == "up" || text == "go up") {
            return TvAction.PressKey(RemoteKey.KEY_UP)
        }
        if (text == "move down" || text == "down" || text == "go down") {
            return TvAction.PressKey(RemoteKey.KEY_DOWN)
        }
        if (text == "move left" || text == "left" || text == "go left") {
            return TvAction.PressKey(RemoteKey.KEY_LEFT)
        }
        if (text == "move right" || text == "right" || text == "go right") {
            return TvAction.PressKey(RemoteKey.KEY_RIGHT)
        }
        if (text == "press ok" || text == "ok" || text == "enter" || text == "select" || text == "press enter") {
            return TvAction.PressKey(RemoteKey.KEY_ENTER)
        }
        if (text == "menu" || text == "open menu") {
            return TvAction.PressKey(RemoteKey.KEY_MENU)
        }
        if (text == "settings" || text == "open settings") {
            return TvAction.PressKey(RemoteKey.KEY_SETTINGS)
        }

        // Channels
        if (text == "channel up" || text == "next channel" || text == "change channel") {
            return TvAction.PressKey(RemoteKey.KEY_CHUP)
        }
        if (text == "channel down" || text == "previous channel") {
            return TvAction.PressKey(RemoteKey.KEY_CHDOWN)
        }

        // Direct Number Channel
        for (i in 0..9) {
            if (text == "channel $i" || text == "number $i") {
                val key = RemoteKey.values().firstOrNull { it.label == i.toString() }
                if (key != null) return TvAction.PressKey(key)
            }
        }

        // Media controls
        if (text == "play" || text == "resume" || text == "play video") {
            return TvAction.PressKey(RemoteKey.KEY_PLAY)
        }
        if (text == "pause" || text == "pause video") {
            return TvAction.PressKey(RemoteKey.KEY_PAUSE)
        }
        if (text == "stop" || text == "stop video") {
            return TvAction.PressKey(RemoteKey.KEY_STOP)
        }
        if (text == "rewind" || text == "rewind video" || text == "scan back") {
            return TvAction.PressKey(RemoteKey.KEY_REWIND)
        }
        if (text == "fast forward" || text == "fast fwd" || text == "forward" || text == "fast forward video") {
            return TvAction.PressKey(RemoteKey.KEY_FF)
        }
        if (text == "exit" || text == "exit app" || text == "close app") {
            return TvAction.PressKey(RemoteKey.KEY_EXIT)
        }

        // Power
        if (text == "power" || text == "turn off tv" || text == "power off") {
            return TvAction.PressKey(RemoteKey.KEY_POWER)
        }

        // Native Voice & Search
        if (text == "voice" || text == "voice button" || text == "voice search" || text == "trigger voice" || text == "open voice" || text == "bixby" || text == "tv mic") {
            return TvAction.PressKey(RemoteKey.KEY_VOICE)
        }
        if (text == "search" || text == "tv search" || text == "open search") {
            return TvAction.PressKey(RemoteKey.KEY_SEARCH)
        }

        // App Launch
        if (text == "open youtube" || text == "launch youtube" || text == "start youtube") {
            return TvAction.LaunchApp(TvApp.YOUTUBE)
        }
        if (text == "open netflix" || text == "launch netflix" || text == "start netflix") {
            return TvAction.LaunchApp(TvApp.NETFLIX)
        }
        if (text == "open spotify" || text == "launch spotify") {
            return TvAction.LaunchApp(TvApp.SPOTIFY)
        }
        if (text == "open prime video" || text == "open prime") {
            return TvAction.LaunchApp(TvApp.PRIME_VIDEO)
        }
        if (text == "open browser" || text == "open internet") {
            return TvAction.LaunchApp(TvApp.BROWSER)
        }

        // YouTube Search patterns:
        // "search youtube for <query>"
        // "find <query> on youtube"
        // "look up <query> on youtube"
        val trimmedRaw = cleanRaw
        val searchPrefixes = listOf(
            "search youtube for ",
            "search youtube ",
            "find on youtube ",
            "play on youtube ",
            "search on youtube for ",
            "search on youtube "
        )
        for (prefix in searchPrefixes) {
            if (text.startsWith(prefix)) {
                val query = trimmedRaw.substring(prefix.length).trim()
                if (query.isNotEmpty()) {
                    return TvAction.YouTubeSearch(query)
                }
            }
        }

        // "find cartoons on youtube" -> suffix pattern
        if (text.startsWith("find ") && text.endsWith(" on youtube")) {
            val query = trimmedRaw.substring(5, trimmedRaw.length - 11).trim()
            if (query.isNotEmpty()) {
                return TvAction.YouTubeSearch(query)
            }
        }
        if (text.startsWith("search ") && text.endsWith(" on youtube")) {
            val query = trimmedRaw.substring(7, trimmedRaw.length - 11).trim()
            if (query.isNotEmpty()) {
                return TvAction.YouTubeSearch(query)
            }
        }

        return null
    }

    /**
     * Validates AI response strictly against predefined allowlists.
     * Never executes arbitrary actions.
     */
    fun validateAndMapAiResult(result: TvCommandAiResult): TvAction {
        return when (result.intent.uppercase()) {
            "PRESS_KEY" -> {
                val keyName = result.keyName ?: return TvAction.Unsupported("No key specified by AI")
                val matchedKey = RemoteKey.values().firstOrNull { it.keyName.equals(keyName, ignoreCase = true) }
                if (matchedKey != null) {
                    TvAction.PressKey(matchedKey)
                } else {
                    TvAction.Unsupported("Key '$keyName' is not in TV key allowlist")
                }
            }
            "ADJUST_VOLUME" -> {
                val delta = result.volumeDelta ?: 1
                val bounded = delta.coerceIn(-15, 15)
                TvAction.AdjustVolume(bounded)
            }
            "LAUNCH_APP" -> {
                val appId = result.appId
                val matchedApp = TvApp.KNOWN_APPS.firstOrNull {
                    it.id == appId || it.name.equals(result.appName, ignoreCase = true)
                }
                if (matchedApp != null) {
                    TvAction.LaunchApp(matchedApp)
                } else {
                    TvAction.Unsupported("App '${result.appName ?: appId}' is not in allowed TV apps")
                }
            }
            "SEARCH_YOUTUBE" -> {
                val query = result.searchQuery
                if (!query.isNullOrBlank()) {
                    TvAction.YouTubeSearch(query.trim())
                } else {
                    TvAction.ClarificationNeeded("What would you like to search on YouTube?")
                }
            }
            "CLARIFY" -> {
                TvAction.ClarificationNeeded(result.explanation.ifBlank { "Could you please clarify your command?" })
            }
            else -> {
                TvAction.Unsupported(result.explanation.ifBlank { "This command is not supported on Samsung Smart TV" })
            }
        }
    }
}
