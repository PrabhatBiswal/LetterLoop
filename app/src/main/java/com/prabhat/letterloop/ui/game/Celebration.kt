// The level-complete moment: confetti drawn on a Canvas, and the card that takes the wheel's place
// showing what was found, then either "Next up" (the ViewModel moves on by itself) or, after the
// last level, a button back to Level Select.
package com.prabhat.letterloop.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.PrimaryButton
import com.prabhat.letterloop.ui.theme.LoopColors
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun LevelCompleteCard(state: GameUiState, onBackToLevels: () -> Unit, modifier: Modifier = Modifier) {
    val game = state.game ?: return
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(LoopColors.Surface, shape)
            .border(1.5.dp, LoopColors.Outline, shape)
            // Scrolls only if a very short screen cannot fit the whole card.
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag(TestTags.LEVEL_COMPLETE_CARD),
    ) {
        Text(
            text = buildAnnotatedString {
                append("Level ${state.levelId} ")
                withStyle(SpanStyle(color = LoopColors.Marigold)) { append("complete") }
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        val words = game.level.words.size
        val bonus = game.foundBonus
        Text(
            text = if (bonus.isEmpty()) "All $words words found" else "All $words words found, plus ${bonus.size} bonus:",
            fontSize = 17.sp,
            color = LoopColors.Muted,
        )
        if (bonus.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            BonusChips(bonus)
        }
        Spacer(Modifier.height(20.dp))
        if (state.phase == GamePhase.AllLevelsDone) {
            Text("That was the last level. Every level is solved!", fontSize = 17.sp)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Back to levels",
                onClick = onBackToLevels,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.BACK_TO_LEVELS_BUTTON),
            )
        } else {
            // Fills over the 1.5 second wait. The value is read while drawing, so it only redraws.
            val fill = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                fill.animateTo(1f, tween(GameViewModel.CELEBRATION_MILLIS.toInt(), easing = LinearEasing))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(LoopColors.EmptyTile, CircleShape)
                    .drawBehind {
                        drawRoundRect(
                            LoopColors.Marigold,
                            size = Size(size.width * fill.value, size.height),
                            cornerRadius = CornerRadius(size.height / 2),
                        )
                    },
            )
            Spacer(Modifier.height(12.dp))
            Text("Next up: Level ${state.levelId + 1}", fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Found bonus words as teal-bordered chips. Also used by the bonus words sheet. */
@Composable
fun BonusChips(words: Collection<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        words.forEach { word ->
            Text(
                text = word,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .background(LoopColors.EmptyTile, CircleShape)
                    .border(1.5.dp, LoopColors.Teal, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

private val ConfettiColors = listOf(LoopColors.Marigold, LoopColors.Teal, LoopColors.Coral, LoopColors.Cream)
private const val CONFETTI_SECONDS = 2.4f
/** Fall acceleration, in screen heights per second squared. */
private const val GRAVITY = 0.5f

/** One piece of confetti. Positions are fractions of the screen, so it looks the same on any size. */
private class ConfettiPiece(
    val startX: Float,
    val delay: Float,
    val fallSpeed: Float,
    val drift: Float,
    val wobblePhase: Float,
    val spin: Float,
    val sizeDp: Float,
    val color: Color,
    val round: Boolean,
)

/**
 * Confetti falling from the top of the screen. Every piece's position is a formula of time, so one
 * animated value drives them all, and it is read only inside the Canvas: a frame is a redraw.
 */
@Composable
fun Confetti(seed: Int, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val pieces = remember(seed) {
        val random = Random(seed)
        List(90) {
            ConfettiPiece(
                startX = random.nextFloat(),
                delay = random.nextFloat() * 0.5f,
                fallSpeed = 0.15f + random.nextFloat() * 0.3f,
                drift = (random.nextFloat() - 0.5f) * 0.2f,
                wobblePhase = random.nextFloat() * 6.28f,
                spin = (random.nextFloat() - 0.5f) * 720f,
                sizeDp = 6f + random.nextFloat() * 6f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                round = random.nextBoolean(),
            )
        }
    }
    val time = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) {
        time.animateTo(CONFETTI_SECONDS, tween((CONFETTI_SECONDS * 1000).toInt(), easing = LinearEasing))
        onFinished()
    }
    Canvas(modifier) {
        val now = time.value
        if (now >= CONFETTI_SECONDS) return@Canvas
        for (piece in pieces) {
            val t = now - piece.delay
            if (t <= 0f) continue
            // Starts just above the top edge, falls with gravity, drifts and wobbles sideways.
            val x = (piece.startX + piece.drift * t + 0.02f * sin(piece.wobblePhase + t * 5f)) * size.width
            val y = (-0.05f + piece.fallSpeed * t + 0.5f * GRAVITY * t * t) * size.height
            val px = piece.sizeDp.dp.toPx()
            rotate(piece.spin * t, pivot = Offset(x, y)) {
                if (piece.round) {
                    drawCircle(piece.color, radius = px / 2f, center = Offset(x, y))
                } else {
                    drawRect(piece.color, topLeft = Offset(x - px / 2f, y - px / 4f), size = Size(px, px / 2f))
                }
            }
        }
    }
}
