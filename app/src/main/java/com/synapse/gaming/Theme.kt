package com.synapse.gaming

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Modern cyberpunk: deep black, soft glass cards, neon yellow / cyan / magenta accents
val SynBg = Color(0xFF0A0A0F)
val SynCard = Color(0x1AFFFFFF)       // frosted glass fill
val SynRed = Color(0xFF00F0FF)       // accent text = neon cyan (name kept for old code)
val SynYellow = Color(0xFFFCEE0A)
val SynPink = Color(0xFFFF2A6D)
val SynBorder = Color(0x1FFFFFFF)    // hairline border
val SynText = Color(0xFFF4F6FA)
val SynMuted = Color(0xFF9AA3B5)
val GxShape = RoundedCornerShape(22.dp)
// Glass rim: bright at the top edge, fading down
val SynGlassRim = Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x14FFFFFF)))

private val BaseType = Typography()
private val ModernType = Typography(
    headlineSmall = BaseType.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = BaseType.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = BaseType.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseType.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = BaseType.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
)

@Composable
fun SynapseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SynYellow, onPrimary = Color(0xFF0A0A0F),
            secondary = SynRed, tertiary = SynPink,
            background = SynBg, onBackground = SynText,
            surface = SynBg, onSurface = SynText,
            surfaceVariant = SynCard, onSurfaceVariant = SynMuted,
            secondaryContainer = Color(0xFF2B2A05), onSecondaryContainer = SynYellow,
            outline = Color(0xFF2A2F3D), outlineVariant = Color(0xFF1E2230)
        ),
        typography = ModernType
    ) {
        // Always light text, even outside a Surface
        CompositionLocalProvider(LocalContentColor provides SynText, content = content)
    }
}

// Accent color per console, used on game cards
val Console.color: Color get() = Color(tint)
