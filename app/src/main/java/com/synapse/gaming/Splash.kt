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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val Neon = Color(0xFF9B6BFF)
private val NeonPink = Color(0xFFC026D3)

// Root: splash on top of the starfield + app
@Composable
fun SynapseApp() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().background(SynBg)) {
        SynapseMain()
        AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(600))) {
            SplashScreen { showSplash = false }
        }
    }
}

// Logo: glowing core with an S, orbit ring, orbiting planet and a small constellation
@Composable
fun GalaxyLogo(logoSize: Dp, orbit: Float = 0f, modifier: Modifier = Modifier, showS: Boolean = true) {
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
                listOf(Color(0xFFC4B5FD), Color(0xFF8B5CF6), Color(0xFFC026D3), Color(0xFF8B5CF6), Color(0xFFC4B5FD)),
                center = c
            ),
            radius = rad * 0.55f, center = c, style = Stroke(width = rad * 0.07f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA78BFA), Color(0xFF6D28D9)),
                center = c, radius = rad * 0.32f
            ),
            radius = rad * 0.3f, center = c
        )
        if (showS) {
            val u = rad * 0.01875f
            val sPts = listOf(
                -8f to -12f, 10f to -12f, 10f to -7.5f, -3.5f to -7.5f, -3.5f to -2.5f, 10f to -2.5f,
                10f to 11.5f, -8f to 11.5f, -8f to 7f, 5.5f to 7f, 5.5f to 2f, -8f to 2f
            )
            val sPath = Path().apply {
                sPts.forEachIndexed { i, (x, y) ->
                    val px = c.x + (x - 1f) * u
                    val py = c.y + y * u
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(sPath, Color.White)
        }
        val pa = Math.toRadians((orbit * 360f).toDouble())
        drawCircle(
            Color.White, radius = rad * 0.08f,
            center = Offset(c.x + rad * 0.55f * cos(pa).toFloat(), c.y + rad * 0.55f * sin(pa).toFloat())
        )
    }
}

// Opera GX style splash: black and neon purple, grid floor, HUD corners, logo, loading bar
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val appear = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val inf = rememberInfiniteTransition(label = "splash")
    val orbit by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "orbitAngle"
    )
    val pulse by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    LaunchedEffect(Unit) {
        launch { progress.animateTo(1f, tween(2400, easing = LinearEasing)) }
        appear.animateTo(1f, tween(800))
        delay(1800)
        onDone()
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF040309), Color(0xFF0C0719), Color(0xFF1E0B45)))
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // neon perspective floor
            val horizon = h * 0.7f
            for (i in 1..8) {
                val f = i / 8f
                val y = horizon + (h - horizon) * f * f
                drawLine(Neon.copy(alpha = 0.10f + 0.25f * f), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
            }
            for (i in -8..8) {
                drawLine(
                    Neon.copy(alpha = 0.22f),
                    Offset(w / 2f + i * w / 60f, horizon), Offset(w / 2f + i * w / 8f, h),
                    strokeWidth = 1.5f
                )
            }
            drawLine(Neon.copy(alpha = 0.6f), Offset(0f, horizon), Offset(w, horizon), strokeWidth = 2f)
            // HUD corner brackets
            val m = 22.dp.toPx()
            val l = 38.dp.toPx()
            val sw = 3.dp.toPx()
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(Neon, Offset(x, y), Offset(x + dx * l, y), strokeWidth = sw)
                drawLine(Neon, Offset(x, y), Offset(x, y + dy * l), strokeWidth = sw)
            }
            corner(m, m, 1f, 1f)
            corner(w - m, m, -1f, 1f)
            corner(m, h - m, 1f, -1f)
            corner(w - m, h - m, -1f, -1f)
        }

        Column(
            Modifier.align(Alignment.Center)
                .alpha(appear.value)
                .scale((0.85f + 0.15f * appear.value) * (0.97f + 0.03f * pulse)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GalaxyLogo(180.dp, orbit)
            Spacer(Modifier.height(22.dp))
            Text(
                "SYNAPSE", color = SynText, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp,
                style = TextStyle(shadow = Shadow(color = Neon, blurRadius = 28f))
            )
            Text(
                "GAMING", color = Neon, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 14.sp,
                style = TextStyle(shadow = Shadow(color = NeonPink, blurRadius = 20f))
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LOADING", color = SynMuted, fontSize = 11.sp, letterSpacing = 5.sp)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(200.dp).height(3.dp).background(Color(0x33FFFFFF))) {
                Box(
                    Modifier.fillMaxWidth(progress.value).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Neon, NeonPink)))
                )
            }
        }
    }
}
