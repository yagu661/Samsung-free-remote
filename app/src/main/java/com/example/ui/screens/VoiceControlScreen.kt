package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.SamsungRemoteViewModel
import com.example.ui.components.ConnectionStatusHeader
import com.example.ui.theme.RemoteBorderColor
import com.example.ui.theme.RemoteCardSurface
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
import com.example.voice.VoiceState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceControlScreen(
    viewModel: SamsungRemoteViewModel,
    onNavigateToConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val partialText by viewModel.partialSpeechText.collectAsState()
    val pendingAction by viewModel.pendingVoiceAction.collectAsState()
    val statusMessage by viewModel.voiceStatusMessage.collectAsState()

    var manualTextInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // System Speech Recognizer launcher (Fail-safe for Galaxy M30s)
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
            if (!spoken.isNullOrBlank()) {
                viewModel.handleVoiceRecognized(spoken)
            }
        }
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceListening()
        }
    }

    val isListening = voiceState is VoiceState.Listening

    // Pulsing animation for listening mic
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.18f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun startSystemSpeechDialog() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a TV command (e.g. Volume Up, Open YouTube)...")
        }
        try {
            speechIntentLauncher.launch(intent)
        } catch (_: Exception) {
            viewModel.startVoiceListening()
        }
    }

    fun onMicTapped() {
        if (isListening) {
            viewModel.stopVoiceListening()
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPerm) {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    viewModel.startVoiceListening()
                } else {
                    startSystemSpeechDialog()
                }
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
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

        // Screen Subtitle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SAMSUNG AI VOICE REMOTE",
                color = RemoteEmerald,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Made with love by Soumya ❤️",
                color = Color(0xFFFF80AB),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Large Central Microphone Button
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glowing pulse ring
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(RemoteNeonGreen.copy(alpha = 0.15f))
                        .border(2.dp, RemoteNeonGreen.copy(alpha = 0.4f), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(CircleShape)
                    .background(if (isListening) RemoteNeonGreen else RemoteElevatedSurface)
                    .border(
                        width = 2.dp,
                        color = if (isListening) RemoteNeonGreen else RemoteBorderColor,
                        shape = CircleShape
                    )
                    .clickable { onMicTapped() }
                    .testTag("large_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Voice Control Microphone",
                    tint = if (isListening) RemotePitchBlack else RemoteNeonGreen,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Text(
            text = if (isListening) "Listening... Tap to stop" else "Tap microphone to speak command",
            color = if (isListening) RemoteNeonGreen else RemoteTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(RemoteElevatedSurface)
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(10.dp))
                .clickable { startSystemSpeechDialog() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("btn_voice_system_dialog")
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = RemoteEmerald,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Use System Voice Dialog (Fail-safe)",
                color = RemoteEmerald,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Live Recognition Card
        AnimatedVisibility(visible = partialText.isNotBlank() || isListening) {
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
                    Text(
                        text = "RECOGNIZED SPEECH",
                        color = RemoteTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (partialText.isNotBlank()) "\"$partialText\"" else "Listening for speech...",
                        color = RemoteTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Action Confirmation Card (Safety Gate before executing)
        AnimatedVisibility(visible = pendingAction != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, RemoteNeonGreen, RoundedCornerShape(16.dp)),
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
                            text = "ACTION DETECTED",
                            color = RemoteNeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RemoteNeonGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VERIFIED ALLOWLIST",
                                color = RemoteNeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    pendingAction?.let { action ->
                        Text(
                            text = viewModel.getActionDescription(action),
                            color = RemoteTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.executePendingVoiceAction() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RemoteNeonGreen,
                                contentColor = RemotePitchBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_confirm_voice_action")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute on TV", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelPendingVoiceAction() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_cancel_voice_action")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel", color = RemoteTextSecondary)
                        }
                    }
                }
            }
        }

        // Status or Error Message
        if (!statusMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RemoteElevatedSurface)
                    .border(1.dp, RemoteBorderColor, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    color = RemoteTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Manual Text Fallback Field
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
                Text(
                    text = "OR TYPE COMMAND (MANUAL FALLBACK)",
                    color = RemoteTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualTextInput,
                        onValueChange = { manualTextInput = it },
                        placeholder = { Text("e.g. Search YouTube for space documentaries") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (manualTextInput.isNotBlank()) {
                                viewModel.handleVoiceRecognized(manualTextInput)
                                manualTextInput = ""
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
                            .testTag("manual_command_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (manualTextInput.isNotBlank()) {
                                viewModel.handleVoiceRecognized(manualTextInput)
                                manualTextInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RemoteNeonGreen)
                            .testTag("btn_send_manual_command")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            tint = RemotePitchBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Quick Suggestion Pills
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SUPPORTED VOICE PHRASES (TAP TO TEST)",
                color = RemoteTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            val samplePhrases = listOf(
                "Volume up",
                "Volume down",
                "Mute",
                "Go home",
                "Go back",
                "Press OK",
                "Open YouTube",
                "Open Netflix",
                "Play",
                "Pause",
                "Search YouTube for funny cat videos",
                "Search YouTube for science experiments",
                "Find cartoons on YouTube"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                samplePhrases.forEach { phrase ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(RemoteElevatedSurface)
                            .border(1.dp, RemoteBorderColor, RoundedCornerShape(20.dp))
                            .clickable { viewModel.handleVoiceRecognized(phrase) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("pill_${phrase.lowercase().replace(" ", "_")}")
                    ) {
                        Text(
                            text = phrase,
                            color = RemoteEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

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
                tint = androidx.compose.ui.graphics.Color(0xFFFF4081),
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
