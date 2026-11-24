package com.synapse.gaming

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val YellowGlass = Brush.verticalGradient(listOf(Color(0xFFFFF66B), Color(0xFFFCEE0A), Color(0xFFD8CB00)))

// Squash-and-stretch while pressed: wider and shorter, then springs back with a wobble
@Composable
fun stretchScale(interaction: MutableInteractionSource, amount: Float = 0.08f): Pair<Float, Float> {
    val pressed by interaction.collectIsPressedAsState()
    val spec = spring<Float>(dampingRatio = 0.38f, stiffness = 380f)
    val sx by animateFloatAsState(if (pressed) 1f + amount else 1f, spec, label = "sx")
    val sy by animateFloatAsState(if (pressed) 1f - amount * 1.4f else 1f, spec, label = "sy")
    return sx to sy
}

@Composable
private fun GlassSurface(
    onClick: () -> Unit, modifier: Modifier, enabled: Boolean, accent: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val (sx, sy) = stretchScale(interaction, 0.10f)
    val shape = RoundedCornerShape(50)
    val fill = if (accent) YellowGlass
    else Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF)))
    Row(
        modifier
            .graphicsLayer { scaleX = sx; scaleY = sy; alpha = if (enabled) 1f else 0.45f }
            .clip(shape)
            .background(fill)
            .drawBehind {
                // specular highlight on the top half
                drawRect(
                    Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color.Transparent), endY = size.height * 0.55f),
                    size = Size(size.width, size.height * 0.55f)
                )
            }
            .border(1.dp, SynGlassRim, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides (if (accent) Color.Black else SynText)) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
        }
    }
}

// These shadow the standard Material buttons everywhere in the app
@Composable
fun Button(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50), content: @Composable RowScope.() -> Unit
) = GlassSurface(onClick, modifier, enabled, true, content)

@Composable
fun OutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50), content: @Composable RowScope.() -> Unit
) = GlassSurface(onClick, modifier, enabled, false, content)

// Floating glass pill navigation: the selected tab stretches open and lights up
@Composable
fun FloatingNav(current: Tab, onTab: (Tab) -> Unit) {
    val barShape = RoundedCornerShape(32.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clip(barShape)
                .background(Color(0xCC12131C))
                .border(1.dp, SynGlassRim, barShape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { t ->
                val sel = t == current
                val interaction = remember { MutableInteractionSource() }
                val (sx, sy) = stretchScale(interaction, 0.08f)
                val w by animateFloatAsState(
                    if (sel) 1.9f else 1f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "w"
                )
                Box(
                    Modifier.weight(w).height(48.dp)
                        .graphicsLayer { scaleX = sx; scaleY = sy }
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (sel) YellowGlass else SolidColor(Color.Transparent))
                        .then(
                            if (sel) Modifier.drawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        listOf(Color(0x66FFFFFF), Color.Transparent), endY = size.height * 0.55f
                                    ),
                                    size = Size(size.width, size.height * 0.55f)
                                )
                            } else Modifier
                        )
                        .clickable(interactionSource = interaction, indication = null) { onTab(t) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(t.icon, t.label, tint = if (sel) Color.Black else SynMuted, modifier = Modifier.size(22.dp))
                        if (sel) {
                            Spacer(Modifier.width(6.dp))
                            Text(t.label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
