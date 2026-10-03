/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.domain.DockLayout
import fr.sygix.sygixos.model.TvApp

@Composable
internal fun Dock(
    apps: List<TvApp>,
    active: Boolean,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val packages = remember(apps) { apps.map { it.packageName } }
    val focus = rememberTileFocus(packages, focusEnabled)
    val entryApp = focus.entry(packages)
    BoxWithConstraints(
        modifier
            .padding(start = Dimens.ScreenMarginH, end = Dimens.ScreenMarginH, bottom = Dimens.DockBottomMargin)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val tileWidth = DockLayout.tileWidth(maxWidth.value, apps.size, Dimens.GridSpacing.value, Dimens.GridColumns).dp
        val dockWidth = DockLayout.dockWidth(tileWidth.value, apps.size, Dimens.GridSpacing.value, Dimens.DockPadding.value).dp
        GlassSurface(
            modifier = Modifier
                .testTag("dock-glass")
                .then(if (apps.isEmpty()) Modifier else Modifier.width(dockWidth))
                .focusGroup(),
            active = active,
        ) {
            if (apps.isEmpty()) {
                Text(
                    "Épinglez des apps depuis la grille (appui long)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                )
            } else {
                Row(
                    modifier = Modifier.padding(Dimens.DockPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
                ) {
                    apps.forEachIndexed { index, app ->
                        key(app.packageName) {
                            AppTile(
                                app = app,
                                focusEnabled = focusEnabled,
                                onClick = { onTileClick(app) },
                                onLongClick = { onTileLongClick(app) },
                                onFocusChanged = { if (it) focus.onFocused(app.packageName, index) },
                                focusRequester = focus.requesterFor(app.packageName),
                                modifier = Modifier
                                    .width(tileWidth)
                                    .then(if (app.packageName == entryApp) Modifier.focusRequester(focusRequester) else Modifier),
                            )
                        }
                    }
                }
            }
        }
    }
}
