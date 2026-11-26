package com.synapse.gaming

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

// Simple original line icons for each console (generic symbols, not official logos)
@Composable
fun ConsoleIcon(console: Console, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.075f
        val st = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun box(x: Float, y: Float, wd: Float, ht: Float, r: Float) =
            drawRoundRect(tint, Offset(x * s, y * s), Size(wd * s, ht * s), CornerRadius(r * s, r * s), style = st)
        fun ring(cx: Float, cy: Float, r: Float) = drawCircle(tint, r * s, Offset(cx * s, cy * s), style = st)
        fun dot(cx: Float, cy: Float, r: Float) = drawCircle(tint, r * s, Offset(cx * s, cy * s))
        fun ln(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), strokeWidth = w, cap = StrokeCap.Round)
        fun pad(cx: Float, cy: Float, r: Float) {
            ln(cx - r, cy, cx + r, cy)
            ln(cx, cy - r, cx, cy + r)
        }
        fun tri(cx: Float, cy: Float, r: Float) {
            val p = Path().apply {
                moveTo(cx * s, (cy - r) * s)
                lineTo((cx + r) * s, (cy + r * 0.8f) * s)
                lineTo((cx - r) * s, (cy + r * 0.8f) * s)
                close()
            }
            drawPath(p, tint, style = st)
        }
        when (console) {
            // X in a circle
            Console.XBOX360 -> {
                ring(0.5f, 0.5f, 0.42f)
                ln(0.34f, 0.34f, 0.66f, 0.66f)
                ln(0.66f, 0.34f, 0.34f, 0.66f)
            }
            // triangle, circle, cross, square
            Console.PS1, Console.PS2 -> {
                tri(0.5f, 0.2f, 0.12f)
                ring(0.8f, 0.5f, 0.1f)
                ln(0.41f, 0.71f, 0.59f, 0.89f)
                ln(0.59f, 0.71f, 0.41f, 0.89f)
                box(0.1f, 0.4f, 0.2f, 0.2f, 0.02f)
            }
            Console.PSP -> {
                box(0.04f, 0.28f, 0.92f, 0.44f, 0.12f)
                box(0.3f, 0.36f, 0.4f, 0.28f, 0.03f)
                pad(0.17f, 0.5f, 0.06f)
                dot(0.83f, 0.5f, 0.05f)
            }
            Console.GAMECUBE -> {
                box(0.14f, 0.2f, 0.72f, 0.66f, 0.12f)
                ring(0.5f, 0.53f, 0.15f)
                box(0.38f, 0.08f, 0.24f, 0.12f, 0.03f)
            }
            Console.WII -> {
                box(0.32f, 0.04f, 0.36f, 0.92f, 0.12f)
                ring(0.5f, 0.26f, 0.08f)
                dot(0.5f, 0.52f, 0.05f)
            }
            Console.N3DS -> {
                box(0.16f, 0.05f, 0.68f, 0.38f, 0.06f)
                box(0.1f, 0.52f, 0.8f, 0.4f, 0.07f)
                ring(0.25f, 0.72f, 0.07f)
                dot(0.76f, 0.72f, 0.05f)
            }
            Console.NDS -> {
                box(0.2f, 0.06f, 0.6f, 0.34f, 0.05f)
                box(0.1f, 0.5f, 0.8f, 0.42f, 0.07f)
                pad(0.26f, 0.71f, 0.07f)
                dot(0.74f, 0.71f, 0.05f)
            }
            Console.GB -> {
                box(0.22f, 0.04f, 0.56f, 0.92f, 0.08f)
                box(0.3f, 0.12f, 0.4f, 0.3f, 0.03f)
                pad(0.38f, 0.68f, 0.08f)
                dot(0.66f, 0.74f, 0.05f)
                dot(0.74f, 0.64f, 0.05f)
            }
            Console.GBA -> {
                box(0.04f, 0.26f, 0.92f, 0.48f, 0.12f)
                box(0.3f, 0.34f, 0.4f, 0.3f, 0.02f)
                pad(0.16f, 0.5f, 0.06f)
                dot(0.84f, 0.46f, 0.045f)
                dot(0.76f, 0.56f, 0.045f)
            }
            Console.N64 -> {
                ln(0.2f, 0.3f, 0.8f, 0.3f)
                ln(0.2f, 0.3f, 0.2f, 0.9f)
                ln(0.8f, 0.3f, 0.8f, 0.9f)
                ln(0.5f, 0.5f, 0.5f, 0.92f)
                ring(0.5f, 0.42f, 0.08f)
            }
            Console.NES -> {
                box(0.04f, 0.3f, 0.92f, 0.4f, 0.05f)
                pad(0.22f, 0.5f, 0.09f)
                dot(0.7f, 0.5f, 0.06f)
                dot(0.84f, 0.5f, 0.06f)
            }
            Console.SNES -> {
                box(0.05f, 0.3f, 0.9f, 0.4f, 0.18f)
                pad(0.22f, 0.5f, 0.08f)
                dot(0.78f, 0.42f, 0.04f)
                dot(0.78f, 0.58f, 0.04f)
                dot(0.7f, 0.5f, 0.04f)
                dot(0.86f, 0.5f, 0.04f)
            }
            Console.GENESIS -> {
                box(0.05f, 0.32f, 0.9f, 0.38f, 0.16f)
                pad(0.22f, 0.51f, 0.08f)
                dot(0.6f, 0.54f, 0.045f)
                dot(0.72f, 0.54f, 0.045f)
                dot(0.84f, 0.54f, 0.045f)
            }
        }
    }
}
