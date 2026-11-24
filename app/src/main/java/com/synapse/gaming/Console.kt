package com.synapse.gaming

// core != null  -> plays inside the app (libretro core, downloaded on first use)
// core == null  -> launches the external emulator app instead
enum class Console(
    val label: String, val emuPackage: String, val exts: Set<String>,
    val folderHints: List<String>, val core: String? = null, val tint: Long = 0xFF5A6572,
    val altPackage: String? = null
) {
    XBOX360("Xbox 360", "aenu.ax360e", setOf("xex", "xbla"), listOf("xbox360", "x360"), tint = 0xFF52B043, altPackage = "aenu.ax360e.free"),
    PS2("PS2", "xyz.aethersx2.android", setOf("chd", "cso", "bin"), listOf("ps2"), tint = 0xFF3B6FD4),
    GAMECUBE("GameCube", "org.dolphinemu.dolphinemu", setOf("gcm", "rvz", "gcz"), listOf("gc", "gamecube"), tint = 0xFF7B5CD6),
    WII("Wii", "org.dolphinemu.dolphinemu", setOf("wbfs", "wad"), listOf("wii"), tint = 0xFF2EB5D6),
    PSP("PSP", "org.ppsspp.ppsspp", setOf("cso", "pbp"), listOf("psp"), core = "ppsspp", tint = 0xFF5A6572),
    N3DS("3DS", "org.azahar_emu.azahar", setOf("3ds", "cia", "cxi"), listOf("3ds"), tint = 0xFFE0A030),
    PS1("PS1", "com.github.stenzek.duckstation", setOf("cue", "pbp", "chd"), listOf("ps1", "psx"), core = "pcsx_rearmed", tint = 0xFF3B6FD4),
    N64("N64", "", setOf("z64", "n64", "v64"), listOf("n64"), core = "mupen64plus_next_gles3", tint = 0xFFD64545),
    NES("NES", "", setOf("nes"), listOf("nes"), core = "fceumm", tint = 0xFFC0392B),
    SNES("SNES", "", setOf("sfc", "smc"), listOf("snes", "sfc"), core = "snes9x", tint = 0xFF8E6BBF),
    GB("Game Boy", "", setOf("gb", "gbc"), listOf("gb", "gbc", "gameboy"), core = "gambatte", tint = 0xFF7F9A3A),
    GBA("GBA", "", setOf("gba"), listOf("gba"), core = "mgba", tint = 0xFF5B4BB5),
    GENESIS("Genesis", "", setOf("md", "gen", "smd"), listOf("genesis", "megadrive"), core = "genesis_plus_gx", tint = 0xFF3A6EA5),
    NDS("DS", "", setOf("nds"), listOf("nds", "nintendods"), core = "melonds", tint = 0xFF9AA5B1);

    companion object {
        // Folder name words win (so .iso files can be sorted), then the file type.
        fun detect(fileName: String, parentFolder: String): Console? {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val words = parentFolder.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
            entries.firstOrNull { c -> c.folderHints.any { it in words } }?.let { return it }
            return entries.firstOrNull { ext in it.exts }
        }
    }
}
