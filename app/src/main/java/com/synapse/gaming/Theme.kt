package com.synapse.gaming

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SynBg = Color(0xFF07050F)
val SynCard = Color(0xE6151530)
val SynRed = Color(0xFF9B6BFF) // main accent (galaxy violet)
val SynText = Color(0xFFEDE9FE)
val SynMuted = Color(0xFF9A98BD)

@Composable
fun SynapseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SynRed, onPrimary = Color.White,
            background = SynBg, onBackground = SynText,
            surface = SynBg, onSurface = SynText,
            surfaceVariant = SynCard, onSurfaceVariant = SynMuted,
            secondaryContainer = Color(0xFF2A1F5C), onSecondaryContainer = SynRed,
            outline = Color(0xFF34305E)
        ),
        content = content
    )
}

// Accent color per console, used on game cards
val Console.color: Color get() = Color(tint)
