/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// Switch style Apple : track coloré, pouce glissant sur ressort.
// Composant purement visuel : non focalisable, non cliquable, sans sémantique propre ;
// la ligne hôte porte le rôle Switch, l'état et l'action.
@Composable
fun AppleSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val trackWidth = 64.dp
    val trackHeight = 36.dp
    val thumbSize = 30.dp
    val travel: Dp = trackWidth - thumbSize - 4.dp
    val trackColor by animateColorAsState(
        if (checked) Color(0xFF34C759) else Color.White.copy(alpha = 0.22f),
        spring(),
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(if (checked) travel else 0.dp, spring(), label = "switchThumb")
    Box(
        modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = 2.dp + thumbOffset)
                .size(thumbSize)
                .zIndex(1f)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
