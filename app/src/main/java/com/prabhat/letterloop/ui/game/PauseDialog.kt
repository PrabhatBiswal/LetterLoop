// The Pause dialog's content. It is a Navigation dialog destination, so rotation and process death
// are handled by the back stack; this file draws the card, scales and fades it in and out, and
// reports which button was tapped once the exit animation has finished.
package com.prabhat.letterloop.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.PrimaryButton
import com.prabhat.letterloop.ui.components.SecondaryButton
import androidx.compose.ui.text.font.FontWeight
import com.prabhat.letterloop.ui.theme.LoopColors

@Composable
fun PauseDialog(state: GameUiState, onResume: () -> Unit, onLevels: () -> Unit, onHome: () -> Unit) {
    // Turn off the window's own grey dim; we draw our own scrim colour, so it can fade with the card.
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0f) }

    // Starts hidden and immediately targets visible, so the dialog animates in.
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    // What to do once the exit animation ends: pop back to the game, Level Select or Home.
    var afterExit by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun close(action: () -> Unit) {
        afterExit = action
        visibility.targetState = false
    }
    LaunchedEffect(visibility.isIdle, visibility.currentState) {
        if (visibility.isIdle && !visibility.currentState) afterExit?.invoke()
    }
    // Back resumes the game like the Resume button, so it gets the same exit animation. Without
    // this the dialog destination would still close on back, just without animating.
    BackHandler(enabled = visibility.targetState) { close(onResume) }

    AnimatedVisibility(visibility, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(LoopColors.Scrim)
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            PauseCard(
                state = state,
                onResume = { close(onResume) },
                onLevels = { close(onLevels) },
                onHome = { close(onHome) },
                // The card scales as well as fading with the scrim.
                modifier = Modifier.animateEnterExit(
                    enter = scaleIn(tween(220), initialScale = 0.88f) + fadeIn(tween(180)),
                    exit = scaleOut(tween(160), targetScale = 0.92f) + fadeOut(tween(140)),
                ),
            )
        }
    }
}

@Composable
private fun PauseCard(
    state: GameUiState,
    onResume: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .background(LoopColors.Tile, RoundedCornerShape(24.dp))
            // Scrolls only if a very short landscape screen cannot fit the card.
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag(TestTags.PAUSE_DIALOG),
    ) {
        Text("Paused", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        val game = state.game
        if (game != null) {
            Text(
                "Level ${state.levelId} · ${game.foundWords.size} of ${game.level.words.size} words found",
                fontSize = 14.sp,
                color = LoopColors.Muted,
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Resume",
            onClick = onResume,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.RESUME_BUTTON),
        )
        Spacer(Modifier.height(10.dp))
        // Side by side, so the card still fits a short landscape screen.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = "Levels",
                onClick = onLevels,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.PAUSE_LEVELS_BUTTON),
            )
            SecondaryButton(
                text = "Home",
                onClick = onHome,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.PAUSE_HOME_BUTTON),
            )
        }
    }
}
