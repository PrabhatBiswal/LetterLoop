// The level-complete card that takes the wheel's place: what was found, then either "Next up"
// (the ViewModel moves on by itself) or, after the last level, a button back to Level Select.
package com.prabhat.letterloop.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.components.PrimaryButton
import com.prabhat.letterloop.ui.theme.LoopColors

@Composable
fun LevelCompleteCard(state: GameUiState, onBackToLevels: () -> Unit, modifier: Modifier = Modifier) {
    val game = state.game ?: return
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(LoopColors.Surface, shape)
            .border(1.5.dp, LoopColors.Outline, shape)
            .padding(24.dp)
            .testTag(TestTags.LEVEL_COMPLETE_CARD),
    ) {
        Text(
            text = buildAnnotatedString {
                append("Level ${state.levelId} ")
                withStyle(SpanStyle(color = LoopColors.Marigold)) { append("complete") }
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        val words = game.level.words.size
        val bonus = game.foundBonus
        Text(
            text = if (bonus.isEmpty()) "All $words words found" else "All $words words found, plus ${bonus.size} bonus:",
            fontSize = 17.sp,
            color = LoopColors.Muted,
        )
        if (bonus.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            BonusChips(bonus)
        }
        Spacer(Modifier.height(20.dp))
        if (state.phase == GamePhase.AllLevelsDone) {
            Text("That was the last level. Every level is solved!", fontSize = 17.sp)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Back to levels",
                onClick = onBackToLevels,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.BACK_TO_LEVELS_BUTTON),
            )
        } else {
            // M4 animates this bar filling over the 1.5 second wait.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(LoopColors.EmptyTile, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(LoopColors.Marigold, CircleShape),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Next up: Level ${state.levelId + 1}", fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Found bonus words as teal-bordered chips. Also used by the bonus words sheet. */
@Composable
fun BonusChips(words: Collection<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        words.forEach { word ->
            Text(
                text = word,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .background(LoopColors.EmptyTile, CircleShape)
                    .border(1.5.dp, LoopColors.Teal, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}
