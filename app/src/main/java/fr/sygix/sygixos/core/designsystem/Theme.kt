/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SygixColors {
    val GlassTint = Color(22, 22, 28, 92)
    val CapsuleTint = Color(22, 22, 28, 97)
    val MenuTint = Color(18, 18, 24, 184)
    val GlassFallback = Color(22, 22, 28, 204)
    val MenuFallback = Color(18, 18, 24, 235)
    val GlassBorder = Color.White.copy(alpha = 0.16f)
    val GlassHighlight = Color.White.copy(alpha = 0.32f)
    val GlassLowlight = Color.White.copy(alpha = 0.06f)
    val GlassShadow = Color.Black.copy(alpha = 0.32f)
    val CapsuleShadow = Color.Black.copy(alpha = 0.28f)
    val MenuShadow = Color.Black.copy(alpha = 0.5f)
    val Scrim = Color.Black.copy(alpha = 0.55f)
    val ButtonRest = Color(22, 22, 28, 115)
    val ButtonRim = Color.White.copy(alpha = 0.22f)
    val ButtonHighlight = Color.White.copy(alpha = 0.30f)
    val ButtonFocusRim = Color.White.copy(alpha = 0.9f)
    val PillFocus = Color(0xFFF2F2F5)
    val OnPill = Color(0xFF0B0B0F)
    val OnPillSecondary = Color(0xFF4A4A52)
    val OnPillTertiary = Color(0xFF3A3A42)
    val PillSelected = Color.White.copy(alpha = 0.14f)
    val PillRest = Color.White.copy(alpha = 0.08f)
    val PillShadow = Color.Black.copy(alpha = 0.35f)
    val OnDark = Color.White
    val OnDarkSecondary = Color.White.copy(alpha = 0.6f)
    val GearRest = Color.White.copy(alpha = 0.10f)
    val TileShadow = Color.Black
    val TileSheen = Color.White
    val SwitchOn = Color(0xFF34C759)
    val SwitchOff = Color(120, 120, 128, 140)
    val Veil = Color.Black
}

private val DarkColors = darkColorScheme(
    background = Color.Black,
    surface = Color(0xFF0A0A0C),
    surfaceVariant = Color(0xFF141418),
    primary = Color(0xFF4D8DF7),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun SygixOsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = SygixTypography,
        content = content,
    )
}
