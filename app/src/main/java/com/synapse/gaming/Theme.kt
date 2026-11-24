package com.synapse.gaming

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cyberpunk: near-black, neon yellow + cyan + magenta, angular cuts, monospace text
val SynBg = Color(0xFF0A0A0F)
val SynCard = Color(0xFF11121A)
val SynRed = Color(0xFF00F0FF)       // main accent text = neon cyan (name kept for old code)
val SynYellow = Color(0xFFFCEE0A)
val SynPink = Color(0xFFFF2A6D)
val SynBorder = Color(0x9900F0FF)
val SynText = Color(0xFFF2F4F8)
val SynMuted = Color(0xFFAAB4C3)
val GxShape = CutCornerShape(topStart = 14.dp, topEnd = 0.dp, bottomEnd = 14.dp, bottomStart = 0.dp)

private fun TextStyle.mono() = copy(fontFamily = FontFamily.Monospace)
private val BaseType = Typography()
private val CyberType = Typography(
    headlineSmall = BaseType.headlineSmall.mono().copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    titleLarge = BaseType.titleLarge.mono(),
    titleMedium = BaseType.titleMedium.mono(),
    bodyLarge = BaseType.bodyLarge.mono(),
    bodyMedium = BaseType.bodyMedium.mono(),
    bodySmall = BaseType.bodySmall.mono(),
    labelLarge = BaseType.labelLarge.mono(),
    labelMedium = BaseType.labelMedium.mono(),
    labelSmall = BaseType.labelSmall.mono()
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
            outline = Color(0xFF3D4B5C), outlineVariant = Color(0xFF263040)
        ),
        typography = CyberType
    ) {
        // Always light text, even outside a Surface
        CompositionLocalProvider(LocalContentColor provides SynText, content = content)
    }
}

// Accent color per console, used on game cards
val Console.color: Color get() = Color(tint)
