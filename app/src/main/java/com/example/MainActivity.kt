package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.ApiManagementScreen
import com.example.ui.screens.ConnectionScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.FullRemoteScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceControlScreen
import com.example.ui.screens.YouTubeSearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteDarkSurface
import com.example.ui.theme.RemoteNeonGreen
import com.example.ui.theme.RemotePitchBlack
import com.example.ui.theme.RemoteTextMuted
import com.example.ui.theme.RemoteTextPrimary

enum class RemoteScreen(val title: String, val icon: ImageVector, val tag: String) {
    REMOTE("Remote", Icons.Default.Tv, "tab_remote"),
    VOICE("Voice", Icons.Default.Mic, "tab_voice"),
    YOUTUBE("YouTube", Icons.Default.PlayArrow, "tab_youtube"),
    ASSISTANT("Assistant", Icons.Default.AutoAwesome, "tab_assistant"),
    APIS("APIs", Icons.Default.Key, "tab_apis"),
    CONNECTION("Connect", Icons.Default.Wifi, "tab_connect"),
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
            Surface(
                color = RemoteDarkSurface,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteScreen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) RemoteNeonGreen.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) RemoteNeonGreen else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { currentScreen = screen }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag(screen.tag),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) RemoteNeonGreen else RemoteTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = screen.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RemoteNeonGreen else RemoteTextMuted
                                )
                            }
                        }
                    }
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
                        onNavigateToSettings = { currentScreen = RemoteScreen.APIS }
                    )
                }
                RemoteScreen.APIS -> {
                    ApiManagementScreen(
                        viewModel = viewModel,
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION }
                    )
                }
                RemoteScreen.CONNECTION -> {
                    ConnectionScreen(
                        viewModel = viewModel
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
