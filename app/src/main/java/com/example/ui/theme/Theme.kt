package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RemoteNeonGreen,
    onPrimary = RemotePitchBlack,
    primaryContainer = RemoteCardSurface,
    onPrimaryContainer = RemoteNeonGreen,
    secondary = RemoteEmerald,
    onSecondary = RemotePitchBlack,
    secondaryContainer = RemoteElevatedSurface,
    onSecondaryContainer = RemoteEmerald,
    background = RemotePitchBlack,
    onBackground = RemoteTextPrimary,
    surface = RemoteDarkSurface,
    onSurface = RemoteTextPrimary,
    surfaceVariant = RemoteCardSurface,
    onSurfaceVariant = RemoteTextSecondary,
    outline = RemoteBorderColor,
    error = RemoteError,
    onError = RemoteTextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
