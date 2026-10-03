/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.clearAndSetSemantics
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tvFocusable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.domain.ListReveal
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.home.TileFocus
import fr.sygix.sygixos.ui.home.rememberAppArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalAppIcons = staticCompositionLocalOf<AppIconCache?> { null }

private const val LeadingItems = 1

internal val RowShape = RoundedCornerShape(Dimens.SettingsRowCorner)
internal val RowHeight = 48.dp
internal val RowVerticalPadding = 6.dp
internal val RowGap = 12.dp
internal val RowSpacing = 5.dp
private val EntryButtonHeight = 42.dp
private val ThumbWidth = 48.dp
private val ThumbHeight = 27.dp
private val ThumbCorner = 5.dp
private const val IconHeightFraction = 0.8f

internal fun programCountLabel(count: Int): String = when (count) {
    0 -> "Aucun programme publié"
    1 -> "1 programme publié"
    else -> "$count programmes publiés"
}

@Composable
internal fun SourcesContent(
    rows: List<SourceRow>,
    counts: State<Map<String, Int>?>,
    listState: LazyListState,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focusedPackage by remember { mutableStateOf<String?>(null) }
    val order = remember(rows) { rows.map { it.app.packageName } }
    LaunchedEffect(order) {
        if (!focusEnabled) return@LaunchedEffect
        withFrameNanos { }
        val visible = listState.layoutInfo.visibleItemsInfo.map { it.key }
        ListReveal.indexToReveal(order, focusedPackage, visible, LeadingItems)?.let { listState.scrollToItem(it) }
    }
    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("settings-sources")
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        item {
            Text(stringResource(R.string.settings_category_sources), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.sources_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(20.dp))
        }
        items(rows, key = { it.app.packageName }) { row ->
            SourceRowLine(
                row = row,
                counts = counts,
                focusEnabled = focusEnabled,
                focusRequester = if (rows.firstOrNull()?.app?.packageName == row.app.packageName) contentFocus else null,
                onFocused = { focusedPackage = row.app.packageName },
                onToggle = { onToggle(row.app.packageName) },
            )
            Spacer(Modifier.height(RowSpacing))
        }
    }
}

@Composable
private fun SourceRowLine(
    row: SourceRow,
    counts: State<Map<String, Int>?>,
    focusEnabled: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onToggle: () -> Unit,
) {
    val programCount by remember(row.app.packageName) {
        derivedStateOf { counts.value?.get(row.app.packageName) ?: 0 }
    }
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused)
    Row(
        Modifier
            .testTag("source-row-${row.app.packageName}")
            .fillMaxWidth()
            .tvFocusable(
                focusRequester = focusRequester,
                enabled = focusEnabled,
                onFocused = {
                    focused = it
                    if (it) onFocused()
                },
            )
            .tvClickable(onClick = onToggle)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(row.enabled)
                onClick { onToggle(); true }
            }
            .focusPill(colors, RowShape)
            .heightIn(min = RowHeight)
            .padding(horizontal = RowPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppThumbnail(row.app)
        Spacer(Modifier.width(RowGap))
        Column(Modifier.weight(1f)) {
            Text(row.app.label, style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content)
            Text(programCountLabel(programCount), style = TextStyles.RowSecondary, color = colors.secondary)
        }
        AppleSwitch(checked = row.enabled, tag = "source-switch-${row.app.packageName}")
    }
}

@Composable
internal fun HiddenContent(
    rows: List<HiddenRow>,
    listState: LazyListState,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    onToggle: (String) -> Unit,
    onUnhideAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) {
        Box(modifier.testTag("hidden-pane").fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.hidden_empty),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.testTag("hidden-empty"),
            )
        }
        return
    }
    val packages = remember(rows) { rows.map { it.app.packageName } }
    val rowFocus = remember { TileFocus(null) }
    var unhideAllFocused by remember { mutableStateOf(false) }
    val refocusActive by rememberUpdatedState(focusEnabled && !unhideAllFocused)
    LaunchedEffect(packages) {
        val target = rowFocus.refocus(packages, refocusActive) ?: return@LaunchedEffect
        withFrameNanos { }
        rowFocus.requesterFor(target).tryRequestFocus()
    }
    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("hidden-pane")
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        item {
            Text(stringResource(R.string.settings_category_hidden), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.hidden_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(20.dp))
        }
        item {
            SettingsEntryButton(
                label = stringResource(R.string.hidden_unhide_all),
                focusEnabled = focusEnabled,
                focusRequester = null,
                onClick = onUnhideAll,
                onFocused = { unhideAllFocused = it },
                modifier = Modifier.testTag("unhide-all"),
            )
            Spacer(Modifier.height(20.dp))
        }
        itemsIndexed(rows, key = { _, row -> row.app.packageName }) { index, row ->
            val packageName = row.app.packageName
            HiddenRowLine(
                row = row,
                focusEnabled = focusEnabled,
                focusRequester = rowFocus.requesterFor(packageName),
                onFocused = { rowFocus.onFocused(packageName, index) },
                onToggle = { onToggle(packageName) },
                modifier = if (index == 0) Modifier.focusRequester(contentFocus) else Modifier,
            )
            Spacer(Modifier.height(RowSpacing))
        }
    }
}

@Composable
private fun HiddenRowLine(
    row: HiddenRow,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused)
    val state = stringResource(if (row.hidden) R.string.hidden_state_hidden else R.string.hidden_state_visible)
    Row(
        modifier
            .testTag("hidden-row-${row.app.packageName}")
            .fillMaxWidth()
            .tvFocusable(
                focusRequester = focusRequester,
                enabled = focusEnabled,
                onFocused = {
                    focused = it
                    if (it) onFocused()
                },
            )
            .tvClickable(onClick = onToggle)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(row.hidden)
                stateDescription = state
                onClick { onToggle(); true }
            }
            .focusPill(colors, RowShape)
            .heightIn(min = RowHeight)
            .padding(horizontal = RowPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppThumbnail(row.app)
        Spacer(Modifier.width(RowGap))
        Text(
            row.app.label,
            style = if (focused) TextStyles.RowFocused else TextStyles.Row,
            color = colors.content,
            modifier = Modifier.weight(1f),
        )
        Text(state, style = TextStyles.RowState, color = colors.tertiary, modifier = Modifier.clearAndSetSemantics { })
        Spacer(Modifier.width(RowGap))
        AppleSwitch(checked = row.hidden, tag = "hidden-switch-${row.app.packageName}")
    }
}

@Composable
internal fun SettingsEntryButton(
    label: String,
    focusEnabled: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onFocused: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused, rest = SygixColors.PillRest)
    Box(
        modifier
            .fillMaxWidth()
            .tvFocusable(
                focusRequester = focusRequester,
                enabled = focusEnabled,
                onFocused = {
                    focused = it
                    onFocused(it)
                },
            )
            .tvClickable(onClick = onClick)
            .focusPill(colors, RowShape)
            .height(EntryButtonHeight)
            .padding(horizontal = RowPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content)
    }
}

@Composable
internal fun AppThumbnail(app: TvApp, modifier: Modifier = Modifier) {
    val artwork by rememberAppArtwork(app)
    val banner = artwork?.takeIf { it.isBanner }
    Box(
        modifier
            .size(ThumbWidth, ThumbHeight)
            .clip(RoundedCornerShape(ThumbCorner))
            .background(Color.White.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        if (banner != null) {
            Image(
                bitmap = banner.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().testTag("app-banner-${app.packageName}"),
            )
        } else {
            AppIcon(app.packageName)
        }
    }
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val provided = LocalAppIcons.current
    val icons = remember(provided) { provided ?: AppIconCache.forPackageManager(context.packageManager) }
    val icon by produceState(initialValue = icons.cached(packageName), packageName, icons) {
        if (value == null) value = withContext(Dispatchers.IO) { icons.get(packageName) }
    }
    val bitmap = icon ?: return
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxHeight(IconHeightFraction).aspectRatio(1f).testTag("app-icon-$packageName"),
    )
}
