package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RemoteKey
import com.example.data.model.TvApp
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

@Composable
fun RemoteIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = RemoteTextPrimary,
    backgroundColor: Color = RemoteElevatedSurface,
    borderColor: Color = RemoteBorderColor,
    size: Dp = 52.dp,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    testTag: String = ""
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1.0f, label = "pressScale")

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (isPressed) 1.5.dp else 1.dp,
                color = if (isPressed) RemoteNeonGreen else borderColor,
                shape = shape
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .testTag(testTag.ifBlank { "btn_${contentDescription.lowercase().replace(" ", "_")}" }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) RemoteNeonGreen else tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DpadControl(
    onNavigate: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dpadSize = 210.dp
    val centerSize = 74.dp

    Box(
        modifier = modifier
            .size(dpadSize)
            .clip(CircleShape)
            .background(RemoteCardSurface)
            .border(2.dp, RemoteBorderColor, CircleShape)
            .testTag("dpad_container"),
        contentAlignment = Alignment.Center
    ) {
        // UP Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(width = 64.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(RemoteKey.KEY_UP)
                }
                .testTag("btn_dpad_up"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = RemoteTextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // DOWN Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(width = 64.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(RemoteKey.KEY_DOWN)
                }
                .testTag("btn_dpad_down"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = RemoteTextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // LEFT Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(width = 48.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(RemoteKey.KEY_LEFT)
                }
                .testTag("btn_dpad_left"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = RemoteTextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // RIGHT Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(width = 48.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(RemoteKey.KEY_RIGHT)
                }
                .testTag("btn_dpad_right"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Right",
                tint = RemoteTextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // Center OK / Enter Button
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(if (isPressed) 0.92f else 1.0f, label = "okPress")

        Box(
            modifier = Modifier
                .size(centerSize)
                .scale(scale)
                .clip(CircleShape)
                .background(if (isPressed) RemoteNeonGreen.copy(alpha = 0.2f) else RemoteElevatedSurface)
                .border(
                    width = if (isPressed) 2.dp else 1.5.dp,
                    color = if (isPressed) RemoteNeonGreen else RemoteBorderColor,
                    shape = CircleShape
                )
                .clickable(interactionSource = interactionSource, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(RemoteKey.KEY_ENTER)
                }
                .testTag("btn_dpad_ok"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = if (isPressed) RemoteNeonGreen else RemoteTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun VolumeAndChannelSection(
    onSendKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Volume Rocker
        Column(
            modifier = Modifier
                .width(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(RemoteCardSurface)
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(20.dp))
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // VOL UP
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSendKey(RemoteKey.KEY_VOLUP)
                    }
                    .testTag("btn_vol_up"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Volume Up",
                    tint = RemoteNeonGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "VOL",
                color = RemoteTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // VOL DOWN
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSendKey(RemoteKey.KEY_VOLDOWN)
                    }
                    .testTag("btn_vol_down"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Volume Down",
                    tint = RemoteNeonGreen,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Center Mute & Tools
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RemoteIconButton(
                icon = Icons.Default.VolumeMute,
                contentDescription = "Mute",
                onClick = { onSendKey(RemoteKey.KEY_MUTE) },
                size = 54.dp,
                shape = CircleShape,
                tint = RemoteEmerald,
                testTag = "btn_mute"
            )

            Text(
                text = "MUTE",
                color = RemoteTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Channel Rocker
        Column(
            modifier = Modifier
                .width(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(RemoteCardSurface)
                .border(1.dp, RemoteBorderColor, RoundedCornerShape(20.dp))
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CH UP
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSendKey(RemoteKey.KEY_CHUP)
                    }
                    .testTag("btn_ch_up"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Channel Up",
                    tint = RemoteTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "CH",
                color = RemoteTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // CH DOWN
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSendKey(RemoteKey.KEY_CHDOWN)
                    }
                    .testTag("btn_ch_down"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Channel Down",
                    tint = RemoteTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun PlaybackSection(
    onSendKey: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RemoteCardSurface)
            .border(1.dp, RemoteBorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteIconButton(
            icon = Icons.Default.FastRewind,
            contentDescription = "Rewind",
            onClick = { onSendKey(RemoteKey.KEY_REWIND) },
            tint = RemoteTextSecondary,
            size = 46.dp,
            testTag = "btn_rewind"
        )
        RemoteIconButton(
            icon = Icons.Default.PlayArrow,
            contentDescription = "Play",
            onClick = { onSendKey(RemoteKey.KEY_PLAY) },
            tint = RemoteNeonGreen,
            size = 48.dp,
            testTag = "btn_play"
        )
        RemoteIconButton(
            icon = Icons.Default.Pause,
            contentDescription = "Pause",
            onClick = { onSendKey(RemoteKey.KEY_PAUSE) },
            tint = RemoteTextPrimary,
            size = 48.dp,
            testTag = "btn_pause"
        )
        RemoteIconButton(
            icon = Icons.Default.FastForward,
            contentDescription = "Fast Forward",
            onClick = { onSendKey(RemoteKey.KEY_FF) },
            tint = RemoteTextSecondary,
            size = 46.dp,
            testTag = "btn_ff"
        )
        RemoteIconButton(
            icon = Icons.Default.Stop,
            contentDescription = "Stop",
            onClick = { onSendKey(RemoteKey.KEY_STOP) },
            tint = RemotePowerRed,
            size = 46.dp,
            testTag = "btn_stop"
        )
    }
}

@Composable
fun AppShortcutsRow(
    onLaunchApp: (TvApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val apps = listOf(TvApp.YOUTUBE, TvApp.NETFLIX, TvApp.PRIME_VIDEO, TvApp.SPOTIFY)
        apps.forEach { app ->
            val color = when (app) {
                TvApp.YOUTUBE -> Color(0xFFFF0000)
                TvApp.NETFLIX -> Color(0xFFE50914)
                TvApp.PRIME_VIDEO -> Color(0xFF00A8E1)
                TvApp.SPOTIFY -> Color(0xFF1DB954)
                else -> RemoteNeonGreen
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RemoteCardSurface)
                    .border(1.dp, RemoteBorderColor, RoundedCornerShape(10.dp))
                    .clickable { onLaunchApp(app) }
                    .testTag("app_${app.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.name,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
