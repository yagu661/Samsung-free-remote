package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RemoteKey
import com.example.data.model.TvConnectionStatus
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.components.AppShortcutsRow
import com.example.ui.components.ConnectionStatusHeader
import com.example.ui.components.DpadControl
import com.example.ui.components.PlaybackSection
import com.example.ui.components.RemoteIconButton
import com.example.ui.components.VolumeAndChannelSection
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteCardSurface
import com.example.ui.theme.RemoteElevatedSurface
import com.example.ui.theme.RemoteEmerald
import com.example.ui.theme.RemoteNeonGreen
import com.example.ui.theme.RemotePitchBlack
import com.example.ui.theme.RemotePowerRed
import com.example.ui.theme.RemoteTextMuted
import com.example.ui.theme.RemoteTextPrimary
import com.example.ui.theme.RemoteTextSecondary
import com.example.ui.theme.RemoteWarning
import kotlinx.coroutines.launch

@Composable
fun FullRemoteScreen(
    viewModel: SamsungRemoteViewModel,
    onNavigateToConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var lastErrorToast by remember { mutableStateOf<String?>(null) }
    var showNumpad by remember { mutableStateOf(false) }

    fun handleSendKey(key: RemoteKey) {
        val err = viewModel.sendKey(key)
        if (err != null) {
            lastErrorToast = err
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RemotePitchBlack)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Top Connection Status Header
        ConnectionStatusHeader(
            connectionState = connectionState,
            onReconnectClick = onNavigateToConnection
        )

        Text(
            text = "Made with love by Soumya ❤️",
            color = Color(0xFFFF80AB),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Error message banner if any
        AnimatedVisibility(visible = lastErrorToast != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RemoteWarning.copy(alpha = 0.15f))
                    .border(1.dp, RemoteWarning.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = lastErrorToast ?: "",
                    color = RemoteWarning,
                    fontSize = 12.sp
                )
            }
        }

        // Top Control Row: Power, Info, Source, Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Power Button (Standby Aware)
            RemoteIconButton(
                icon = Icons.Default.PowerSettingsNew,
                contentDescription = "Power",
                onClick = { handleSendKey(RemoteKey.KEY_POWER) },
                tint = RemotePowerRed,
                backgroundColor = RemoteElevatedSurface,
                borderColor = RemotePowerRed.copy(alpha = 0.4f),
                size = 56.dp,
                shape = CircleShape,
                testTag = "btn_power"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // TV Native Voice Trigger
                RemoteIconButton(
                    icon = Icons.Default.Mic,
                    contentDescription = "TV Voice",
                    onClick = { handleSendKey(RemoteKey.KEY_VOICE) },
                    tint = RemoteNeonGreen,
                    size = 50.dp,
                    testTag = "btn_tv_voice"
                )

                RemoteIconButton(
                    icon = Icons.Default.Dialpad,
                    contentDescription = "Keypad",
                    onClick = { showNumpad = !showNumpad },
                    tint = if (showNumpad) RemoteNeonGreen else RemoteTextSecondary,
                    size = 50.dp,
                    testTag = "btn_keypad_toggle"
                )

                RemoteIconButton(
                    icon = Icons.Default.Tv,
                    contentDescription = "Source",
                    onClick = { handleSendKey(RemoteKey.KEY_SOURCE) },
                    size = 50.dp,
                    testTag = "btn_source"
                )

                RemoteIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = { handleSendKey(RemoteKey.KEY_SETTINGS) },
                    size = 50.dp,
                    testTag = "btn_settings"
                )
            }
        }

        // Number Pad Expandable
        AnimatedVisibility(visible = showNumpad) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val numberRows = listOf(
                        listOf(RemoteKey.KEY_1, RemoteKey.KEY_2, RemoteKey.KEY_3),
                        listOf(RemoteKey.KEY_4, RemoteKey.KEY_5, RemoteKey.KEY_6),
                        listOf(RemoteKey.KEY_7, RemoteKey.KEY_8, RemoteKey.KEY_9),
                        listOf(RemoteKey.KEY_INFO, RemoteKey.KEY_0, RemoteKey.KEY_SETTINGS)
                    )

                    numberRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { numKey ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(RemoteElevatedSurface)
                                        .border(1.dp, RemoteBorderColor, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("num_${numKey.label.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.TextButton(
                                        onClick = { handleSendKey(numKey) }
                                    ) {
                                        Text(
                                            text = numKey.label,
                                            color = RemoteTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Apps Row
        AppShortcutsRow(
            onLaunchApp = { app -> viewModel.launchApp(app) }
        )

        // Navigation Row: Back, Home, Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = { handleSendKey(RemoteKey.KEY_RETURN) },
                size = 52.dp,
                testTag = "btn_back"
            )

            RemoteIconButton(
                icon = Icons.Default.Home,
                contentDescription = "Home",
                onClick = { handleSendKey(RemoteKey.KEY_HOME) },
                tint = RemoteNeonGreen,
                size = 56.dp,
                shape = CircleShape,
                testTag = "btn_home"
            )

            RemoteIconButton(
                icon = Icons.Default.Menu,
                contentDescription = "Menu",
                onClick = { handleSendKey(RemoteKey.KEY_MENU) },
                size = 50.dp,
                testTag = "btn_menu"
            )

            RemoteIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Exit",
                onClick = { handleSendKey(RemoteKey.KEY_EXIT) },
                tint = RemoteTextSecondary,
                size = 50.dp,
                testTag = "btn_exit"
            )
        }

        // Circular D-Pad Control
        DpadControl(
            onNavigate = { key -> handleSendKey(key) }
        )

        // Volume & Channel Dual Rockers + Mute
        VolumeAndChannelSection(
            onSendKey = { key -> handleSendKey(key) }
        )

        // Media Playback Bar (Play, Pause, Stop)
        PlaybackSection(
            onSendKey = { key -> handleSendKey(key) }
        )

        // Made with love by Soumya
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF4081),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Made with love by Soumya",
                color = RemoteTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
