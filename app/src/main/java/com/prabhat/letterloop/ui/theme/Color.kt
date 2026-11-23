// The colour tokens from the approved design (development.md section 9).
// Every colour in the app is defined once here so screens never hard-code a hex value.
// They live in an object so names like `Surface` do not clash with Compose's own Surface.
package com.prabhat.letterloop.ui.theme

import androidx.compose.ui.graphics.Color

object LoopColors {
    val Ground = Color(0xFF0E1726)
    val Surface = Color(0xFF18243A)
    val EmptyTile = Color(0xFF1F2E48)
    val Outline = Color(0xFF2C3F60)

    val Marigold = Color(0xFFFFB627)
    val MarigoldEdge = Color(0xFFC9820A)
    val OnMarigold = Color(0xFF201400)

    val Teal = Color(0xFF3DD6C3)
    val TealEdge = Color(0xFF1E9E8F)
    val OnTeal = Color(0xFF04201C)

    val Coral = Color(0xFFFF6F61)
    val CoralEdge = Color(0xFFC2463A)
    val OnCoral = Color(0xFF2A0906)

    val Cream = Color(0xFFF6F1E7)
    val Muted = Color(0xFFA9B8D0)

    val LockedTile = Color(0xFF152033)
    val LockedBorder = Color(0xFF223149)
    val LockedContent = Color(0xFF8FA0BC)

    // Pause dialog scrim: #060A12 at about 80%.
    val Scrim = Color(0xCC060A12)

    // Background "loop" rings: white at about 3.5% opacity.
    val Ring = Color(0x09FFFFFF)
}
