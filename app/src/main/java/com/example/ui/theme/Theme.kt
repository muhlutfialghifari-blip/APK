package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonViolet,
    tertiary = NeonGreen,
    background = SolidBlack,
    surface = DarkGrey,
    onPrimary = SolidBlack,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CardGrey,
    onSurfaceVariant = TextSecondary,
    error = NeonRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Force Cosmic High-Tech Dark Mode as requested for the full premium feel
    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
