package com.synapse.gaming

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    Settings("Settings", Icons.Default.Settings)
}

fun isInstalled(ctx: Context, pkg: String) =
    ctx.packageManager.getLaunchIntentForPackage(pkg) != null

@Composable
fun SynapseApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("synapse", Context.MODE_PRIVATE) }
    var games by remember { mutableStateOf(emptyList<Game>()) }
    var tab by rememberSaveable { mutableStateOf(Tab.Library) }
    var filter by remember { mutableStateOf<Console?>(null) }

    fun rescan() { prefs.getString("root", null)?.let { games = scanLibrary(ctx, Uri.parse(it)) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.edit().putString("root", uri.toString()).apply()
            rescan()
            tab = Tab.Library
        }
    }
    LaunchedEffect(Unit) { rescan() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = SynCard) {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it, onClick = { tab = it },
                        icon = { Icon(it.icon, it.label) }, label = { Text(it.label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Menu -> R4Screen(games)
                Tab.Library -> LibraryScreen(games, filter, { filter = it }, { picker.launch(null) })
                Tab.Consoles -> ConsolesScreen(games) { filter = it; tab = Tab.Library }
                Tab.Settings -> SettingsScreen(games.size, { picker.launch(null) }, ::rescan)
            }
        }
    }
}

@Composable
fun Logo() {
    Row {
        Text("SYNAPSE", color = SynRed, fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(6.dp))
        Text("GAMING", fontWeight = FontWeight.Light,
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
            shape = RoundedCornerShape(12.dp)
        )
        LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(filter == null, { onFilter(null) }, { Text("All") }) }
            items(Console.entries.filter { c -> games.any { it.console == c } }) { c ->
                FilterChip(filter == c, { onFilter(c) }, { Text(c.label) })
            }
        }
        when {
            games.isEmpty() -> EmptyState(
                "No games yet", "Pick the folder where your games are stored and we'll sort them for you.",
                "Add games folder", onAddFolder
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
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SynCard)
    ) {
        Box(
            Modifier.fillMaxWidth().height(70.dp).background(game.console.color.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) { Text(game.console.label, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White) }
        Column(Modifier.padding(10.dp).heightIn(min = 56.dp)) {
            Text(game.title, maxLines = 2, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("Tap to play", style = MaterialTheme.typography.labelSmall, color = SynRed)
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
fun ConsolesScreen(games: List<Game>, onPick: (Console) -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text("Consoles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Tap a console to see its games", color = SynMuted)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Console.entries.toList()) { c ->
                val count = games.count { it.console == c }
                val installed = c.emuPackage.isNotEmpty() && isInstalled(ctx, c.emuPackage)
                Card(
                    onClick = { onPick(c) }, shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SynCard)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(c.color))
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
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(count: Int, onPick: () -> Unit, onRescan: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = SynCard), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Games folder", fontWeight = FontWeight.Bold)
                Text("$count games found", color = SynMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPick) { Text("Change folder") }
                    OutlinedButton(onClick = onRescan) { Text("Rescan") }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = SynCard), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your device", fontWeight = FontWeight.Bold)
                Text(SocInfo.model, color = SynMuted)
                Text(if (SocInfo.isSnapdragon) "Snapdragon detected" else "Not a Snapdragon chip", color = SynMuted)
            }
        }
        Text("Synapse Gaming v0.5", color = SynMuted, style = MaterialTheme.typography.labelSmall)
    }
}
