package ru.kubsu.borshchevyk.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The main theme provider for the Borshchevyk application.
 *
 * Wraps the given [content] in a [CompositionLocalProvider] to supply the custom
 * [BorshchevykColors] and [BorshchevykTypography] down the Compose tree.
 * Elements inside [content] can access the theme values via `BorshchevykTheme.colors`
 * and `BorshchevykTheme.typography`.
 *
 * @param darkTheme Indicates whether the system is currently in dark mode. (Currently unused as only light theme is provided).
 * @param content The composable content that will inherit the theme.
 */
@Composable
fun BorshchevykTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For now we only have Light theme implementation as requested.
    // In a real app we'd also define DarkBorshchevykColors.
    val colors = LightBorshchevykColors
    val typography = BorshchevykTypography()

    CompositionLocalProvider(
        LocalBorshchevykColors provides colors,
        LocalBorshchevykTypography provides typography
    ) {
        content()
    }
}

/**
 * Object that provides convenient access to the current theme values (colors and typography)
 * within a `@Composable` context.
 *
 * Example usage: `BorshchevykTheme.colors.primary`
 */
object BorshchevykTheme {
    /**
     * Retrieves the current [BorshchevykColors] provided to the CompositionLocal.
     */
    val colors: BorshchevykColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBorshchevykColors.current

    /**
     * Retrieves the current [BorshchevykTypography] provided to the CompositionLocal.
     */
    val typography: BorshchevykTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalBorshchevykTypography.current
}

