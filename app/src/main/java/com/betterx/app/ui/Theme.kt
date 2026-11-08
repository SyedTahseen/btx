package com.betterx.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Palette taken from the banking-app reference: soft graphite background, a bright white sheet,
 * light "pill" buttons, and three saturated card colours (yellow, mint, blue).
 */
object BX {
    // Surfaces (dark)
    val Bg = Color(0xFF141517)
    val Card = Color(0xFF212225)
    val CardBorder = Color(0xFF2A2B2E)
    val Chip = Color(0xFF2D2E32)
    val Track = Color(0xFF2A2B2E)
    val Nav = Color(0xFF232427)

    // Light sheet
    val Sheet = Color(0xFFFFFFFF)
    val SheetInk = Color(0xFF16171A)
    val SheetMuted = Color(0xFF8D8E93)
    val SheetChip = Color(0xFFF2F2F4)

    // Interactive pill (the white "Send" button / selected tab)
    val Pill = Color(0xFFEDEDED)

    // Brand colours
    val Yellow = Color(0xFFFFE32B)
    val Mint = Color(0xFF2CF5CF)
    val Blue = Color(0xFF1E9BF5)
    val Coral = Color(0xFFFF7A59)
    val Violet = Color(0xFFA48BFF)
    val Rose = Color(0xFFFF7EB6)
    val Success = Color(0xFF19D81F)
    val Danger = Color(0xFFFF8A7A)
    val Positive = Color(0xFF2E9B3E)

    // Neutral greys used for icons and disabled states (one value each, used everywhere)
    val IconMuted = Color(0xFFB4B5BA)
    val Disabled = Color(0xFF77787D)
    val Outline = Color(0xFF5A5B60)

    /** Highlight colour for small accents (progress, bars, links). */
    val Accent = Mint

    /** Card colours used for programs, in order. */
    val CardColors = listOf(Yellow, Mint, Blue, Coral, Violet, Rose)
}

private val Scheme = darkColorScheme(
    primary = BX.Pill,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF26282B),
    onPrimaryContainer = Color(0xFFEDEDED),
    secondary = Color(0xFFD9D9DC),
    onSecondary = Color.Black,
    secondaryContainer = BX.Pill,
    onSecondaryContainer = Color.Black,
    background = BX.Bg,
    onBackground = Color.White,
    surface = BX.Bg,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1D20),
    onSurfaceVariant = Color(0xFFA0A1A6),
    surfaceContainer = Color(0xFF1C1D20),
    surfaceContainerHigh = BX.Card,
    surfaceContainerHighest = BX.Chip,
    outline = Color(0xFF5A5B60),
    outlineVariant = Color(0xFF2D2E32),
    error = Color(0xFFFF8A7A),
    errorContainer = Color(0xFF3A1F1B),
    onErrorContainer = Color(0xFFFFD9D2),
)

/** Used inside the white bottom sheet so every Text and Icon there reads on white. */
private val SheetScheme = lightColorScheme(
    primary = BX.SheetInk,
    onPrimary = Color.White,
    primaryContainer = BX.SheetChip,
    onPrimaryContainer = BX.SheetInk,
    secondary = BX.SheetInk,
    secondaryContainer = BX.SheetChip,
    onSecondaryContainer = BX.SheetInk,
    background = BX.Sheet,
    onBackground = BX.SheetInk,
    surface = BX.Sheet,
    onSurface = BX.SheetInk,
    surfaceVariant = BX.SheetChip,
    onSurfaceVariant = BX.SheetMuted,
    outline = Color(0xFFC9CACE),
    outlineVariant = Color(0xFFE6E6E9),
    error = Color(0xFFD64535),
    errorContainer = Color(0xFFFDECE9),
    onErrorContainer = Color(0xFF5A1A12),
)

@Composable
fun SheetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SheetScheme, typography = Type, shapes = AppShapes) {
        // Plain Text/Icon read LocalContentColor, which is white from the dark app surface: make it dark on the white sheet.
        CompositionLocalProvider(LocalContentColor provides BX.SheetInk, content = content)
    }
}

private val base = Typography()
private val Type = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraSmall = RoundedCornerShape(14.dp), // text fields, menus
    extraLarge = RoundedCornerShape(32.dp), // dialogs
)

@Composable
fun BetterXTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = AppShapes, content = content)
}
