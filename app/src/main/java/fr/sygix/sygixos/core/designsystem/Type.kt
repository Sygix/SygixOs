/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.sygix.sygixos.R

private fun figtree(weight: FontWeight) = Font(
    resId = R.font.figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Figtree = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
    figtree(FontWeight.ExtraBold),
)

private val Base = Typography()

val SygixTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Figtree),
    displayMedium = Base.displayMedium.copy(fontFamily = Figtree),
    displaySmall = Base.displaySmall.copy(fontFamily = Figtree),
    headlineLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Figtree),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Figtree),
    titleLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = Base.titleSmall.copy(fontFamily = Figtree),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Figtree),
    bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = Figtree),
    labelLarge = Base.labelLarge.copy(fontFamily = Figtree),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.2.sp),
    labelSmall = Base.labelSmall.copy(fontFamily = Figtree),
)

object TextStyles {
    private val Sans = Figtree
    val HeroTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 39.sp, letterSpacing = (-0.5).sp)
    val HeroSource = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    val HeroDetails = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val HeroRemaining = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
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
