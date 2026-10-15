package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.components.ConnectionStatusHeader
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteCardSurface
import com.example.ui.theme.RemoteDarkSurface
import com.example.ui.theme.RemoteElevatedSurface
import com.example.ui.theme.RemoteEmerald
import com.example.ui.theme.RemoteInfo
import com.example.ui.theme.RemoteNeonGreen
import com.example.ui.theme.RemotePitchBlack
import com.example.ui.theme.RemotePowerRed
import com.example.ui.theme.RemoteTextMuted
import com.example.ui.theme.RemoteTextPrimary
import com.example.ui.theme.RemoteTextSecondary
import com.example.ui.theme.RemoteWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiManagementScreen(
    viewModel: SamsungRemoteViewModel,
    onNavigateToConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val tvServerState by viewModel.tvServerState.collectAsState()

    val customGeminiApiKey by viewModel.customGeminiApiKey.collectAsState()
    val groqApiKey by viewModel.groqApiKey.collectAsState()
    val cerebrasApiKey by viewModel.cerebrasApiKey.collectAsState()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsState()
    val openRouterModel by viewModel.openRouterModel.collectAsState()
    val mistralApiKey by viewModel.mistralApiKey.collectAsState()
    val cloudflareApiKey by viewModel.cloudflareApiKey.collectAsState()
    val cloudflareAccountId by viewModel.cloudflareAccountId.collectAsState()
    val enableFallback by viewModel.enableMultiProviderFallback.collectAsState()

    // Local editing state so user can type freely before saving
    var geminiInput by remember(customGeminiApiKey) { mutableStateOf(customGeminiApiKey) }
    var groqInput by remember(groqApiKey) { mutableStateOf(groqApiKey) }
    var cerebrasInput by remember(cerebrasApiKey) { mutableStateOf(cerebrasApiKey) }
    var openRouterInput by remember(openRouterApiKey) { mutableStateOf(openRouterApiKey) }
    var mistralInput by remember(mistralApiKey) { mutableStateOf(mistralApiKey) }
    var cfKeyInput by remember(cloudflareApiKey) { mutableStateOf(cloudflareApiKey) }
    var cfAccountInput by remember(cloudflareAccountId) { mutableStateOf(cloudflareAccountId) }

    // Test status feedback per provider
    val testStatusMap = remember { mutableStateMapOf<String, String>() }
    val isTestingMap = remember { mutableStateMapOf<String, Boolean>() }

    // Visibility toggles
    var showGemini by remember { mutableStateOf(false) }
    var showGroq by remember { mutableStateOf(false) }
    var showCerebras by remember { mutableStateOf(false) }
    var showOpenRouter by remember { mutableStateOf(false) }
    var showMistral by remember { mutableStateOf(false) }
    var showCloudflare by remember { mutableStateOf(false) }

    // Count configured
    val configuredCount = listOf(
        geminiInput.isNotBlank(),
        groqInput.isNotBlank(),
        cerebrasInput.isNotBlank(),
        openRouterInput.isNotBlank(),
        mistralInput.isNotBlank(),
        (cfKeyInput.isNotBlank() && cfAccountInput.isNotBlank())
    ).count { it }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open browser for $url", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveAll() {
        viewModel.setCustomGeminiApiKey(geminiInput)
        viewModel.setGroqApiKey(groqInput)
        viewModel.setCerebrasApiKey(cerebrasInput)
        viewModel.setOpenRouterApiKey(openRouterInput)
        viewModel.setMistralApiKey(mistralInput)
        viewModel.setCloudflareApiKey(cfKeyInput)
        viewModel.setCloudflareAccountId(cfAccountInput)
        Toast.makeText(context, "✅ All AI API keys saved successfully!", Toast.LENGTH_SHORT).show()
    }

    fun testProvider(providerKey: String) {
        saveAll()
        isTestingMap[providerKey] = true
        testStatusMap[providerKey] = "Testing connection..."
        viewModel.testAiProvider(providerKey) { success, message ->
            isTestingMap[providerKey] = false
            testStatusMap[providerKey] = if (success) "🟢 $message" else "🔴 $message"
        }
    }

    fun copyTermuxScript() {
        saveAll()
        val script = viewModel.generateTermuxExportScript()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Termux AI Environment Export", script)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "📋 Export script copied! Paste directly into Termux.", Toast.LENGTH_LONG).show()
    }

    fun copyToClipboard(label: String, text: String, successMessage: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RemotePitchBlack)
    ) {
        ConnectionStatusHeader(
            connectionState = connectionState,
            onReconnectClick = onNavigateToConnection
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Hero Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = RemoteCardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp))
                    .testTag("api_hub_header_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(RemoteNeonGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "API Hub",
                                    tint = RemoteNeonGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AI API Hub & Fallback Engine",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RemoteTextPrimary
                                )
                                Text(
                                    text = "Ultra-resilient multi-provider failover chain",
                                    fontSize = 12.sp,
                                    color = RemoteTextSecondary
                                )
                            }
                        }

                        // Configured badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (configuredCount > 0) RemoteEmerald.copy(alpha = 0.2f)
                                    else RemoteWarning.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    if (configuredCount > 0) RemoteEmerald else RemoteWarning,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$configuredCount/6 Configured",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (configuredCount > 0) RemoteNeonGreen else RemoteWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Add your free API keys below. The remote app and Python engine cycle automatically through all 6 tiers: Google Gemini → Groq → Cerebras → OpenRouter → Mistral → Cloudflare. If one hits rate limits (429) or errors, the next tier responds instantly without failing!",
                        fontSize = 12.sp,
                        color = RemoteTextMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-failover switch row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RemoteDarkSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Failover Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RemoteTextPrimary
                            )
                            Text(
                                text = "Auto-retry next tier on rate limits (429) or timeouts",
                                fontSize = 11.sp,
                                color = RemoteTextMuted
                            )
                        }
                        Switch(
                            checked = enableFallback,
                            onCheckedChange = { viewModel.setEnableMultiProviderFallback(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RemotePitchBlack,
                                checkedTrackColor = RemoteNeonGreen,
                                uncheckedThumbColor = RemoteTextMuted,
                                uncheckedTrackColor = RemoteCardSurface
                            ),
                            modifier = Modifier.testTag("switch_auto_failover")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { saveAll() },
                            colors = ButtonDefaults.buttonColors(containerColor = RemoteNeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_all_keys")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = RemotePitchBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save All Keys",
                                color = RemotePitchBlack,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { copyTermuxScript() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RemoteNeonGreen),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(RemoteEmerald)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("btn_copy_termux_export")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = RemoteNeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export for Termux",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // LOCAL SERVER WI-FI SYNC & BROADCAST CARD
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = RemoteCardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (tvServerState.isRunning) RemoteNeonGreen else RemoteBorderColor,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("card_local_server_sync")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (tvServerState.isRunning) RemoteNeonGreen.copy(alpha = 0.2f)
                                        else RemoteTextMuted.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = "Server Sync",
                                    tint = if (tvServerState.isRunning) RemoteNeonGreen else RemoteTextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Local Server Wi-Fi Sync",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RemoteTextPrimary
                                )
                                Text(
                                    text = if (tvServerState.isRunning) "Broadcasting on ${tvServerState.serverUrl}"
                                    else "Send & serve APIs over local network",
                                    fontSize = 11.sp,
                                    color = if (tvServerState.isRunning) RemoteEmerald else RemoteTextMuted
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.toggleTvServer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tvServerState.isRunning) RemotePowerRed else RemoteNeonGreen,
                                contentColor = if (tvServerState.isRunning) androidx.compose.ui.graphics.Color.White else RemotePitchBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_toggle_api_server")
                        ) {
                            Text(
                                text = if (tvServerState.isRunning) "Stop Server" else "Start Server",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (tvServerState.isRunning) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Endpoints & Quick-Actions Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RemoteDarkSurface)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Endpoint 1: JSON Keys API
                            Column {
                                Text(
                                    text = "1. JSON API Endpoint (For TV & Custom Apps):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RemoteTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RemoteElevatedSurface)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${tvServerState.serverUrl}/api/keys",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = RemoteNeonGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                copyToClipboard("API Keys Endpoint", "${tvServerState.serverUrl}/api/keys", "Copied /api/keys URL!")
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy URL",
                                                tint = RemoteTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { openUrl("${tvServerState.serverUrl}/api/keys") },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = "Open in browser",
                                                tint = RemoteTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Endpoint 2: Termux / Bash 1-Liner
                            Column {
                                Text(
                                    text = "2. Termux 1-Liner (Run on Galaxy M30s / PC):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RemoteTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RemoteElevatedSurface)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "eval $(curl -s ${tvServerState.serverUrl}/env.sh)",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = RemoteInfo,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            copyToClipboard("Termux Command", "eval \$(curl -s ${tvServerState.serverUrl}/env.sh)", "Copied Termux curl command!")
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Command",
                                            tint = RemoteTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Endpoint 3: Web BYOK Manager Form
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3. Web BYOK Manager (Paste keys on PC):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RemoteTextPrimary
                                    )
                                    Text(
                                        text = "Open in laptop browser to paste keys easily",
                                        fontSize = 10.sp,
                                        color = RemoteTextMuted
                                    )
                                }
                                OutlinedButton(
                                    onClick = { openUrl(tvServerState.serverUrl) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RemoteNeonGreen),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Web UI", fontSize = 10.sp)
                                }
                            }

                            // Server Stats and Last Event
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RemotePitchBlack)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "📊 Sync Activity: ${tvServerState.apiSyncCount} syncs • ${tvServerState.lastEvent}",
                                    fontSize = 10.sp,
                                    color = RemoteEmerald,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Start the local server to send your API keys wirelessly to your Samsung TV, Termux script, or paste keys from your laptop's browser over local Wi-Fi without any cloud intermediary.",
                            fontSize = 11.sp,
                            color = RemoteTextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // TIER 1: GOOGLE AI STUDIO
            // ==========================================
            ProviderCard(
                tierNumber = 1,
                providerName = "Google AI Studio",
                modelsSummary = "gemini-2.0-flash, gemini-1.5-flash",
                badgeText = "Primary Tier • High RPM",
                accentColor = RemoteNeonGreen,
                icon = Icons.Default.AutoAwesome,
                getKeyUrl = "https://aistudio.google.com/apikey",
                inputValue = geminiInput,
                onValueChange = { geminiInput = it },
                inputLabel = "Google Gemini API Key (AIzaSy...)",
                isConfigured = geminiInput.isNotBlank(),
                isPasswordVisible = showGemini,
                onTogglePassword = { showGemini = !showGemini },
                testStatus = testStatusMap["gemini"],
                isTesting = isTestingMap["gemini"] == true,
                onTestClick = { testProvider("gemini") },
                onOpenUrl = { openUrl("https://aistudio.google.com/apikey") },
                testTagPrefix = "gemini"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // TIER 2: GROQCLOUD
            // ==========================================
            ProviderCard(
                tierNumber = 2,
                providerName = "GroqCloud",
                modelsSummary = "llama-3.3-70b-versatile, llama-3.1-8b-instant, mixtral-8x7b",
                badgeText = "Fastest LPUs • Generous Free Tier",
                accentColor = Color(0xFFFF9800),
                icon = Icons.Default.Bolt,
                getKeyUrl = "https://console.groq.com/keys",
                inputValue = groqInput,
                onValueChange = { groqInput = it },
                inputLabel = "Groq API Key (gsk_...)",
                isConfigured = groqInput.isNotBlank(),
                isPasswordVisible = showGroq,
                onTogglePassword = { showGroq = !showGroq },
                testStatus = testStatusMap["groq"],
                isTesting = isTestingMap["groq"] == true,
                onTestClick = { testProvider("groq") },
                onOpenUrl = { openUrl("https://console.groq.com/keys") },
                testTagPrefix = "groq"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // TIER 3: CEREBRAS CLOUD
            // ==========================================
            ProviderCard(
                tierNumber = 3,
                providerName = "Cerebras Cloud",
                modelsSummary = "llama3.1-8b, llama3.3-70b",
                badgeText = "Wafer-Scale Inference • High Tokens/sec",
                accentColor = Color(0xFFAB47BC),
                icon = Icons.Default.Speed,
                getKeyUrl = "https://cloud.cerebras.ai",
                inputValue = cerebrasInput,
                onValueChange = { cerebrasInput = it },
                inputLabel = "Cerebras API Key (csk_...)",
                isConfigured = cerebrasInput.isNotBlank(),
                isPasswordVisible = showCerebras,
                onTogglePassword = { showCerebras = !showCerebras },
                testStatus = testStatusMap["cerebras"],
                isTesting = isTestingMap["cerebras"] == true,
                onTestClick = { testProvider("cerebras") },
                onOpenUrl = { openUrl("https://cloud.cerebras.ai") },
                testTagPrefix = "cerebras"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // TIER 4: OPENROUTER
            // ==========================================
            ProviderCard(
                tierNumber = 4,
                providerName = "OpenRouter",
                modelsSummary = "openrouter/free, qwen/qwen3.8-27b:free, nemotron:free",
                badgeText = "Universal Gateway • Free Community Pool",
                accentColor = Color(0xFF29B6F6),
                icon = Icons.Default.Public,
                getKeyUrl = "https://openrouter.ai/keys",
                inputValue = openRouterInput,
                onValueChange = { openRouterInput = it },
                inputLabel = "OpenRouter API Key (sk-or-v1-...)",
                isConfigured = openRouterInput.isNotBlank(),
                isPasswordVisible = showOpenRouter,
                onTogglePassword = { showOpenRouter = !showOpenRouter },
                testStatus = testStatusMap["openrouter"],
                isTesting = isTestingMap["openrouter"] == true,
                onTestClick = { testProvider("openrouter") },
                onOpenUrl = { openUrl("https://openrouter.ai/keys") },
                testTagPrefix = "openrouter",
                extraContent = {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "Active OpenRouter Model: $openRouterModel",
                            fontSize = 11.sp,
                            color = RemoteTextSecondary
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // TIER 5: MISTRAL AI
            // ==========================================
            ProviderCard(
                tierNumber = 5,
                providerName = "Mistral AI",
                modelsSummary = "mistral-small-latest, open-mistral-7b",
                badgeText = "European Frontier AI • Free Tier",
                accentColor = Color(0xFFFF7043),
                icon = Icons.Default.Psychology,
                getKeyUrl = "https://console.mistral.ai",
                inputValue = mistralInput,
                onValueChange = { mistralInput = it },
                inputLabel = "Mistral API Key",
                isConfigured = mistralInput.isNotBlank(),
                isPasswordVisible = showMistral,
                onTogglePassword = { showMistral = !showMistral },
                testStatus = testStatusMap["mistral"],
                isTesting = isTestingMap["mistral"] == true,
                onTestClick = { testProvider("mistral") },
                onOpenUrl = { openUrl("https://console.mistral.ai") },
                testTagPrefix = "mistral"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // TIER 6: CLOUDFLARE WORKERS AI
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = RemoteCardSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RemoteBorderColor, RoundedCornerShape(14.dp))
                    .testTag("card_tier_6_cloudflare")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA726).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Cloudflare",
                                    tint = Color(0xFFFFA726),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Tier 6: Cloudflare Workers AI",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RemoteTextPrimary
                                    )
                                }
                                Text(
                                    text = "@cf/meta/llama-3.1-8b-instruct",
                                    fontSize = 11.sp,
                                    color = RemoteTextSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (cfKeyInput.isNotBlank() && cfAccountInput.isNotBlank()) RemoteEmerald.copy(alpha = 0.2f)
                                    else RemotePitchBlack
                                )
                                .border(
                                    1.dp,
                                    if (cfKeyInput.isNotBlank() && cfAccountInput.isNotBlank()) RemoteEmerald else RemoteBorderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (cfKeyInput.isNotBlank() && cfAccountInput.isNotBlank()) "Configured" else "Not Set",
                                fontSize = 10.sp,
                                color = if (cfKeyInput.isNotBlank() && cfAccountInput.isNotBlank()) RemoteNeonGreen else RemoteTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Serverless Edge AI • 10,000 neurons/day free tier",
                        fontSize = 11.sp,
                        color = Color(0xFFFFA726)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cfKeyInput,
                        onValueChange = { cfKeyInput = it },
                        label = { Text("Cloudflare API Token", fontSize = 12.sp) },
                        visualTransformation = if (showCloudflare) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCloudflare = !showCloudflare }) {
                                Icon(
                                    imageVector = if (showCloudflare) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = RemoteTextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFA726),
                            unfocusedBorderColor = RemoteBorderColor,
                            focusedTextColor = RemoteTextPrimary,
                            unfocusedTextColor = RemoteTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cloudflare_key")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cfAccountInput,
                        onValueChange = { cfAccountInput = it },
                        label = { Text("Cloudflare Account ID (32 hex characters)", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFA726),
                            unfocusedBorderColor = RemoteBorderColor,
                            focusedTextColor = RemoteTextPrimary,
                            unfocusedTextColor = RemoteTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cloudflare_account_id")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { openUrl("https://dash.cloudflare.com") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_get_cloudflare_key")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Get Free Key", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { testProvider("cloudflare") },
                            colors = ButtonDefaults.buttonColors(containerColor = RemoteElevatedSurface),
                            shape = RoundedCornerShape(8.dp),
                            enabled = isTestingMap["cloudflare"] != true,
                            modifier = Modifier.testTag("btn_test_cloudflare")
                        ) {
                            if (isTestingMap["cloudflare"] == true) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = RemoteNeonGreen)
                            } else {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = RemoteNeonGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Cloudflare", fontSize = 11.sp, color = RemoteTextPrimary)
                            }
                        }
                    }

                    testStatusMap["cloudflare"]?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = msg, fontSize = 11.sp, color = RemoteTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // PYTHON SCRIPT & TERMUX INSTRUCTIONS
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = RemoteDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RemoteBorderColor, RoundedCornerShape(14.dp))
                    .testTag("python_fallback_info_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🐍 Running the Python Fallback System on Android Termux",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RemoteNeonGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The file 'llm_fallback_system.py' is bundled in this repository. To run it inside Termux on your Galaxy M30s:\n\n" +
                                "1. Install Python & dependencies:\n" +
                                "   pkg install python\n" +
                                "   pip install openai google-genai\n\n" +
                                "2. Tap 'Export for Termux' above to copy your API keys, then paste them into Termux.\n\n" +
                                "3. Execute commands with full 13-model fallback:\n" +
                                "   python3 llm_fallback_system.py \"Search YouTube for Bengali songs\"",
                        fontSize = 11.sp,
                        color = RemoteTextMuted,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProviderCard(
    tierNumber: Int,
    providerName: String,
    modelsSummary: String,
    badgeText: String,
    accentColor: Color,
    icon: ImageVector,
    getKeyUrl: String,
    inputValue: String,
    onValueChange: (String) -> Unit,
    inputLabel: String,
    isConfigured: Boolean,
    isPasswordVisible: Boolean,
    onTogglePassword: () -> Unit,
    testStatus: String?,
    isTesting: Boolean,
    onTestClick: () -> Unit,
    onOpenUrl: () -> Unit,
    testTagPrefix: String,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RemoteCardSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RemoteBorderColor, RoundedCornerShape(14.dp))
            .testTag("card_tier_${tierNumber}_$testTagPrefix")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = providerName,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tier $tierNumber: $providerName",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RemoteTextPrimary
                            )
                        }
                        Text(
                            text = modelsSummary,
                            fontSize = 11.sp,
                            color = RemoteTextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isConfigured) RemoteEmerald.copy(alpha = 0.2f)
                            else RemotePitchBlack
                        )
                        .border(
                            1.dp,
                            if (isConfigured) RemoteEmerald else RemoteBorderColor,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isConfigured) "Configured" else "Not Set",
                        fontSize = 10.sp,
                        color = if (isConfigured) RemoteNeonGreen else RemoteTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = badgeText,
                fontSize = 11.sp,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputValue,
                onValueChange = onValueChange,
                label = { Text(inputLabel, fontSize = 12.sp) },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = RemoteTextMuted
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = RemoteBorderColor,
                    focusedTextColor = RemoteTextPrimary,
                    unfocusedTextColor = RemoteTextPrimary
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_${testTagPrefix}_key")
            )

            extraContent?.invoke()

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenUrl,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_get_${testTagPrefix}_key")
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get Free Key", fontSize = 11.sp)
                }

                Button(
                    onClick = onTestClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RemoteElevatedSurface),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isTesting,
                    modifier = Modifier.testTag("btn_test_$testTagPrefix")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = RemoteNeonGreen)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = RemoteNeonGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test $providerName", fontSize = 11.sp, color = RemoteTextPrimary)
                    }
                }
            }

            testStatus?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    fontSize = 11.sp,
                    color = RemoteTextSecondary
                )
            }
        }
    }
}
