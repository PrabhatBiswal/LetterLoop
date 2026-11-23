// The gameplay screen: top bar, crossword grid, word preview, letter wheel and its three buttons.
// Portrait stacks the grid above the wheel; landscape puts them side by side. It draws GameUiState,
// plays the feedback animations, and forwards user actions to GameViewModel; it holds no game rules.
package com.prabhat.letterloop.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prabhat.letterloop.R
import com.prabhat.letterloop.game.PlacedWord
import com.prabhat.letterloop.game.SubmitResult
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.IconOnlyButton
import com.prabhat.letterloop.ui.components.Scene
import com.prabhat.letterloop.ui.components.SceneScreen
import com.prabhat.letterloop.ui.components.sceneFor
import com.prabhat.letterloop.ui.theme.LoopColors
import kotlinx.coroutines.delay

/** The user actions the game screen can send: function references to the ViewModel and nav. */
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
    onScene: (Scene) -> Unit = {},
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

    // One-shot feedback (section 9 rules): play it, then report it shown, so a rotation afterwards
    // does not replay it. Keyed on the id: a new word restarts this even mid-animation. The game
    // state has already changed; this only decides how long the pill and tiles keep animating.
    val haptics = LocalHapticFeedback.current
    val feedback = state.feedback
    LaunchedEffect(feedback?.id) {
        if (feedback == null) return@LaunchedEffect
        if (feedback.result == SubmitResult.GridWord) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        delay(feedbackMillis(feedback.result))
        viewModel.onFeedbackShown(feedback.id)
    }

    // Saveable, so the bonus words sheet stays open across a rotation.
    var showBonusWords by rememberSaveable { mutableStateOf(false) }
    // Confetti plays once per completed level. Saveable, so rotating while the complete card is
    // still showing (the all-done card stays until the player leaves) does not replay it.
    var confettiDoneFor by rememberSaveable { mutableIntStateOf(0) }

    val actions = remember(viewModel, onPause, onBackToLevels) {
        GameActions(
            onWordSubmitted = viewModel::onWordSubmitted,
            onPause = onPause,
            onHint = viewModel::onHint,
            onShuffle = viewModel::onShuffle,
            onBonus = { showBonusWords = true },
            onBackToLevels = onBackToLevels,
        )
    }

    // The scenery comes from the level's chapter (its letter count). No letters yet means the
    // level is still loading.
    val scene = if (state.letters.isEmpty()) null else sceneFor(state.letters.length)
    SceneScreen(
        scene,
        onScene,
        Modifier
            .fillMaxSize()
            .testTag(TestTags.GAME_SCREEN),
    ) {
        // Next level: the new one slides in from the side. Within a level the content just updates.
        AnimatedContent(
            targetState = state,
            contentKey = { it.levelId },
            transitionSpec = {
                (slideInHorizontally(tween(400)) { it } + fadeIn(tween(400))) togetherWith
                    (slideOutHorizontally(tween(400)) { -it } + fadeOut(tween(300)))
            },
            label = "levelChange",
        ) { levelState ->
            GameLayout(levelState, actions)
        }
        if (state.phase != GamePhase.Playing && confettiDoneFor != state.levelId) {
            val levelId = state.levelId
            Confetti(seed = levelId, onFinished = { confettiDoneFor = levelId }, modifier = Modifier.fillMaxSize())
        }
    }

    if (showBonusWords) {
        BonusWordsSheet(state.game?.foundBonus.orEmpty(), onDismiss = { showBonusWords = false })
    }
}

@Composable
private fun GameLayout(state: GameUiState, actions: GameActions) {
    // The word being swiped right now, reported by the wheel. Short-lived UI state: it stays here
    // and never goes to the ViewModel (Decision log #6).
    var swipeWord by remember { mutableStateOf("") }
    val flight = remember { FlightPath() }

    // What the grid should animate for the current feedback, if anything.
    val feedback = state.feedback
    val words = state.game?.level?.words.orEmpty()
    val popWord: PlacedWord? = words.firstOrNull { feedback?.result == SubmitResult.GridWord && it.word == feedback.word }
    val pulse: Pair<Long, PlacedWord>? = words
        .firstOrNull { feedback?.result == SubmitResult.AlreadyFound && it.word == feedback.word }
        ?.let { feedback!!.id to it }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        // Read once here: Row and Column scopes below cannot see these sizes implicitly.
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        if (screenWidth > screenHeight) {
            // Landscape: grid on the left, wheel on the right. An 844dp-wide landscape phone gets
            // 44dp side padding for cut-outs and a 320dp wheel column; both scale down on smaller
            // screens so the grid keeps enough room (a 640dp-wide phone crashed before).
            val sidePadding = 44.dp * (screenWidth / 844.dp).coerceAtMost(1f)
            val usableWidth = screenWidth - sidePadding * 2 - 24.dp
            val wheelColumnWidth = minOf(320.dp, usableWidth * 0.5f)
            // Keeps the column at least 1.3x the wheel, so its corner buttons clear the circle.
            val landscapeWheel = minOf(240.dp, screenHeight - 72.dp, wheelColumnWidth / 1.3f)
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = sidePadding, vertical = 8.dp),
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
                    val cell = game?.let { gridCellSize(it.level, maxWidth, maxHeight - 60.dp) }
                    val columnWidth = maxWidth
                    val gridWidth = if (game != null && cell != null) gridWidth(game.level, cell) else columnWidth
                    // As wide as the grid, but at least 260dp when the column allows it.
                    val barWidth = maxOf(gridWidth, minOf(260.dp, columnWidth))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GameTopBar(state, actions.onPause, Modifier.width(barWidth))
                        Spacer(Modifier.height(12.dp))
                        if (game != null && cell != null) CrosswordGrid(game, cell, popWord = popWord, pulse = pulse)
                    }
                }
                Spacer(Modifier.width(24.dp))
                WheelSection(
                    state = state,
                    wheelSize = landscapeWheel,
                    swipeWord = swipeWord,
                    onSwipeWordChange = { swipeWord = it },
                    flight = flight,
                    actions = actions,
                    modifier = Modifier
                        .width(wheelColumnWidth)
                        .fillMaxHeight(),
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
                    state.game?.let {
                        CrosswordGrid(it, gridCellSize(it.level, maxWidth, maxHeight), popWord = popWord, pulse = pulse)
                    }
                }
                WheelSection(
                    state = state,
                    // 280dp on a normal phone; smaller on a narrow or short screen.
                    wheelSize = minOf(280.dp, screenWidth - 120.dp, screenHeight * 0.4f),
                    swipeWord = swipeWord,
                    onSwipeWordChange = { swipeWord = it },
                    flight = flight,
                    actions = actions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
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
    flight: FlightPath,
    actions: GameActions,
    modifier: Modifier,
) {
    // Level complete: the wheel and pill fade out completely and the card slides up in their place.
    AnimatedContent(
        targetState = state.phase == GamePhase.Playing,
        transitionSpec = {
            (fadeIn(tween(250, delayMillis = 150)) + slideInVertically(tween(450)) { it / 3 }) togetherWith
                fadeOut(tween(200))
        },
        contentAlignment = Alignment.Center,
        label = "wheelOrCard",
        modifier = modifier,
    ) { playing ->
        if (playing) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                WordPreview(swipeWord, state.feedback, flight)
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
                            .testTag(TestTags.WHEEL),
                    )
                    IconOnlyButton(
                        icon = R.drawable.ic_hint,
                        contentDescription = "Hint",
                        onClick = actions.onHint,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag(TestTags.HINT_BUTTON),
                    )
                    IconOnlyButton(
                        icon = R.drawable.ic_shuffle,
                        contentDescription = "Shuffle letters",
                        onClick = actions.onShuffle,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .testTag(TestTags.SHUFFLE_BUTTON),
                    )
                    BonusButton(
                        count = state.game?.foundBonus?.size ?: 0,
                        onClick = actions.onBonus,
                        flight = flight,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        } else {
            // At least the space the pill and wheel used, but free to grow: on a small screen the
            // card (two-line title, or the "Back to levels" button) can be taller than the wheel.
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = wheelSize + 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                LevelCompleteCard(state, actions.onBackToLevels)
            }
        }
    }
}

@Composable
private fun BonusButton(count: Int, onClick: () -> Unit, flight: FlightPath, modifier: Modifier) {
    // The badge shows `shownCount`, which catches up with `count` as the gold word box arrives. It
    // starts at the current count, so a re-created screen neither bumps nor ticks.
    var shownCount by remember { mutableIntStateOf(count) }
    val bump = remember { Animatable(1f) }
    LaunchedEffect(count) {
        if (count > shownCount) {
            delay(420) // the word box's flight time
            shownCount = count
            bump.animateTo(1.22f, tween(110))
            bump.animateTo(1f, spring(dampingRatio = 0.4f))
        } else {
            shownCount = count
        }
    }
    Box(
        modifier
            .onGloballyPositioned { flight.bonusButton = it.boundsInRoot().center }
            .graphicsLayer { scaleX = bump.value; scaleY = bump.value }
            .testTag(TestTags.BONUS_BUTTON),
    ) {
        IconOnlyButton(
            icon = R.drawable.ic_star,
            contentDescription = "Bonus words, $count found",
            onClick = onClick,
            tint = LoopColors.Gold,
        )
        // No badge until the first bonus word: a "0" is only noise.
        if (shownCount > 0) {
            CountBadge(
                shownCount,
                Modifier
                    .align(Alignment.TopEnd)
                    .testTag(TestTags.BONUS_COUNT),
            )
        }
    }
}

/** The small count badge; the number ticks up with a short slide. */
@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(18.dp)
            .background(LoopColors.Ink, CircleShape)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
            label = "badgeCount",
        ) { value ->
            Text("$value", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LoopColors.Tile)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BonusWordsSheet(words: Collection<String>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LoopColors.Tile,
        scrimColor = LoopColors.Scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = LoopColors.TileEdge) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bonus words", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                CountBadge(words.size)
            }
            Spacer(Modifier.height(12.dp))
            if (words.isEmpty()) {
                Text(
                    "None yet. Real words the letters spell that are not in the grid count as bonus words.",
                    fontSize = 14.sp,
                    color = LoopColors.Muted,
                )
            } else {
                BonusChips(words)
            }
        }
    }
}

@Composable
private fun GameTopBar(state: GameUiState, onPause: () -> Unit, modifier: Modifier) {
    Box(modifier.height(48.dp)) {
        IconOnlyButton(
            icon = R.drawable.ic_pause,
            contentDescription = "Pause",
            onClick = onPause,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .testTag(TestTags.PAUSE_BUTTON),
        )
        Text(
            text = "Level ${state.levelId}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = LoopColors.Tile,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(TestTags.LEVEL_TITLE),
        )
        state.game?.let { game ->
            // Grid words found so far, out of the level's total.
            Text(
                "${game.foundWords.size}/${game.level.words.size}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = LoopColors.Tile,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .testTag(TestTags.FOUND_COUNTER),
            )
        }
    }
}
