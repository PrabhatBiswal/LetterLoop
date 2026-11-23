// The crossword grid. Every cell of the level is drawn from the start (so the puzzle's shape is
// visible), as hidden, hinted or revealed. It draws GameState and contains no game rules.
package com.prabhat.letterloop.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prabhat.letterloop.game.GameState
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.keycap
import com.prabhat.letterloop.ui.theme.LoopColors

enum class TileState { Hidden, Hinted, Revealed }

private val Gap = 4.dp

@Composable
fun CrosswordGrid(game: GameState, modifier: Modifier = Modifier) {
    val level = game.level
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // 44dp cells on the 390dp design phone (358dp of usable width), growing with the screen up
        // to 56dp, then shrunk if the grid would not fit the space this screen gives it.
        val preferred = (44.dp * (maxWidth / 358.dp)).coerceIn(44.dp, 56.dp)
        val fitsWidth = (maxWidth - Gap * (level.cols - 1)) / level.cols
        val fitsHeight = (maxHeight - Gap * (level.rows - 1)) / level.rows
        val cell = minOf(preferred, fitsWidth, fitsHeight)

        Box(Modifier.size(cell * level.cols + Gap * (level.cols - 1), cell * level.rows + Gap * (level.rows - 1))) {
            for (c in level.cells) {
                val state = when (c) {
                    in game.wordCells -> TileState.Revealed
                    in game.hintedCells -> TileState.Hinted
                    else -> TileState.Hidden
                }
                GridTile(
                    letter = level.letterAt.getValue(c),
                    state = state,
                    size = cell,
                    modifier = Modifier
                        .offset(x = (cell + Gap) * c.col, y = (cell + Gap) * c.row)
                        .testTag(TestTags.gridCell(c.row, c.col)),
                )
            }
        }
    }
}

@Composable
private fun GridTile(letter: Char, state: TileState, size: Dp, modifier: Modifier) {
    // The design is 44dp tiles with a 10dp radius and 26sp letters; keep those proportions.
    val shape = RoundedCornerShape(size * (10f / 44f))
    val fontSize = with(LocalDensity.current) { (size * (26f / 44f)).toSp() }
    val look = when (state) {
        TileState.Revealed -> Modifier.keycap(LoopColors.Marigold, LoopColors.MarigoldEdge, 3.dp, shape)
        TileState.Hinted -> Modifier.background(LoopColors.EmptyTile, shape).border(2.dp, LoopColors.Marigold, shape)
        TileState.Hidden -> Modifier.background(LoopColors.EmptyTile, shape).border(1.5.dp, LoopColors.Outline, shape)
    }
    Box(
        modifier
            .size(size)
            .then(look)
            // Tests (and screen readers) read the tile's state; a hidden tile exposes no letter.
            .semantics(mergeDescendants = true) { stateDescription = state.name },
        contentAlignment = Alignment.Center,
    ) {
        if (state != TileState.Hidden) {
            Text(
                text = letter.toString(),
                color = if (state == TileState.Revealed) LoopColors.OnMarigold else LoopColors.Marigold,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
