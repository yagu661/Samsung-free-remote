package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.ConnectionScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.FullRemoteScreen
import com.example.ui.screens.VoiceControlScreen
import com.example.ui.screens.YouTubeSearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteCardSurface
import com.example.ui.theme.RemoteDarkSurface
import com.example.ui.theme.RemoteEmerald
import com.example.ui.theme.RemoteNeonGreen
import com.example.ui.theme.RemotePitchBlack
import com.example.ui.theme.RemoteTextMuted
import com.example.ui.theme.RemoteTextPrimary

import com.example.ui.screens.SettingsScreen
import androidx.compose.material.icons.filled.Settings

enum class RemoteScreen(val title: String, val icon: ImageVector, val tag: String) {
    CONNECTION("Connect", Icons.Default.Wifi, "tab_connect"),
    REMOTE("Remote", Icons.Default.Tv, "tab_remote"),
    VOICE("Voice", Icons.Default.Mic, "tab_voice"),
    YOUTUBE("YouTube", Icons.Default.PlayArrow, "tab_youtube"),
    ASSISTANT("Assistant", Icons.Default.AutoAwesome, "tab_assistant"),
    SETTINGS("Settings", Icons.Default.Settings, "tab_settings"),
    DIAGNOSTICS("Diagnostics", Icons.Default.Build, "tab_diagnostics")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SamsungRemoteApp()
            }
        }
    }
}

@Composable
fun SamsungRemoteApp(viewModel: SamsungRemoteViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(RemoteScreen.REMOTE) }

    // System Back Handler: return to Remote screen if on sub-screens
    BackHandler(enabled = currentScreen != RemoteScreen.REMOTE) {
        currentScreen = RemoteScreen.REMOTE
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(RemotePitchBlack),
        bottomBar = {
            NavigationBar(
                containerColor = RemoteDarkSurface,
                contentColor = RemoteTextPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .border(
                        width = 1.dp,
                        color = RemoteBorderColor,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .testTag("bottom_nav_bar")
            ) {
                RemoteScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RemotePitchBlack,
                            selectedTextColor = RemoteNeonGreen,
                            indicatorColor = RemoteNeonGreen,
                            unselectedIconColor = RemoteTextMuted,
                            unselectedTextColor = RemoteTextMuted
                        ),
                        modifier = Modifier.testTag(screen.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(RemotePitchBlack)
        ) {
            when (currentScreen) {
                RemoteScreen.CONNECTION -> {
                    ConnectionScreen(
                        viewModel = viewModel
                    )
                }
                RemoteScreen.REMOTE -> {
                    FullRemoteScreen(
                        viewModel = viewModel,
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION }
                    )
                }
                RemoteScreen.VOICE -> {
                    VoiceControlScreen(
                        viewModel = viewModel,
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION }
                    )
                }
                RemoteScreen.YOUTUBE -> {
                    YouTubeSearchScreen(
                        viewModel = viewModel,
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION }
                    )
                }
                RemoteScreen.ASSISTANT -> {
                    AiAssistantScreen(
                        viewModel = viewModel,
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION },
                        onNavigateToSettings = { currentScreen = RemoteScreen.SETTINGS }
                    )
                }
                RemoteScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION }
                    )
                }
                RemoteScreen.DIAGNOSTICS -> {
                    DiagnosticsScreen(
                        viewModel = viewModel,
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION }
                    )
                }
            }
        }
    }
}
