package com.synapse.gaming

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SynapseTheme { SynapseApp() } }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Default.Home),
    Menu("Menu", MenuIcon),
    Consoles("Consoles", Icons.AutoMirrored.Filled.List),
    Cloud("Cloud", CloudIcon),
    Settings("Settings", Icons.Default.Settings)
}

fun isInstalled(ctx: Context, pkg: String) =
    ctx.packageManager.getLaunchIntentForPackage(pkg) != null

@Composable
fun SynapseMain() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("synapse", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var games by remember { mutableStateOf(emptyList<Game>()) }
    var tab by rememberSaveable { mutableStateOf(Tab.Library) }
    var filter by remember { mutableStateOf<Console?>(null) }
    var progress by remember { mutableStateOf<ImportProgress?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Console?>(null) }

    fun root(): Uri? = prefs.getString("root", null)?.let { Uri.parse(it) }

    fun rescan() {
        val r = root() ?: return
        scope.launch { games = withContext(Dispatchers.IO) { scanLibrary(ctx, r) } }
    }

    // Pick (or create) the main folder. Synapse then makes a folder for every console inside it.
    val treePicker = rememberLauncherForActivityResult(OpenTreeRW()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                try {
                    ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }
            prefs.edit().putString("root", uri.toString()).apply()
            scope.launch {
                val ok = withContext(Dispatchers.IO) { ensureConsoleFolders(ctx, uri) }
                notice = if (ok) "Console folders are ready. Tap + Add on a console to add games."
                else "Couldn't set up folders here. Pick or create a new folder (not the top-level Downloads folder)."
                games = withContext(Dispatchers.IO) { scanLibrary(ctx, uri) }
            }
            tab = Tab.Consoles
        }
    }

    // Pick game files from the phone's file manager and copy them into the console's folder
    val gamePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val c = pending
        val r = root()
        pending = null
        if (c != null && r != null && uris.isNotEmpty()) {
            scope.launch {
                progress = ImportProgress("", 0, uris.size, 0f)
                val res = importGames(ctx, r, c, uris) { progress = it }
                progress = null
                notice = buildString {
                    append("Added ${res.copied} to ${c.label}")
                    if (res.skipped > 0) append(", ${res.skipped} already there")
                    if (res.failed > 0) append(", ${res.failed} failed")
                }
                games = withContext(Dispatchers.IO) { scanLibrary(ctx, r) }
            }
        }
    }

    fun addGames(c: Console) {
        val r = root()
        if (r == null) {
            notice = "First choose or create a folder for your games."
            treePicker.launch(null)
            return
        }
        if (!hasWriteAccess(ctx, r)) {
            notice = "Pick your games folder once more so Synapse can add games to it."
            treePicker.launch(null)
            return
        }
        pending = c
        gamePicker.launch(arrayOf("*/*"))
    }

    LaunchedEffect(Unit) {
        val r = root()
        if (r != null) {
            withContext(Dispatchers.IO) { if (hasWriteAccess(ctx, r)) ensureConsoleFolders(ctx, r) }
            rescan()
        }
    }
    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
            notice = null
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = SynText,
        bottomBar = { FloatingNav(tab) { tab = it } }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Menu -> R4Screen(games) { c ->
                    if (c != null) addGames(c) else {
                        tab = Tab.Consoles
                        notice = "Tap + Add next to a console."
                    }
                }
                Tab.Library -> LibraryScreen(games, filter, { filter = it }) {
                    if (root() == null) treePicker.launch(null) else tab = Tab.Consoles
                }
                Tab.Consoles -> ConsolesScreen(games, { filter = it; tab = Tab.Library }, ::addGames)
                Tab.Cloud -> CloudScreen()
                Tab.Settings -> SettingsScreen(games.size, { treePicker.launch(null) }, ::rescan)
            }
        }
    }

    progress?.let { p ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            containerColor = androidx.compose.ui.graphics.Color(0xFF14151F),
            title = { Text("Adding games") },
            text = {
                Column {
                    Text(
                        if (p.fileName.isEmpty()) "Starting..." else "${p.index + 1} of ${p.total}: ${p.fileName}",
                        maxLines = 2
                    )
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { p.fraction }, modifier = Modifier.fillMaxWidth(),
                        color = SynYellow, trackColor = Color(0x33FFFFFF)
                    )
                }
            }
        )
    }
}

@Composable
fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GalaxyLogo(32.dp)
        Spacer(Modifier.width(8.dp))
        Text("SYNAPSE", color = SynRed, fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(6.dp))
        Text("GAMING", color = SynYellow, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun LibraryScreen(games: List<Game>, filter: Console?, onFilter: (Console?) -> Unit, onAddFolder: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val shown = games.filter {
        (filter == null || it.console == filter) && it.title.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(12.dp))
        Logo()
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search games") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SynCard, focusedContainerColor = SynCard,
                unfocusedBorderColor = Color(0x22FFFFFF), focusedBorderColor = SynRed
            )
        )
        LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(filter == null, { onFilter(null) }, { Text("All") }, shape = RoundedCornerShape(50)) }
            items(Console.entries.filter { c -> games.any { it.console == c } }) { c ->
                FilterChip(
                    filter == c, { onFilter(c) }, { Text(c.label) }, shape = RoundedCornerShape(50),
                    leadingIcon = { ConsoleIcon(c, Modifier.size(16.dp), c.color) }
                )
            }
        }
        when {
            games.isEmpty() -> EmptyState(
                "No games yet", "Choose or create a folder for Synapse. It sets up a folder for every console, then you add games with the + Add buttons.",
                "Add games", onAddFolder
            )
            shown.isEmpty() -> EmptyState("Nothing found", "Try a different search or filter.", null, {})
            else -> LazyVerticalGrid(
                GridCells.Adaptive(150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) { items(shown) { GameCard(it) { launchGame(ctx, it) } } }
        }
    }
}

@Composable
fun GameCard(game: Game, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val (sx, sy) = stretchScale(interaction, 0.05f)
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.graphicsLayer { scaleX = sx; scaleY = sy },
        shape = GxShape,
        colors = CardDefaults.cardColors(containerColor = SynCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SynGlassRim)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.8f)) {
            CoverArt(game, Modifier.fillMaxSize())
            Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp, end = 54.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConsoleIcon(game.console, Modifier.size(14.dp), game.console.color)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        game.console.label.uppercase(), color = game.console.color,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(
                    game.title, maxLines = 2, color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(10.dp).size(38.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape).background(SynYellow),
                contentAlignment = Alignment.Center
            ) { Icon(MenuIcon, null, tint = androidx.compose.ui.graphics.Color.Black) }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, action: String?, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = SynMuted)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(10.dp)) { Text(action) }
        }
    }
}

@Composable
fun ConsolesScreen(games: List<Game>, onPick: (Console) -> Unit, onAdd: (Console) -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text("Consoles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Tap a console to see its games, or + Add to put games in its folder", color = SynMuted)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Console.entries.toList()) { c ->
                val count = games.count { it.console == c }
                val installed = emulatorPackage(ctx, c) != null
                Card(
                    onClick = { onPick(c) }, shape = GxShape,
                    colors = CardDefaults.cardColors(containerColor = SynCard), border = androidx.compose.foundation.BorderStroke(1.dp, SynGlassRim)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(c.color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) { ConsoleIcon(c, Modifier.size(28.dp), c.color) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.label, fontWeight = FontWeight.Bold)
                            Text(
                                if (c.core != null) "Built in" else if (installed) "Emulator ready" else "Emulator not installed",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (c.core != null || installed) SynMuted else SynRed
                            )
                        }
                        Text("$count", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { onAdd(c) }) { Text("+ Add") }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(count: Int, onPick: () -> Unit, onRescan: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = SynCard), border = androidx.compose.foundation.BorderStroke(1.dp, SynGlassRim), shape = GxShape) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Games folder", fontWeight = FontWeight.Bold)
                Text("$count games found", color = SynMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPick) { Text("Change folder") }
                    OutlinedButton(onClick = onRescan) { Text("Rescan") }
                }
            }
        }
        AutosaveCard()
        Card(colors = CardDefaults.cardColors(containerColor = SynCard), border = androidx.compose.foundation.BorderStroke(1.dp, SynGlassRim), shape = GxShape) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your device", fontWeight = FontWeight.Bold)
                Text(SocInfo.model, color = SynMuted)
                Text(if (SocInfo.isSnapdragon) "Snapdragon detected" else "Not a Snapdragon chip", color = SynMuted)
            }
        }
        Text("Synapse Gaming v0.16", color = SynMuted, style = MaterialTheme.typography.labelSmall)
    }
}
