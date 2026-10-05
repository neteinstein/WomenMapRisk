package com.womenriskmap.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand: deep violet (trust, solidarity) + warm coral accent. Same palette on every platform, so the
// brand and the risk colours stay recognisable. Android 12+ dynamic colour is intentionally not used.
internal val Violet10 = Color(0xFF240046)
internal val Violet20 = Color(0xFF3C0A6B)
internal val Violet30 = Color(0xFF53288A)
internal val Violet40 = Color(0xFF6B3FA0)
internal val Violet80 = Color(0xFFD7BAFF)
internal val Violet90 = Color(0xFFEDDCFF)
internal val Coral30 = Color(0xFF7A2638)
internal val Coral40 = Color(0xFFB4475E)
internal val Coral80 = Color(0xFFFFB2BE)
internal val Coral90 = Color(0xFFFFD9DE)
internal val Teal40 = Color(0xFF00696E)
internal val Teal80 = Color(0xFF6FD6DC)
internal val Teal90 = Color(0xFFB6F1F4)
internal val Teal20 = Color(0xFF00373A)

internal val LightColors = lightColorScheme(
    primary = Violet40,
    onPrimary = Color.White,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet10,
    secondary = Coral40,
    onSecondary = Color.White,
    secondaryContainer = Coral90,
    onSecondaryContainer = Coral30,
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal20,
    background = Color(0xFFFFF7FE),
    onBackground = Color(0xFF1D1A20),
    surface = Color(0xFFFFF7FE),
    onSurface = Color(0xFF1D1A20),
    surfaceVariant = Color(0xFFE9DFEB),
    onSurfaceVariant = Color(0xFF4A454E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9F1FA),
    surfaceContainer = Color(0xFFF3EBF4),
    surfaceContainerHigh = Color(0xFFEDE5EE),
    surfaceContainerHighest = Color(0xFFE7E0E8),
    outline = Color(0xFF7B757F),
    outlineVariant = Color(0xFFCCC4CF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

internal val DarkColors = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet20,
    primaryContainer = Violet30,
    onPrimaryContainer = Violet90,
    secondary = Coral80,
    onSecondary = Color(0xFF5E1124),
    secondaryContainer = Coral30,
    onSecondaryContainer = Coral90,
    tertiary = Teal80,
    onTertiary = Teal20,
    tertiaryContainer = Color(0xFF004F53),
    onTertiaryContainer = Teal90,
    background = Color(0xFF151218),
    onBackground = Color(0xFFE7E0E8),
    surface = Color(0xFF151218),
    onSurface = Color(0xFFE7E0E8),
    surfaceVariant = Color(0xFF4A454E),
    onSurfaceVariant = Color(0xFFCCC4CF),
    surfaceContainerLowest = Color(0xFF100D12),
    surfaceContainerLow = Color(0xFF1D1A20),
    surfaceContainer = Color(0xFF221E24),
    surfaceContainerHigh = Color(0xFF2C292F),
    surfaceContainerHighest = Color(0xFF37333A),
    outline = Color(0xFF958E99),
    outlineVariant = Color(0xFF4A454E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** Risk colours (spec §6). Always paired with a shape/symbol: never rely on colour alone (colour-blind users). */
data class RiskColors(val green: Color, val yellow: Color, val red: Color, val onGreen: Color, val onYellow: Color, val onRed: Color)

internal val LightRiskColors = RiskColors(
    green = Color(0xFF2E7D32),
    yellow = Color(0xFFF9A825),
    red = Color(0xFFC62828),
    onGreen = Color.White,
    onYellow = Color(0xFF231A00),
    onRed = Color.White,
)

internal val DarkRiskColors = RiskColors(
    green = Color(0xFF81C784),
    yellow = Color(0xFFFFD54F),
    red = Color(0xFFEF9A9A),
    onGreen = Color(0xFF00390A),
    onYellow = Color(0xFF3B2F00),
    onRed = Color(0xFF5F0A0A),
)
