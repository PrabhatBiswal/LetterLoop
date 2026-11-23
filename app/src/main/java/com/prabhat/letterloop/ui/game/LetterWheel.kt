// The letter wheel: draws the disc, letters and swipe line, and turns one finger's swipe into a word.
// It owns the short-lived selection itself (Decision log #6). The ViewModel only hears about a
// finished word, through onWordSubmitted, never about individual touch moves.
package com.prabhat.letterloop.ui.game

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.LocalScene
import com.prabhat.letterloop.ui.components.keycap
import com.prabhat.letterloop.ui.theme.LoopColors
import kotlin.math.min
import kotlin.math.roundToInt

/** Sizes below are given for a 280dp wheel; everything scales from this. */
private val DesignDiameter = 280.dp

@Composable
fun LetterWheel(
    letters: String,
    enabled: Boolean,
    onWordSubmitted: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSelectionChange: (String) -> Unit = {},
) {
    // Indices of the selected letters, in the order they were swiped.
    var selection by remember { mutableStateOf(emptyList<Int>()) }
    // Written on every touch move, but read only inside the Canvas draw lambda below. Compose then
    // redraws the Canvas on a move without recomposing anything, which keeps the line on the finger.
    var finger by remember { mutableStateOf<Offset?>(null) }
    val haptics = LocalHapticFeedback.current
    // The gesture coroutine below lives across recompositions. These always hold the newest
    // callbacks without having to restart (and so cancel) a swipe in progress.
    val submit by rememberUpdatedState(onWordSubmitted)
    val selectionChanged by rememberUpdatedState(onSelectionChange)

    BoxWithConstraints(
        modifier
            // Keyed on the letters and enabled flag: a shuffle or the celebration restarts this
            // block, which cancels any swipe in progress (the finally below clears it).
            .pointerInput(letters, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    // Step 1: the first finger down starts the swipe. Remember which pointer it is.
                    val down = awaitFirstDown()
                    val pointerId = down.id

                    // Same centres the letters are drawn at, from this node's real size in pixels.
                    val wheelRadius = min(size.width, size.height) / 2f
                    val centers = letterCenters(
                        letters.length, Offset(size.width / 2f, size.height / 2f), wheelRadius,
                    )
                    val hitRadius = wheelRadius * HIT_RADIUS_FRACTION
                    var last = down.position

                    // Step 2 and 3: treat the movement from the last point to `to` as a line segment,
                    // and apply the selection rule to every letter it touches, in travel order.
                    fun moveFingerTo(to: Offset) {
                        for (hit in lettersOnSegment(last, to, centers, hitRadius)) {
                            val next = nextSelection(selection, hit)
                            if (next == selection) continue
                            if (next.size > selection.size) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                            selection = next
                            selectionChanged(wordFor(next, letters))
                        }
                        last = to
                        finger = to
                    }

                    try {
                        down.consume()
                        // A zero-length segment: selects the letter under the first touch, if any.
                        moveFingerTo(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            // Only the finger that started the swipe counts; a second finger is ignored.
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                            if (!change.pressed) {
                                // Step 4 and 5: a real lift arrives unconsumed. When the system cancels
                                // the gesture, Compose sends a lift that is already consumed, and
                                // nothing is submitted. Fewer than 2 letters (a tap) is not a word.
                                if (!change.isConsumed && selection.size >= 2) {
                                    submit(wordFor(selection, letters))
                                }
                                break
                            }
                            // A fast finger produces several touch samples per frame. Android batches
                            // the older ones as "historical" points; walk them all, then the newest,
                            // so no part of the path between samples is skipped.
                            change.historical.forEach { moveFingerTo(it.position) }
                            moveFingerTo(change.position)
                            change.consume()
                        }
                    } finally {
                        // Runs on lift, on system cancel, and when this coroutine itself is
                        // cancelled (shuffle, celebration, screen left): the swipe never lingers.
                        selection = emptyList()
                        finger = null
                        selectionChanged("")
                    }
                }
            },
    ) {
        val diameter = min(maxWidth, maxHeight)
        val scale = diameter / DesignDiameter
        val density = LocalDensity.current
        val centers = with(density) {
            letterCenters(
                letters.length,
                Offset(maxWidth.toPx() / 2f, maxHeight.toPx() / 2f),
                diameter.toPx() / 2f,
            )
        }
        val keySize = 60.dp * scale
        val scene = LocalScene.current

        Canvas(Modifier.fillMaxSize()) {
            // A white disc, slightly see-through so the hills behind it still show.
            drawCircle(LoopColors.Tile.copy(alpha = 0.92f), radius = size.minDimension / 2f)
            // Reading `finger` here (draw phase), and nowhere in composition, is what makes a
            // move only redraw. The line is not animated: it must follow the finger exactly.
            val fingerNow = finger
            if (selection.isNotEmpty() && fingerNow != null) {
                val path = Path().apply {
                    val first = centers[selection.first()]
                    moveTo(first.x, first.y)
                    selection.drop(1).forEach { lineTo(centers[it].x, centers[it].y) }
                    lineTo(fingerNow.x, fingerNow.y)
                }
                drawPath(
                    path,
                    scene.accent,
                    style = Stroke(width = 10.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        val keyPx = with(density) { keySize.toPx() }
        // Each letter keeps an identity across a shuffle ("O#0", "O#1" for a repeated O), so key()
        // moves the same composable to its new index and its position animates there: the
        // letters glide instead of jumping. A fresh screen starts at the targets, so nothing moves.
        val identities = letterIdentities(letters)
        letters.forEachIndexed { index, letter ->
            key(identities[index]) {
                val position by animateOffsetAsState(centers[index], tween(350), label = "letterGlide")
                LetterKey(
                    letter = letter,
                    selected = index in selection,
                    size = keySize,
                    scale = scale,
                    modifier = Modifier
                        // Read in the layout phase, so the glide moves the key without recomposing.
                        .offset { IntOffset((position.x - keyPx / 2f).roundToInt(), (position.y - keyPx / 2f).roundToInt()) }
                        .testTag(TestTags.wheelLetter(index)),
                )
            }
        }
    }
}

/** A stable name for each wheel letter: the character plus which copy of it this is. */
private fun letterIdentities(letters: String): List<String> =
    letters.mapIndexed { i, ch -> "$ch#${letters.take(i).count { it == ch }}" }

@Composable
private fun LetterKey(letter: Char, selected: Boolean, size: Dp, scale: Float, modifier: Modifier) {
    // Sizes in dp converted to sp, so the letter always fits its circle whatever the
    // phone's font-size setting (Decision log #28).
    val fontSize = with(LocalDensity.current) { (34.dp * scale).toSp() }
    val scene = LocalScene.current
    // Touched: spring up to 1.12 and fade the accent circle in; released: settle back.
    val keyScale by animateFloatAsState(
        if (selected) 1.12f else 1f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "keyScale",
    )
    val circleAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(120), label = "keyCircle")
    Box(
        modifier
            .size(size)
            // Both values are read inside graphicsLayer, so the animation only redraws this key.
            .graphicsLayer { scaleX = keyScale; scaleY = keyScale }
            // Merge so tests can read the letter from the tagged box.
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = circleAlpha }
                .keycap(scene.accent, scene.accentEdge, 3.dp * scale, CircleShape),
        )
        Text(
            text = letter.toString(),
            color = if (selected) LoopColors.Tile else LoopColors.Ink,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
