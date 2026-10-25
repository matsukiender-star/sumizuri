package com.sumizuri.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The accent of the desktop: the accent color Windows was set to, or the one picked in the settings of a Mac. It is read once, away from the window.
@Composable
internal actual fun rememberSystemAccent(): Color? {
    val accent: State<Color?> = produceState(DesktopAccent.known) {
        if (DesktopAccent.known == null) value = withContext(Dispatchers.IO) { DesktopAccent.read() }
    }
    return accent.value
}

// The accent once it is read, so every window and page after the first has it at once.
private object DesktopAccent {
    @Volatile
    var known: Color? = null

    // The colors of a Mac by the number its settings keep for each, with blue when none is set.
    private val macColors = mapOf(
        -1 to 0xFF8C8C8C,
        0 to 0xFFFF5257,
        1 to 0xFFF7821B,
        2 to 0xFFFFC600,
        3 to 0xFF62BA46,
        4 to 0xFF007AFF,
        5 to 0xFFA550A7,
        6 to 0xFFF74F9E,
    )

    fun read(): Color? {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val found = runCatching {
            when {
                os.contains("win") -> windows()
                os.contains("mac") -> mac()
                else -> null
            }
        }.getOrNull()
        known = found
        return found
    }

    // Windows keeps the accent as a number in the order alpha, blue, green, red.
    private fun windows(): Color? {
        val out = run("reg", "query", """HKCU\Software\Microsoft\Windows\DWM""", "/v", "AccentColor")
        val hex = Regex("0x([0-9a-fA-F]+)").find(out)?.groupValues?.get(1) ?: return null
        val abgr = hex.toLong(16)
        val red = (abgr and 0xFF).toInt()
        val green = ((abgr shr 8) and 0xFF).toInt()
        val blue = ((abgr shr 16) and 0xFF).toInt()
        return Color(red, green, blue)
    }

    private fun mac(): Color? {
        val out = run("defaults", "read", "-g", "AppleAccentColor").trim()
        val index = out.toIntOrNull() ?: 4
        return macColors[index]?.let { Color(it.toInt()) }
    }

    private fun run(vararg command: String): String {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return text
    }
}
