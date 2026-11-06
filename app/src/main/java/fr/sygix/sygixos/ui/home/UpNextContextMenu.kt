/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry

@Composable
internal fun UpNextContextMenu(item: UpNextItem, onOpen: (UpNextSourceEntry) -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier, onAddToTracking: (() -> Unit)? = null) {
    val first = remember { FocusRequester() }
    LaunchedEffect(item.key) { withFrameNanos { }; first.tryRequestFocus() }
    ContextMenuPanel(modifier.testTag("upnext-menu")) {
        UpNextMenuHeader(item, stringResource(R.string.upnext_open_with))
        item.sources.distinctBy { it.packageName }.forEachIndexed { index, entry ->
            key(entry.packageName) {
                MenuEntry("upnext-menu-entry-${entry.packageName}", entry.label, { onOpen(entry) }, if (index == 0) first else null) {
                    entry.icon?.let { icon ->
                        Image(rememberIconBitmap(icon, Dimens.MenuEntryIcon), null, Modifier.size(Dimens.MenuEntryIcon).clip(RoundedCornerShape(Dimens.MenuEntryIconCorner)))
                    }
                }
            }
        }
        if (onAddToTracking != null) MenuEntry("tracking-action-add", stringResource(R.string.tracking_add), onAddToTracking) { }
        MenuEntry("upnext-menu-remove", stringResource(R.string.upnext_remove), onRemove) { color ->
            HiddenEyeGlyph(color, Modifier.size(Dimens.MenuEntryIcon).testTag("upnext-menu-remove-icon"))
        }
    }
}

@Composable
internal fun UpNextMenuHeader(item: UpNextItem, subtitle: String) {
    ContextMenuHeader(item.displayTitle, subtitle, "upnext-menu") {
        UpNextArtwork(item, Modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(Dimens.MenuThumbnailCorner)).testTag("upnext-menu-thumbnail"))
    }
}

@Composable
private fun MenuEntry(tag: String, label: String, onClick: () -> Unit, focusRequester: FocusRequester? = null, icon: @Composable (ColorProducer) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = rememberPillPaint(focused)
    Row(
        Modifier.testTag(tag).fillMaxWidth()
            .tvFocusable(focusRequester = focusRequester, onFocused = { focused = it })
            .tvClickable(onClick = onClick).focusPill(colors, RoundedCornerShape(Dimens.PillCorner))
            .heightIn(min = Dimens.MenuActionHeight).padding(horizontal = Dimens.MenuEntryPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.MenuEntryGap),
    ) {
        icon(colors.content)
        FocusText(label, style = if (colors.emphasized) TextStyles.RowFocused else TextStyles.Row, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HiddenEyeGlyph(color: ColorProducer, modifier: Modifier) {
    Canvas(modifier) {
        val tint = color()
        val unit = size.minDimension / EyeGrid
        val stroke = Stroke(unit * EyeStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val eye = Path().apply {
            moveTo(unit * 2, unit * 12)
            quadraticTo(unit * 12, unit * 2, unit * 22, unit * 12)
            quadraticTo(unit * 12, unit * 22, unit * 2, unit * 12)
            close()
        }
        drawPath(eye, tint, style = stroke)
        drawCircle(tint, unit * EyePupil, Offset(unit * 12, unit * 12), style = stroke)
        drawLine(tint, Offset(unit * 4, unit * 4), Offset(unit * 20, unit * 20), unit * EyeStroke, StrokeCap.Round)
    }
}

private const val EyeGrid = 24f
private const val EyeStroke = 1.8f
private const val EyePupil = 3f
