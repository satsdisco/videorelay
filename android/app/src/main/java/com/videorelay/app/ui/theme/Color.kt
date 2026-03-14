package com.videorelay.app.ui.theme

import androidx.compose.ui.graphics.Color

// VideoRelay brand — matches web app exactly
// Primary: HSL(265, 80%, 60%) = purple/violet
// Background: HSL(250, 15%, 7%) = deep dark purple-black
// Card: HSL(250, 15%, 10%)
// Border: HSL(250, 15%, 18%)
// Zap gold: HSL(45, 100%, 55%)

val VRPurple = Color(0xFF8B5CF6)          // primary — hsl(265, 80%, 60%)
val VRPurpleLight = Color(0xFF7C3AED)     // primary pressed
val VRPurpleDim = Color(0xFF6D28D9)       // primary variant

val VRZapGold = Color(0xFFEAB308)         // zap accent — hsl(45, 100%, 55%)

// Dark scheme (default — matches web)
val DarkBackground = Color(0xFF111015)    // hsl(250, 15%, 7%)
val DarkSurface = Color(0xFF18161F)       // hsl(250, 15%, 10%)
val DarkCard = Color(0xFF18161F)          // card bg
val DarkCardElevated = Color(0xFF1F1D28)  // elevated surface
val DarkBorder = Color(0xFF2A2733)        // hsl(250, 15%, 18%)
val DarkOnSurface = Color(0xFFEEECF3)     // hsl(250, 10%, 95%)
val DarkOnSurfaceVariant = Color(0xFF87839A) // hsl(250, 10%, 55%)
val DarkSecondary = Color(0xFF1F1D28)     // hsl(250, 15%, 14%)
val DarkMuted = Color(0xFF87839A)         // muted text

// Light scheme
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE0DDE8)       // hsl(250, 10%, 88%)
val LightOnSurface = Color(0xFF18161F)
val LightOnSurfaceVariant = Color(0xFF6B6780)
val LightSecondary = Color(0xFFEDEBF3)    // hsl(250, 10%, 93%)
