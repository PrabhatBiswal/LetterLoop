// Level Select: every level as a tile, grouped by wheel size, showing solved / current / locked.
// The groups come from each level's letter count in the data, not from hard-coded ranges.
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.prabhat.letterloop.ui.components.RoundIconButton
import com.prabhat.letterloop.ui.components.keycap
import com.prabhat.letterloop.ui.components.rememberPressScale
import com.prabhat.letterloop.ui.theme.LoopColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LevelSelectScreen(
    onBack: () -> Unit,
    onLevelClick: (Int) -> Unit,
    viewModel: LevelSelectViewModel = viewModel(factory = LevelSelectViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(LoopColors.Ground)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp)
            .testTag(TestTags.LEVEL_SELECT_SCREEN),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = R.drawable.ic_back,
                contentDescription = "Back",
                onClick = onBack,
                modifier = Modifier.testTag(TestTags.LEVELS_BACK_BUTTON),
            )
            Spacer(Modifier.width(14.dp))
            Text("Levels", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (state.levels.isNotEmpty()) {
                Text("${state.solvedCount}/${state.levels.size}", fontSize = 18.sp, color = LoopColors.Muted)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            modifier = Modifier.weight(1f),
        ) {
            // groupBy keeps the order levels appear in the file: 3, 4, then 5 letters.
            state.levels.groupBy { it.letterCount }.forEach { (letterCount, tiles) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "header_$letterCount") {
                    Text(
                        "$letterCount letters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = LoopColors.Muted,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(tiles, key = { it.id }) { tile ->
                    LevelTileButton(
                        tile = tile,
                        // A small stagger: each tile appears 25ms after the one before it.
                        appearDelayMillis = (tile.id - 1) * 25L,
                        onOpen = { onLevelClick(tile.id) },
                    )
                }
            }
            if (state.levels.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "footer") {
                    Text(
                        "Solve a level to unlock the next one.",
                        fontSize = 15.sp,
                        color = LoopColors.Muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelTileButton(tile: LevelTile, appearDelayMillis: Long, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val look = when (tile.status) {
        LevelStatus.Completed -> Modifier.keycap(LoopColors.Marigold, LoopColors.MarigoldEdge, 5.dp, shape)
        LevelStatus.Current -> Modifier
            .marigoldGlow()
            .background(LoopColors.Surface, shape)
            .border(3.dp, LoopColors.Marigold, shape)
        LevelStatus.Locked -> Modifier
            .background(LoopColors.LockedTile, shape)
            .border(1.5.dp, LoopColors.LockedBorder, shape)
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

    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            // All animated values are read here, in the layer, so they only redraw the tile.
            .graphicsLayer {
                val grow = 0.85f + 0.15f * appear.value
                alpha = appear.value
                scaleX = grow * pressScale
                scaleY = grow * pressScale
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
        when (tile.status) {
            LevelStatus.Completed -> {
                Text("${tile.id}", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = LoopColors.OnMarigold)
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = LoopColors.OnMarigold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp),
                )
            }
            LevelStatus.Current ->
                Text("${tile.id}", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = LoopColors.Cream)
            LevelStatus.Locked -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = LoopColors.LockedContent, modifier = Modifier.size(20.dp))
                Text("${tile.id}", fontSize = 13.sp, color = LoopColors.LockedContent)
            }
        }
    }
}

// A soft glow: a few rounded rectangles around the tile, each larger and fainter than the last.
private fun Modifier.marigoldGlow(): Modifier = drawBehind {
    for (i in 1..4) {
        val spread = (i * 2).dp.toPx()
        drawRoundRect(
            color = LoopColors.Marigold.copy(alpha = 0.14f - i * 0.03f),
            topLeft = Offset(-spread, -spread),
            size = Size(size.width + spread * 2, size.height + spread * 2),
            cornerRadius = CornerRadius(16.dp.toPx() + spread),
        )
    }
}
