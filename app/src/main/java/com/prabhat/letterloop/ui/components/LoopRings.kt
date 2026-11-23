// The faint concentric "loop" rings from the design, drawn on Canvas behind Home and Game.
package com.prabhat.letterloop.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.prabhat.letterloop.ui.theme.LoopColors

/**
 * Draws rings every 48dp around the centre of the element this is applied to, reaching far past
 * its bounds. Nothing is measured: put it on the element the rings belong to (the logo's L, the
 * wheel), make sure that element draws before its siblings, and clip at the screen's root.
 */
fun Modifier.loopRings(): Modifier = drawBehind {
    val spacing = 48.dp.toPx()
    val stroke = Stroke(width = 2.dp.toPx())
    // 30 rings reach about 1440dp, past the corners of any phone or small tablet.
    for (i in 1..30) drawCircle(LoopColors.Ring, radius = spacing * i, style = stroke)
}
