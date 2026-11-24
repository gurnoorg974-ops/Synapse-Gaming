package com.synapse.gaming

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

// Folder name Synapse creates for each console
val Console.folderName: String
    get() = when (this) {
        Console.XBOX360 -> "Xbox360"
        Console.PS2 -> "PS2"
        Console.GAMECUBE -> "GameCube"
        Console.WII -> "Wii"
        Console.PSP -> "PSP"
        Console.N3DS -> "3DS"
        Console.PS1 -> "PS1"
        Console.N64 -> "N64"
        Console.NES -> "NES"
        Console.SNES -> "SNES"
        Console.GB -> "GameBoy"
        Console.GBA -> "GBA"
        Console.GENESIS -> "Genesis"
        Console.NDS -> "NDS"
    }

// Folder picker that asks for read AND write access (needed to create folders and add games)
class OpenTreeRW : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

fun hasWriteAccess(ctx: Context, root: Uri): Boolean =
    ctx.contentResolver.persistedUriPermissions.any { it.uri == root && it.isWritePermission }

// Creates one folder per console inside the chosen main folder
fun ensureConsoleFolders(ctx: Context, root: Uri): Boolean {
    return try {
        val dir = DocumentFile.fromTreeUri(ctx, root) ?: return false
        val existing = dir.listFiles().mapNotNull { it.name?.lowercase() }.toSet()
        for (c in Console.entries) {
            if (c.folderName.lowercase() !in existing) dir.createDirectory(c.folderName)
        }
        true
    } catch (e: Exception) {
        false
    }
}

data class ImportProgress(val fileName: String, val index: Int, val total: Int, val fraction: Float)
data class ImportResult(val copied: Int, val skipped: Int, val failed: Int)

private fun nameAndSize(ctx: Context, uri: Uri): Pair<String?, Long> {
    ctx.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null
    )?.use { c ->
        if (c.moveToFirst()) {
            val size = if (c.isNull(1)) -1L else c.getLong(1)
            return c.getString(0) to size
        }
    }
    return null to -1L
}

// Copies the picked files into <main folder>/<console folder>
suspend fun importGames(
    ctx: Context, root: Uri, console: Console, uris: List<Uri>, onProgress: (ImportProgress) -> Unit
): ImportResult = withContext(Dispatchers.IO) {
    val rootDir = DocumentFile.fromTreeUri(ctx, root)
        ?: return@withContext ImportResult(0, 0, uris.size)
    val dir = rootDir.findFile(console.folderName)?.takeIf { it.isDirectory }
        ?: rootDir.createDirectory(console.folderName)
        ?: return@withContext ImportResult(0, 0, uris.size)

    var copied = 0
    var skipped = 0
    var failed = 0
    uris.forEachIndexed { i, uri ->
        val (maybeName, size) = nameAndSize(ctx, uri)
        val name = maybeName ?: "game_$i"
        if (dir.findFile(name) != null) {
            skipped++
            return@forEachIndexed
        }
        try {
            onProgress(ImportProgress(name, i, uris.size, 0f))
            val ext = name.substringAfterLast('.', "").lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            val dest = dir.createFile(mime, name) ?: throw IOException("create failed")
            ctx.contentResolver.openInputStream(uri)!!.use { input ->
                ctx.contentResolver.openOutputStream(dest.uri)!!.use { out ->
                    val buf = ByteArray(256 * 1024)
                    var done = 0L
                    var last = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (size > 0) {
                            val pct = (done * 100 / size).toInt()
                            if (pct != last) {
                                last = pct
                                onProgress(ImportProgress(name, i, uris.size, done.toFloat() / size))
                            }
                        }
                    }
                }
            }
            if (dest.name != name) dest.renameTo(name)
            copied++
        } catch (e: Exception) {
            failed++
            try { dir.findFile(name)?.delete() } catch (_: Exception) {}
        }
    }
    ImportResult(copied, skipped, failed)
}
