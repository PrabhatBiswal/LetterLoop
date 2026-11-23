// The Pause dialog's content. It is a Navigation dialog destination, so back, rotation and process
// death are handled by the back stack; this file only draws the card and reports button taps.
package com.prabhat.letterloop.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.prabhat.letterloop.R
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.PrimaryButton
import com.prabhat.letterloop.ui.components.SecondaryButton
import com.prabhat.letterloop.ui.theme.LoopColors

@Composable
fun PauseDialog(state: GameUiState, onResume: () -> Unit, onLevels: () -> Unit, onHome: () -> Unit) {
    // Turn off the window's own grey dim; we draw the design's scrim colour ourselves.
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(LoopColors.Scrim)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .background(LoopColors.Surface, shape)
                .border(1.5.dp, LoopColors.Outline, shape)
                // Scrolls only if a very short landscape screen cannot fit the card.
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag(TestTags.PAUSE_DIALOG),
        ) {
            Text("Paused", fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            val game = state.game
            if (game != null) {
                Text(
                    "Level ${state.levelId} · ${game.foundWords.size} of ${game.level.words.size} words found",
                    fontSize = 16.sp,
                    color = LoopColors.Muted,
                )
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Resume",
                onClick = onResume,
                height = 60.dp,
                icon = R.drawable.ic_play,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.RESUME_BUTTON),
            )
            Spacer(Modifier.height(12.dp))
            // Side by side, so the card still fits a short landscape screen.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    text = "Levels",
                    onClick = onLevels,
                    icon = R.drawable.ic_levels,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.PAUSE_LEVELS_BUTTON),
                )
                SecondaryButton(
                    text = "Home",
                    onClick = onHome,
                    icon = R.drawable.ic_home,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.PAUSE_HOME_BUTTON),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Your progress is saved.",
                fontSize = 14.sp,
                color = LoopColors.Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
