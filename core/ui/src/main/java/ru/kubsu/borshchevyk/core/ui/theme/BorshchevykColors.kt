package ru.kubsu.borshchevyk.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BorshchevykColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color,
    val onError: Color
)

val LightBorshchevykColors = BorshchevykColors(
    primary = Color(0xFF10B981), // Emerald
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF9FAFB),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFD1D5DB),
    error = Color(0xFFEF4444),
    onError = Color.White
)

val LocalBorshchevykColors = staticCompositionLocalOf { LightBorshchevykColors }
