package com.videorelay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// VideoRelay dark scheme — purple/violet accent on deep dark backgrounds
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
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    inverseSurface = DarkOnSurface,
    inverseOnSurface = DarkBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = VRPurple,
    onPrimary = LightSurface,
    primaryContainer = VRPurple.copy(alpha = 0.12f),
    onPrimaryContainer = VRPurpleDim,
    secondary = LightSecondary,
    onSecondary = LightOnSurface,
    tertiary = VRZapGold,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightBorder,
    outlineVariant = LightBorder,
)

@Composable
fun VideoRelayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Always use our brand colors — no dynamic color (it would override our purple theme)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VideoRelayTypography,
        content = content,
    )
}
