// The start screen: the crossword logo (LETTER across, LOOP down from the shared L) dropping in
// over the scenery, how far the player has got, and the Play button. Play always goes to Level Select.
package com.prabhat.letterloop.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.LocalScene
import com.prabhat.letterloop.ui.components.PrimaryButton
import com.prabhat.letterloop.ui.components.Scene
import com.prabhat.letterloop.ui.components.SceneScreen
import com.prabhat.letterloop.ui.components.keycap
import com.prabhat.letterloop.ui.components.rememberPulse
import com.prabhat.letterloop.ui.components.sceneFor
import com.prabhat.letterloop.ui.levels.LevelSelectUiState
import com.prabhat.letterloop.ui.levels.LevelSelectViewModel
import com.prabhat.letterloop.ui.levels.LevelStatus
import com.prabhat.letterloop.ui.theme.LoopColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    onScene: (Scene) -> Unit = {},
    viewModel: LevelSelectViewModel = viewModel(factory = LevelSelectViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The scenery of the chapter the player has reached (the last chapter once all are solved).
    val reached = state.levels.firstOrNull { it.status == LevelStatus.Current } ?: state.levels.lastOrNull()

    SceneScreen(
        reached?.let { sceneFor(it.letterCount) },
        onScene,
        Modifier
            .fillMaxSize()
            .testTag(TestTags.HOME_SCREEN),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
        ) {
            // Landscape puts the logo beside the Play area so nothing is clipped on a short screen.
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Logo(Modifier.weight(1f))
                    Spacer(Modifier.width(24.dp))
                    PlayArea(state, onPlay, Modifier.width(280.dp))
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    // The logo sits in the sky; the button sits on the hills, within thumb reach.
                    Spacer(Modifier.weight(0.6f))
                    Logo(Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    PlayArea(state, onPlay, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayArea(state: LevelSelectUiState, onPlay: () -> Unit, modifier: Modifier) {
    val total = state.levels.size
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (total > 0) {
            val next = state.nextLevel
            // On a dark see-through pill, so it reads over any scenery.
            Text(
                text = (if (next != null) "Level $next is next" else "All levels solved") + "  ·  ${state.solvedCount} of $total solved",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = LoopColors.Tile,
                modifier = Modifier
                    .background(Color(0x4D000000), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
        // The Play button "breathes" to invite a tap.
        val pulse by rememberPulse()
        PrimaryButton(
            text = "Play",
            onClick = onPlay,
            modifier = Modifier
                .width(220.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .testTag(TestTags.PLAY_BUTTON),
        )
    }
}

// LETTER runs across in six white tiles; LOOP runs down from the same L in the scene's accent
// colour, like a found word. Tiles are 48dp with 5dp gaps, smaller if six would not fit the width.
@Composable
private fun Logo(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val gap = 5.dp
        val tile = minOf(48.dp, (maxWidth - gap * 5) / 6)
        val step = tile + gap
        val letterSize = with(LocalDensity.current) { (tile * 0.55f).toSp() }

        // One description for the whole logo instead of nine separate letters.
        Box(
            Modifier
                .size(step * 6 - gap, step * 4 - gap)
                .clearAndSetSemantics { contentDescription = "LetterLoop" },
        ) {
            // `order` is the tile's turn to drop in: L, then ETTER, then OOP.
            LogoTile('L', accent = true, size = tile, fontSize = letterSize, order = 0, modifier = Modifier)
            "ETTER".forEachIndexed { i, ch ->
                LogoTile(ch, accent = false, size = tile, fontSize = letterSize, order = i + 1, modifier = Modifier.offset(x = step * (i + 1)))
            }
            "OOP".forEachIndexed { i, ch ->
                LogoTile(ch, accent = true, size = tile, fontSize = letterSize, order = i + 6, modifier = Modifier.offset(y = step * (i + 1)))
            }
        }
    }
}

@Composable
private fun LogoTile(letter: Char, accent: Boolean, size: Dp, fontSize: TextUnit, order: Int, modifier: Modifier) {
    val scene = LocalScene.current
    // Drops in from above with a small bounce, 70ms after the tile before it.
    val drop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order * 70L)
        drop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    Box(
        modifier
            .size(size)
            // Read in the layer, so the drop only redraws the tile.
            .graphicsLayer {
                translationY = (drop.value - 1f) * 60.dp.toPx()
                alpha = drop.value.coerceIn(0f, 1f)
            }
            .keycap(
                face = if (accent) scene.accent else LoopColors.Tile,
                edge = if (accent) scene.accentEdge else LoopColors.TileEdge,
                depth = 4.dp,
                shape = RoundedCornerShape(size * 0.2f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = if (accent) LoopColors.Tile else LoopColors.Ink,
        )
    }
}
