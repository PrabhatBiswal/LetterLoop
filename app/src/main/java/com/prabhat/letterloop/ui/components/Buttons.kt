// The app's buttons: a green primary button, a pale grey secondary button and a round icon button.
// Used on Home, Pause, Game and the complete card.
package com.prabhat.letterloop.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prabhat.letterloop.ui.theme.LoopColors

/**
 * Every button scales to about 0.96 while pressed. Returns the animated scale for a button's
 * interaction source; the caller applies it with graphicsLayer, so pressing only redraws.
 */
@Composable
fun rememberPressScale(interactionSource: InteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 0.96f else 1f, label = "pressScale")
}

/**
 * A slow "breathing" scale between 1 and [max], for the thing the player should tap next (the
 * Play button, the current level). Apply it with graphicsLayer so it only redraws.
 */
@Composable
fun rememberPulse(max: Float = 1.05f): State<Float> =
    rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = max,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    LabelButton(text, onClick, modifier, face = LoopColors.Play, edge = LoopColors.PlayEdge, content = LoopColors.Tile)

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    LabelButton(text, onClick, modifier, face = LoopColors.Locked, edge = LoopColors.TileEdge, content = LoopColors.Ink)

@Composable
private fun LabelButton(text: String, onClick: () -> Unit, modifier: Modifier, face: Color, edge: Color, content: Color) {
    val depth = 4.dp
    val interactionSource = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interactionSource)
    // 56dp tall in total: a 52dp face with its 4dp edge showing below it.
    Box(modifier.height(52.dp + depth).graphicsLayer { scaleX = scale; scaleY = scale }) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .keycap(face, edge, depth, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = content, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A round white 48dp button with an icon; 48dp so it is easy to hit. */
@Composable
fun IconOnlyButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LoopColors.Ink,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interactionSource)
    Box(
        modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            // The face is 3dp shorter than the touch target; its edge fills the rest.
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            Modifier
                .size(45.dp)
                .keycap(LoopColors.Tile, LoopColors.TileEdge, 3.dp, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}
