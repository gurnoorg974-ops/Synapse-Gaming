package com.synapse.gaming

import android.net.Uri
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

class PlayerActivity : ComponentActivity() {
    var retro: GLRetroView? = null
    var canSave = false
    private var saveKey = "game"

    private fun autosaveOn() = getSharedPreferences("synapse", MODE_PRIVATE).getBoolean("autosave", true)
    private fun stateFile() = File(File(filesDir, "autosave").apply { mkdirs() }, "$saveKey.state")

    // Saves the exact moment you are at, so the next launch resumes right there
    fun saveState() {
        val v = retro ?: return
        if (!canSave || !autosaveOn()) return
        try {
            val bytes = v.serializeState()
            if (bytes.isNotEmpty()) {
                val tmp = File(stateFile().path + ".tmp")
                tmp.writeBytes(bytes)
                tmp.renameTo(stateFile())
            }
        } catch (e: Exception) {
        }
    }

    fun restoreState() {
        val v = retro ?: return
        if (!autosaveOn()) return
        val f = stateFile()
        if (!f.exists()) return
        try {
            v.unserializeState(f.readBytes())
            Toast.makeText(this, "Resumed from your autosave", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
        }
    }

    override fun onPause() {
        saveState()
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val uri = Uri.parse(intent.getStringExtra("uri"))
        saveKey = (intent.getStringExtra("uri") ?: "").hashCode().toString(16)
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

    // Resume from the autosave a moment after the game starts, then keep saving in the background
    LaunchedEffect(paths) {
        if (paths == null) return@LaunchedEffect
        delay(2200)
        act.restoreState()
        act.canSave = true
        if (core != "ppsspp") {
            while (true) {
                delay(120_000)
                act.saveState()
            }
        }
    }

    val send: (Int, Int) -> Unit = { action, key -> act.retro?.sendKeyEvent(action, key) }
    val sendAxis: (Float, Float) -> Unit = { x, y ->
        act.retro?.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, x, y)
    }
    val psStyle = core == "ppsspp" || core == "pcsx_rearmed"
    val showStick = psStyle || core == "mupen64plus_next_gles3"

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
            Controls(send, sendAxis, psStyle, showStick)
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

private val PadBg = Color(0x80303A46)

private fun Modifier.padPress(key: Int, send: (Int, Int) -> Unit): Modifier =
    pointerInput(key) {
        detectTapGestures(onPress = {
            send(KeyEvent.ACTION_DOWN, key)
            try { awaitRelease() } finally { send(KeyEvent.ACTION_UP, key) }
        })
    }

@Composable
private fun PadButton(
    label: String, key: Int, send: (Int, Int) -> Unit, size: Int = 52,
    textColor: Color = Color.White, textSize: Int = 16
) {
    Box(
        Modifier.size(size.dp).background(PadBg, CircleShape).padPress(key, send),
        contentAlignment = Alignment.Center
    ) { Text(label, color = textColor, fontSize = textSize.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PillButton(label: String, key: Int, send: (Int, Int) -> Unit) {
    Box(
        Modifier.width(76.dp).height(32.dp).background(PadBg, RoundedCornerShape(16.dp)).padPress(key, send),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

// On-screen analog stick: drag the knob, values go to the emulator as -1..1
@Composable
private fun Stick(sendAxis: (Float, Float) -> Unit) {
    var thumb by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.size(112.dp).background(Color(0x40303A46), CircleShape)
            .pointerInput(Unit) {
                fun update(p: Offset) {
                    val r = size.width / 2f
                    val c = Offset(r, size.height / 2f)
                    var d = p - c
                    val dist = d.getDistance()
                    if (dist > r) d = d * (r / dist)
                    thumb = d
                    sendAxis(d.x / r, d.y / r)
                }
                detectDragGestures(
                    onDragStart = { update(it) },
                    onDrag = { change, _ -> update(change.position) },
                    onDragEnd = { thumb = Offset.Zero; sendAxis(0f, 0f) },
                    onDragCancel = { thumb = Offset.Zero; sendAxis(0f, 0f) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.offset { IntOffset(thumb.x.roundToInt(), thumb.y.roundToInt()) }
                .size(52.dp).background(Color(0xB0ECE8E1), CircleShape)
        )
    }
}

@Composable
private fun BoxScope.Controls(
    send: (Int, Int) -> Unit, sendAxis: (Float, Float) -> Unit, psStyle: Boolean, showStick: Boolean
) {
    // Left: D-pad (+ analog stick)
    Row(
        Modifier.align(Alignment.BottomStart).padding(20.dp),
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PadButton("\u25B2", KeyEvent.KEYCODE_DPAD_UP, send)
            Row {
                PadButton("\u25C4", KeyEvent.KEYCODE_DPAD_LEFT, send)
                Spacer(Modifier.size(52.dp))
                PadButton("\u25BA", KeyEvent.KEYCODE_DPAD_RIGHT, send)
            }
            PadButton("\u25BC", KeyEvent.KEYCODE_DPAD_DOWN, send)
        }
        if (showStick) Stick(sendAxis)
    }

    // Right: face buttons
    Column(Modifier.align(Alignment.BottomEnd).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (psStyle) {
            // Triangle, Square, Circle, Cross (PlayStation layout)
            PadButton("\u25B3", KeyEvent.KEYCODE_BUTTON_X, send, textColor = Color(0xFF4CD08B), textSize = 24)
            Row {
                PadButton("\u25A1", KeyEvent.KEYCODE_BUTTON_Y, send, textColor = Color(0xFFE87BB0), textSize = 24)
                Spacer(Modifier.size(52.dp))
                PadButton("\u25CB", KeyEvent.KEYCODE_BUTTON_A, send, textColor = Color(0xFFFF5C5C), textSize = 24)
            }
            PadButton("\u2715", KeyEvent.KEYCODE_BUTTON_B, send, textColor = Color(0xFF6FA8FF), textSize = 24)
        } else {
            PadButton("X", KeyEvent.KEYCODE_BUTTON_X, send)
            Row {
                PadButton("Y", KeyEvent.KEYCODE_BUTTON_Y, send)
                Spacer(Modifier.size(52.dp))
                PadButton("A", KeyEvent.KEYCODE_BUTTON_A, send)
            }
            PadButton("B", KeyEvent.KEYCODE_BUTTON_B, send)
        }
    }

    // Shoulders
    Row(Modifier.align(Alignment.TopStart).padding(16.dp)) { PadButton("L", KeyEvent.KEYCODE_BUTTON_L1, send, 48) }
    Row(Modifier.align(Alignment.TopEnd).padding(16.dp)) { PadButton("R", KeyEvent.KEYCODE_BUTTON_R1, send, 48) }

    // Select / Start
    Row(Modifier.align(Alignment.TopCenter).padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillButton("SELECT", KeyEvent.KEYCODE_BUTTON_SELECT, send)
        PillButton("START", KeyEvent.KEYCODE_BUTTON_START, send)
    }
}
