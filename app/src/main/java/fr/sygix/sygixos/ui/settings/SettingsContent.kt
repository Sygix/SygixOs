/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import fr.sygix.sygixos.core.designsystem.SygixTypography
import fr.sygix.sygixos.domain.HiddenGroup
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.FocusTargets
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.home.rememberAppArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalAppIcons = staticCompositionLocalOf<AppIconCache?> { null }

private const val HeaderItems = 1

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

internal suspend fun FocusTargets.revealRow(list: LazyListState, key: String, position: Int): Boolean =
    position >= 0 && reveal(key) {
        val index = position + HeaderItems
        if (list.layoutInfo.visibleItemsInfo.none { it.index == index }) list.scrollToItem(index)
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
    val focus = remember { FocusTargets() }
    LaunchedEffect(order) {
        val target = focusedPackage ?: return@LaunchedEffect
        if (focusEnabled) focus.revealRow(listState, target, order.indexOf(target))
    }
    LazyColumn(state = listState, modifier = modifier.testTag("settings-sources").fillMaxWidth().fillMaxHeight()) {
        item { SettingsHeader(R.string.settings_category_sources, R.string.sources_description) }
        items(rows, key = { it.app.packageName }) { row ->
            SourceRowLine(
                row = row,
                counts = counts,
                focusEnabled = focusEnabled,
                focusRequester = focus.bind(row.app.packageName, contentFocus.takeIf { rows.firstOrNull()?.app?.packageName == row.app.packageName }),
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
    PreferenceRow(
        label = row.app.label,
        enabled = focusEnabled,
        onClick = onToggle,
        modifier = Modifier.testTag("source-row-${row.app.packageName}").switchSemantics(row.enabled, onToggle),
        focusRequester = focusRequester,
        detail = programCountLabel(programCount),
        onFocused = { if (it) onFocused() },
        leading = { AppThumbnail(row.app); Spacer(Modifier.width(RowGap)) },
    ) {
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
    initialFocus: String? = null,
    refocus: HiddenRefocus? = null,
    onFocused: (String) -> Unit = {},
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
    val focus = remember { FocusTargets() }
    val focused by rememberUpdatedState(onFocused)
    LaunchedEffect(refocus) {
        val target = refocus?.key ?: return@LaunchedEffect
        if (focusEnabled) focus.revealRow(listState, target, entries.indexOfFirst { it.key == target })
    }
    fun onFocused(entry: HiddenEntry) = focused(entry.key)
    LazyColumn(state = listState, modifier = modifier.testTag("hidden-pane").fillMaxWidth().fillMaxHeight()) {
        item(key = "header") { SettingsHeader(R.string.settings_category_hidden, R.string.hidden_description) }
        items(entries, key = { it.key }) { entry ->
            val requester = focus.bind(entry.key, contentFocus.takeIf { entry.key == initialFocus })
            when (entry) {
                HiddenEntry.UnhideAll -> {
                    SettingsEntryButton(
                        label = stringResource(R.string.hidden_unhide_all),
                        focusEnabled = focusEnabled,
                        focusRequester = requester,
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
                        focusRequester = requester,
                        onFocused = { onFocused(entry) },
                        onToggle = { onToggle(packageName) },
                    )
                    Spacer(Modifier.height(RowSpacing))
                }
                is HiddenEntry.More -> {
                    ShowMoreRow(
                        entry = entry,
                        focusEnabled = focusEnabled,
                        focusRequester = requester,
                        onFocused = { onFocused(entry) },
                        onClick = { onExpand(entry.group) },
                    )
                    Spacer(Modifier.height(RowSpacing))
                }
                HiddenEntry.ContentsTitle -> SettingsGroupTitle(stringResource(R.string.hidden_group_upnext), "hidden-group-upnext")
                is HiddenEntry.Content -> {
                    HiddenContentLine(
                        row = entry.row,
                        focusEnabled = focusEnabled,
                        focusRequester = requester,
                        onFocused = { onFocused(entry) },
                        onRestore = { onRestore(entry.row.key) },
                    )
                    Spacer(Modifier.height(RowSpacing))
                }
            }
        }
    }
}

@Composable
internal fun SettingsHeader(@StringRes title: Int, @StringRes description: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, color = SygixColors.OnDark)
    Spacer(Modifier.height(12.dp))
    Text(stringResource(description), style = MaterialTheme.typography.bodyMedium, color = SygixColors.OnDarkSecondary)
    Spacer(Modifier.height(Dimens.SettingsHeaderGap))
}

@Composable
internal fun SettingsGroupTitle(text: String, tag: String) {
    Text(
        text,
        style = SygixTypography.labelMedium,
        color = SygixColors.OnDarkSecondary,
        modifier = Modifier
            .testTag(tag)
            .focusProperties { canFocus = false }
            .padding(start = RowPadding, top = Dimens.SettingsGroupTitleTop, bottom = Dimens.SettingsGroupTitleBottom),
    )
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
        ContentThumbnail(row.imageUrl, "hidden-content-thumbnail-$key", row.sourcePackage)
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
private fun ContentThumbnail(imageUrl: String?, tag: String, sourcePackage: String?) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val revision = fr.sygix.sygixos.ui.home.artworkRevision()
    val request = remember(context, density, imageUrl, sourcePackage, revision) { with(density) {
        val source = sourcePackage?.let { fr.sygix.sygixos.model.ProgramArtwork(it, listOfNotNull(imageUrl), "removed:$imageUrl") }
        ImageRequest.Builder(context).data(source ?: imageUrl).setParameter("artwork-revision", revision)
            .size(Dimens.SettingsThumbWidth.roundToPx(), Dimens.SettingsThumbHeight.roundToPx()).build()
    } }
    val painter = rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
    Box(
        Modifier
            .testTag(tag)
            .size(Dimens.SettingsThumbWidth, Dimens.SettingsThumbHeight)
            .clip(RoundedCornerShape(Dimens.SettingsThumbCorner))
            .background(SygixColors.TileBackground),
    ) {
        if (painter.state is AsyncImagePainter.State.Success) {
            Image(painter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            val icon = UpNextArtworkQuality.placeholderIcon(Dimens.UpNextPlaceholderIcon.value, Dimens.SettingsThumbHeight.value).dp
            Box(Modifier.fillMaxWidth().fillMaxHeight(PlaceholderArea).align(Alignment.TopCenter).testTag("$tag-placeholder"), contentAlignment = Alignment.Center) {
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
) {
    val state = stringResource(if (row.hidden) R.string.hidden_state_hidden else R.string.hidden_state_visible)
    PreferenceRow(
        label = row.app.label,
        enabled = focusEnabled,
        onClick = onToggle,
        modifier = Modifier.testTag("hidden-row-${row.app.packageName}").switchSemantics(row.hidden, onToggle, state),
        focusRequester = focusRequester,
        onFocused = { if (it) onFocused() },
        leading = { AppThumbnail(row.app); Spacer(Modifier.width(RowGap)) },
    ) { colors ->
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
