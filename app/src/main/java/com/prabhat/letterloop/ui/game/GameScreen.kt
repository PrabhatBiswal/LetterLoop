// The gameplay screen: top bar, crossword grid, word preview, letter wheel and its three buttons.
// Portrait stacks the grid above the wheel; landscape puts them side by side. It draws GameUiState
// and forwards user actions to GameViewModel; it holds no game rules.
package com.prabhat.letterloop.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prabhat.letterloop.R
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.RoundIconButton
import com.prabhat.letterloop.ui.components.loopRings
import com.prabhat.letterloop.ui.theme.LoopColors

/** The user actions the game screen can send. Plain function references to the ViewModel. */
private class GameActions(
    val onWordSubmitted: (String) -> Unit,
    val onPause: () -> Unit,
    val onHint: () -> Unit,
    val onShuffle: () -> Unit,
    val onBonus: () -> Unit,
    val onBackToLevels: () -> Unit,
)

@Composable
fun GameScreen(
    onPause: () -> Unit,
    onBackToLevels: () -> Unit,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // In the game, system back opens Pause instead of leaving the level.
    BackHandler(onBack = onPause)

    // The celebration countdown only runs while this screen is started (GameViewModel.onScreenStopped).
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenStarted()
        onStopOrDispose { viewModel.onScreenStopped() }
    }

    val actions = remember(viewModel, onPause, onBackToLevels) {
        GameActions(
            onWordSubmitted = viewModel::onWordSubmitted,
            onPause = onPause,
            onHint = viewModel::onHint,
            onShuffle = viewModel::onShuffle,
            onBonus = {}, // The bonus words sheet arrives in M4.
            onBackToLevels = onBackToLevels,
        )
    }
    GameContent(state, actions)
}

@Composable
private fun GameContent(state: GameUiState, actions: GameActions) {
    // The word being swiped right now, reported by the wheel. Short-lived UI state: it stays here
    // and never goes to the ViewModel (Decision log #6).
    var swipeWord by remember { mutableStateOf("") }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(LoopColors.Ground)
            // The rings reach far beyond the wheel; keep them inside this screen.
            .clipToBounds()
            .safeDrawingPadding()
            .testTag(TestTags.GAME_SCREEN),
    ) {
        // Read once here: Row and Column scopes below cannot see these sizes implicitly.
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        if (screenWidth > screenHeight) {
            // Landscape: grid on the left, wheel on the right; 44dp side padding for cut-outs.
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 44.dp, vertical = 8.dp),
            ) {
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    val game = state.game
                    // The top bar takes the grid's width, so the pause button and the counter line
                    // up with the grid's edges (with a minimum so its contents always fit).
                    val cell = game?.let { gridCellSize(it.level, maxWidth, maxHeight - 68.dp) }
                    val columnWidth = maxWidth
                    val barWidth = if (game != null && cell != null) gridWidth(game.level, cell) else columnWidth
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GameTopBar(state, actions.onPause, Modifier.width(barWidth.coerceIn(260.dp, columnWidth)))
                        Spacer(Modifier.height(12.dp))
                        if (game != null && cell != null) CrosswordGrid(game, cell)
                    }
                }
                Spacer(Modifier.width(24.dp))
                // zIndex -1 draws this column first, so its background rings pass under the grid.
                WheelSection(
                    state = state,
                    wheelSize = minOf(240.dp, screenHeight - 72.dp),
                    swipeWord = swipeWord,
                    onSwipeWordChange = { swipeWord = it },
                    actions = actions,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .zIndex(-1f),
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                GameTopBar(
                    state,
                    actions.onPause,
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // While loading, the top bar already shows the level number, so the screen is
                    // never blank; the grid appears as soon as the level is parsed.
                    state.game?.let { CrosswordGrid(it, gridCellSize(it.level, maxWidth, maxHeight)) }
                }
                WheelSection(
                    state = state,
                    // 280dp on the design phone; smaller on a narrow or short screen.
                    wheelSize = minOf(280.dp, screenWidth - 120.dp, screenHeight * 0.4f),
                    swipeWord = swipeWord,
                    onSwipeWordChange = { swipeWord = it },
                    actions = actions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .zIndex(-1f),
                )
            }
        }
    }
}

/** The preview pill and the wheel with its corner buttons, or the level-complete card. */
@Composable
private fun WheelSection(
    state: GameUiState,
    wheelSize: Dp,
    swipeWord: String,
    onSwipeWordChange: (String) -> Unit,
    actions: GameActions,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.phase == GamePhase.Playing) {
            WordPreview(swipeWord)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(wheelSize),
            ) {
                LetterWheel(
                    letters = state.letters,
                    enabled = state.game != null,
                    onWordSubmitted = actions.onWordSubmitted,
                    onSelectionChange = onSwipeWordChange,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(wheelSize)
                        .loopRings()
                        .testTag(TestTags.WHEEL),
                )
                RoundIconButton(
                    icon = R.drawable.ic_hint,
                    contentDescription = "Hint",
                    onClick = actions.onHint,
                    size = 52.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .testTag(TestTags.HINT_BUTTON),
                )
                RoundIconButton(
                    icon = R.drawable.ic_shuffle,
                    contentDescription = "Shuffle letters",
                    onClick = actions.onShuffle,
                    size = 52.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .testTag(TestTags.SHUFFLE_BUTTON),
                )
                BonusButton(
                    count = state.game?.foundBonus?.size ?: 0,
                    onClick = actions.onBonus,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        } else {
            // The card takes the place of the preview and wheel; the grid stays fully revealed.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(wheelSize + 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                LevelCompleteCard(state, actions.onBackToLevels)
            }
        }
    }
}

@Composable
private fun BonusButton(count: Int, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.testTag(TestTags.BONUS_BUTTON)) {
        RoundIconButton(
            icon = R.drawable.ic_star,
            contentDescription = "Bonus words, $count found",
            onClick = onClick,
            size = 52.dp,
            tint = LoopColors.Teal,
        )
        // The teal count badge, sitting on the button's top-right edge.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .background(LoopColors.Teal, CircleShape)
                .testTag(TestTags.BONUS_COUNT)
                .semantics(mergeDescendants = true) {},
            contentAlignment = Alignment.Center,
        ) {
            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LoopColors.OnTeal)
        }
    }
}

@Composable
private fun GameTopBar(state: GameUiState, onPause: () -> Unit, modifier: Modifier) {
    Box(modifier.height(56.dp)) {
        RoundIconButton(
            icon = R.drawable.ic_pause,
            contentDescription = "Pause",
            onClick = onPause,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .testTag(TestTags.PAUSE_BUTTON),
        )
        Text(
            text = "Level ${state.levelId}",
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(TestTags.LEVEL_TITLE),
        )
        state.game?.let { game ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .widthIn(min = 48.dp)
                    .testTag(TestTags.FOUND_COUNTER)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text("${game.foundWords.size}/${game.level.words.size}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("words", fontSize = 13.sp, color = LoopColors.Muted)
            }
        }
    }
}
