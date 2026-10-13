package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.OpenRouterClient
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.components.ConnectionStatusHeader
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SamsungRemoteViewModel,
    onNavigateToConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val autoExecuteVoice by viewModel.autoExecuteVoice.collectAsState()
    val useGeminiAi by viewModel.useGeminiAi.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()
    val customGeminiApiKey by viewModel.customGeminiApiKey.collectAsState()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsState()
    val openRouterModel by viewModel.openRouterModel.collectAsState()
    val speechLanguage by viewModel.speechLanguage.collectAsState()
    val scrollState = rememberScrollState()

    var customGeminiKeyInput by remember(customGeminiApiKey) { mutableStateOf(customGeminiApiKey) }
    var openRouterKeyInput by remember(openRouterApiKey) { mutableStateOf(openRouterApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RemotePitchBlack)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        ConnectionStatusHeader(
            connectionState = connectionState,
            onReconnectClick = onNavigateToConnection
        )

        // Header Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = RemoteNeonGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Preferences & Settings",
                    color = RemoteTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI API keys, speech language, TV credentials & credits",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // --- 1. AI Engine & API Configuration Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteNeonGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = RemoteNeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI ASSISTANT & API CONFIGURATION",
                        color = RemoteNeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Configure the AI engine used for voice intent translation and conversational assistant chat.",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp
                )

                // Provider Toggle Chips
                Text(
                    text = "Select AI Engine Provider:",
                    color = RemoteTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Gemini Option
                    val isGemini = aiProvider == "gemini"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGemini) RemoteNeonGreen.copy(alpha = 0.15f) else RemoteElevatedSurface)
                            .border(1.dp, if (isGemini) RemoteNeonGreen else RemoteBorderColor, RoundedCornerShape(12.dp))
                            .clickable { viewModel.setAiProvider("gemini") }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Google Gemini",
                                color = if (isGemini) RemoteNeonGreen else RemoteTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Built-in / Direct",
                                color = RemoteTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // OpenRouter Option
                    val isOpenRouter = aiProvider == "openrouter"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOpenRouter) RemoteNeonGreen.copy(alpha = 0.15f) else RemoteElevatedSurface)
                            .border(1.dp, if (isOpenRouter) RemoteNeonGreen else RemoteBorderColor, RoundedCornerShape(12.dp))
                            .clickable { viewModel.setAiProvider("openrouter") }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "OpenRouter",
                                color = if (isOpenRouter) RemoteNeonGreen else RemoteTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Multi-Model API",
                                color = RemoteTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Details based on active provider
                // Details based on active provider
                if (aiProvider == "gemini") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RemoteEmerald.copy(alpha = 0.12f))
                            .border(1.dp, RemoteEmerald.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = RemoteEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Provider: Google Gemini 2.0 Flash",
                                    color = RemoteEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Get your free API key at aistudio.google.com and paste it below.",
                                color = RemoteTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Gemini API Key Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Gemini API Key (Required for AI chat):",
                            color = RemoteTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = customGeminiKeyInput,
                            onValueChange = { customGeminiKeyInput = it },
                            placeholder = { Text("Paste your Gemini API key (starts with AIzaSy...)", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = RemoteTextMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RemoteNeonGreen,
                                unfocusedBorderColor = RemoteBorderColor,
                                focusedTextColor = RemoteTextPrimary,
                                unfocusedTextColor = RemoteTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_custom_gemini_key")
                        )
                    }
                } else {
                    // OpenRouter Configuration
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "OpenRouter API Key:",
                            color = RemoteTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = openRouterKeyInput,
                            onValueChange = { openRouterKeyInput = it },
                            placeholder = { Text("Paste your OpenRouter key (sk-or-v1-...)", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = RemoteTextMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RemoteNeonGreen,
                                unfocusedBorderColor = RemoteBorderColor,
                                focusedTextColor = RemoteTextPrimary,
                                unfocusedTextColor = RemoteTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_openrouter_key")
                        )

                        Text(
                            text = "Choose AI Model:",
                            color = RemoteTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OpenRouterClient.POPULAR_MODELS.forEach { modelName ->
                                val isSelected = openRouterModel == modelName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) RemoteNeonGreen else RemoteElevatedSurface)
                                        .border(1.dp, if (isSelected) RemoteNeonGreen else RemoteBorderColor, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setOpenRouterModel(modelName) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = modelName.substringAfter("/"),
                                        color = if (isSelected) RemotePitchBlack else RemoteTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons: Save & Test
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (aiProvider == "gemini") {
                                viewModel.setCustomGeminiApiKey(customGeminiKeyInput)
                                saveFeedback = "Gemini API Key saved successfully!"
                            } else {
                                viewModel.setOpenRouterApiKey(openRouterKeyInput)
                                saveFeedback = "OpenRouter API Key saved successfully!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RemoteNeonGreen,
                            contentColor = RemotePitchBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_save_ai_config")
                    ) {
                        Text("Save API Key", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            saveFeedback = "Testing API connection..."
                            if (aiProvider == "gemini") {
                                viewModel.setCustomGeminiApiKey(customGeminiKeyInput)
                            } else {
                                viewModel.setOpenRouterApiKey(openRouterKeyInput)
                            }
                            viewModel.testAiConnection { success, message ->
                                saveFeedback = if (success) "✓ $message" else "✗ Test failed: $message"
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_ai_config")
                    ) {
                        Text("Test Connection", color = RemoteNeonGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                if (saveFeedback != null) {
                    Text(
                        text = saveFeedback ?: "",
                        color = if (saveFeedback?.startsWith("✓") == true || saveFeedback?.contains("saved", ignoreCase = true) == true) RemoteNeonGreen else RemoteWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "💡 Tip: Direct remote commands (Volume, Mute, Power, Channels, YouTube Cast) work 100% offline without any API key.",
                    color = RemoteTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // --- 2. Voice Recognition & Language Settings Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = RemoteNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VOICE CONTROLS & SPEECH LANGUAGE",
                        color = RemoteNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Speech Language Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Spoken Language Recognition:",
                        color = RemoteTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val languageOptions = listOf(
                        "en-US" to "English (US)",
                        "en-IN" to "English (India)",
                        "bn-IN" to "Bengali (বাংলা)",
                        "hi-IN" to "Hindi (हिन्दी)"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languageOptions.forEach { (code, label) ->
                            val isSelected = speechLanguage == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) RemoteNeonGreen else RemoteElevatedSurface)
                                    .border(1.dp, if (isSelected) RemoteNeonGreen else RemoteBorderColor, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setSpeechLanguage(code) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) RemotePitchBlack else RemoteTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Auto-Execute Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Execute Voice Commands",
                            color = RemoteTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Execute high-confidence commands immediately without manual confirmation tap.",
                            color = RemoteTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoExecuteVoice,
                        onCheckedChange = { viewModel.setAutoExecuteVoice(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RemotePitchBlack,
                            checkedTrackColor = RemoteNeonGreen,
                            uncheckedTrackColor = RemoteElevatedSurface
                        ),
                        modifier = Modifier.testTag("switch_auto_execute")
                    )
                }

                // AI Engine Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Natural-Language Interpretation",
                            color = RemoteTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Interprets conversational requests (e.g. 'Turn volume down a little') into TV commands.",
                            color = RemoteTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = useGeminiAi,
                        onCheckedChange = { viewModel.setUseGeminiAi(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RemotePitchBlack,
                            checkedTrackColor = RemoteNeonGreen,
                            uncheckedTrackColor = RemoteElevatedSurface
                        ),
                        modifier = Modifier.testTag("switch_gemini_ai")
                    )
                }
            }
        }

        // --- 3. Security & Credentials Card ---
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RemoteNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURITY & TV CREDENTIALS",
                        color = RemoteNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "• Private connection tokens are stored securely in on-device SharedPreferences and never shared.\n" +
                            "• All control occurs locally on your home Wi-Fi directly to the TV without cloud intermediaries.\n" +
                            "• API keys are managed securely and never transmitted to untrusted endpoints.",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Button(
                    onClick = { viewModel.clearPairingToken() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RemoteElevatedSurface,
                        contentColor = RemoteWarning
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_reset_token_settings")
                ) {
                    Text("Clear Saved TV Token & Re-pair", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 4. Battery Optimization Card ---
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
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = RemoteEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BATTERY EFFICIENCY",
                        color = RemoteEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "This app employs zero background wake-locks. Discovery sockets and speech listeners terminate immediately upon completing an action, preserving battery life on your Samsung Galaxy M30s.",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // --- 5. "Made with love by Soumya" Card (PROMINENT DEVELOPER BADGE) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFFF4081).copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF160E18))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4081).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Made with Love",
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Made with love by Soumya",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Custom engineered for Samsung UA32T4410 Smart TV & Galaxy M30s",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RemoteElevatedSurface)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "soumyarana77789@gmail.com • v1.0",
                        color = RemoteNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
