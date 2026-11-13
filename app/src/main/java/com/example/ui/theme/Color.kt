package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Maddookie Obsidian Crimson & Electric Amber Palette
val MidnightObsidian = Color(0xFF0A0B10)
val ObsidianSurface = Color(0xFF11131C)
val SmokedSlate = Color(0xFF171A26)
val SmokedSlateElevated = Color(0xFF202536)
val HairlineBorder = Color(0xFF2B3147)

val CrimsonPrimary = Color(0xFFFF3B5C)
val CrimsonOnPrimary = Color(0xFFFFFFFF)
val CrimsonContainer = Color(0xFF4D1020)
val CrimsonOnContainer = Color(0xFFFFD9DF)

val AmberSecondary = Color(0xFFFFB020)
val AmberOnSecondary = Color(0xFF241600)
val AmberContainer = Color(0xFF473000)
val AmberOnContainer = Color(0xFFFFE0A3)

val ElectricCyanTertiary = Color(0xFF38BDF8)
val SilverText = Color(0xFFF2F4F8)
val MutedSlateText = Color(0xFF9AA3B8)

// Domain Integration Accents
val AniListCyan = Color(0xFF02A9FF)
val MalBlue = Color(0xFF3B66D4)
val SimklEmerald = Color(0xFF00E5A3)
val DiscordBlurple = Color(0xFF5865F2)
val TurboLime = Color(0xFFA3E635)

// Light Theme Tokens
val LightCanvas = Color(0xFFF7F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFECEFF6)
val LightText = Color(0xFF11131C)
val LightMutedText = Color(0xFF525A70)

val MaddookieDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = CrimsonOnPrimary,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = CrimsonOnContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    tertiary = ElectricCyanTertiary,
    onTertiary = MidnightObsidian,
    background = MidnightObsidian,
    onBackground = SilverText,
    surface = ObsidianSurface,
    onSurface = SilverText,
    surfaceVariant = SmokedSlate,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = SmokedSlate,
    surfaceContainerHigh = SmokedSlateElevated,
    outline = HairlineBorder,
    outlineVariant = Color(0xFF1F2436),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2D0000)
)

val MaddookieLightColorScheme = lightColorScheme(
    primary = Color(0xFFD91E43),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3E0011),
    secondary = Color(0xFFC97E00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8BE),
    onSecondaryContainer = Color(0xFF2B1900),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedText,
    surfaceContainerLow = Color(0xFFF1F3F9),
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFE2E6F0),
    outline = Color(0xFFCBD2E1),
    outlineVariant = Color(0xFFE0E5F0),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)
