/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import fr.sygix.sygixos.core.designsystem.SygixTypography
import fr.sygix.sygixos.domain.HiddenGroup
import fr.sygix.sygixos.domain.HiddenPaneLayout
import fr.sygix.sygixos.domain.UpNextArtworkQuality
import fr.sygix.sygixos.domain.UpNextContentKey
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.ui.home.UpNextGlyph
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
import fr.sygix.sygixos.ui.home.rememberAppArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalAppIcons = staticCompositionLocalOf<AppIconCache?> { null }

private const val LeadingItems = 1

internal val RowShape = RoundedCornerShape(Dimens.SettingsRowCorner)
internal val RowHeight = Dimens.SettingsRowHeight
internal val RowVerticalPadding = 6.dp
internal val RowGap = 12.dp
internal val RowSpacing = 5.dp
private val EntryButtonHeight = 42.dp
private val UnhideAllGap = 20.dp
private const val PlaceholderArea = 0.5f
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
            Spacer(Modifier.height(Dimens.SettingsHeaderGap))
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
    entries: List<HiddenEntry>,
    listState: LazyListState,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    onToggle: (String) -> Unit,
    onUnhideAll: () -> Unit,
    modifier: Modifier = Modifier,
    onRestore: (UpNextContentKey) -> Unit = {},
    onExpand: (HiddenGroup) -> Unit = {},
) {
    if (entries.isEmpty()) {
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
    val focusable = remember(entries) { entries.filter { it != HiddenEntry.ContentsTitle }.map { it.key } }
    val initial = remember(entries) { entries.firstOrNull { it is HiddenEntry.App || it is HiddenEntry.Content }?.key }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(key: String) = requesters.getOrPut(key) { FocusRequester() }
    var focusedKey by remember { mutableStateOf<String?>(null) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    val active by rememberUpdatedState(focusEnabled)
    val target = remember(focusable) { HiddenPaneLayout.refocus(focusable, focusedKey, focusedIndex) }
    LaunchedEffect(focusable) {
        if (!active || target == null) return@LaunchedEffect
        withFrameNanos { }
        requesterFor(target).tryRequestFocus()
    }
    fun focusModifier(entry: HiddenEntry): Modifier = if (entry.key == initial) Modifier.focusRequester(contentFocus) else Modifier
    fun onFocused(entry: HiddenEntry) {
        focusedKey = entry.key
        focusedIndex = focusable.indexOf(entry.key).coerceAtLeast(0)
    }
    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("hidden-pane")
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        item(key = "header") {
            Text(stringResource(R.string.settings_category_hidden), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.hidden_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(Dimens.SettingsHeaderGap))
        }
        items(entries, key = { it.key }) { entry ->
            when (entry) {
                HiddenEntry.UnhideAll -> {
                    SettingsEntryButton(
                        label = stringResource(R.string.hidden_unhide_all),
                        focusEnabled = focusEnabled,
                        focusRequester = requesterFor(entry.key),
                        onClick = onUnhideAll,
                        onFocused = { if (it) onFocused(entry) },
                        modifier = Modifier.testTag("unhide-all"),
                    )
                    Spacer(Modifier.height(UnhideAllGap))
                }
                is HiddenEntry.App -> {
                    val packageName = entry.row.app.packageName
                    HiddenRowLine(
                        row = entry.row,
                        focusEnabled = focusEnabled,
                        focusRequester = requesterFor(entry.key),
                        onFocused = { onFocused(entry) },
                        onToggle = { onToggle(packageName) },
                        modifier = focusModifier(entry),
                    )
                    Spacer(Modifier.height(RowSpacing))
                }
                is HiddenEntry.More -> {
                    ShowMoreRow(
                        entry = entry,
                        focusEnabled = focusEnabled,
                        focusRequester = requesterFor(entry.key),
                        onFocused = { onFocused(entry) },
                        onClick = { onExpand(entry.group) },
                    )
                    Spacer(Modifier.height(RowSpacing))
                }
                HiddenEntry.ContentsTitle -> Text(
                    stringResource(R.string.hidden_group_upnext),
                    style = SygixTypography.labelMedium,
                    color = SygixColors.OnDarkSecondary,
                    modifier = Modifier
                        .testTag("hidden-group-upnext")
                        .focusProperties { canFocus = false }
                        .padding(start = RowPadding, top = Dimens.SettingsGroupTitleTop, bottom = Dimens.SettingsGroupTitleBottom),
                )
                is HiddenEntry.Content -> {
                    HiddenContentLine(
                        row = entry.row,
                        focusEnabled = focusEnabled,
                        focusRequester = requesterFor(entry.key),
                        onFocused = { onFocused(entry) },
                        onRestore = { onRestore(entry.row.key) },
                        modifier = focusModifier(entry),
                    )
                    Spacer(Modifier.height(RowSpacing))
                }
            }
        }
    }
}

@Composable
private fun ShowMoreRow(
    entry: HiddenEntry.More,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val tag = when (entry.group) {
        HiddenGroup.APPS -> "hidden-show-more-apps"
        HiddenGroup.UPNEXT -> "hidden-show-more-upnext"
    }
    PreferenceRow(
        label = pluralStringResource(R.plurals.hidden_show_more, entry.count, entry.count),
        enabled = focusEnabled,
        onClick = onClick,
        focusRequester = focusRequester,
        onFocused = { if (it) onFocused() },
        modifier = Modifier.testTag(tag),
    ) { colors ->
        Chevron(up = false, color = colors.tertiary, modifier = Modifier.size(Dimens.SettingsChevron))
    }
}

@Composable
internal fun HiddenContentLine(
    row: HiddenContentRow,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused)
    val key = row.key.serialized
    val type = stringResource(
        if (row.key.type == UpNextContentType.EPISODE) R.string.hidden_content_series else R.string.hidden_content_movie,
    )
    val detail = if (row.apps.isEmpty()) {
        type
    } else {
        type + stringResource(R.string.hero_details_separator) + row.apps.joinToString(stringResource(R.string.hidden_content_apps_separator))
    }
    Row(
        modifier
            .testTag("hidden-content-row-$key")
            .fillMaxWidth()
            .tvFocusable(
                focusRequester = focusRequester,
                enabled = focusEnabled,
                onFocused = {
                    focused = it
                    if (it) onFocused()
                },
            )
            .tvClickable(onClick = onRestore)
            .focusPill(colors, RowShape)
            .heightIn(min = RowHeight)
            .padding(horizontal = RowPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentThumbnail(row.imageUrl, Modifier.testTag("hidden-content-thumbnail-$key"))
        Spacer(Modifier.width(RowGap))
        Column(Modifier.weight(1f)) {
            Text(row.title, style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = TextStyles.RowSecondary, color = colors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (focused) {
            Spacer(Modifier.width(RowGap))
            Text(
                stringResource(R.string.hidden_content_restore),
                style = TextStyles.RowState,
                color = colors.tertiary,
                modifier = Modifier.testTag("hidden-content-action-$key"),
            )
        }
    }
}

@Composable
private fun ContentThumbnail(imageUrl: String?, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .size(Dimens.SettingsThumbWidth, Dimens.SettingsThumbHeight)
            .clip(RoundedCornerShape(Dimens.SettingsThumbCorner))
            .background(SygixColors.TileBackground),
    ) {
        val painter = rememberAsyncImagePainter(imageUrl, contentScale = ContentScale.Crop)
        if (painter.state is AsyncImagePainter.State.Success) {
            Image(painter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            val icon = UpNextArtworkQuality.placeholderIcon(Dimens.UpNextPlaceholderIcon.value, maxHeight.value).dp
            Box(Modifier.fillMaxWidth().fillMaxHeight(PlaceholderArea).align(Alignment.TopCenter), contentAlignment = Alignment.Center) {
                UpNextGlyph(false, SygixColors.PlaceholderGlyph, Modifier.size(icon))
            }
        }
    }
}

@Composable
internal fun HiddenRowLine(
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
            .testTag("app-thumbnail-${app.packageName}")
            .size(Dimens.SettingsThumbWidth, Dimens.SettingsThumbHeight)
            .clip(RoundedCornerShape(Dimens.SettingsThumbCorner))
            .background(Color.White.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        if (banner != null) {
            Image(
                bitmap = banner.image,
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
