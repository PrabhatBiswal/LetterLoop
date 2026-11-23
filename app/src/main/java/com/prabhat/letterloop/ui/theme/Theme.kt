// The app theme: fixed colours and the Fredoka font. There is no dark theme and no dynamic
// (wallpaper) colour, so the game looks the same on every device.
package com.prabhat.letterloop.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Mapped so that any Material component we do use (bottom sheet) picks our colours, not Material's.
private val LoopColorScheme = lightColorScheme(
    primary = LoopColors.Ink,
    onPrimary = LoopColors.Tile,
    secondary = LoopColors.Play,
    onSecondary = LoopColors.Tile,
    error = LoopColors.Wrong,
    onError = LoopColors.Tile,
    background = LoopColors.Tile,
    onBackground = LoopColors.Ink,
    surface = LoopColors.Tile,
    onSurface = LoopColors.Ink,
    surfaceContainerLow = LoopColors.Tile,
    onSurfaceVariant = LoopColors.Muted,
    outline = LoopColors.TileEdge,
    scrim = LoopColors.Scrim,
)

@Composable
fun LetterLoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LoopColorScheme, typography = Typography) {
        // Text and icons are ink unless a screen says otherwise.
        CompositionLocalProvider(LocalContentColor provides LoopColors.Ink, content = content)
    }
}
