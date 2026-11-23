// Level Select: one card per chapter (Meadow, Dusk, Night), each holding its levels as tiles that
// show solved / current / locked. The chapters come from each level's letter count in the data,
// not from hard-coded ranges.
package com.prabhat.letterloop.ui.levels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prabhat.letterloop.R
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.IconOnlyButton
import com.prabhat.letterloop.ui.components.Scene
import com.prabhat.letterloop.ui.components.SceneScreen
import com.prabhat.letterloop.ui.components.keycap
import com.prabhat.letterloop.ui.components.rememberPressScale
import com.prabhat.letterloop.ui.components.rememberPulse
import com.prabhat.letterloop.ui.components.sceneFor
import com.prabhat.letterloop.ui.theme.LoopColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LevelSelectScreen(
    onBack: () -> Unit,
    onLevelClick: (Int) -> Unit,
    onScene: (Scene) -> Unit = {},
    viewModel: LevelSelectViewModel = viewModel(factory = LevelSelectViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The scenery of the chapter the player has reached, the same as on Home.
    val reached = state.levels.firstOrNull { it.status == LevelStatus.Current } ?: state.levels.lastOrNull()

    SceneScreen(
        reached?.let { sceneFor(it.letterCount) },
        onScene,
        Modifier
            .fillMaxSize()
            .testTag(TestTags.LEVEL_SELECT_SCREEN),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconOnlyButton(
                    icon = R.drawable.ic_back,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.testTag(TestTags.LEVELS_BACK_BUTTON),
                )
                Spacer(Modifier.width(12.dp))
                Text("Levels", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = LoopColors.Tile, modifier = Modifier.weight(1f))
                if (state.levels.isNotEmpty()) {
                    Text(
                        "${state.solvedCount}/${state.levels.size}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = LoopColors.Tile,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            // Fifteen tiles is few enough to lay out all at once; no lazy list needed.
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // groupBy keeps the order levels appear in the file: 3, 4, then 5 letters.
                state.levels.groupBy { it.letterCount }.forEach { (letterCount, tiles) ->
                    ChapterCard(sceneFor(letterCount), letterCount, tiles, onLevelClick)
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(scene: Scene, letterCount: Int, tiles: List<LevelTile>, onLevelClick: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LoopColors.Tile),
    ) {
        // The banner is a strip of the chapter's own sky.
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(scene.skyTop, scene.skyBottom)))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(scene.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = LoopColors.Tile)
            Spacer(Modifier.width(8.dp))
            Text("$letterCount letters", fontSize = 14.sp, color = LoopColors.Tile)
        }
        FlowRow(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            tiles.forEach { tile ->
                LevelTileButton(
                    tile = tile,
                    scene = scene,
                    // A small stagger: each tile appears 25ms after the one before it.
                    appearDelayMillis = (tile.id - 1) * 25L,
                    onOpen = { onLevelClick(tile.id) },
                )
            }
        }
    }
}

@Composable
private fun LevelTileButton(tile: LevelTile, scene: Scene, appearDelayMillis: Long, onOpen: () -> Unit) {
    // Solved: a tile in the chapter's accent colour. Current: a white tile with an accent border
    // that pulses. Locked: a flat grey tile with a lock.
    val shape = RoundedCornerShape(12.dp)
    val look = when (tile.status) {
        LevelStatus.Completed -> Modifier.keycap(scene.accent, scene.accentEdge, 3.dp, shape)
        LevelStatus.Current -> Modifier
            .keycap(LoopColors.Tile, LoopColors.TileEdge, 3.dp, shape)
            .border(2.5.dp, scene.accent, shape)
        LevelStatus.Locked -> Modifier.background(LoopColors.Locked, shape)
    }
    val statusText = when (tile.status) {
        LevelStatus.Completed -> "solved"
        LevelStatus.Current -> "next to play"
        LevelStatus.Locked -> "locked"
    }

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(appearDelayMillis)
        appear.animateTo(1f, tween(220))
    }
    // A locked level does nothing except a small sideways shake (section 8).
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource)
    // Only the level to play next "breathes".
    val pulse = if (tile.status == LevelStatus.Current) rememberPulse(max = 1.08f).value else 1f

    Box(
        Modifier
            .size(52.dp)
            // All animated values are read here, in the layer, so they only redraw the tile.
            .graphicsLayer {
                val grow = 0.85f + 0.15f * appear.value
                alpha = appear.value
                scaleX = grow * pressScale * pulse
                scaleY = grow * pressScale * pulse
                translationX = shake.value.dp.toPx()
            }
            .then(look)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    if (tile.status == LevelStatus.Locked) {
                        scope.launch {
                            shake.animateTo(0f, keyframes {
                                durationMillis = 300
                                -8f at 50
                                8f at 110
                                -5f at 170
                                3f at 230
                            })
                        }
                    } else {
                        onOpen()
                    }
                },
            )
            .semantics(mergeDescendants = true) { contentDescription = "Level ${tile.id}, $statusText" }
            .testTag(TestTags.levelTile(tile.id)),
        contentAlignment = Alignment.Center,
    ) {
        if (tile.status == LevelStatus.Locked) {
            Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = LoopColors.Muted, modifier = Modifier.size(18.dp))
        } else {
            Text(
                "${tile.id}",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (tile.status == LevelStatus.Completed) LoopColors.Tile else scene.accent,
            )
        }
    }
}
