package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TvConnectionState
import com.example.data.model.TvConnectionStatus
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteCardSurface
import com.example.ui.theme.RemoteError
import com.example.ui.theme.RemoteNeonGreen
import com.example.ui.theme.RemoteTextMuted
import com.example.ui.theme.RemoteTextSecondary
import com.example.ui.theme.RemoteWarning

@Composable
fun ConnectionStatusHeader(
    connectionState: TvConnectionState,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (connectionState.status) {
        TvConnectionStatus.CONNECTED -> RemoteNeonGreen
        TvConnectionStatus.CONNECTING -> RemoteWarning
        TvConnectionStatus.AUTHORIZATION_REQUIRED -> RemoteWarning
        TvConnectionStatus.ERROR -> RemoteError
        TvConnectionStatus.DISCONNECTED -> RemoteTextMuted
    }

    val statusText = when (connectionState.status) {
        TvConnectionStatus.CONNECTED -> "CONNECTED"
        TvConnectionStatus.CONNECTING -> "CONNECTING..."
        TvConnectionStatus.AUTHORIZATION_REQUIRED -> "ALLOW ON TV"
        TvConnectionStatus.ERROR -> "ERROR"
        TvConnectionStatus.DISCONNECTED -> "DISCONNECTED"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RemoteCardSurface)
            .border(1.dp, RemoteBorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("connection_status_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (connectionState.status == TvConnectionStatus.AUTHORIZATION_REQUIRED)
                            Icons.Default.Warning else Icons.Default.Tv,
                        contentDescription = "TV Status",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    val tvLabel = if (connectionState.ipAddress.isNotBlank()) {
                        "${connectionState.deviceInfo?.name ?: "Samsung UA32T4410"} (${connectionState.ipAddress})"
                    } else {
                        "No TV IP Configured"
                    }

                    Text(
                        text = tvLabel,
                        color = RemoteTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            if (connectionState.status != TvConnectionStatus.CONNECTED) {
                IconButton(
                    onClick = onReconnectClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("reconnect_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconnect",
                        tint = RemoteNeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
