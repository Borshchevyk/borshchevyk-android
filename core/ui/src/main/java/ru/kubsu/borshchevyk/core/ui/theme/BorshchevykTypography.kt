package ru.kubsu.borshchevyk.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Defines the custom typography scale for the Borshchevyk design system.
 *
 * This immutable data class provides a standardized set of text styles used
 * across the application, mapped roughly to Material Design 3 typography categories.
 *
 * @property titleLarge Used for prominent screen titles or main headers.
 * @property titleMedium Used for medium-emphasis headers or section titles.
 * @property bodyLarge Standard body text used for the majority of the content.
 * @property bodyMedium Used for secondary body text or less emphasized information.
 * @property labelSmall Used for small labels, captions, or auxiliary text like timestamps.
 */
@Immutable
data class BorshchevykTypography(
    val titleLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    val labelSmall: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * A CompositionLocal used to pass [BorshchevykTypography] down the Compose tree.
 * Defaults to a standard instance of [BorshchevykTypography].
 */
val LocalBorshchevykTypography = staticCompositionLocalOf { BorshchevykTypography() }

