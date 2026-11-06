/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.domain.QrCode

@Composable
internal fun QrCodeImage(qr: QrCode, modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .size(Dimens.QrSize)
            .clip(RoundedCornerShape(qrCorner(qr)))
            .drawWithCache {
                val grid = qr.grid(size.minDimension.toInt())
                val module = grid.module.toFloat()
                val modules = Path().apply {
                    for (y in 0 until qr.size) for (x in 0 until qr.size) if (qr[x, y]) {
                        addRect(Rect(Offset((grid.origin + x * grid.module).toFloat(), (grid.origin + y * grid.module).toFloat()), Size(module, module)))
                    }
                }
                onDrawBehind {
                    drawRect(SygixColors.QrLight)
                    drawPath(modules, SygixColors.QrDark)
                }
            },
    )
}

internal fun qrCorner(qr: QrCode): Dp = Dimens.QrSize * QrCode.QUIET_ZONE / qr.size
