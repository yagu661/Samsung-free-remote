package com.example

import com.example.data.model.RemoteKey
import com.example.data.model.TvAction
import com.example.data.model.TvApp
import com.example.data.model.TvCommandAiResult
import com.example.voice.TvCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDeterministicVolumeCommands() {
        val volUp = TvCommandParser.parseDeterministic("volume up")
        assertTrue(volUp is TvAction.PressKey && volUp.key == RemoteKey.KEY_VOLUP)

        val volDown = TvCommandParser.parseDeterministic("volume down")
        assertTrue(volDown is TvAction.PressKey && volDown.key == RemoteKey.KEY_VOLDOWN)

        val mute = TvCommandParser.parseDeterministic("mute")
        assertTrue(mute is TvAction.PressKey && mute.key == RemoteKey.KEY_MUTE)

        val muteTv = TvCommandParser.parseDeterministic("mute the tv")
        assertTrue(muteTv is TvAction.PressKey && muteTv.key == RemoteKey.KEY_MUTE)
    }

    @Test
    fun testDeterministicNavigationCommands() {
        val home = TvCommandParser.parseDeterministic("go home")
        assertTrue(home is TvAction.PressKey && home.key == RemoteKey.KEY_HOME)

        val back = TvCommandParser.parseDeterministic("go back")
        assertTrue(back is TvAction.PressKey && back.key == RemoteKey.KEY_RETURN)

        val ok = TvCommandParser.parseDeterministic("press ok")
        assertTrue(ok is TvAction.PressKey && ok.key == RemoteKey.KEY_ENTER)

        val exit = TvCommandParser.parseDeterministic("exit")
        assertTrue(exit is TvAction.PressKey && exit.key == RemoteKey.KEY_EXIT)
    }

    @Test
    fun testMediaAndChannelCommands() {
        val ch = TvCommandParser.parseDeterministic("change channel")
        assertTrue(ch is TvAction.PressKey && ch.key == RemoteKey.KEY_CHUP)

        val rew = TvCommandParser.parseDeterministic("rewind")
        assertTrue(rew is TvAction.PressKey && rew.key == RemoteKey.KEY_REWIND)

        val ff = TvCommandParser.parseDeterministic("fast forward")
        assertTrue(ff is TvAction.PressKey && ff.key == RemoteKey.KEY_FF)
    }

    @Test
    fun testDeterministicAppLaunch() {
        val yt = TvCommandParser.parseDeterministic("open youtube")
        assertTrue(yt is TvAction.LaunchApp && yt.app == TvApp.YOUTUBE)

        val netflix = TvCommandParser.parseDeterministic("open netflix")
        assertTrue(netflix is TvAction.LaunchApp && netflix.app == TvApp.NETFLIX)
    }

    @Test
    fun testYouTubeSearchExtraction() {
        val action1 = TvCommandParser.parseDeterministic("search youtube for funny cat videos")
        assertTrue(action1 is TvAction.YouTubeSearch)
        assertEquals("funny cat videos", (action1 as TvAction.YouTubeSearch).query)

        val action2 = TvCommandParser.parseDeterministic("search youtube for Bengali songs")
        assertTrue(action2 is TvAction.YouTubeSearch)
        assertEquals("Bengali songs", (action2 as TvAction.YouTubeSearch).query)

        val action3 = TvCommandParser.parseDeterministic("find Minecraft videos on youtube")
        assertTrue(action3 is TvAction.YouTubeSearch)
        assertEquals("Minecraft videos", (action3 as TvAction.YouTubeSearch).query)
    }

    @Test
    fun testAiAllowlistValidation() {
        val validAiResult = TvCommandAiResult(
            intent = "PRESS_KEY",
            keyName = "KEY_VOLUP",
            explanation = "Turn up volume"
        )
        val validated = TvCommandParser.validateAndMapAiResult(validAiResult)
        assertTrue(validated is TvAction.PressKey && validated.key == RemoteKey.KEY_VOLUP)

        val rogueAiResult = TvCommandAiResult(
            intent = "PRESS_KEY",
            keyName = "KEY_DROP_DATABASE_ROGUE",
            explanation = "Invalid key"
        )
        val rejected = TvCommandParser.validateAndMapAiResult(rogueAiResult)
        assertTrue(rejected is TvAction.Unsupported)
    }
}
