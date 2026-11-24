package com.synapse.gaming

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class Game(val title: String, val uri: Uri, val console: Console, val ext: String = "")

private val gameExts = Console.entries.flatMap { it.exts }.toSet() + setOf("iso")

fun scanLibrary(ctx: Context, root: Uri): List<Game> {
    val out = mutableListOf<Game>()
    fun walk(dir: DocumentFile) {
        for (f in dir.listFiles()) {
            if (f.isDirectory) { walk(f); continue }
            val name = f.name ?: continue
            if (name.substringAfterLast('.', "").lowercase() !in gameExts) continue
            val console = Console.detect(name, dir.name ?: "") ?: continue
            out += Game(name.substringBeforeLast('.'), f.uri, console, name.substringAfterLast('.', ""))
        }
    }
    DocumentFile.fromTreeUri(ctx, root)?.let(::walk)
    return out.sortedBy { it.title.lowercase() }
}
