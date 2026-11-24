package com.synapse.gaming

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// Root: splash on top of the starfield + app
@Composable
fun SynapseApp() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().background(SynBg)) {
        Starfield()
        SynapseMain()
        AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(600))) {
            SplashScreen { showSplash = false }
        }
    }
}

// Twinkling stars and soft nebula glows
@Composable
fun Starfield(modifier: Modifier = Modifier) {
    val stars = remember {
        val r = java.util.Random(7)
        List(90) { Triple(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 1.6f) }
    }
    val t = rememberInfiniteTransition(label = "stars")
    val twinkle by t.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle"
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x558B5CF6), Color.Transparent),
                center = Offset(w * 0.1f, h * 0.08f), radius = w * 0.8f
            ),
            radius = w * 0.8f, center = Offset(w * 0.1f, h * 0.08f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3322D3EE), Color.Transparent),
                center = Offset(w * 0.95f, h * 0.9f), radius = w * 0.8f
            ),
            radius = w * 0.8f, center = Offset(w * 0.95f, h * 0.9f)
        )
        stars.forEachIndexed { i, (x, y, r) ->
            val a = when (i % 3) {
                0 -> twinkle
                1 -> 1.3f - twinkle
                else -> 0.7f
            }
            drawCircle(
                color = Color.White.copy(alpha = a.coerceIn(0.1f, 1f)),
                radius = r * density, center = Offset(x * w, y * h)
            )
        }
    }
}

// Logo: glowing core, orbit ring, orbiting planet and a small constellation
@Composable
fun GalaxyLogo(logoSize: Dp, orbit: Float = 0f, modifier: Modifier = Modifier) {
    Canvas(modifier.size(logoSize)) {
        val c = center
        val rad = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x778B5CF6), Color.Transparent), center = c, radius = rad
            ),
            radius = rad, center = c
        )
        val nodeDist = rad * 0.8f
        val pts = listOf(-90f, -30f, 30f, 90f, 150f, 210f).map { a ->
            val r = Math.toRadians(a.toDouble())
            Offset(c.x + nodeDist * cos(r).toFloat(), c.y + nodeDist * sin(r).toFloat())
        }
        pts.forEach { drawLine(Color(0x66C4B5FD), c, it, strokeWidth = rad * 0.02f) }
        pts.forEach { drawCircle(Color(0xFF67E8F9), radius = rad * 0.045f, center = it) }
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(Color(0xFF8B5CF6), Color(0xFFF472B6), Color(0xFF22D3EE), Color(0xFF8B5CF6)),
                center = c
            ),
            radius = rad * 0.55f, center = c, style = Stroke(width = rad * 0.07f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFF472B6), Color(0xFF8B5CF6)),
                center = c, radius = rad * 0.35f
            ),
            radius = rad * 0.3f, center = c
        )
        val pa = Math.toRadians((orbit * 360f).toDouble())
        drawCircle(
            Color.White, radius = rad * 0.08f,
            center = Offset(c.x + rad * 0.55f * cos(pa).toFloat(), c.y + rad * 0.55f * sin(pa).toFloat())
        )
    }
}

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val appear = remember { Animatable(0f) }
    val t = rememberInfiniteTransition(label = "orbit")
    val orbit by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbitAngle"
    )
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(900))
        delay(1400)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(SynBg), contentAlignment = Alignment.Center) {
        Starfield()
        Column(
            Modifier.alpha(appear.value).scale(0.8f + 0.2f * appear.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GalaxyLogo(160.dp, orbit)
            Spacer(Modifier.height(20.dp))
            Text("SYNAPSE", color = SynText, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
            Text("GAMING", color = SynRed, fontSize = 18.sp, fontWeight = FontWeight.Light, letterSpacing = 12.sp)
            Spacer(Modifier.height(12.dp))
            Text("Every console. One galaxy.", color = SynMuted, fontSize = 13.sp)
        }
    }
}
