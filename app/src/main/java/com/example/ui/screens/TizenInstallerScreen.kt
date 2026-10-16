package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TizenInstallState
import com.example.data.model.TizenInstalledApp
import com.example.data.model.TizenPackageInfo
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.components.ConnectionStatusHeader
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
fun TizenInstallerScreen(
    viewModel: SamsungRemoteViewModel,
    onNavigateToConnection: () -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val packageInfo by viewModel.tizenPackageInfo.collectAsState()
    val installState by viewModel.tizenInstallState.collectAsState()
    val tizenLogs by viewModel.tizenLogs.collectAsState()
    val installedApps by viewModel.installedTizenApps.collectAsState()
    val isAppsLoading by viewModel.isTizenAppsLoading.collectAsState()
    val diagnosticsResult by viewModel.tizenDiagnostics.collectAsState()
    val isRunningDiagnostics by viewModel.isRunningTizenDiagnostics.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showInstallConfirmDialog by remember { mutableStateOf(false) }
    var appToUninstall by remember { mutableStateOf<TizenInstalledApp?>(null) }

    // SAF Document Picker for .wgt files
    val wgtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectWgtUri(uri, context)
        }
    }

    // Auto-scroll terminal log
    val logListState = rememberLazyListState()
    LaunchedEffect(tizenLogs.size) {
        if (tizenLogs.isNotEmpty()) {
            logListState.animateScrollToItem(tizenLogs.size - 1)
        }
    }

    // Load initial built-in package if available
    LaunchedEffect(Unit) {
        if (packageInfo == null) {
            viewModel.loadDefaultBuiltInWgt(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RemotePitchBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Top Navigation Bar: Back & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBackClick() }
                    .background(RemoteElevatedSurface)
                    .border(1.dp, RemoteBorderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_installer_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = RemoteNeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remote", color = RemoteNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = RemoteNeonGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tizen TV SDB Installer",
                    color = RemoteTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        ConnectionStatusHeader(
            connectionState = connectionState,
            onReconnectClick = onNavigateToConnection
        )

        // Sub Tabs: 1. Install WGT | 2. Apps Manager | 3. Diagnostics & Guide
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = RemoteCardSurface,
            contentColor = RemoteNeonGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = RemoteNeonGreen
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Install .WGT", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = {
                    selectedTabIndex = 1
                    if (installedApps.isEmpty()) viewModel.loadInstalledTizenApps()
                },
                text = { Text("App Manager", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = {
                    selectedTabIndex = 2
                    if (diagnosticsResult == null) viewModel.runTizenSdbDiagnostics()
                },
                text = { Text("Diagnostics & Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> InstallWgtTab(
                packageInfo = packageInfo,
                installState = installState,
                logs = tizenLogs,
                logListState = logListState,
                onPickWgt = { wgtPickerLauncher.launch(arrayOf("*/*")) },
                onUseBuiltIn = { viewModel.loadDefaultBuiltInWgt(context) },
                onStartInstall = { showInstallConfirmDialog = true },
                onClearLogs = { viewModel.clearTizenLogs() }
            )
            1 -> InstalledAppsTab(
                installedApps = installedApps,
                isLoading = isAppsLoading,
                onRefresh = { viewModel.loadInstalledTizenApps() },
                onLaunch = { appId -> viewModel.launchTizenApp(appId) },
                onStop = { appId -> viewModel.stopTizenApp(appId) },
                onUninstallClick = { app -> appToUninstall = app }
            )
            2 -> DiagnosticsAndGuideTab(
                diagnostics = diagnosticsResult,
                isLoading = isRunningDiagnostics,
                onRunDiagnostics = { viewModel.runTizenSdbDiagnostics() }
            )
        }
    }

    // Confirmation Dialog before Installing
    if (showInstallConfirmDialog && packageInfo != null) {
        AlertDialog(
            onDismissRequest = { showInstallConfirmDialog = false },
            title = { Text("Confirm Tizen Installation", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Install '${packageInfo?.name}' (ID: ${packageInfo?.packageId}, Version: ${packageInfo?.version}) directly onto your Samsung Smart TV via SDB Port 26101?\n\nMake sure your TV is turned ON and Developer Mode is enabled."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInstallConfirmDialog = false
                        viewModel.startTizenInstallation(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemoteNeonGreen, contentColor = RemotePitchBlack)
                ) {
                    Text("Install Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallConfirmDialog = false }) {
                    Text("Cancel", color = RemoteTextSecondary)
                }
            },
            containerColor = RemoteCardSurface,
            titleContentColor = RemoteTextPrimary,
            textContentColor = RemoteTextSecondary
        )
    }

    // Confirmation Dialog before Uninstalling
    appToUninstall?.let { app ->
        AlertDialog(
            onDismissRequest = { appToUninstall = null },
            title = { Text("Confirm App Uninstall", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to uninstall package '${app.packageId}' from your Samsung Smart TV?\n\nThis will remove the application from your TV Apps bar.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.uninstallTizenApp(app.packageId)
                        appToUninstall = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RemotePowerRed, contentColor = Color.White)
                ) {
                    Text("Uninstall", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { appToUninstall = null }) {
                    Text("Cancel", color = RemoteTextSecondary)
                }
            },
            containerColor = RemoteCardSurface,
            titleContentColor = RemoteTextPrimary,
            textContentColor = RemoteTextSecondary
        )
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: INSTALL .WGT PACKAGE
// -------------------------------------------------------------------------------------------------

@Composable
private fun InstallWgtTab(
    packageInfo: TizenPackageInfo?,
    installState: TizenInstallState,
    logs: List<String>,
    logListState: androidx.compose.foundation.lazy.LazyListState,
    onPickWgt: () -> Unit,
    onUseBuiltIn: () -> Unit,
    onStartInstall: () -> Unit,
    onClearLogs: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Step 1: Package Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. SELECT TIZEN APPLICATION (.WGT)",
                    color = RemoteNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPickWgt,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RemoteElevatedSurface,
                            contentColor = RemoteNeonGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("btn_select_wgt_file")
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select .WGT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onUseBuiltIn,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f).testTag("btn_use_builtin_wgt")
                    ) {
                        Text("Load Assistant.wgt", fontSize = 11.sp, color = RemoteTextPrimary)
                    }
                }

                // Inspected Package Details Card
                if (packageInfo != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RemoteDarkSurface)
                            .border(1.dp, RemoteNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (packageInfo.iconBitmap != null) {
                                Image(
                                    bitmap = packageInfo.iconBitmap.asImageBitmap(),
                                    contentDescription = "App Icon",
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(RemoteNeonGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Tv, contentDescription = null, tint = RemoteNeonGreen, modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = packageInfo.name,
                                    color = RemoteTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ID: ${packageInfo.packageId} (v${packageInfo.version})",
                                    color = RemoteTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Size: ${String.format("%.1f", packageInfo.sizeBytes / 1024.0)} KB • ${if (packageInfo.hasAuthorSignature) "✓ Signed" else "Debug Package"}",
                                    color = if (packageInfo.hasAuthorSignature) RemoteEmerald else RemoteWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No .wgt file selected. Choose a package from your phone's storage or load SamsungAssistant.wgt.",
                        color = RemoteTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Step 2: Install Action & Progress
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteNeonGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "2. WIRELESS SDB INSTALLATION",
                    color = RemoteNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                val isInstalling = installState is TizenInstallState.Connecting ||
                        installState is TizenInstallState.Pushing ||
                        installState is TizenInstallState.ExecutingInstall

                Button(
                    onClick = onStartInstall,
                    enabled = packageInfo != null && !isInstalling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RemoteNeonGreen,
                        contentColor = RemotePitchBlack,
                        disabledContainerColor = RemoteElevatedSurface,
                        disabledContentColor = RemoteTextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_trigger_sdb_install")
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = RemotePitchBlack, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Installing to TV...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install Application to TV", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Progress Bar
                when (installState) {
                    is TizenInstallState.Pushing -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transferring file to TV...", color = RemoteNeonGreen, fontSize = 11.sp)
                                Text("${installState.progressPercent}%", color = RemoteNeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { installState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = RemoteNeonGreen,
                                trackColor = RemoteElevatedSurface,
                            )
                        }
                    }
                    is TizenInstallState.ExecutingInstall -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(installState.statusText, color = RemoteNeonGreen, fontSize = 11.sp)
                                Text("${installState.progressPercent}%", color = RemoteNeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { installState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = RemoteNeonGreen,
                                trackColor = RemoteElevatedSurface,
                            )
                        }
                    }
                    is TizenInstallState.Success -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RemoteEmerald.copy(alpha = 0.15f))
                                .border(1.dp, RemoteEmerald, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "✓ ${installState.message}",
                                color = RemoteEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is TizenInstallState.Failed -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RemotePowerRed.copy(alpha = 0.15f))
                                .border(1.dp, RemotePowerRed, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("✗ ${installState.title}: ${installState.errorMessage}", color = RemotePowerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(installState.actionableGuide, color = RemoteTextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Step 3: Real-Time SDB Terminal Logs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemotePitchBlack)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RemoteNeonGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SDB Live Terminal", color = RemoteTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = onClearLogs) {
                        Text("Clear", color = RemoteTextMuted, fontSize = 10.sp)
                    }
                }

                HorizontalDivider(color = RemoteBorderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    state = logListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs) { logLine ->
                        val color = when {
                            logLine.contains("✓") || logLine.contains("SUCCESS") -> RemoteNeonGreen
                            logLine.contains("ERROR") || logLine.contains("✗") -> RemotePowerRed
                            logLine.contains("[TV]") -> RemoteEmerald
                            else -> RemoteTextSecondary
                        }
                        Text(
                            text = logLine,
                            color = color,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: INSTALLED APPS MANAGER
// -------------------------------------------------------------------------------------------------

@Composable
private fun InstalledAppsTab(
    installedApps: List<TizenInstalledApp>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onLaunch: (String) -> Unit,
    onStop: (String) -> Unit,
    onUninstallClick: (TizenInstalledApp) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Installed Apps (${installedApps.size})",
                color = RemoteTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RemoteElevatedSurface)
                    .size(36.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RemoteNeonGreen, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = RemoteNeonGreen, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (isLoading && installedApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = RemoteNeonGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Querying TV packages via SDB...", color = RemoteTextSecondary, fontSize = 12.sp)
                }
            }
        } else if (installedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = RemoteTextMuted, modifier = Modifier.size(40.dp))
                    Text("No installed apps retrieved yet.", color = RemoteTextSecondary, fontSize = 12.sp)
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = RemoteElevatedSurface, contentColor = RemoteNeonGreen)
                    ) {
                        Text("Scan TV Packages (pkgcmd -l)")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(installedApps) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RemoteBorderColor, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label.ifBlank { app.packageId },
                                    color = RemoteTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Package: ${app.packageId}",
                                    color = RemoteTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Launch App
                                IconButton(
                                    onClick = { onLaunch(app.label.ifBlank { app.packageId }) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(RemoteElevatedSurface)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Launch", tint = RemoteNeonGreen, modifier = Modifier.size(18.dp))
                                }

                                // Stop App
                                IconButton(
                                    onClick = { onStop(app.label.ifBlank { app.packageId }) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(RemoteElevatedSurface)
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = RemoteWarning, modifier = Modifier.size(16.dp))
                                }

                                // Uninstall App
                                IconButton(
                                    onClick = { onUninstallClick(app) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(RemoteElevatedSurface)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Uninstall", tint = RemotePowerRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 3: DIAGNOSTICS & SETUP GUIDE
// -------------------------------------------------------------------------------------------------

@Composable
private fun DiagnosticsAndGuideTab(
    diagnostics: com.example.data.model.TizenDiagnosticsResult?,
    isLoading: Boolean,
    onRunDiagnostics: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Run Diagnostic Test Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Connection Diagnostics", color = RemoteNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onRunDiagnostics,
                        colors = ButtonDefaults.buttonColors(containerColor = RemoteElevatedSurface, contentColor = RemoteNeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = RemoteNeonGreen, strokeWidth = 2.dp)
                        } else {
                            Text("Test Ports", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (diagnostics != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DiagnosticStatusRow(
                            label = "SDB Port 26101 (Developer Mode)",
                            isOpen = diagnostics.isSdbPort26101Open,
                            hint = if (diagnostics.isSdbPort26101Open) "Active & Ready" else "Closed / Developer Mode OFF"
                        )
                        DiagnosticStatusRow(
                            label = "WebSocket Port 8002 (Remote Control)",
                            isOpen = diagnostics.isPort8002Open,
                            hint = if (diagnostics.isPort8002Open) "Reachable" else "Offline"
                        )
                        DiagnosticStatusRow(
                            label = "REST Port 8001 (Device Info)",
                            isOpen = diagnostics.isRestPort8001Open,
                            hint = if (diagnostics.isRestPort8001Open) "Reachable" else "Offline"
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RemoteDarkSurface)
                                .padding(10.dp)
                        ) {
                            Text(diagnostics.recommendation, color = RemoteTextPrimary, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                } else {
                    Text("Tap 'Test Ports' to check if your TV Developer Mode SDB port (26101) is accessible.", color = RemoteTextMuted, fontSize = 11.sp)
                }
            }
        }

        // Step-by-Step Guide for Samsung UA32T4410AKXXL
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📺 DEVELOPER MODE SETUP (SAMSUNG T4410 / TIZEN 5.5)",
                    color = RemoteNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Follow these 4 quick steps once to allow your Galaxy M30s to install apps onto your TV:\n\n" +
                            "1. Open Apps on TV: Press Home on the Samsung TV remote, then open 'APPS'.\n" +
                            "2. Enter PIN: Press 1, 2, 3, 4, 5 on your remote (or use the on-screen number pad).\n" +
                            "3. Toggle Developer Mode: Set Developer Mode to ON, and enter your Phone's Wi-Fi IP as 'Host PC IP'.\n" +
                            "4. Cold Reboot TV: Press and HOLD the Power button on the TV remote for 5 seconds until the Samsung logo appears.\n\n" +
                            "Once rebooted, the TV opens port 26101, and you can install any .wgt file wirelessly from this app!",
                    color = RemoteTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // Security & Certificates Transparency
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = RemoteEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Security & Privacy", color = RemoteEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "• Zero Cloud Intermediaries: All SDB communication occurs strictly on your local home Wi-Fi directly from your Galaxy M30s to your TV.\n" +
                            "• No Wi-Fi Passwords: The app connects through your existing home Wi-Fi network and never asks for or stores your Wi-Fi password.\n" +
                            "• Developer Certificates: Samsung Smart TVs require developer mode and author signatures to safeguard against unauthorized package execution. We never bypass device security.",
                    color = RemoteTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DiagnosticStatusRow(label: String, isOpen: Boolean, hint: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = RemoteTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(hint, color = if (isOpen) RemoteEmerald else RemotePowerRed, fontSize = 10.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isOpen) RemoteEmerald.copy(alpha = 0.2f) else RemotePowerRed.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (isOpen) "OPEN" else "CLOSED",
                color = if (isOpen) RemoteEmerald else RemotePowerRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
