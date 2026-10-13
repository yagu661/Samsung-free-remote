package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
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
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.components.ConnectionStatusHeader
import com.example.ui.components.DpadControl
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceListening()
        }
    }

    val isListening = voiceState is VoiceState.Listening

    fun startListeningForQuery() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            viewModel.startVoiceListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
                    text = "Deep-link launch & query workflow",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Voice Search Box
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
                        placeholder = { Text("e.g. funny cat videos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (queryInput.isNotBlank()) {
                                viewModel.searchYouTubeWorkflow(queryInput.trim())
                            }
                        }),
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

                    // Voice Mic
                    IconButton(
                        onClick = { startListeningForQuery() },
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

                // Sample Query Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("funny cat videos", "science experiments", "space documentaries").forEach { sample ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(RemoteElevatedSurface)
                                .border(1.dp, RemoteBorderColor, RoundedCornerShape(14.dp))
                                .clickable {
                                    queryInput = sample
                                    viewModel.searchYouTubeWorkflow(sample)
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("yt_pill_${sample.replace(" ", "_")}")
                        ) {
                            Text(
                                text = sample,
                                color = RemoteEmerald,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 4-Step Verification Workflow Progress Card
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WORKFLOW VERIFICATION STATUS",
                        color = RemoteTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (searchState.isSearching) {
                        CircularProgressIndicator(
                            color = RemoteNeonGreen,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                // Step 1: Voice Recognition & Query
                WorkflowStepRow(
                    stepNumber = 1,
                    title = "Voice Recognized & Query Extracted",
                    detail = if (searchState.query.isNotBlank()) "Query: \"${searchState.query}\"" else "Waiting for query",
                    status = searchState.step1VoiceRecognized
                )

                // Step 2: Check TV Connection
                WorkflowStepRow(
                    stepNumber = 2,
                    title = "Check Samsung TV Connection",
                    detail = if (searchState.step2TvConnected == true) "WebSocket Connected" else "Testing connection state",
                    status = searchState.step2TvConnected
                )

                // Step 3: Launch YouTube App (ID: 111299001912)
                WorkflowStepRow(
                    stepNumber = 3,
                    title = "Launch YouTube App (ID: 111299001912)",
                    detail = if (searchState.step3AppLaunched == true) "App launched via Tizen REST" else "Triggering app launch",
                    status = searchState.step3AppLaunched
                )

                // Step 4: Text Entry / Injection
                WorkflowStepRow(
                    stepNumber = 4,
                    title = "Inject Search Query Text",
                    detail = if (searchState.step4TextInjected == true) "Text injected via SendRemoteText"
                    else if (searchState.step4TextInjected == false) "Text injection not accepted by YouTube Tizen input. Manual fallback active."
                    else "Waiting for app to load",
                    status = searchState.step4TextInjected
                )

                if (!searchState.resultMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(RemoteElevatedSurface)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = searchState.resultMessage ?: "",
                            color = RemoteTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Honest Limitation & Manual Navigation Fallback
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp)),
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
                        imageVector = Icons.Default.Help,
                        contentDescription = "Limitation Info",
                        tint = RemoteWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TIZEN YOUTUBE LIMITATION & FALLBACK",
                        color = RemoteWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "On Samsung Tizen Smart TVs, the installed YouTube app runs inside a closed HTML5 sandbox. If the app does not automatically populate the search box, use the shortcut button below to focus the search icon, or use the D-pad to navigate.",
                    color = RemoteTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                // Quick Navigation Shortcut
                Button(
                    onClick = {
                        // Simulates moving to search icon: Left -> Up -> Enter
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
                        .fillMaxWidth()
                        .testTag("btn_focus_youtube_search")
                ) {
                    Text("Focus Search Icon (Left -> Up -> OK)", fontSize = 13.sp)
                }

                // D-Pad for manual on-screen keyboard navigation
                DpadControl(
                    onNavigate = { key -> viewModel.sendKey(key) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
