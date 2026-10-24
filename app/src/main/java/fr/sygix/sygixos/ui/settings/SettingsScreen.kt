/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import fr.sygix.sygixos.core.designsystem.Badge
import fr.sygix.sygixos.core.designsystem.BadgePlacement
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tvFocusable
import fr.sygix.sygixos.ui.hero.AmbientGradient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.domain.AppLanguage
import fr.sygix.sygixos.domain.HiddenGroup
import fr.sygix.sygixos.domain.UpNextContentKey

enum class SettingsPane { CATEGORIES, CONTENT }

enum class SettingsCategory(@StringRes val label: Int) {
    SOURCES(R.string.settings_category_sources),
    HIDDEN(R.string.settings_category_hidden),
    HOME_SCREEN(R.string.settings_category_home_screen),
    TRACKING(R.string.tracking_title),
    LANGUAGE(R.string.settings_category_language),
    ABOUT(R.string.settings_category_about),
    ;

    companion object {
        val Initial: SettingsCategory get() = entries.first()
    }
}

class SettingsActions(
    val onToggleSource: (String) -> Unit = {},
    val onToggleHidden: (String) -> Unit = {},
    val onUnhideAll: () -> Unit = {},
    val onRestoreUpNext: (UpNextContentKey) -> Unit = {},
    val onExpandHidden: (HiddenGroup) -> Unit = {},
    val onHiddenFocused: (String) -> Unit = {},
    val onCategoryEntered: (SettingsCategory) -> Unit = {},
    val onSelectLanguage: (AppLanguage?) -> Unit = {},
    val update: UpdateActions = UpdateActions(),
    val homeScreen: HomeScreenActions = HomeScreenActions(),
)

@Composable
fun SettingsScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
    counts: State<Map<String, Int>?> = remember { mutableStateOf(emptyMap<String, Int>()) },
    actions: SettingsActions = SettingsActions(),
    onBack: () -> Unit = {},
    homeHandlesBack: Boolean = true,
    homeScreenRows: @Composable ColumnScope.(Boolean) -> Unit = {},
    trackingContent: @Composable (Boolean, FocusRequester) -> Unit = { _, _ -> },
) {
    var categoryIndex by rememberSaveable { mutableIntStateOf(SettingsCategory.Initial.ordinal) }
    var pane by rememberSaveable { mutableStateOf(SettingsPane.CATEGORIES) }
    val categoryFocusers = remember { SettingsCategory.entries.map { FocusRequester() } }
    val contentFocus = remember { FocusRequester() }
    val contentListState = remember(categoryIndex) { LazyListState() }
    val category = SettingsCategory.entries[categoryIndex]

    fun enterContent() {
        if (state.canEnter(SettingsCategory.entries[categoryIndex])) pane = SettingsPane.CONTENT
    }

    fun selectCategory(index: Int) {
        if (index == categoryIndex) return
        categoryIndex = index
        actions.onCategoryEntered(SettingsCategory.entries[index])
    }

    val contentAvailable = state.canEnter(category)
    LaunchedEffect(contentAvailable) {
        if (!contentAvailable && pane == SettingsPane.CONTENT) pane = SettingsPane.CATEGORIES
    }
    LaunchedEffect(pane, categoryIndex) {
        when (pane) {
            SettingsPane.CATEGORIES -> categoryFocusers[categoryIndex].tryRequestFocus()
            SettingsPane.CONTENT -> {
                contentListState.scrollToItem(0)
                if (!contentFocus.tryRequestFocus()) pane = SettingsPane.CATEGORIES
            }
        }
    }

    Box(
        modifier
            .testTag("settings-screen")
            .fillMaxSize()
            .zIndex(20f)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionUp, Key.DirectionDown -> if (pane == SettingsPane.CATEGORIES) {
                        val delta = if (e.key == Key.DirectionUp) -1 else 1
                        selectCategory((categoryIndex + delta).coerceIn(0, SettingsCategory.entries.lastIndex))
                        true
                    } else {
                        false
                    }
                    Key.DirectionRight -> if (pane == SettingsPane.CATEGORIES) { enterContent(); true } else false
                    Key.DirectionLeft -> if (pane == SettingsPane.CONTENT) { pane = SettingsPane.CATEGORIES; true } else false
                    Key.Back -> if (homeHandlesBack) { onBack(); true } else false
                    else -> false
                }
            },
    ) {
        AmbientGradient(Modifier.matchParentSize())
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(CategoryPaneWidth)
                    .fillMaxHeight()
                    .padding(start = Dimens.ScreenMarginH, top = PaneTop, end = PaneGap),
                verticalArrangement = Arrangement.spacedBy(CategoryGap),
            ) {
                Text(
                    stringResource(R.string.settings_title),
                    style = TextStyles.SettingsTitle,
                    color = SygixColors.OnDark,
                    modifier = Modifier.padding(bottom = TitleGap),
                )
                SettingsCategory.entries.forEachIndexed { index, entry ->
                    SettingsCategoryRow(
                        label = stringResource(entry.label),
                        badgeTag = if (entry == SettingsCategory.ABOUT && state.update.badge) "update-badge-about" else null,
                        selected = index == categoryIndex,
                        focusEnabled = pane == SettingsPane.CATEGORIES,
                        focusRequester = categoryFocusers[index],
                        onClick = {
                            selectCategory(index)
                            enterContent()
                        },
                        modifier = Modifier.testTag("settings-category-${entry.name}"),
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight().padding(start = PaneGap, end = ContentEnd, top = PaneTop)) {
                when (category) {
                    SettingsCategory.TRACKING -> trackingContent(pane == SettingsPane.CONTENT, contentFocus)
                    SettingsCategory.SOURCES -> SourcesContent(
                        rows = state.sources,
                        counts = counts,
                        listState = contentListState,
                        focusEnabled = pane == SettingsPane.CONTENT,
                        contentFocus = contentFocus,
                        onToggle = actions.onToggleSource,
                    )
                    SettingsCategory.HIDDEN -> HiddenContent(
                        entries = state.hiddenEntries,
                        listState = contentListState,
                        focusEnabled = pane == SettingsPane.CONTENT,
                        contentFocus = contentFocus,
                        onToggle = actions.onToggleHidden,
                        onUnhideAll = actions.onUnhideAll,
                        onRestore = actions.onRestoreUpNext,
                        onExpand = actions.onExpandHidden,
                        initialFocus = state.hiddenInitialFocus,
                        refocus = state.hiddenRefocus,
                        onFocused = actions.onHiddenFocused,
                    )
                    SettingsCategory.ABOUT -> AboutContent(
                        version = state.version,
                        update = state.update,
                        actions = actions.update,
                        listState = contentListState,
                        focusEnabled = pane == SettingsPane.CONTENT,
                        contentFocus = contentFocus,
                    )
                    SettingsCategory.LANGUAGE -> LanguageContent(
                        state = state.language,
                        focusEnabled = pane == SettingsPane.CONTENT,
                        contentFocus = contentFocus,
                        onSelect = actions.onSelectLanguage,
                    )
                    SettingsCategory.HOME_SCREEN -> HomeScreenContent(
                        visible = state.upNextVisible,
                        position = state.upNextPosition,
                        visualMotion = state.visualMotion,
                        actions = actions.homeScreen,
                        focusEnabled = pane == SettingsPane.CONTENT,
                        contentFocus = contentFocus,
                        additionalRows = homeScreenRows,
                    )
                }
            }
        }
    }
}
@Composable
private fun SettingsCategoryRow(
    label: String,
    badgeTag: String?,
    selected: Boolean,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused, selected, restContent = SygixColors.OnDarkRest)
    Box(
        modifier
            .fillMaxWidth()
            .tvFocusable(
                focusRequester = focusRequester,
                enabled = focusEnabled,
                onFocused = { focused = it },
            )
            .tvClickable(onClick = onClick)
            .focusPill(colors, RoundedCornerShape(Dimens.SettingsRowCorner))
            .height(CategoryHeight)
            .padding(horizontal = RowPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            label,
            style = if (focused) TextStyles.RowFocused else TextStyles.Row,
            color = colors.content,
            maxLines = 1,
        )
        if (badgeTag != null) Badge(BadgePlacement.ROW_END, tag = badgeTag)
    }
}

private val CategoryPaneWidth = 340.dp
private val PaneTop = 48.dp
private val PaneGap = 24.dp
private val ContentEnd = 60.dp
private val CategoryGap = 6.dp
private val TitleGap = 14.dp
private val CategoryHeight = 38.dp
internal val RowPadding = Dimens.SettingsRowPadding
