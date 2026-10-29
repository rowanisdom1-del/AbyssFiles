package com.rowan.abyssfiles.launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Same Abyss-inspired palette as Abyss Files so the whole system feels like one design. */
object Abyss {
    val bg = Color(0xFF05070D)
    val bg2 = Color(0xFF0B1020)
    val surface = Color(0xFF111729)
    val surfaceHi = Color(0xFF1C2646)
    val accent = Color(0xFF7AA2FF)
    val accent2 = Color(0xFFB794F6)
    val text = Color(0xFFE6EAF5)
    val dim = Color(0xFF8A94B2)

    val backdrop: Brush
        get() = Brush.verticalGradient(listOf(Color(0xFF070B18), bg))
}

@Composable
fun AbyssTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Abyss.accent,
            onPrimary = Abyss.bg,
            secondary = Abyss.accent2,
            background = Abyss.bg,
            onBackground = Abyss.text,
            surface = Abyss.surface,
            onSurface = Abyss.text,
        ),
        content = content,
    )
}
