// The maths behind the letter wheel: where each letter sits, which letters a finger movement
// passes over, and how a hit changes the selection. Pure functions on plain numbers (Offset is
// just an x/y pair; nothing here touches Compose state), so it is all unit tested on the JVM.
package com.prabhat.letterloop.ui.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Letter centres sit on a ring at this fraction of the wheel radius. */
const val LETTER_RING_FRACTION = 0.62f

/**
 * A letter is hit within this fraction of the wheel radius from its centre. It must stay below
 * half the gap between neighbours on a 5-letter wheel (0.62 * sin(36°) ≈ 0.364), so two hit
 * circles never overlap and one point can never hit two letters.
 */
const val HIT_RADIUS_FRACTION = 0.25f

/**
 * Centres of [count] letters spaced evenly on the ring, first letter straight up, then clockwise.
 * Screen y grows downward, so -90° is the top and increasing angles turn clockwise.
 */
fun letterCenters(count: Int, center: Offset, wheelRadius: Float): List<Offset> {
    val ring = wheelRadius * LETTER_RING_FRACTION
    return List(count) { i ->
        val angle = -PI / 2 + 2 * PI * i / count
        Offset(center.x + ring * cos(angle).toFloat(), center.y + ring * sin(angle).toFloat())
    }
}

/**
 * The letters whose hit circle the straight finger path [from] → [to] touches, in the order the
 * finger reaches them. Testing the whole segment, not just its end point, is what stops a fast
 * swipe from skipping a letter that lies between two touch samples.
 */
fun lettersOnSegment(from: Offset, to: Offset, centers: List<Offset>, hitRadius: Float): List<Int> {
    val d = to - from
    val lengthSquared = d.x * d.x + d.y * d.y
    val hits = mutableListOf<Pair<Int, Float>>()
    centers.forEachIndexed { index, c ->
        // Project the letter centre onto the line: t = 0 at `from`, t = 1 at `to`.
        // A zero-length segment (a single touch point) has nothing to project onto, so use t = 0.
        val t = if (lengthSquared == 0f) 0f else ((c.x - from.x) * d.x + (c.y - from.y) * d.y) / lengthSquared
        // The closest point of the segment itself to the centre: clamp to the segment's ends.
        val closest = from + d * t.coerceIn(0f, 1f)
        val gap = c - closest
        // Compare squared distances to avoid a square root.
        if (gap.x * gap.x + gap.y * gap.y <= hitRadius * hitRadius) hits += index to t
    }
    // Sort by the unclamped t. Hit circles never overlap, so the order of their closest
    // points along the line is the order the finger enters them.
    return hits.sortedBy { it.second }.map { it.first }
}

/**
 * The selection rule (section 7, step 3). Works on letter indices, not characters, so a wheel
 * with two O's lets you pick each one.
 *  - a letter not yet selected is appended;
 *  - the second-to-last selected letter means the finger went back: drop the last one;
 *  - anything else (the letter under the finger now, or an older one) changes nothing.
 * Backtracking stops at one letter: with a single letter selected there is no second-to-last.
 */
fun nextSelection(selected: List<Int>, hit: Int): List<Int> = when {
    hit !in selected -> selected + hit
    selected.size >= 2 && hit == selected[selected.size - 2] -> selected.dropLast(1)
    else -> selected
}

/** The word spelled by the selected letter indices, in selection order. */
fun wordFor(selected: List<Int>, letters: String): String =
    selected.joinToString("") { letters[it].toString() }
