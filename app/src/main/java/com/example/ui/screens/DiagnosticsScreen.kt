package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiagnosticItem
import com.example.data.model.DiagnosticStatus
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.components.ConnectionStatusHeader
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteCardSurface
import com.example.ui.theme.RemoteElevatedSurface
import com.example.ui.theme.RemoteEmerald
import com.example.ui.theme.RemoteError
import com.example.ui.theme.RemoteNeonGreen
import com.example.ui.theme.RemotePitchBlack
import com.example.ui.theme.RemoteTextMuted
import com.example.ui.theme.RemoteTextPrimary
import com.example.ui.theme.RemoteTextSecondary
import com.example.ui.theme.RemoteWarning

@Composable
fun DiagnosticsScreen(
    viewModel: SamsungRemoteViewModel,
    onNavigateToConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val isRunning by viewModel.isRunningDiagnostics.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RemotePitchBlack)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        ConnectionStatusHeader(
            connectionState = connectionState,
            onReconnectClick = onNavigateToConnection
        )

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = RemoteNeonGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "TV Control Diagnostics",
                        color = RemoteTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real protocol & hardware test suite",
                        color = RemoteTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = { viewModel.runAllDiagnostics() },
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RemoteNeonGreen,
                    contentColor = RemotePitchBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_run_diagnostics")
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        color = RemotePitchBlack,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Testing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Legend
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "STATUS CRITERIA KEY",
                    color = RemoteTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusLegendBadge("CONNECTED", RemoteNeonGreen)
                    StatusLegendBadge("NOT CONNECTED", RemoteTextMuted)
                    StatusLegendBadge("AUTH REQ", RemoteWarning)
                    StatusLegendBadge("UNSUPPORTED", RemoteError)
                }
            }
        }

        // Diagnostic Items List
        diagnostics.forEach { item ->
            DiagnosticCard(item)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatusLegendBadge(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DiagnosticCard(item: DiagnosticItem) {
    val (statusLabel, statusColor) = when (item.status) {
        DiagnosticStatus.CONNECTED -> "CONNECTED" to RemoteNeonGreen
        DiagnosticStatus.SUCCESS -> "SUCCESS" to RemoteNeonGreen
        DiagnosticStatus.NOT_CONNECTED -> "NOT CONNECTED" to RemoteTextMuted
        DiagnosticStatus.AUTHORIZATION_REQUIRED -> "AUTHORIZATION REQUIRED" to RemoteWarning
        DiagnosticStatus.UNSUPPORTED -> "UNSUPPORTED" to RemoteError
        DiagnosticStatus.FAILED -> "FAILED" to RemoteError
        DiagnosticStatus.TESTING -> "TESTING..." to RemoteWarning
        DiagnosticStatus.NOT_TESTED -> "NOT TESTED" to RemoteTextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RemoteBorderColor, RoundedCornerShape(14.dp))
            .testTag("diag_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = RemoteTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = item.description,
                color = RemoteTextSecondary,
                fontSize = 12.sp
            )

            if (!item.details.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RemoteElevatedSurface)
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.details,
                        color = if (item.status == DiagnosticStatus.FAILED || item.status == DiagnosticStatus.UNSUPPORTED)
                            RemoteError else RemoteEmerald,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
