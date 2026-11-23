// The gameplay screen: top bar, crossword grid, word preview and letter wheel.
// It draws GameUiState and forwards user actions to GameViewModel; it holds no game rules.
package com.prabhat.letterloop.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.theme.LoopColors

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GameContent(state = state, onWordSubmitted = viewModel::onWordSubmitted)
}

@Composable
private fun GameContent(state: GameUiState, onWordSubmitted: (String) -> Unit) {
    // The word being swiped right now, reported by the wheel. Short-lived UI state: it stays here
    // and never goes to the ViewModel (Decision log #6).
    var swipeWord by remember { mutableStateOf("") }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(LoopColors.Ground)
            .safeDrawingPadding(),
    ) {
        // 280dp on the design phone; smaller if the screen is narrow or short.
        val wheelSize = minOf(280.dp, maxWidth - 32.dp, maxHeight * 0.42f)

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            GameTopBar(state)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // While loading, the top bar already shows the level number, so the screen is
                // never blank; the grid appears as soon as the level is parsed.
                state.game?.let { CrosswordGrid(it, Modifier.fillMaxSize()) }
            }
            WordPreview(swipeWord)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.phase == GamePhase.Playing) {
                    LetterWheel(
                        letters = state.letters,
                        enabled = state.game != null,
                        onWordSubmitted = onWordSubmitted,
                        onSelectionChange = { swipeWord = it },
                        modifier = Modifier
                            .size(wheelSize)
                            .testTag(TestTags.WHEEL),
                    )
                } else {
                    Box(Modifier.size(wheelSize), contentAlignment = Alignment.Center) {
                        Text("Level ${state.levelId} complete", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameTopBar(state: GameUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
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
                    .testTag(TestTags.FOUND_COUNTER)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text("${game.foundWords.size}/${game.level.words.size}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("words", fontSize = 13.sp, color = LoopColors.Muted)
            }
        }
    }
}
