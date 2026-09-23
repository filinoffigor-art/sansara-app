package ru.sansara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0A0A09)
val Panel = Color(0xFF171613)
val Panel2 = Color(0xFF211E19)
val Gold = Color(0xFFD8B07C)
val GoldSoft = Color(0xFFE7C59C)
val Text = Color(0xFFF5F0E8)
val Muted = Color(0xFF9D9992)
val Green = Color(0xFF4BE087)
val Red = Color(0xFFFF746C)
val Border = Color(0xFF4B4033)

private val Scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF17120D),
    background = Bg,
    onBackground = Text,
    surface = Panel,
    onSurface = Text,
    surfaceVariant = Panel2,
    onSurfaceVariant = Muted,
    secondary = GoldSoft,
    error = Red
)

@Composable
fun SansaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
