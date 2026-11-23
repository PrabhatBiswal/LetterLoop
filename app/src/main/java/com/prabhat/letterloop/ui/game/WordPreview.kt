// The pill above the wheel. While swiping it shows the word being built. After a submission it
// plays that word's feedback: a praise pill for a grid word, a teal pill that flies into the bonus
// button, a coral shake for an invalid word, grey for a word already found. Its 48dp of height is
// always reserved, so the wheel never jumps.
package com.prabhat.letterloop.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prabhat.letterloop.game.SubmitResult
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.theme.LoopColors
import kotlinx.coroutines.delay

/**
 * Where the bonus flight starts (the pill) and ends (the bonus button), in screen coordinates.
 * Layout writes them; the flight reads them while drawing. Plain fields, not Compose state: the
 * running animation already redraws every frame, so nothing needs to observe them.
 */
class FlightPath {
    var pill: Offset? = null
    var bonusButton: Offset? = null
}

private val Praise = listOf("Nice!", "Great!", "Lovely!", "Brilliant!", "Super!")

/** How long the screen keeps each kind of feedback before reporting it shown. */
fun feedbackMillis(result: SubmitResult): Long = when (result) {
    SubmitResult.GridWord -> 800
    SubmitResult.BonusWord -> 650
    SubmitResult.Invalid -> 500
    SubmitResult.AlreadyFound -> 650
}

@Composable
fun WordPreview(swipeWord: String, feedback: Feedback?, flight: FlightPath, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(48.dp)
            .fillMaxWidth()
            .onGloballyPositioned { flight.pill = it.boundsInRoot().center },
        contentAlignment = Alignment.Center,
    ) {
        when {
            // A new swipe always wins: animations never block input.
            swipeWord.isNotEmpty() -> Pill(swipeWord, LoopColors.Surface, LoopColors.Cream, border = LoopColors.Marigold)
            // key(): each new feedback id starts its animation from the beginning.
            feedback != null -> key(feedback.id) { FeedbackPill(feedback, flight) }
        }
    }
}

@Composable
private fun FeedbackPill(feedback: Feedback, flight: FlightPath) {
    val appear = remember { Animatable(0.75f) }
    val leave = remember { Animatable(0f) } // 0 = in place, 1 = gone (faded, or arrived at the button)
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.5f))
    }
    LaunchedEffect(Unit) {
        when (feedback.result) {
            SubmitResult.Invalid -> {
                // A quick sideways shake (about 300ms), then fade.
                shake.animateTo(0f, keyframes {
                    durationMillis = 300
                    -16f at 40
                    14f at 90
                    -10f at 140
                    8f at 190
                    -4f at 240
                })
                leave.animateTo(1f, tween(150))
            }
            SubmitResult.BonusWord -> {
                delay(120)
                leave.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            }
            SubmitResult.GridWord -> {
                delay(500)
                leave.animateTo(1f, tween(250))
            }
            SubmitResult.AlreadyFound -> {
                delay(400)
                leave.animateTo(1f, tween(200))
            }
        }
    }

    val (background, content, text) = when (feedback.result) {
        SubmitResult.GridWord -> Triple(LoopColors.Marigold, LoopColors.OnMarigold, Praise[(feedback.id % Praise.size).toInt()])
        SubmitResult.BonusWord -> Triple(LoopColors.Teal, LoopColors.OnTeal, feedback.word)
        SubmitResult.Invalid -> Triple(LoopColors.Coral, LoopColors.OnCoral, feedback.word)
        SubmitResult.AlreadyFound -> Triple(LoopColors.Outline, LoopColors.Cream, feedback.word)
    }
    Pill(
        text,
        background,
        content,
        Modifier.graphicsLayer {
            val p = leave.value
            var scale = appear.value
            translationX = shake.value.dp.toPx()
            if (feedback.result == SubmitResult.BonusWord) {
                // Fly from the pill's place to the bonus button, shrinking on the way.
                val from = flight.pill
                val to = flight.bonusButton
                if (from != null && to != null) {
                    translationX += (to.x - from.x) * p
                    translationY = (to.y - from.y) * p
                }
                scale *= 1f - 0.75f * p
            }
            scaleX = scale
            scaleY = scale
            alpha = 1f - p
        },
    )
}

@Composable
private fun Pill(text: String, background: Color, content: Color, modifier: Modifier = Modifier, border: Color? = null) {
    Box(
        modifier
            .fillMaxHeight()
            .background(background, CircleShape)
            .then(if (border != null) Modifier.border(2.dp, border, CircleShape) else Modifier)
            .padding(horizontal = 22.dp)
            .testTag(TestTags.WORD_PREVIEW)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content, fontSize = 26.sp, letterSpacing = 5.sp, fontWeight = FontWeight.SemiBold)
    }
}
