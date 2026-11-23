// The start screen: the crossword logo (LETTER across, LOOP down from the shared L), the Play
// button, and how far the player has got. Play always goes to Level Select.
package com.prabhat.letterloop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prabhat.letterloop.R
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.PrimaryButton
import com.prabhat.letterloop.ui.components.keycap
import com.prabhat.letterloop.ui.components.loopRings
import com.prabhat.letterloop.ui.levels.LevelSelectUiState
import com.prabhat.letterloop.ui.levels.LevelSelectViewModel
import com.prabhat.letterloop.ui.theme.LoopColors

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    viewModel: LevelSelectViewModel = viewModel(factory = LevelSelectViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(LoopColors.Ground)
            // The rings reach far beyond the logo; keep them inside this screen.
            .clipToBounds()
            .safeDrawingPadding()
            .padding(24.dp)
            .testTag(TestTags.HOME_SCREEN),
    ) {
        // Landscape puts the logo beside the Play area so nothing is clipped on a short screen.
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Logo(Modifier.weight(1f))
                Spacer(Modifier.width(24.dp))
                PlayArea(state, onPlay, Modifier.width(320.dp))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Logo(Modifier.padding(top = 24.dp))
                Spacer(Modifier.weight(1f))
                PlayArea(state, onPlay, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PlayArea(state: LevelSelectUiState, onPlay: () -> Unit, modifier: Modifier) {
    val total = state.levels.size
    Column(modifier) {
        PrimaryButton(
            text = "Play",
            onClick = onPlay,
            icon = R.drawable.ic_play,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.PLAY_BUTTON),
        )
        if (total > 0) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val next = state.nextLevel
                Text(
                    text = if (next != null) "Next up: Level $next" else "All levels solved",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text("${state.solvedCount} of $total solved", fontSize = 16.sp, color = LoopColors.Muted)
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(state.solvedCount / total.toFloat())
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(LoopColors.EmptyTile, CircleShape),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(LoopColors.Marigold, CircleShape),
            )
        }
    }
}

// LETTER runs across in six tiles; LOOP runs down from the same L. Tiles are 48dp with 5dp gaps
// on the design phone, smaller if six of them would not fit the width.
@Composable
private fun Logo(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 5.dp
        val tile = minOf(48.dp, (maxWidth - gap * 5) / 6)
        val step = tile + gap
        val letterSize = with(LocalDensity.current) { (tile * (28f / 48f)).toSp() }

        Box(Modifier.size(step * 6 - gap, step * 4 - gap)) {
            // One description for the whole logo instead of eight separate letters.
            Box(Modifier.clearAndSetSemantics { contentDescription = "LetterLoop" }) {
                // The shared L is drawn first, and carries the background rings centred on it.
                LogoTile('L', marigold = true, size = tile, fontSize = letterSize, modifier = Modifier.loopRings())
                "ETTER".forEachIndexed { i, ch ->
                    LogoTile(ch, marigold = false, size = tile, fontSize = letterSize, modifier = Modifier.offset(x = step * (i + 1)))
                }
                "OOP".forEachIndexed { i, ch ->
                    LogoTile(ch, marigold = true, size = tile, fontSize = letterSize, modifier = Modifier.offset(y = step * (i + 1)))
                }
            }
            Text(
                text = "Swipe the letters.\nFill the grid.",
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                color = LoopColors.Muted,
                lineHeight = 30.sp,
                modifier = Modifier.offset(x = step + 14.dp, y = step + 10.dp),
            )
        }
    }
}

@Composable
private fun LogoTile(letter: Char, marigold: Boolean, size: Dp, fontSize: TextUnit, modifier: Modifier) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier
            .size(size)
            .then(
                if (marigold) Modifier.keycap(LoopColors.Marigold, LoopColors.MarigoldEdge, 4.dp, shape)
                else Modifier.background(LoopColors.Surface, shape).border(1.5.dp, LoopColors.Outline, shape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = if (marigold) LoopColors.OnMarigold else LoopColors.Cream,
        )
    }
}
