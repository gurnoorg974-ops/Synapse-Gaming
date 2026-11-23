package com.synapse.gaming

import android.content.Context
import android.content.Intent
import android.widget.Toast

fun launchGame(ctx: Context, game: Game) {
    val pkg = game.console.emuPackage
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
