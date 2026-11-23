// The app's buttons, built on the keycap look instead of Material's: primary (marigold),
// secondary (dark tile) and round icon buttons. Used on Home, Pause, Game and the complete card.
package com.prabhat.letterloop.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prabhat.letterloop.ui.theme.LoopColors

/**
 * "Any button scales to about 0.96 while pressed" (section 9). Returns the animated scale for a
 * button's interaction source; the caller applies it with graphicsLayer, so pressing only redraws.
 */
@Composable
fun rememberPressScale(interactionSource: InteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 0.96f else 1f, label = "pressScale")
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    @DrawableRes icon: Int? = null,
) = KeycapButton(text, onClick, modifier, height, icon, LoopColors.Marigold, LoopColors.MarigoldEdge, LoopColors.OnMarigold)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    @DrawableRes icon: Int? = null,
) = KeycapButton(text, onClick, modifier, height, icon, LoopColors.EmptyTile, LoopColors.Ground, LoopColors.Cream)

@Composable
private fun KeycapButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    height: Dp,
    @DrawableRes icon: Int?,
    face: Color,
    edge: Color,
    content: Color,
) {
    val depth = 6.dp
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interactionSource)
    // [height] includes the edge: the face is shorter by `depth` and the edge fills the rest.
    Box(modifier.height(height).graphicsLayer { scaleX = scale; scaleY = scale }) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(height - depth)
                .keycap(face, edge, depth, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = content, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A round Surface button with an icon, at least 48dp so it is easy to hit. */
@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = LoopColors.Cream,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interactionSource)
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(LoopColors.Surface, CircleShape)
            .border(1.5.dp, LoopColors.Outline, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}
