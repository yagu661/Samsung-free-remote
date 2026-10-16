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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.TvConnectionStatus
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.ApiManagementScreen
import com.example.ui.screens.ConnectionScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.FullRemoteScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TizenInstallerScreen
import com.example.ui.screens.VoiceControlScreen
import com.example.ui.screens.YouTubeSearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteDarkSurface
import com.example.ui.theme.RemoteError
import com.example.ui.theme.RemoteNeonGreen
import com.example.ui.theme.RemotePitchBlack
import com.example.ui.theme.RemoteTextMuted
import com.example.ui.theme.RemoteTextPrimary
import com.example.ui.theme.RemoteTextSecondary
import com.example.ui.theme.RemoteWarning

enum class RemoteScreen(val title: String, val icon: ImageVector, val tag: String) {
    REMOTE("Remote", Icons.Default.Tv, "tab_remote"),
    INSTALLER("Installer", Icons.Default.Apps, "tab_installer"),
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
    val connectionState by viewModel.connectionState.collectAsState()
    val navScrollState = rememberScrollState()

    // System Back Handler: return to Remote screen if on sub-screens
    BackHandler(enabled = currentScreen != RemoteScreen.REMOTE) {
        currentScreen = RemoteScreen.REMOTE
    }

    // Auto-scroll the bottom navigation bar so the active tab is always visible
    LaunchedEffect(currentScreen) {
        val index = RemoteScreen.values().indexOf(currentScreen)
        if (index >= 0) {
            val targetOffset = (index * 95).coerceAtLeast(0)
            navScrollState.animateScrollTo(targetOffset)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(RemotePitchBlack),
        topBar = {
            Surface(
                color = RemotePitchBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .testTag("app_top_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { currentScreen = RemoteScreen.REMOTE }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RemoteNeonGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "App Icon",
                                tint = RemoteNeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Samsung Remote",
                            color = RemoteTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Quick Connection status pill
                        val (statusText, statusColor) = when (connectionState.status) {
                            TvConnectionStatus.CONNECTED -> "Online" to RemoteNeonGreen
                            TvConnectionStatus.CONNECTING -> "Connecting" to RemoteWarning
                            TvConnectionStatus.AUTHORIZATION_REQUIRED -> "Authorize" to RemoteWarning
                            TvConnectionStatus.ERROR -> "Error" to RemoteError
                            TvConnectionStatus.DISCONNECTED -> "Offline" to RemoteTextMuted
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { currentScreen = RemoteScreen.CONNECTION }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("top_status_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Direct top shortcut: Tizen Installer
                        IconButton(
                            onClick = { currentScreen = RemoteScreen.INSTALLER },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_top_shortcut_installer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = "Tizen Installer",
                                tint = if (currentScreen == RemoteScreen.INSTALLER) RemoteNeonGreen else RemoteTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Direct top shortcut: APIs Tab
                        IconButton(
                            onClick = { currentScreen = RemoteScreen.APIS },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_top_shortcut_apis")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "APIs",
                                tint = if (currentScreen == RemoteScreen.APIS) RemoteNeonGreen else RemoteTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Direct top shortcut: Settings
                        IconButton(
                            onClick = { currentScreen = RemoteScreen.SETTINGS },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_top_shortcut_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (currentScreen == RemoteScreen.SETTINGS) RemoteNeonGreen else RemoteTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 18.dp)
            ) {
                Surface(
                    color = RemoteDarkSurface,
                    tonalElevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = RemoteBorderColor,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .testTag("bottom_nav_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(navScrollState)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RemoteScreen.values().forEach { screen ->
                            val isSelected = currentScreen == screen
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) RemoteNeonGreen.copy(alpha = 0.22f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) RemoteNeonGreen else Color.Transparent,
                                        RoundedCornerShape(14.dp)
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
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION },
                        onNavigateToSettings = { currentScreen = RemoteScreen.SETTINGS },
                        onNavigateToVoice = { currentScreen = RemoteScreen.VOICE }
                    )
                }
                RemoteScreen.INSTALLER -> {
                    TizenInstallerScreen(
                        viewModel = viewModel,
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION },
                        onBackClick = { currentScreen = RemoteScreen.REMOTE }
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
                        onNavigateToConnection = { currentScreen = RemoteScreen.CONNECTION },
                        onNavigateToApis = { currentScreen = RemoteScreen.APIS },
                        onBackClick = { currentScreen = RemoteScreen.REMOTE }
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
