// The app's fixed colours. The colours that change from chapter to chapter (sky, hills and the
// accent used for found words) live in components/Scene.kt; everything else is defined once here
// so screens never hard-code a hex value.
package com.prabhat.letterloop.ui.theme

import androidx.compose.ui.graphics.Color

object LoopColors {
    val Ink = Color(0xFF1F2540)       // text and icons on white
    val Muted = Color(0xFF6C7389)     // secondary text on white
    val Tile = Color(0xFFFFFFFF)      // tiles, cards, the wheel; also text on colour
    val TileEdge = Color(0xFFC5CDE0)  // the bottom edge under white tiles and buttons
    val Glass = Color(0x4DFFFFFF)     // empty grid tiles: 30% white over the scenery
    val GlassLine = Color(0xB3FFFFFF) // their outline: 70% white
    val Locked = Color(0xFFE9ECF3)    // locked level tiles, the secondary button

    val Play = Color(0xFF2FBF71)      // the primary button
    val PlayEdge = Color(0xFF1E8F52)
    val Gold = Color(0xFFFFC233)      // bonus words
    val Wrong = Color(0xFFE5484D)     // an invalid word

    // Behind the Pause dialog and the bonus sheet.
    val Scrim = Color(0x99101730)
}
