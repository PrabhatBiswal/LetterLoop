// The crossword grid. Every cell of the level is drawn from the start (so the puzzle's shape is
// visible), as hidden, hinted or revealed. It draws GameState and contains no game rules.
package com.prabhat.letterloop.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prabhat.letterloop.game.GameState
import com.prabhat.letterloop.game.Level
import com.prabhat.letterloop.game.PlacedWord
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.keycap
import com.prabhat.letterloop.ui.theme.LoopColors
import kotlinx.coroutines.delay

enum class TileState { Hidden, Hinted, Revealed }

private val Gap = 4.dp

/**
 * The cell size for [level] in a space of [maxWidth] x [maxHeight]: 44dp on the 390dp design phone
 * (358dp of usable width), growing with the screen up to 56dp, and shrunk if the grid would not fit.
 */
fun gridCellSize(level: Level, maxWidth: Dp, maxHeight: Dp): Dp {
    val preferred = (44.dp * (maxWidth / 358.dp)).coerceIn(44.dp, 56.dp)
    val fitsWidth = (maxWidth - Gap * (level.cols - 1)) / level.cols
    val fitsHeight = (maxHeight - Gap * (level.rows - 1)) / level.rows
    return minOf(preferred, fitsWidth, fitsHeight)
}

/** The grid's total width at [cell] size; landscape lines the top bar up with it. */
fun gridWidth(level: Level, cell: Dp): Dp = cell * level.cols + Gap * (level.cols - 1)

/**
 * [popWord] is the grid word just found: its newly revealed tiles pop in one after another.
 * [pulse] is (feedback id, word) for a word submitted again: its tiles pulse once.
 */
@Composable
fun CrosswordGrid(
    game: GameState,
    cell: Dp,
    modifier: Modifier = Modifier,
    popWord: PlacedWord? = null,
    pulse: Pair<Long, PlacedWord>? = null,
) {
    val level = game.level
    Box(modifier.size(gridWidth(level, cell), cell * level.rows + Gap * (level.rows - 1))) {
        for (c in level.cells) {
            val state = when (c) {
                in game.wordCells -> TileState.Revealed
                in game.hintedCells -> TileState.Hinted
                else -> TileState.Hidden
            }
            // About 60ms between neighbouring letters of the word just found.
            val indexInWord = popWord?.cells?.indexOf(c) ?: -1
            GridTile(
                letter = level.letterAt.getValue(c),
                state = state,
                size = cell,
                popDelayMillis = if (indexInWord >= 0) indexInWord * 60L else 0L,
                pulseKey = pulse?.takeIf { c in it.second.cells }?.first,
                modifier = Modifier
                    .offset(x = (cell + Gap) * c.col, y = (cell + Gap) * c.row)
                    .testTag(TestTags.gridCell(c.row, c.col)),
            )
        }
    }
}

@Composable
private fun GridTile(
    letter: Char,
    state: TileState,
    size: Dp,
    popDelayMillis: Long,
    pulseKey: Long?,
    modifier: Modifier,
) {
    // The design is 44dp tiles with a 10dp radius and 26sp letters; keep those proportions.
    val shape = RoundedCornerShape(size * (10f / 44f))
    val fontSize = with(LocalDensity.current) { (size * (26f / 44f)).toSp() }
    val shown = state != TileState.Hidden

    // Starts from the tile's current state: a tile already revealed when the screen is created
    // (or re-created by a rotation) is simply there, and never pops in again.
    val pop = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(shown) {
        if (shown && pop.value < 1f) {
            delay(popDelayMillis)
            // A low damping ratio gives the slight overshoot.
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
        } else if (!shown) {
            pop.snapTo(0f)
        }
    }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey != null) {
            pulse.animateTo(1.15f, tween(110))
            pulse.animateTo(1f, spring(dampingRatio = 0.4f))
        }
    }

    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            // The hidden tile is always underneath, so the crossword's shape shows from the start.
            .background(LoopColors.EmptyTile, shape)
            .border(1.5.dp, LoopColors.Outline, shape)
            // Tests (and screen readers) read the tile's state; a hidden tile exposes no letter.
            .semantics(mergeDescendants = true) { stateDescription = state.name },
        contentAlignment = Alignment.Center,
    ) {
        if (shown) {
            val face = if (state == TileState.Revealed) {
                Modifier.keycap(LoopColors.Marigold, LoopColors.MarigoldEdge, 3.dp, shape)
            } else {
                // Hinted: the dimmer style, a marigold outline and letter on the empty tile.
                Modifier.border(2.dp, LoopColors.Marigold, shape)
            }
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        alpha = pop.value.coerceIn(0f, 1f)
                    }
                    .then(face),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.toString(),
                    color = if (state == TileState.Revealed) LoopColors.OnMarigold else LoopColors.Marigold,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
