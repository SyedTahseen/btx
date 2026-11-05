package com.betterx.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Scheme = darkColorScheme(
    primary = Color(0xFFE8A0BC),
    onPrimary = Color(0xFF3A0F24),
    primaryContainer = Color(0xFF5A2740),
    onPrimaryContainer = Color(0xFFFFD8E7),
    secondary = Color(0xFF8FD1C5),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF1F4D45),
    onSecondaryContainer = Color(0xFFB2EFE3),
    background = Color(0xFF0F0C16),
    onBackground = Color(0xFFEDE6F2),
    surface = Color(0xFF16121F),
    onSurface = Color(0xFFEDE6F2),
    surfaceVariant = Color(0xFF241E30),
    onSurfaceVariant = Color(0xFFC9BFD6),
    surfaceContainer = Color(0xFF1B1626),
    surfaceContainerHigh = Color(0xFF241E30),
    outline = Color(0xFF8A8096),
    outlineVariant = Color(0xFF3A3346),
)

@Composable
fun BetterXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
