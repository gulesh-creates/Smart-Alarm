package com.zenith.study.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonLime,
    onPrimary = TextInverse,
    primaryContainer = NeonLimeSubtle,
    onPrimaryContainer = NeonLime,
    secondary = CardElevated,
    onSecondary = TextPrimary,
    background = CanvasBlack,
    onBackground = TextPrimary,
    surface = CardCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = CardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderMedium,
    error = Danger,
    onError = TextPrimary
)

@Composable
fun ZenithTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

