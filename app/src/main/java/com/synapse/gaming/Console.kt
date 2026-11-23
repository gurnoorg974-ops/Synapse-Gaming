package com.synapse.gaming

// Package names: verify against the emulator builds you install.
enum class Console(val label: String, val emuPackage: String, val exts: Set<String>, val folderHints: List<String>) {
    XBOX360("Xbox 360", "aenu.ax360e", setOf("xex", "xbla"), listOf("xbox360", "x360")),
    PS2("PS2", "xyz.aethersx2.android", setOf("chd", "cso", "bin"), listOf("ps2")),
    GAMECUBE("GameCube", "org.dolphinemu.dolphinemu", setOf("gcm", "rvz", "gcz"), listOf("gc", "gamecube")),
    WII("Wii", "org.dolphinemu.dolphinemu", setOf("wbfs", "wad"), listOf("wii")),
    PSP("PSP", "org.ppsspp.ppsspp", setOf("cso", "pbp"), listOf("psp")),
    N3DS("3DS", "org.azahar_emu.azahar", setOf("3ds", "cia", "cxi"), listOf("3ds")),
    PS1("PS1", "com.github.stenzek.duckstation", setOf("cue", "pbp", "chd"), listOf("ps1", "psx"));

    companion object {
        // .iso etc. are ambiguous, so the parent folder name breaks ties.
        fun detect(fileName: String, parentFolder: String): Console? {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val folder = parentFolder.lowercase()
            entries.firstOrNull { c -> c.folderHints.any { it in folder } }?.let { return it }
            return entries.firstOrNull { ext in it.exts }
        }
    }
}
