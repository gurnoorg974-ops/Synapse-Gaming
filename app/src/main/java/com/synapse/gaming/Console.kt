package com.synapse.gaming

// core != null  -> plays inside the app (libretro core, downloaded on first use)
// core == null  -> launches the external emulator app instead
enum class Console(
    val label: String, val emuPackage: String, val exts: Set<String>,
    val folderHints: List<String>, val core: String? = null
) {
    XBOX360("Xbox 360", "aenu.ax360e", setOf("xex", "xbla"), listOf("xbox360", "x360")),
    PS2("PS2", "xyz.aethersx2.android", setOf("chd", "cso", "bin"), listOf("ps2")),
    GAMECUBE("GameCube", "org.dolphinemu.dolphinemu", setOf("gcm", "rvz", "gcz"), listOf("gc", "gamecube")),
    WII("Wii", "org.dolphinemu.dolphinemu", setOf("wbfs", "wad"), listOf("wii")),
    PSP("PSP", "org.ppsspp.ppsspp", setOf("cso", "pbp"), listOf("psp"), core = "ppsspp"),
    N3DS("3DS", "org.azahar_emu.azahar", setOf("3ds", "cia", "cxi"), listOf("3ds")),
    PS1("PS1", "com.github.stenzek.duckstation", setOf("cue", "pbp", "chd"), listOf("ps1", "psx"), core = "pcsx_rearmed"),
    N64("N64", "", setOf("z64", "n64", "v64"), listOf("n64"), core = "mupen64plus_next_gles3");

    companion object {
        fun detect(fileName: String, parentFolder: String): Console? {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val folder = parentFolder.lowercase()
            entries.firstOrNull { c -> c.folderHints.any { it in folder } }?.let { return it }
            return entries.firstOrNull { ext in it.exts }
        }
    }
}
