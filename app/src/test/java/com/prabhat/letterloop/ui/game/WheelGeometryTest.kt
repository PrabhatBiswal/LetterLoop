// Unit tests for the wheel geometry: letter placement and segment-vs-circle hit testing.
// All numbers are relative to the wheel radius R, so the same cases run at any screen size.
package com.prabhat.letterloop.ui.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.atan2

class WheelGeometryTest {

    private val tolerance = 1e-3f

    /** A wheel of radius [r] whose bounding box starts at (0,0), so its centre is (r, r). */
    private fun centers(count: Int, r: Float) = letterCenters(count, Offset(r, r), r)
    private fun hits(from: Offset, to: Offset, count: Int, r: Float) =
        lettersOnSegment(from, to, centers(count, r), r * HIT_RADIUS_FRACTION)

    @Test
    fun firstLetterAtTopCentre_allEquallyFarFromCentre_equalAnglesBetweenNeighbours() {
        val r = 100f
        val center = Offset(r, r)
        for (count in 3..5) {
            val c = centers(count, r)

            assertEquals("count $count: first letter x", center.x, c[0].x, tolerance)
            assertTrue("count $count: first letter is above the centre", c[0].y < center.y)

            c.forEach { assertEquals("count $count", r * LETTER_RING_FRACTION, (it - center).getDistance(), tolerance) }

            val step = 2 * PI / count
            for (i in 0 until count) {
                val a = atan2((c[i] - center).y, (c[i] - center).x)
                val b = atan2((c[(i + 1) % count] - center).y, (c[(i + 1) % count] - center).x)
                // Normalise to 0..2π: going from letter i to i+1 is always the same clockwise step.
                val diff = ((b - a) % (2 * PI) + 2 * PI) % (2 * PI)
                assertEquals("count $count, letters $i→${i + 1}", step, diff, 1e-4)
            }
        }
    }

    @Test
    fun neighbouringHitCircles_neverOverlap() {
        val r = 100f
        val hitRadius = r * HIT_RADIUS_FRACTION
        for (count in 3..5) {
            val c = centers(count, r)
            for (i in 0 until count) {
                val gap = (c[i] - c[(i + 1) % count]).getDistance()
                assertTrue("count $count: letters $i and ${i + 1} are $gap apart", gap > 2 * hitRadius)
            }
        }
    }

    @Test
    fun pointInsideHitCircle_hits_pointJustOutside_doesNot() {
        val r = 100f
        val c0 = centers(5, r)[0]
        val hitRadius = r * HIT_RADIUS_FRACTION

        val inside = c0 + Offset(hitRadius - 0.5f, 0f)
        val outside = c0 + Offset(hitRadius + 0.5f, 0f)

        assertEquals(listOf(0), hits(inside, inside, 5, r))
        assertEquals(emptyList<Int>(), hits(outside, outside, 5, r))
    }

    @Test
    fun segmentThroughTwoLetters_returnsBothInTravelOrder_andReversedInOppositeOrder() {
        val r = 100f
        val c = centers(5, r)

        assertEquals(listOf(0, 1), hits(c[0], c[1], 5, r))
        assertEquals(listOf(1, 0), hits(c[1], c[0], 5, r))
    }

    @Test
    fun segmentWithBothEndsOutsideButPassingThrough_returnsTheLetter() {
        // The fast-swipe case: two touch samples on either side of letter 0, neither inside it.
        val r = 100f
        val c0 = centers(5, r)[0]
        val from = c0 - Offset(r * 0.4f, 0f)
        val to = c0 + Offset(r * 0.4f, 0f)

        assertEquals(emptyList<Int>(), hits(from, from, 5, r))
        assertEquals(emptyList<Int>(), hits(to, to, 5, r))
        assertEquals(listOf(0), hits(from, to, 5, r))
    }

    @Test
    fun segmentStartingInsideAndEndingOutside_returnsTheLetter() {
        val r = 100f
        val c0 = centers(5, r)[0]

        assertEquals(listOf(0), hits(c0, c0 + Offset(0f, -r * 0.35f), 5, r))
    }

    @Test
    fun segmentPassingBetweenTwoLetters_returnsNothing() {
        // From the wheel centre straight out along the bisector between letters 0 and 1 (-54°).
        // The letters are 0.62R * sin(36°) ≈ 0.36R from that line; the hit radius is 0.25R.
        val r = 100f
        val center = Offset(r, r)
        val angle = (-PI / 2 + PI / 5).toFloat()
        val edge = center + Offset(kotlin.math.cos(angle), kotlin.math.sin(angle)) * r

        assertEquals(emptyList<Int>(), hits(center, edge, 5, r))
    }

    @Test
    fun segmentThatJustMissesACircle_returnsNothing() {
        // A short horizontal pass just below letter 0's hit circle.
        val r = 100f
        val c0 = centers(5, r)[0]
        val y = c0.y + r * HIT_RADIUS_FRACTION + 0.5f
        val from = Offset(c0.x - r * 0.2f, y)
        val to = Offset(c0.x + r * 0.2f, y)

        assertEquals(emptyList<Int>(), hits(from, to, 5, r))
    }

    @Test
    fun zeroLengthSegment_returnsLetterIfInside_nothingIfOutside() {
        val r = 100f
        val c0 = centers(5, r)[0]

        assertEquals(listOf(0), hits(c0, c0, 5, r))
        val centre = Offset(r, r)
        assertEquals(emptyList<Int>(), hits(centre, centre, 5, r))
    }

    @Test
    fun resultsAreTheSameAtRadius100And500() {
        // The same swipes, described as fractions of R, must give the same letters at any size.
        val swipes = listOf(
            Offset(1.0f, 0.3f) to Offset(1.7f, 0.9f),   // letter 0 to letter 1 area
            Offset(0.2f, 1.6f) to Offset(1.8f, 1.6f),   // along the bottom
            Offset(1.0f, 1.0f) to Offset(1.0f, 0.0f),   // centre straight up through letter 0
            Offset(0.5f, 0.5f) to Offset(1.5f, 1.5f),   // a diagonal through the middle
        )
        for (count in 3..5) {
            for ((a, b) in swipes) {
                assertEquals(
                    "count $count, swipe $a→$b",
                    hits(a * 100f, b * 100f, count, 100f),
                    hits(a * 500f, b * 500f, count, 500f),
                )
            }
        }
    }
}
