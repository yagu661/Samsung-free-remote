package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.RemoteKey
import com.example.data.model.TvApp
import com.example.data.model.TvConnectionStatus
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
import com.example.voice.VoiceState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YouTubeSearchScreen(
    viewModel: SamsungRemoteViewModel,
    onNavigateToConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val searchState by viewModel.youtubeSearchState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()

    var queryInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // System Speech Recognizer Intent Launcher (Fail-safe for Galaxy M30s)
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
            if (!spoken.isNullOrBlank()) {
                viewModel.handleYouTubeQuerySpoken(spoken) { cleanQuery ->
                    queryInput = cleanQuery
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceListeningForYouTube { spokenQuery ->
                queryInput = spokenQuery
            }
        }
    }

    val isListening = voiceState is VoiceState.Listening

    fun startListeningForQuery() {
        if (isListening) {
            viewModel.stopVoiceListening()
            return
        }

        val hasPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            viewModel.startVoiceListeningForYouTube { spokenQuery ->
                queryInput = spokenQuery
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun startSystemSpeechDialog() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say what you want to search on YouTube...")
        }
        try {
            speechIntentLauncher.launch(intent)
        } catch (_: Exception) {
            startListeningForQuery()
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

        ConnectionStatusHeader(
            connectionState = connectionState,
            onReconnectClick = onNavigateToConnection
        )

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF0000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "YouTube",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "YouTube Voice Search",
                    color = RemoteTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Made with love by Soumya ❤️",
                    color = Color(0xFFFF80AB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Voice Search Box Card
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
                Text(
                    text = "VOICE OR TEXT QUERY",
                    color = RemoteTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        placeholder = { Text("e.g. Bengali songs, funny cat videos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (queryInput.isNotBlank()) {
                                viewModel.searchYouTubeWorkflow(queryInput.trim())
                            }
                        }),
                        trailingIcon = {
                            if (queryInput.isNotBlank()) {
                                IconButton(onClick = { queryInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = RemoteTextMuted
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RemoteNeonGreen,
                            unfocusedBorderColor = RemoteBorderColor,
                            focusedTextColor = RemoteTextPrimary,
                            unfocusedTextColor = RemoteTextPrimary,
                            cursorColor = RemoteNeonGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("youtube_search_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Voice Mic (Calls Google Speech Dialog for 100% reliable voice search)
                    IconButton(
                        onClick = { startSystemSpeechDialog() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isListening) RemoteNeonGreen else RemoteElevatedSurface)
                            .border(1.dp, if (isListening) RemoteNeonGreen else RemoteBorderColor, RoundedCornerShape(12.dp))
                            .testTag("youtube_voice_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = if (isListening) RemotePitchBlack else RemoteNeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Search Submit
                    IconButton(
                        onClick = {
                            if (queryInput.isNotBlank()) {
                                viewModel.searchYouTubeWorkflow(queryInput.trim())
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RemoteNeonGreen)
                            .testTag("youtube_search_submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = RemotePitchBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Voice status indicator & System mic dialog button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isListening) "🎙️ Listening... Speak query" else "Tap mic or speak via system dialog:",
                        color = if (isListening) RemoteNeonGreen else RemoteTextMuted,
                        fontSize = 11.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RemoteElevatedSurface)
                            .border(1.dp, RemoteBorderColor, RoundedCornerShape(8.dp))
                            .clickable { startSystemSpeechDialog() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("btn_yt_system_mic")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = RemoteEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "System Mic",
                            color = RemoteEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Popular Query Pills
                Text(
                    text = "TAP TO SEARCH INSTANTLY:",
                    color = RemoteTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Bengali songs", "funny cat videos", "Old Hindi songs", "Minecraft videos", "Live News").forEach { sample ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(RemoteElevatedSurface)
                                .border(1.dp, RemoteBorderColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    queryInput = sample
                                    viewModel.searchYouTubeWorkflow(sample)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("yt_pill_${sample.replace(" ", "_")}")
                        ) {
                            Text(
                                text = sample,
                                color = RemoteEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4-Step Verification Workflow Card
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
                Text(
                    text = "SEARCH EXECUTION PIPELINE",
                    color = RemoteNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                WorkflowStepRow(
                    stepNumber = 1,
                    title = "Capture Voice / Query",
                    detail = if (searchState.query.isNotBlank()) "Query: \"${searchState.query}\"" else "Waiting for query...",
                    status = if (searchState.query.isNotBlank()) true else null
                )

                WorkflowStepRow(
                    stepNumber = 2,
                    title = "TV Reachability Check",
                    detail = if (searchState.step2TvConnected == true) "TV is connected on Wi-Fi" else "Checking Wi-Fi to TV...",
                    status = searchState.step2TvConnected
                )

                WorkflowStepRow(
                    stepNumber = 3,
                    title = "Launch YouTube App on TV",
                    detail = when (searchState.step3AppLaunched) {
                        true -> "YouTube app launched on TV (App ID: ${TvApp.YOUTUBE.id})"
                        false -> "Failed to launch YouTube via REST"
                        null -> "Waiting for launch..."
                    },
                    status = searchState.step3AppLaunched
                )

                WorkflowStepRow(
                    stepNumber = 4,
                    title = "Inject Search Text",
                    detail = if (searchState.step4TextInjected == true) "Text injected via SendRemoteText"
                    else if (searchState.step4TextInjected == false) "Tizen keyboard limitation: Use D-Pad search shortcut"
                    else "Waiting for injection...",
                    status = searchState.step4TextInjected
                )

                if (searchState.isSearching) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = RemoteNeonGreen,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = searchState.resultMessage ?: "",
                            color = RemoteTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else if (!searchState.resultMessage.isNullOrBlank()) {
                    Text(
                        text = searchState.resultMessage ?: "",
                        color = if (searchState.step3AppLaunched == true) RemoteEmerald else RemoteWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Tizen YouTube Limitation Card & Fallback Navigation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteWarning.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemoteCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = "Limitation Info",
                        tint = RemoteWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TIZEN YOUTUBE LIMITATION & FALLBACK",
                        color = RemoteWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "On Samsung Series 4 (UA32T4410), YouTube launches the search URL or main app. If the search box is not automatically focused, use the Quick Focus button below (Left → Up → OK):",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.sendKey(RemoteKey.KEY_LEFT)
                            viewModel.sendKey(RemoteKey.KEY_UP)
                            viewModel.sendKey(RemoteKey.KEY_ENTER)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RemoteElevatedSurface,
                            contentColor = RemoteNeonGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_focus_youtube_search")
                    ) {
                        Text("Focus Search (Left+Up+OK)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.launchApp(TvApp.YOUTUBE) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0000),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_reopen_youtube")
                    ) {
                        Text("Reopen YouTube", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Made with love by Soumya Footer
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WorkflowStepRow(
    stepNumber: Int,
    title: String,
    detail: String,
    status: Boolean?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, tint) = when (status) {
            true -> Icons.Default.CheckCircle to RemoteNeonGreen
            false -> Icons.Default.Error to RemoteError
            null -> Icons.Default.HourglassEmpty to RemoteTextMuted
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "Step $stepNumber: $title",
                color = if (status == true) RemoteTextPrimary else RemoteTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = RemoteTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
