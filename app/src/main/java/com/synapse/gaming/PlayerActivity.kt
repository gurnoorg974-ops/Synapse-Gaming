package com.synapse.gaming

import android.net.Uri
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream

class PlayerActivity : ComponentActivity() {
    var retro: GLRetroView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val uri = Uri.parse(intent.getStringExtra("uri"))
        val core = intent.getStringExtra("core")
        if (core == null) { finish(); return }
        val ext = intent.getStringExtra("ext") ?: ""
        setContent { SynapseTheme { PlayerScreen(uri, core, ext) } }
    }

    // Physical controllers go straight to the emulator
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val v = retro
        val fromPad = (event.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK)) != 0
        if (v != null && fromPad && event.keyCode != KeyEvent.KEYCODE_BACK) {
            v.sendKeyEvent(event.action, event.keyCode)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

private fun downloadCore(core: String, out: File) {
    out.parentFile?.mkdirs()
    val tmp = File(out.path + ".tmp")
    val url = URL("https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/${core}_libretro_android.so.zip")
    var found = false
    url.openStream().use { raw ->
        ZipInputStream(raw).use { zip ->
            var e = zip.nextEntry
            while (e != null && !found) {
                if (e.name.endsWith(".so")) {
                    tmp.outputStream().use { zip.copyTo(it) }
                    found = true
                } else e = zip.nextEntry
            }
        }
    }
    if (!found) { tmp.delete(); throw IllegalStateException("Core not found in download") }
    tmp.renameTo(out)
    out.setReadOnly() // Android 14 wants loaded code to be read-only
}

@Composable
fun PlayerScreen(uri: Uri, core: String, ext: String) {
    val act = LocalContext.current as PlayerActivity
    var status by remember { mutableStateOf("Starting...") }
    var error by remember { mutableStateOf<String?>(null) }
    var paths by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        try {
            paths = withContext(Dispatchers.IO) {
                val coreFile = File(act.filesDir, "cores/${core}_libretro_android.so")
                if (!coreFile.exists()) {
                    status = "Downloading emulator (first time only)..."
                    downloadCore(core, coreFile)
                }
                status = "Preparing game..."
                val dir = File(act.cacheDir, "games").apply { deleteRecursively(); mkdirs() }
                val game = File(dir, "game" + if (ext.isNotEmpty()) ".$ext" else "")
                act.contentResolver.openInputStream(uri)!!.use { i -> game.outputStream().use { i.copyTo(it) } }
                coreFile.path to game.path
            }
        } catch (e: Exception) {
            error = "Could not start: ${e.message}"
        }
    }

    val send = { action: Int, key: Int -> act.retro?.sendKeyEvent(action, key) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val p = paths
        if (p != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    val data = GLRetroViewData(c).apply {
                        coreFilePath = p.first
                        gameFilePath = p.second
                        systemDirectory = File(c.filesDir, "system").apply { mkdirs() }.path
                        savesDirectory = File(c.filesDir, "saves").apply { mkdirs() }.path
                        shader = ShaderConfig.Default
                    }
                    GLRetroView(c, data).also {
                        act.retro = it
                        act.lifecycle.addObserver(it)
                    }
                }
            )
            Controls(send)
        } else {
            Column(
                Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error ?: status, color = Color.White)
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { act.finish() }) { Text("Back") }
                }
            }
        }
    }
}

@Composable
private fun PadButton(label: String, key: Int, send: (Int, Int) -> Unit, size: Int = 52) {
    Box(
        Modifier.size(size.dp)
            .background(Color(0x55FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    send(KeyEvent.ACTION_DOWN, key)
                    try { awaitRelease() } finally { send(KeyEvent.ACTION_UP, key) }
                })
            },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White) }
}

@Composable
private fun BoxScope.Controls(send: (Int, Int) -> Unit) {
    // D-pad
    Column(Modifier.align(Alignment.BottomStart).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton("^", KeyEvent.KEYCODE_DPAD_UP, send)
        Row {
            PadButton("<", KeyEvent.KEYCODE_DPAD_LEFT, send)
            Spacer(Modifier.size(52.dp))
            PadButton(">", KeyEvent.KEYCODE_DPAD_RIGHT, send)
        }
        PadButton("v", KeyEvent.KEYCODE_DPAD_DOWN, send)
    }
    // Face buttons
    Column(Modifier.align(Alignment.BottomEnd).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton("X", KeyEvent.KEYCODE_BUTTON_X, send)
        Row {
            PadButton("Y", KeyEvent.KEYCODE_BUTTON_Y, send)
            Spacer(Modifier.size(52.dp))
            PadButton("A", KeyEvent.KEYCODE_BUTTON_A, send)
        }
        PadButton("B", KeyEvent.KEYCODE_BUTTON_B, send)
    }
    // Shoulders
    Row(Modifier.align(Alignment.TopStart).padding(16.dp)) { PadButton("L", KeyEvent.KEYCODE_BUTTON_L1, send, 48) }
    Row(Modifier.align(Alignment.TopEnd).padding(16.dp)) { PadButton("R", KeyEvent.KEYCODE_BUTTON_R1, send, 48) }
    // Start / Select
    Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PadButton("Sel", KeyEvent.KEYCODE_BUTTON_SELECT, send, 44)
        PadButton("Go", KeyEvent.KEYCODE_BUTTON_START, send, 44)
    }
}
