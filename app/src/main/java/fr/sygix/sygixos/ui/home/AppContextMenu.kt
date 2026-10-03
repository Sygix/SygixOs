/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassLook
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocusable
import fr.sygix.sygixos.model.TvApp

private val PackageColor = Color(235, 235, 245).copy(alpha = 0.7f)
private val ThumbnailWidth = 56.dp
private val HeaderGap = 10.dp
private val ActionGap = 4.dp

@Composable
internal fun AppContextMenu(
    app: TvApp,
    pinned: Boolean,
    onTogglePin: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: (() -> Unit)? = null,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(app.packageName) {
        withFrameNanos { }
        firstFocus.tryRequestFocus()
    }
    Box(
        modifier
            .testTag("app-menu")
            .fillMaxSize()
            .background(SygixColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.width(Dimens.MenuWidth),
            shape = RoundedCornerShape(Dimens.MenuCorner),
            look = GlassLook.Menu,
        ) {
            Column(
                Modifier
                    .padding(Dimens.MenuPadding)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(ActionGap),
            ) {
                MenuHeader(app)
                MenuAction(
                    text = if (pinned) "Retirer du dock" else "Épingler au dock",
                    action = "pin",
                    onClick = onTogglePin,
                    focusRequester = firstFocus,
                )
                if (onMove != null) MenuAction(text = "Déplacer", action = "move", onClick = onMove)
                MenuAction(text = "Cacher", action = "hide", onClick = onHide)
                MenuAction(text = "Fermer", action = "close", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun MenuHeader(app: TvApp) {
    val artwork by rememberAppArtwork(app)
    Row(
        Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = HeaderGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeaderGap),
    ) {
        Box(Modifier.width(ThumbnailWidth)) { TileBox(artwork, app.label) }
        Column {
            Text(app.label, style = TextStyles.MenuTitle, color = SygixColors.OnDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.packageName, style = TextStyles.MenuSubtitle, color = PackageColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MenuAction(text: String, action: String, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused)
    Box(
        Modifier
            .testTag("menu-action-$action")
            .fillMaxWidth()
            .height(Dimens.MenuActionHeight)
            .tvFocusable(onFocused = { focused = it }, focusRequester = focusRequester)
            .tvClickable(onClick = onClick)
            .focusPill(colors, RoundedCornerShape(Dimens.PillCorner))
            .padding(horizontal = Dimens.MenuPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            style = if (focused) TextStyles.RowFocused else TextStyles.Row,
            color = colors.content,
            maxLines = 1,
            softWrap = false,
        )
    }
}
