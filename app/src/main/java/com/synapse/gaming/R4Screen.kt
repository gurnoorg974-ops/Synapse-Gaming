package com.synapse.gaming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val MenuIcon: ImageVector get() = Icons.Default.PlayArrow

// R4-card style menu: one simple list of every game, pick one and press PLAY.
@Composable
fun R4Screen(games: List<Game>) {
    val ctx = LocalContext.current
    var console by remember { mutableStateOf<Console?>(null) }
    var sel by remember { mutableIntStateOf(0) }
    val list = games.filter { console == null || it.console == console }
    val options = listOf<Console?>(null) + Console.entries.filter { c -> games.any { it.console == c } }
    val state = rememberLazyListState()
    val focus = remember { FocusRequester() }

    LaunchedEffect(console) { sel = 0 }
    LaunchedEffect(sel, list.size) {
        if (list.isNotEmpty()) state.animateScrollToItem(sel.coerceIn(0, list.lastIndex))
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun move(d: Int) { if (list.isNotEmpty()) sel = (sel + d).coerceIn(0, list.lastIndex) }
    fun play() { list.getOrNull(sel)?.let { launchGame(ctx, it) } }
    fun cycle(d: Int) {
        val i = options.indexOf(console).coerceAtLeast(0)
        console = options[(i + d).mod(options.size)]
    }

    Column(
        Modifier.fillMaxSize()
            .focusRequester(focus).focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionUp -> { move(-1); true }
                    Key.DirectionDown -> { move(1); true }
                    Key.DirectionLeft, Key.ButtonL1 -> { cycle(-1); true }
                    Key.DirectionRight, Key.ButtonR1 -> { cycle(1); true }
                    Key.ButtonA, Key.Enter, Key.ButtonStart -> { play(); true }
                    else -> false
                }
            }
            .padding(14.dp)
    ) {
        Text("Quick Menu", color = SynRed, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { cycle(-1) }) { Text("L") }
            Text(
                "${console?.label ?: "ALL"}  ·  ${if (list.isEmpty()) 0 else sel + 1}/${list.size}",
                Modifier.weight(1f), fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            OutlinedButton(onClick = { cycle(1) }) { Text("R") }
        }

        if (list.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No games. Add a folder in Settings.", color = SynMuted)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().background(SynCard, RoundedCornerShape(12.dp))) {
                itemsIndexed(list) { i, g ->
                    val on = i == sel
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (on) SynYellow else Color.Transparent)
                            .clickable { if (on) play() else sel = i }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (on) "> " else "  ", fontFamily = FontFamily.Monospace, color = if (on) Color.Black else SynRed)
                        Text(g.title, Modifier.weight(1f), maxLines = 1, fontFamily = FontFamily.Monospace,
                            color = if (on) Color.Black else SynText)
                        Text(g.console.label, style = MaterialTheme.typography.labelSmall,
                            color = if (on) Color.Black else g.console.color)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { move(-1) }, Modifier.weight(1f)) { Text("UP") }
            OutlinedButton(onClick = { move(1) }, Modifier.weight(1f)) { Text("DOWN") }
            Button(onClick = { play() }, Modifier.weight(1.4f), enabled = list.isNotEmpty()) { Text("PLAY") }
        }
    }
}
