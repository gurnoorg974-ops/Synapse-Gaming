package com.synapse.gaming

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Opera GX style: near-black, neon purple accent, cut corners, high-contrast text
val SynBg = Color(0xFF07050F)
val SynCard = Color(0xFF15131F)
val SynRed = Color(0xFFB57CFF)      // main neon accent (name kept for old code)
val SynBorder = Color(0x669B6BFF)
val SynText = Color(0xFFF5F3FF)
val SynMuted = Color(0xFFB8B4D6)
val GxShape = CutCornerShape(topStart = 0.dp, topEnd = 14.dp, bottomEnd = 0.dp, bottomStart = 14.dp)

@Composable
fun SynapseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SynRed, onPrimary = Color(0xFF0B0518),
            background = SynBg, onBackground = SynText,
            surface = SynBg, onSurface = SynText,
            surfaceVariant = SynCard, onSurfaceVariant = SynMuted,
            secondaryContainer = Color(0xFF2E1F66), onSecondaryContainer = Color(0xFFE9DDFF),
            outline = Color(0xFF5A5390), outlineVariant = Color(0xFF34305E)
        )
    ) {
        // Make sure text is always light, even outside a Surface
        CompositionLocalProvider(LocalContentColor provides SynText, content = content)
    }
}

// Accent color per console, used on game cards
val Console.color: Color get() = Color(tint)
