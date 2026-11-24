package com.synapse.gaming

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SynBg = Color(0xFF0F1923)
val SynCard = Color(0xFF1B2733)
val SynRed = Color(0xFFFF4655)
val SynText = Color(0xFFECE8E1)
val SynMuted = Color(0xFF8B97A3)

@Composable
fun SynapseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SynRed, onPrimary = Color.White,
            background = SynBg, onBackground = SynText,
            surface = SynBg, onSurface = SynText,
            surfaceVariant = SynCard, onSurfaceVariant = SynMuted,
            secondaryContainer = Color(0xFF4A2229), onSecondaryContainer = SynRed,
            outline = Color(0xFF2E3B48)
        ),
        content = content
    )
}

// Accent color per console, used on game cards
val Console.color: Color get() = when (this) {
    Console.XBOX360 -> Color(0xFF52B043)
    Console.PS2, Console.PS1 -> Color(0xFF3B6FD4)
    Console.GAMECUBE -> Color(0xFF7B5CD6)
    Console.WII -> Color(0xFF2EB5D6)
    Console.PSP -> Color(0xFF5A6572)
    Console.N3DS -> Color(0xFFE0A030)
    Console.N64 -> Color(0xFFD64545)
}
