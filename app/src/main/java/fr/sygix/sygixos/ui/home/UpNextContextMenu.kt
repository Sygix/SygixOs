/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry

@Composable
internal fun UpNextContextMenu(item: UpNextItem, onOpen: (UpNextSourceEntry) -> Unit, modifier: Modifier = Modifier) {
    val first = remember { FocusRequester() }
    LaunchedEffect(item.key) { withFrameNanos { }; first.tryRequestFocus() }
    ContextMenuPanel(modifier.testTag("upnext-menu")) {
        ContextMenuHeader(item.displayTitle, stringResource(R.string.upnext_open_with), "upnext-menu") {
            UpNextArtwork(item, Modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(Dimens.MenuThumbnailCorner)).testTag("upnext-menu-thumbnail"))
        }
        item.sources.forEachIndexed { index, entry ->
            key(entry.packageName) {
                var focused by remember { mutableStateOf(false) }
                val colors = animatedPillColors(focused)
                Row(
                    Modifier.testTag("upnext-menu-entry-${entry.packageName}").fillMaxWidth()
                        .tvFocusable(focusRequester = if (index == 0) first else null, onFocused = { focused = it })
                        .tvClickable(onClick = { onOpen(entry) }).focusPill(colors, RoundedCornerShape(Dimens.PillCorner))
                        .heightIn(min = Dimens.MenuActionHeight).padding(horizontal = Dimens.MenuEntryPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.MenuEntryGap),
                ) {
                    entry.icon?.let { icon ->
                        val bitmap = remember(icon) { icon.toBitmap().asImageBitmap() }
                        Image(bitmap, null, Modifier.size(Dimens.MenuEntryIcon).clip(RoundedCornerShape(Dimens.MenuEntryIconCorner)))
                    }
                    Text(entry.label, style = if (focused) TextStyles.RowFocused else TextStyles.Row,
                        color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
