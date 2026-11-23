// The scenery behind the screens: sky, sun or moon, clouds or stars, and three rows of hills,
// all drawn on a Canvas (no image files). Each chapter of levels has its own Scene, which also
// supplies the accent colour used for found words and the swipe line.
package com.prabhat.letterloop.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Scene(
    val name: String,
    /** The wheel size of this chapter's levels; also how a scene is saved and found again. */
    val letterCount: Int,
    val skyTop: Color,
    val skyBottom: Color,
    /** The sun (or moon): its colour and its centre as fractions of the screen. */
    val sun: Color,
    val sunX: Float,
    val sunY: Float,
    /** Three hill colours, far to near. */
    val hills: List<Color>,
    val cloud: Color,
    /** Found words, selected letters and the swipe line; [accentEdge] is the tile's bottom edge. */
    val accent: Color,
    val accentEdge: Color,
    val night: Boolean = false,
)

val Meadow = Scene(
    name = "Meadow", letterCount = 3,
    skyTop = Color(0xFF1E88E5), skyBottom = Color(0xFFA8E0FF),
    sun = Color(0xFFFFE27A), sunX = 0.8f, sunY = 0.14f,
    hills = listOf(Color(0xFF8FD694), Color(0xFF55B96A), Color(0xFF2F9A57)),
    cloud = Color(0xFFFFFFFF),
    accent = Color(0xFFF4511E), accentEdge = Color(0xFFB93A12),
)
val Dusk = Scene(
    name = "Dusk", letterCount = 4,
    skyTop = Color(0xFF4A2A7A), skyBottom = Color(0xFFFF9E6B),
    sun = Color(0xFFFFD08A), sunX = 0.5f, sunY = 0.55f,
    hills = listOf(Color(0xFF8E4A8C), Color(0xFF63357A), Color(0xFF3F245E)),
    cloud = Color(0xFFFFB9A3),
    accent = Color(0xFFE91E63), accentEdge = Color(0xFFAD1457),
)
val Night = Scene(
    name = "Night", letterCount = 5,
    skyTop = Color(0xFF0B1638), skyBottom = Color(0xFF27407A),
    sun = Color(0xFFF4F1DE), sunX = 0.2f, sunY = 0.13f,
    hills = listOf(Color(0xFF21456B), Color(0xFF173557), Color(0xFF0F2742)),
    cloud = Color(0xFFFFFFFF),
    accent = Color(0xFF7C5CFF), accentEdge = Color(0xFF5A3FD1),
    night = true,
)

/** The chapter a level belongs to, from how many letters its wheel has. */
fun sceneFor(letterCount: Int): Scene = when {
    letterCount <= 3 -> Meadow
    letterCount == 4 -> Dusk
    else -> Night
}

/** The scene of the screen being drawn, so tiles and the wheel can read its accent colour. */
val LocalScene = staticCompositionLocalOf { Meadow }

/**
 * The scenery layer. It is placed once, behind the whole NavHost, so it stays still while the
 * screens change on top of it. [scene] is null until the first screen has loaded its data; until
 * then only the plain sky colour shows. Changing scene (level 3 to level 4) cross-fades.
 */
@Composable
fun SceneBackdrop(scene: Scene?, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .background(Meadow.skyTop)
            // Clouds and screen transitions move past the edges; keep them inside this box.
            .clipToBounds(),
    ) {
        Crossfade(scene, animationSpec = tween(500), label = "scene") { shown ->
            if (shown != null) Scenery(shown)
        }
        content()
    }
}

/**
 * What each screen wraps itself in. It reports which scenery belongs behind the screen through
 * [onScene], and makes that scene's accent colour available to the screen's tiles through
 * [LocalScene]. [scene] is null while the screen's data is still loading; nothing is reported then.
 */
@Composable
fun SceneScreen(
    scene: Scene?,
    onScene: (Scene) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Runs when the screen appears, and again whenever its scene changes.
    LaunchedEffect(scene) { if (scene != null) onScene(scene) }
    CompositionLocalProvider(LocalScene provides (scene ?: Meadow)) {
        Box(modifier, content = content)
    }
}

private class Cloud(val x: Float, val y: Float, val scale: Float, val laps: Int)
private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

private val Clouds = listOf(Cloud(0.1f, 0.10f, 1.0f, 1), Cloud(0.55f, 0.22f, 0.7f, 2), Cloud(0.8f, 0.34f, 0.85f, 1))

@Composable
private fun Scenery(scene: Scene) {
    // One value that runs 0..1 every 90 seconds drives the clouds and the stars' twinkle. It is
    // read only inside the Canvas draw lambda, so each frame is a redraw, never a recomposition.
    val time by rememberInfiniteTransition(label = "scenery").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing)),
        label = "time",
    )
    val stars = remember {
        val random = Random(7)
        List(40) { Star(random.nextFloat(), random.nextFloat() * 0.5f, 0.6f + random.nextFloat(), random.nextFloat() * 6.28f) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(scene.skyTop, scene.skyBottom), endY = h * 0.7f))

        // Sun or moon, with two faint halos.
        val sunCenter = Offset(w * scene.sunX, h * scene.sunY)
        val sunRadius = min(34.dp.toPx(), h * 0.1f)
        drawCircle(scene.sun.copy(alpha = 0.15f), sunRadius * 2.3f, sunCenter)
        drawCircle(scene.sun.copy(alpha = 0.25f), sunRadius * 1.6f, sunCenter)
        drawCircle(scene.sun, sunRadius, sunCenter)

        if (scene.night) {
            for (star in stars) {
                // 40 slow twinkles per loop, each star at its own phase.
                val twinkle = 0.55f + 0.45f * sin(star.phase + time * 2f * PI.toFloat() * 40f)
                drawCircle(Color.White.copy(alpha = twinkle), star.radius.dp.toPx(), Offset(w * star.x, h * star.y))
            }
        } else {
            for (cloud in Clouds) {
                val width = 76.dp.toPx() * cloud.scale
                // Whole laps per loop, so the cloud is back where it started when `time` restarts.
                val progress = (cloud.x + time * cloud.laps) % 1f
                drawCloud(scene.cloud, Offset(progress * (w + width) - width / 2f, h * cloud.y), cloud.scale)
            }
        }

        drawHill(scene.hills[0], top = 0.60f, rise = 0.07f, peakX = 0.28f)
        drawHill(scene.hills[1], top = 0.68f, rise = 0.06f, peakX = 0.72f)
        drawHill(scene.hills[2], top = 0.77f, rise = 0.05f, peakX = 0.35f)
    }
}

/** A hill row: a curve that peaks [rise] above [top] at [peakX], filled down to the bottom. */
private fun DrawScope.drawHill(color: Color, top: Float, rise: Float, peakX: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, h * top)
        quadraticTo(w * peakX, h * (top - rise * 2f), w * (peakX + 0.35f), h * top)
        quadraticTo(w * (peakX + 0.55f), h * (top + rise), w, h * (top - rise * 0.5f))
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(path, color)
}

/** A cartoon cloud: three circles on a rounded base. */
private fun DrawScope.drawCloud(color: Color, center: Offset, scale: Float) {
    val u = 1.dp.toPx() * scale
    drawCircle(color, 13 * u, center + Offset(-18 * u, 0f))
    drawCircle(color, 19 * u, center + Offset(0f, -8 * u))
    drawCircle(color, 14 * u, center + Offset(19 * u, 0f))
    drawRoundRect(color, center + Offset(-31 * u, -2 * u), Size(64 * u, 15 * u), CornerRadius(7.5f * u))
}
