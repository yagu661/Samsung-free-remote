package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiscoveredTv
import com.example.data.model.TvConnectionStatus
import com.example.data.remote.DiscoveryState
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteCardSurface
import com.example.ui.theme.RemoteDarkSurface
import com.example.ui.theme.RemoteElevatedSurface
import com.example.ui.theme.RemoteEmerald
import com.example.ui.theme.RemoteError
import com.example.ui.theme.RemoteNeonGreen
import com.example.ui.theme.RemotePitchBlack
import com.example.ui.theme.RemotePowerRed
import com.example.ui.theme.RemoteTextMuted
import com.example.ui.theme.RemoteTextPrimary
import com.example.ui.theme.RemoteTextSecondary
import com.example.ui.theme.RemoteWarning

@Composable
fun ConnectionScreen(
    viewModel: SamsungRemoteViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveryState by viewModel.discoveryState.collectAsState()
    val discoveredTvs by viewModel.discoveredTvs.collectAsState()
    val ipInput by viewModel.ipAddressInput.collectAsState()
    val portInput by viewModel.portInput.collectAsState()
    val statusMessage by viewModel.voiceStatusMessage.collectAsState()

    var showManualSettings by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val isScanning = discoveryState is DiscoveryState.Scanning

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RemotePitchBlack)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title and TV Model Tag
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RemoteNeonGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Samsung Remote",
                        tint = RemoteNeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Samsung AI Remote",
                        color = RemoteTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target: Samsung UA32T4410AKXXL (Tizen OS)",
                        color = RemoteEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Live Connection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CURRENT CONNECTION",
                        color = RemoteTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    val (badgeText, badgeColor) = when (connectionState.status) {
                        TvConnectionStatus.CONNECTED -> "CONNECTED" to RemoteNeonGreen
                        TvConnectionStatus.CONNECTING -> "CONNECTING" to RemoteWarning
                        TvConnectionStatus.AUTHORIZATION_REQUIRED -> "ALLOW ON TV" to RemoteWarning
                        TvConnectionStatus.ERROR -> "ERROR" to RemoteError
                        TvConnectionStatus.DISCONNECTED -> "DISCONNECTED" to RemoteTextMuted
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("connection_badge")
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val currentLabel = if (connectionState.ipAddress.isNotBlank()) {
                    "${connectionState.deviceInfo?.name ?: "Samsung UA32T4410"} (${connectionState.ipAddress}:${connectionState.port})"
                } else {
                    "No TV selected yet"
                }

                Text(
                    text = currentLabel,
                    color = RemoteTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (connectionState.status == TvConnectionStatus.CONNECTED) {
                    Button(
                        onClick = { viewModel.disconnectTv() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RemotePowerRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_disconnect_active")
                    ) {
                        Text("Disconnect", fontWeight = FontWeight.Bold)
                    }
                }

                if (connectionState.status == TvConnectionStatus.AUTHORIZATION_REQUIRED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RemoteWarning.copy(alpha = 0.15f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Authorization Required",
                            tint = RemoteWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ACTION REQUIRED: Look at your Samsung TV screen and select 'Allow' using your physical remote to authorize connection.",
                            color = RemoteTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!connectionState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = connectionState.errorMessage ?: "",
                        color = if (connectionState.status == TvConnectionStatus.AUTHORIZATION_REQUIRED)
                            RemoteWarning else RemoteError,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Feature 1: Automatic TV Discovery Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, if (isScanning) RemoteNeonGreen else RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = RemoteNeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AUTOMATIC TV DISCOVERY",
                            color = RemoteNeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (isScanning) {
                        CircularProgressIndicator(
                            color = RemoteNeonGreen,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Text(
                    text = "Scans your Wi-Fi network using SSDP/UPnP multicast and subnet discovery to find your Samsung Smart TV without entering an IP address.",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                // "Find My Samsung TV" Primary Action Button
                Button(
                    onClick = { viewModel.findMySamsungTv() },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RemoteNeonGreen,
                        contentColor = RemotePitchBlack,
                        disabledContainerColor = RemoteElevatedSurface,
                        disabledContentColor = RemoteTextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_find_samsung_tv")
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScanning) "Searching Wi-Fi..." else "Find My Samsung TV",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isScanning) {
                    val progress = (discoveryState as? DiscoveryState.Scanning)?.progress ?: 0.5f
                    val msg = (discoveryState as? DiscoveryState.Scanning)?.message ?: "Searching..."
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = RemoteNeonGreen,
                            trackColor = RemoteElevatedSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = msg,
                            color = RemoteEmerald,
                            fontSize = 11.sp
                        )
                    }
                }

                // Discovered TVs List
                if (discoveredTvs.isNotEmpty()) {
                    Text(
                        text = "DISCOVERED SAMSUNG TVS (TAP TO PAIR)",
                        color = RemoteTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    discoveredTvs.forEach { tv ->
                        DiscoveredTvCard(
                            tv = tv,
                            isSelected = connectionState.ipAddress == tv.ip && connectionState.status == TvConnectionStatus.CONNECTED,
                            onSelect = { viewModel.selectDiscoveredTv(tv) }
                        )
                    }
                } else if (discoveryState is DiscoveryState.Finished && (discoveryState as DiscoveryState.Finished).discoveredTvs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RemoteElevatedSurface)
                            .border(1.dp, RemoteBorderColor, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = RemoteWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "No TV Detected Automatically",
                                    color = RemoteWarning,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "1. Ensure your Samsung TV is turned ON and connected to the same Wi-Fi.\n" +
                                        "2. Some home routers block multicast (AP Isolation). You can tap 'Find My Samsung TV' again or enter the TV IP address manually below.",
                                color = RemoteTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Expandable Manual IP Entry (Fallback)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualSettings = !showManualSettings },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MANUAL IP / ADVANCED",
                            color = RemoteTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (ipInput.isNotBlank()) "Configured IP: $ipInput:$portInput" else "Optional manual fallback",
                            color = RemoteTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = if (showManualSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Manual",
                        tint = RemoteTextSecondary
                    )
                }

                AnimatedVisibility(visible = showManualSettings) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { viewModel.onIpChanged(it) },
                            label = { Text("TV IP Address") },
                            placeholder = { Text("e.g. 192.168.1.45") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.connectTv()
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RemoteNeonGreen,
                                unfocusedBorderColor = RemoteBorderColor,
                                focusedTextColor = RemoteTextPrimary,
                                unfocusedTextColor = RemoteTextPrimary,
                                cursorColor = RemoteNeonGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tv_manual_ip_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.connectTv()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RemoteNeonGreen,
                                    contentColor = RemotePitchBlack
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_manual_connect")
                            ) {
                                Text("Connect", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.testConnection()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_manual_test")
                            ) {
                                Text("Test Ping", color = RemoteTextPrimary)
                            }
                        }

                        if (connectionState.token != null) {
                            TextButton(
                                onClick = { viewModel.clearPairingToken() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    "Reset Saved Pairing Token",
                                    color = RemoteWarning,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Instructions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = RemoteNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAMSUNG PAIRING CHECKLIST",
                        color = RemoteNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "• Samsung Galaxy M30s and TV must be on the same Wi-Fi SSID.\n" +
                            "• On first connection, Samsung TV will ask: 'Allow Samsung AI Remote to connect?'. Press Allow on your original remote.\n" +
                            "• Once allowed, the pairing token is stored permanently on your phone.\n" +
                            "• Standby note: Turn on 'Power On with Mobile' in TV Expert Network Settings to enable wake-up via magic packet.",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DiscoveredTvCard(
    tv: DiscoveredTv,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) RemoteNeonGreen.copy(alpha = 0.15f) else RemoteElevatedSurface)
            .border(
                1.dp,
                if (isSelected) RemoteNeonGreen else RemoteBorderColor,
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(14.dp)
            .testTag("discovered_tv_${tv.ip.replace(".", "_")}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = tv.name,
                    color = RemoteTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Model: ${tv.modelName}  •  IP: ${tv.ip}",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Found via ${tv.discoveryMethod}",
                    color = RemoteEmerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) RemoteEmerald else RemoteNeonGreen,
                    contentColor = RemotePitchBlack
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_pair_${tv.ip.replace(".", "_")}")
            ) {
                Text(
                    text = if (isSelected) "Connected" else "Connect & Pair",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
