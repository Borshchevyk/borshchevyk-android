package ru.kubsu.borshchevyk.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

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

object BorshchevykTheme {
    val colors: BorshchevykColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBorshchevykColors.current

    val typography: BorshchevykTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalBorshchevykTypography.current
}
