// Device tests for the LetterWheel gesture on its own. Touch positions come from WheelGeometry and
// the wheel's real size on screen, so these drive the same maths the app uses.
package com.prabhat.letterloop.ui.game

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.theme.LetterLoopTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class LetterWheelGestureTest {

    @get:Rule
    val rule = createComposeRule()

    private val letters = "ABCDE"
    private val submitted = mutableListOf<String>()
    private var lastSelection = ""

    private fun showWheel(enabled: Boolean = true) {
        rule.setContent {
            LetterLoopTheme {
                LetterWheel(
                    letters = letters,
                    enabled = enabled,
                    onWordSubmitted = { submitted += it },
                    onSelectionChange = { lastSelection = it },
                    modifier = Modifier
                        .size(280.dp)
                        .testTag(TestTags.WHEEL),
                )
            }
        }
    }

    private fun swipe(block: TouchInjectionScope.(centers: List<Offset>) -> Unit) {
        rule.onNodeWithTag(TestTags.WHEEL).performTouchInput {
            val radius = min(width, height) / 2f
            block(letterCenters(letters.length, Offset(width / 2f, height / 2f), radius))
        }
    }

    // A slow finger: many small moves instead of one jump.
    private fun TouchInjectionScope.slowMoveTo(target: Offset, steps: Int = 12) {
        val start = currentPosition()!!
        for (step in 1..steps) moveTo(lerp(start, target, step / steps.toFloat()))
    }

    private fun assertSubmitted(vararg words: String) = rule.runOnIdle {
        assertEquals(words.toList(), submitted)
    }

    @Test
    fun slowDragOverLetters012_submitsThatWordOnce() {
        showWheel()
        swipe { c ->
            down(c[0])
            slowMoveTo(c[1])
            slowMoveTo(c[2])
            up()
        }
        assertSubmitted("ABC")
    }

    @Test
    fun singleMoveJumpingPastALetter_stillSelectsIt() {
        showWheel()
        swipe { c ->
            val radius = min(width, height) / 2f
            val hitRadius = radius * HIT_RADIUS_FRACTION
            // One move event from letter 0, straight through letter 1, to a point past it.
            val direction = (c[1] - c[0]) / (c[1] - c[0]).getDistance()
            val pastLetter1 = c[1] + direction * (hitRadius * 1.5f)
            // Neither end of that move is inside letter 1's hit circle.
            assertTrue((c[0] - c[1]).getDistance() > hitRadius)
            assertTrue((pastLetter1 - c[1]).getDistance() > hitRadius)

            down(c[0])
            moveTo(pastLetter1)
            up()
        }
        assertSubmitted("AB")
    }

    @Test
    fun tapOnOneLetter_submitsNothing() {
        showWheel()
        swipe { c ->
            down(c[0])
            up()
        }
        assertSubmitted()
    }

    @Test
    fun draggingBackOntoPreviousLetter_submitsShortenedWord() {
        showWheel()
        swipe { c ->
            down(c[0])
            slowMoveTo(c[1])
            slowMoveTo(c[2])
            slowMoveTo(c[1])
            up()
        }
        assertSubmitted("AB")
    }

    @Test
    fun draggingOutsideTheWheelAndBackIn_keepsTheSelection() {
        showWheel()
        swipe { c ->
            down(c[0])
            slowMoveTo(c[1])
            // Straight up and out above the wheel's top edge, then back in to letter 2.
            slowMoveTo(Offset(c[1].x, -60f))
            slowMoveTo(c[2])
            up()
        }
        assertSubmitted("ABC")
    }

    @Test
    fun cancelledGesture_submitsNothing_andClearsSelection() {
        showWheel()
        swipe { c ->
            down(c[0])
            slowMoveTo(c[1])
            slowMoveTo(c[2])
            cancel()
        }
        assertSubmitted()
        rule.runOnIdle { assertEquals("", lastSelection) }

        // The next swipe starts from nothing, not from the cancelled ABC.
        swipe { c ->
            down(c[3])
            slowMoveTo(c[4])
            up()
        }
        assertSubmitted("DE")
    }

    @Test
    fun secondFingerMidDrag_doesNotChangeTheWord() {
        showWheel()
        swipe { c ->
            down(0, c[0])
            slowMoveTo(c[1])
            // A second finger lands on letter 3 and moves to letter 4.
            down(1, c[3])
            moveTo(1, c[4])
            moveTo(0, c[2])
            up(0)
            up(1)
        }
        assertSubmitted("ABC")
    }

    @Test
    fun disabledWheel_submitsNothing() {
        showWheel(enabled = false)
        swipe { c ->
            down(c[0])
            slowMoveTo(c[1])
            slowMoveTo(c[2])
            up()
        }
        assertSubmitted()
    }

    @Test
    fun twentyFastSwipesInARow_giveExactlyTwentyCorrectSubmissions() {
        showWheel()
        val expected = List(20) { i -> wordFor(listOf(i % 5, (i + 1) % 5, (i + 2) % 5), letters) }
        swipe { c ->
            // As fast as the test can send them: one move per letter, no pauses.
            repeat(20) { i ->
                down(c[i % 5])
                moveTo(c[(i + 1) % 5])
                moveTo(c[(i + 2) % 5])
                up()
            }
        }
        assertSubmitted(*expected.toTypedArray())
    }
}
