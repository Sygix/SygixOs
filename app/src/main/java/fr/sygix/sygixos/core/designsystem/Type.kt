/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SygixTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.2.sp),
)

object TextStyles {
    private val Sans = FontFamily.SansSerif
    val HeroTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 39.sp, letterSpacing = (-0.5).sp)
    val HeroSource = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    val Button = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    val Clock = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp, fontFeatureSettings = "tnum")
    val MenuTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val MenuSubtitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 10.sp)
    val Row = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    val RowFocused = Row.copy(fontWeight = FontWeight.Bold)
    val RowSecondary = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 10.sp)
    val RowState = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    val SettingsTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.25).sp)
}
