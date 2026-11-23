// The app theme: one fixed dark palette and the Fredoka font.
// There is no light theme and no dynamic (wallpaper) colour, so the game looks the same on every device.
package com.prabhat.letterloop.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Mapped so that any Material component we do use (bottom sheet) picks our colours, not Material's.
private val LoopColorScheme = darkColorScheme(
    primary = LoopColors.Marigold,
    onPrimary = LoopColors.OnMarigold,
    secondary = LoopColors.Teal,
    onSecondary = LoopColors.OnTeal,
    error = LoopColors.Coral,
    onError = LoopColors.OnCoral,
    background = LoopColors.Ground,
    onBackground = LoopColors.Cream,
    surface = LoopColors.Surface,
    onSurface = LoopColors.Cream,
    surfaceContainerLow = LoopColors.Surface,
    surfaceVariant = LoopColors.EmptyTile,
    onSurfaceVariant = LoopColors.Muted,
    outline = LoopColors.Outline,
    scrim = LoopColors.Scrim,
)

@Composable
fun LetterLoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LoopColorScheme, typography = Typography) {
        // Text and icons are cream unless a screen says otherwise.
        CompositionLocalProvider(LocalContentColor provides LoopColors.Cream, content = content)
    }
}
