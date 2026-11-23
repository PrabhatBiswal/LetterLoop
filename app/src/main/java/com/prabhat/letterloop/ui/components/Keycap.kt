// The "keycap" look from the design: a flat face with a hard, darker copy of its shape showing
// below it, instead of a soft blurred shadow. Shared by grid tiles, wheel letters and buttons.
package com.prabhat.letterloop.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/**
 * Draws [edge] in [shape], shifted down by [depth], then [face] on top at the element's bounds.
 * Only the bottom strip of the edge shows. The edge draws outside the bounds, so leave at least
 * [depth] of space below the element (the grid's gaps and the button layouts do).
 */
fun Modifier.keycap(face: Color, edge: Color, depth: Dp, shape: Shape): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    translate(top = depth.toPx()) { drawOutline(outline, edge) }
    drawOutline(outline, face)
}
