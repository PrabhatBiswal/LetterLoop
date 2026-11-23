// The pill above the wheel that shows the word being swiped. Its 48dp of height is always
// reserved, so the wheel never jumps when the pill appears or disappears.
package com.prabhat.letterloop.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.theme.LoopColors

@Composable
fun WordPreview(word: String, modifier: Modifier = Modifier) {
    Box(modifier.height(48.dp), contentAlignment = Alignment.Center) {
        if (word.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .background(LoopColors.Surface, CircleShape)
                    .border(2.dp, LoopColors.Marigold, CircleShape)
                    .padding(horizontal = 22.dp)
                    .testTag(TestTags.WORD_PREVIEW)
                    .semantics(mergeDescendants = true) {},
                contentAlignment = Alignment.Center,
            ) {
                Text(word, fontSize = 26.sp, letterSpacing = 5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
