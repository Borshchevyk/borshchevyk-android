package ru.kubsu.borshchevyk.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Defines the custom color palette for the Borshchevyk design system.
 *
 * This immutable data class holds all the semantically named colors used
 * throughout the application to maintain a consistent visual style,
 * following Material Design principles adapted for custom needs.
 *
 * @property primary The primary color used for main components like buttons and active states.
 * @property onPrimary The color used for text and icons displayed on top of the primary color.
 * @property primaryContainer A lighter variant of the primary color, often used for backgrounds of selected items.
 * @property onPrimaryContainer The color used for text and icons displayed on top of the primary container.
 * @property background The background color for screens and large structural areas.
 * @property onBackground The color used for text and icons displayed on the background.
 * @property surface The color of surfaces like cards, sheets, and menus.
 * @property onSurface The color used for text and icons displayed on surfaces.
 * @property surfaceVariant A subtle variant of the surface color, used to distinguish elements.
 * @property onSurfaceVariant The color used for text and icons displayed on the surface variant.
 * @property outline A subtle color used for borders and dividers.
 * @property error The color used for error states and destructive actions.
 * @property onError The color used for text and icons displayed on top of the error color.
 */
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

/**
 * The default light theme color palette for the Borshchevyk application.
 * Utilizes an Emerald green primary color scheme.
 */
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

/**
 * A CompositionLocal used to pass [BorshchevykColors] down the Compose tree.
 * Defaults to [LightBorshchevykColors].
 */
val LocalBorshchevykColors = staticCompositionLocalOf { LightBorshchevykColors }

