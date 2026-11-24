package com.synapse.gaming

import android.content.Context
import android.content.Intent
import android.widget.Toast

fun launchGame(ctx: Context, game: Game) {
    game.console.core?.let { core ->
        ctx.startActivity(
            Intent(ctx, PlayerActivity::class.java)
                .putExtra("uri", game.uri.toString())
                .putExtra("core", core)
                .putExtra("ext", game.ext)
                .putExtra("title", game.title)
        )
        return
    }
    val pkg = emulatorPackage(ctx, game.console) ?: game.console.emuPackage
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setPackage(pkg)
        setDataAndType(game.uri, "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: just open the emulator
        val fallback = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (fallback != null) ctx.startActivity(fallback)
        else Toast.makeText(ctx, "${game.console.label} emulator not installed", Toast.LENGTH_LONG).show()
    }
}

// Returns the installed emulator app for a console (checks the paid and free versions)
fun emulatorPackage(ctx: Context, c: Console): String? =
    listOfNotNull(c.emuPackage.ifEmpty { null }, c.altPackage)
        .firstOrNull { ctx.packageManager.getLaunchIntentForPackage(it) != null }
