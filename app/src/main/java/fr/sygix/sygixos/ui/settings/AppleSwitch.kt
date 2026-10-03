/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.sygix.sygixos.core.designsystem.SygixColors

private val TrackWidth = 38.dp
private val TrackHeight = 22.dp
private val ThumbSize = 18.dp
private val ThumbInset = 2.dp
private val ThumbShadow = Shadow(radius = 2.dp, color = Color.Black.copy(alpha = 0.25f), offset = DpOffset(0.dp, 1.dp))

@Composable
fun AppleSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val travel = TrackWidth - ThumbSize - ThumbInset * 2
    val trackColor by animateColorAsState(
        if (checked) SygixColors.SwitchOn else SygixColors.SwitchOff,
        spring(),
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(if (checked) travel else 0.dp, spring(), label = "switchThumb")
    Box(
        modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .width(TrackWidth)
            .height(TrackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = ThumbInset + thumbOffset)
                .size(ThumbSize)
                .zIndex(1f)
                .dropShadow(CircleShape, ThumbShadow)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
