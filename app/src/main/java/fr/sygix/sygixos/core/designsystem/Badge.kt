/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag

enum class BadgePlacement(val alignment: Alignment) {
    CORNER(Alignment.TopEnd),
    ROW_END(Alignment.CenterEnd),
}

@Composable
fun BoxScope.Badge(
    placement: BadgePlacement,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    Spacer(
        modifier
            .align(placement.alignment)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .focusProperties { canFocus = false }
            .size(Dimens.Badge)
            .background(SygixColors.Badge, CircleShape),
    )
}
