package com.synapse.gaming

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val Neon = Color(0xFF00F0FF)
private val NeonYellow = Color(0xFFFCEE0A)
private val NeonPink = Color(0xFFFF2A6D)

// Root: app with a soft background glow, splash on top
@Composable
fun SynapseApp() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().background(SynBg)) {
        AuroraBackground()
        SynapseMain()
        AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(600))) {
            SplashScreen { showSplash = false }
        }
    }
}

// Two faint color glows in the corners
@Composable
fun AuroraBackground() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val a = Offset(w * 0.95f, h * 0.05f)
        val b = Offset(w * 0.05f, h * 0.95f)
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(Color(0x2900F0FF), Color.Transparent), center = a, radius = w * 0.9f),
            radius = w * 0.9f, center = a
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(Color(0x26FF2A6D), Color.Transparent), center = b, radius = w * 0.9f),
            radius = w * 0.9f, center = b
        )
    }
}

// The S mark (same shape as the app icon) with a chromatic glitch
private val SPoints = listOf(
    33f to 29f, 68f to 29f, 75f to 36f, 75f to 39f, 43f to 39f, 43f to 49f, 75f to 49f,
    75f to 79f, 40f to 79f, 33f to 72f, 33f to 69f, 65f to 69f, 65f to 59f, 33f to 59f
)

@Composable
fun GalaxyLogo(logoSize: Dp, glitch: Float = 0f, modifier: Modifier = Modifier) {
    Canvas(modifier.size(logoSize)) {
        val s = size.minDimension
        val u = s / 64f
        val c = center
        val path = Path().apply {
            SPoints.forEachIndexed { i, (x, y) ->
                val px = c.x + (x - 54f) * u
                val py = c.y + (y - 54f) * u
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(Color(0x3300F0FF), Color.Transparent), center = c, radius = s * 0.6f),
            radius = s * 0.6f, center = c
        )
        val d = s * (0.03f + 0.10f * glitch)
        translate(d, -d * 0.4f) { drawPath(path, NeonPink.copy(alpha = 0.95f)) }
        translate(-d, d * 0.4f) { drawPath(path, Neon.copy(alpha = 0.95f)) }
        drawPath(path, Color.White)
        if (glitch > 0.05f) {
            val top1 = s * 0.30f
            clipRect(0f, top1, size.width, top1 + s * 0.12f) {
                translate(glitch * s * 0.25f, 0f) { drawPath(path, NeonYellow) }
            }
            val top2 = s * 0.62f
            clipRect(0f, top2, size.width, top2 + s * 0.08f) {
                translate(-glitch * s * 0.2f, 0f) { drawPath(path, Neon) }
            }
        }
    }
}

@Composable
private fun WordText(t: String, c: Color, m: Modifier) {
    Text(t, color = c, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp, modifier = m)
}

@Composable
private fun Wordmark(g: Float) {
    val dx = (1.5f + g * 18f).dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            WordText("SYNAPSE", NeonPink, Modifier.offset(x = -dx))
            WordText("SYNAPSE", Neon, Modifier.offset(x = dx))
            WordText("SYNAPSE", Color.White, Modifier)
        }
        Spacer(Modifier.height(6.dp))
        Text("GAMING", color = NeonYellow, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 16.sp)
    }
}

private class Particle(val angle: Float, val dist: Float, val len: Float, val width: Float, val color: Color)

// Splash: logo swipes in from the left and title from the right with a glitch,
// holds, then bursts into light streaks and a soft shockwave.
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val swipe = remember { Animatable(0f) }
    val blast = remember { Animatable(0f) }
    val inf = rememberInfiniteTransition(label = "splash")
    val tick by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "tick"
    )
    val screenW = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val particles = remember {
        val palette = listOf(Neon, NeonPink, NeonYellow, Color.White)
        val r = java.util.Random(11)
        List(46) {
            Particle(
                angle = r.nextFloat() * 6.2831855f, dist = 140f + r.nextFloat() * 300f,
                len = 14f + r.nextFloat() * 36f, width = 1.5f + r.nextFloat() * 2f,
                color = palette[r.nextInt(palette.size)]
            )
        }
    }

    LaunchedEffect(Unit) {
        swipe.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        delay(950)
        launch { blast.animateTo(1f, tween(950, easing = LinearEasing)) }
        delay(650)
        onDone()
    }

    val spike = if (tick > 0.78f && tick < 0.86f) 0.45f else 0f
    val g = ((1f - swipe.value) + spike).coerceIn(0f, 1f)
    val b = blast.value
    val fade = (1f - b * 1.5f).coerceIn(0f, 1f)
    val burst = 1f + 0.25f * b

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF05050A), Color(0xFF0B0B14)))
        )
    ) {
        AuroraBackground()
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            GalaxyLogo(
                170.dp, g,
                Modifier.graphicsLayer {
                    translationX = -(1f - swipe.value) * screenW
                    alpha = min(swipe.value * 3f, 1f) * fade
                    scaleX = burst
                    scaleY = burst
                }
            )
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.graphicsLayer {
                    translationX = (1f - swipe.value) * screenW
                    alpha = min(swipe.value * 3f, 1f) * fade
                    scaleX = burst
                    scaleY = burst
                }
            ) { Wordmark(g) }
        }
        Canvas(Modifier.fillMaxSize()) {
            if (b > 0f) {
                val e = 1f - (1f - b) * (1f - b) * (1f - b)
                val c = Offset(size.width / 2f, size.height / 2f - 45.dp.toPx())
                val flash = 0.20f * (1f - b) * (1f - b)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = flash), Color.Transparent),
                        center = c, radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f, center = c
                )
                drawCircle(
                    Neon.copy(alpha = 0.55f * (1f - b)), radius = e * size.width * 0.75f, center = c,
                    style = Stroke(width = 3.dp.toPx() * (1f - b) + 0.5f)
                )
                drawCircle(
                    NeonPink.copy(alpha = 0.40f * (1f - b)), radius = e * size.width * 0.55f, center = c,
                    style = Stroke(width = 2.dp.toPx() * (1f - b) + 0.5f)
                )
                particles.forEach { p ->
                    val d = p.dist * density * e
                    val dirX = cos(p.angle)
                    val dirY = sin(p.angle)
                    val head = Offset(c.x + dirX * d, c.y + dirY * d)
                    val tailLen = p.len * density * (1f - b * 0.5f)
                    val tail = Offset(head.x - dirX * tailLen, head.y - dirY * tailLen)
                    drawLine(
                        p.color.copy(alpha = (1f - b).coerceIn(0f, 1f) * 0.9f),
                        tail, head, strokeWidth = p.width * density
                    )
                }
            }
        }
    }
}
