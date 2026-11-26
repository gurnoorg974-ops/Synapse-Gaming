package com.synapse.gaming

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Downloads box art from the public libretro thumbnail server and caches it on the phone.
object CoverLoader {
    private fun systems(c: Console): List<String> = when (c) {
        Console.XBOX360 -> listOf("Microsoft - Xbox 360")
        Console.PS2 -> listOf("Sony - PlayStation 2")
        Console.GAMECUBE -> listOf("Nintendo - GameCube")
        Console.WII -> listOf("Nintendo - Wii")
        Console.PSP -> listOf("Sony - PlayStation Portable")
        Console.N3DS -> listOf("Nintendo - Nintendo 3DS")
        Console.PS1 -> listOf("Sony - PlayStation")
        Console.N64 -> listOf("Nintendo - Nintendo 64")
        Console.NES -> listOf("Nintendo - Nintendo Entertainment System")
        Console.SNES -> listOf("Nintendo - Super Nintendo Entertainment System")
        Console.GB -> listOf("Nintendo - Game Boy", "Nintendo - Game Boy Color")
        Console.GBA -> listOf("Nintendo - Game Boy Advance")
        Console.GENESIS -> listOf("Sega - Mega Drive - Genesis")
        Console.NDS -> listOf("Nintendo - Nintendo DS")
    }

    private fun clean(s: String) = s.map { if (it in "&*/:<>?\\|\"`") '_' else it }.joinToString("")

    suspend fun load(ctx: Context, game: Game): ImageBitmap? = withContext(Dispatchers.IO) {
        val dir = File(ctx.filesDir, "covers").apply { mkdirs() }
        val key = (game.console.name + "|" + game.title).hashCode().toString(16)
        val hit = File(dir, "$key.png")
        val miss = File(dir, "$key.none")
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        if (hit.exists()) return@withContext BitmapFactory.decodeFile(hit.path, opts)?.asImageBitmap()
        if (miss.exists()) return@withContext null

        val title = clean(game.title.replace('_', ' ').trim())
        val names = listOf(title) + if ('(' in title) emptyList() else
            listOf("$title (USA)", "$title (Europe)", "$title (USA, Europe)", "$title (World)", "$title (Japan)")
        var networkError = false
        for (sys in systems(game.console)) {
            for (name in names) {
                try {
                    val url = URL("https://thumbnails.libretro.com/${Uri.encode(sys)}/Named_Boxarts/${Uri.encode(name)}.png")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 8000
                    }
                    if (conn.responseCode == 200) {
                        conn.inputStream.use { input -> hit.outputStream().use { input.copyTo(it) } }
                        val bmp = BitmapFactory.decodeFile(hit.path, opts)
                        if (bmp != null) return@withContext bmp.asImageBitmap()
                        hit.delete()
                    } else conn.disconnect()
                } catch (e: Exception) {
                    networkError = true
                }
            }
        }
        if (!networkError) miss.createNewFile()
        null
    }
}

private fun initials(title: String): String {
    val parts = title.split(' ', '-', '_', ':').filter { it.isNotBlank() }
    val s = parts.take(2).joinToString("") { it.first().uppercase() }
    return s.ifEmpty { "?" }
}

// Box art if we can find it, otherwise generated art in the console's color
@Composable
fun CoverArt(game: Game, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, game.uri) { value = CoverLoader.load(ctx, game) }
    val tint = game.console.color
    Box(modifier.background(Brush.linearGradient(listOf(tint.copy(alpha = 0.85f), SynBg)))) {
        val b = bmp
        if (b != null) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        } else {
            ConsoleIcon(game.console, Modifier.align(Alignment.Center).size(64.dp), Color.White.copy(alpha = 0.45f))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
            )
        )
    }
}
