package com.synapse.gaming

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

val CloudIcon: ImageVector = ImageVector.Builder(
    name = "Cloud", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
).path(fill = SolidColor(Color.Black)) {
    moveTo(19.35f, 10.04f)
    curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
    curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
    curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
    curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
    horizontalLineTo(19f)
    curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
    curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
    close()
}.build()

private fun openCloud(ctx: Context, url: String) {
    ctx.startActivity(Intent(ctx, CloudActivity::class.java).putExtra("url", url))
}

private class HubApp(val name: String, val keywords: List<String>, val packages: List<String>, val getUrl: String)

private val PsRemote = HubApp(
    "PS Remote Play", listOf("remote play"), listOf("com.playstation.remoteplay"),
    "market://details?id=com.playstation.remoteplay"
)
private val Momiji = HubApp(
    "Momiji", listOf("momiji"), listOf("io.github.yazanj082.momiji"),
    "https://f-droid.org/packages/io.github.yazanj082.momiji/"
)
private val SteamLink = HubApp(
    "Steam Link", listOf("steam link"), listOf("com.valvesoftware.steamlink"),
    "market://details?id=com.valvesoftware.steamlink"
)
private val GameNative = HubApp(
    "GameNative", listOf("gamenative"), listOf("app.gamenative"),
    "https://github.com/utkarshdalal/GameNative"
)
private val Winlator = HubApp("Winlator", listOf("winlator"), listOf("com.winlator", "com.winlator.cmod"), "")

// Finds an installed app by package name, or by its launcher name
private fun findApp(ctx: Context, app: HubApp): String? {
    val pm = ctx.packageManager
    app.packages.firstOrNull { pm.getLaunchIntentForPackage(it) != null }?.let { return it }
    val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(main, 0).firstOrNull { ri ->
        val label = ri.loadLabel(pm).toString().lowercase()
        app.keywords.any { it in label }
    }?.activityInfo?.packageName
}

private fun openApp(ctx: Context, app: HubApp) {
    val pkg = findApp(ctx, app)
    val launch = pkg?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
    try {
        if (launch != null) {
            ctx.startActivity(launch)
        } else if (app.getUrl.isNotEmpty()) {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.getUrl)))
        } else {
            Toast.makeText(ctx, "Install ${app.name} first", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(ctx, "Couldn't open ${app.name}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AppButtons(apps: List<HubApp>) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        apps.forEach { app ->
            val installed = remember { findApp(ctx, app) != null }
            if (installed) {
                Button(onClick = { openApp(ctx, app) }, Modifier.fillMaxWidth()) { Text("Open ${app.name}") }
            } else if (app.getUrl.isNotEmpty()) {
                OutlinedButton(onClick = { openApp(ctx, app) }, Modifier.fillMaxWidth()) { Text("Get ${app.name}") }
            }
        }
    }
}

@Composable
private fun CloudCard(title: String, body: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SynCard),
        border = BorderStroke(1.dp, SynGlassRim), shape = GxShape
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Text(body, color = SynMuted, style = MaterialTheme.typography.bodyMedium)
            content()
        }
    }
}

@Composable
fun CloudScreen() {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cloud & PC", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Play without downloading games. You need your own accounts and a good internet connection.", color = SynMuted)
        CloudCard(
            "Xbox Cloud Gaming",
            "Built in. Stream Game Pass games right inside Synapse, or stream games from your own Xbox. Needs a Microsoft account.",
            Color(0xFF52B043)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { openCloud(ctx, "https://www.xbox.com/play") }) { Text("Play") }
                OutlinedButton(onClick = { openCloud(ctx, "https://www.xbox.com/remoteplay") }) { Text("My Xbox") }
            }
        }
        CloudCard(
            "PlayStation Remote Play",
            "Stream games from your own PS5 or PS4. Turn on Remote Play in your console settings first. Opens in its own app.",
            Color(0xFF3B6FD4)
        ) { AppButtons(listOf(PsRemote, Momiji)) }
        CloudCard(
            "Steam & Windows games",
            "Steam Link streams from your own PC. GameNative plays your Steam, Epic and GOG games on this phone. Winlator runs .exe files. Each opens in its own app.",
            Color(0xFF66C0F4)
        ) { AppButtons(listOf(SteamLink, GameNative, Winlator)) }
    }
}

@Composable
fun AutosaveCard() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("synapse", Context.MODE_PRIVATE) }
    var on by remember { mutableStateOf(prefs.getBoolean("autosave", true)) }
    Card(
        colors = CardDefaults.cardColors(containerColor = SynCard),
        border = BorderStroke(1.dp, SynGlassRim), shape = GxShape
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Autosave", fontWeight = FontWeight.Bold)
                    Text(
                        "Saves your place when you leave a game and picks up right there next time",
                        color = SynMuted, style = MaterialTheme.typography.labelMedium
                    )
                }
                Switch(checked = on, onCheckedChange = { on = it; prefs.edit().putBoolean("autosave", it).apply() })
            }
            OutlinedButton(onClick = {
                File(ctx.filesDir, "autosave").deleteRecursively()
                Toast.makeText(ctx, "Autosaves cleared", Toast.LENGTH_SHORT).show()
            }) { Text("Clear autosaves") }
        }
    }
}

// Built-in browser for Xbox Cloud Gaming / Xbox Remote Play
class CloudActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val url = intent.getStringExtra("url") ?: "https://www.xbox.com/play"
        root = FrameLayout(this).apply { setBackgroundColor(0xFF07050F.toInt()) }
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            // Look like regular mobile Chrome so Xbox's site accepts it
            settings.userAgentString = settings.userAgentString
                .replace("; wv", "").replace(Regex("Version/\\d+\\.\\d+ "), "")
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) return
                    customView = view
                    customCallback = callback
                    root.addView(
                        view,
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    )
                    hideBars()
                }

                override fun onHideCustomView() {
                    hideCustom()
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.deny()
                }
            }
            loadUrl(url)
        }
        root.addView(
            web,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setContentView(root)
        hideBars()
        onBackPressedDispatcher.addCallback(this) {
            val current = web.url ?: ""
            when {
                customView != null -> hideCustom()
                web.canGoBack() && !current.contains("xbox.com/play") && !current.contains("xbox.com/remoteplay") ->
                    web.goBack()
                else -> finish()
            }
        }
    }

    private fun hideCustom() {
        customView?.let { root.removeView(it) }
        customView = null
        customCallback?.onCustomViewHidden()
        customCallback = null
    }

    private fun hideBars() {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        root.removeAllViews()
        web.destroy()
        super.onDestroy()
    }
}
