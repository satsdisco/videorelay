package com.videorelay.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// VideoRelay is dark-first. Always dark. Matches the web app.
private val DarkColorScheme = darkColorScheme(
    primary = VRPurple,
    onPrimary = DarkOnSurface,
    primaryContainer = VRPurpleDim,
    onPrimaryContainer = DarkOnSurface,
    secondary = DarkSecondary,
    onSecondary = DarkOnSurface,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = DarkOnSurface,
    tertiary = VRZapGold,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkCardElevated,
    surfaceContainerHighest = DarkCardElevated,
    surfaceContainerLow = DarkBackground,
    surfaceContainerLowest = DarkBackground,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    inverseSurface = DarkOnSurface,
    inverseOnSurface = DarkBackground,
    inversePrimary = VRPurpleDim,
    surfaceTint = VRPurple,
    surfaceBright = DarkCardElevated,
    surfaceDim = DarkBackground,
    scrim = DarkBackground,
)

@Composable
fun VideoRelayTheme(
    content: @Composable () -> Unit,
) {
    // Always dark — VideoRelay is a dark-first app
    val colorScheme = DarkColorScheme

    // Force dark status bar and navigation bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VideoRelayTypography,
        content = content,
    )
}
