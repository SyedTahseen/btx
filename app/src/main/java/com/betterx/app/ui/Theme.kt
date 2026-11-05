package com.betterx.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Brand palette: true black, one neon lime accent, olive for secondary surfaces. */
object BX {
    val Lime = Color(0xFFEEF22A)
    val Black = Color(0xFF000000)
    val Card = Color(0xFF0E0E0E)
    val CardBorder = Color(0xFF30330F)
    val Olive1 = Color(0xFFB9BD62)
    val Olive2 = Color(0xFF7C8030)
    val OliveInk = Color(0xFF14150A)
    val Track = Color(0xFF1B1C0C)
    val Chip = Color(0xFF2A2A2A)
    val Nav = Color(0xFF2B2B2B)
}

private val Scheme = darkColorScheme(
    primary = BX.Lime,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF23250A),
    onPrimaryContainer = Color(0xFFF1F48A),
    secondary = Color(0xFFD7DC5B),
    onSecondary = Color.Black,
    secondaryContainer = BX.Lime,
    onSecondaryContainer = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0A0A0A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFA3A3A3),
    surfaceContainer = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1B1B1B),
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF2A2A2A),
    error = Color(0xFFFF8A7A),
    errorContainer = Color(0xFF3A1A14),
    onErrorContainer = Color(0xFFFFD9D2),
)

private val base = Typography()
private val Type = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun BetterXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = Type,
        shapes = Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
