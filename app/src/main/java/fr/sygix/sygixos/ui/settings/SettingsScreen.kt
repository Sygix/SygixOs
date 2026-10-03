/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tvFocusable
import fr.sygix.sygixos.ui.hero.AmbientGradient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

enum class SettingsPane { CATEGORIES, CONTENT }

enum class SettingsCategory(@StringRes val label: Int) {
    SOURCES(R.string.settings_category_sources),
    HIDDEN(R.string.settings_category_hidden),
    ABOUT(R.string.settings_category_about),
    ;

    companion object {
        val Initial: SettingsCategory get() = entries.first()
    }
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    counts: State<Map<String, Int>?> = remember { mutableStateOf(emptyMap<String, Int>()) },
    onToggleSource: (String) -> Unit,
    onToggleHidden: (String) -> Unit,
    onUnhideAll: () -> Unit,
    onCategoryEntered: (SettingsCategory) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
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
        onCategoryEntered(SettingsCategory.entries[index])
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
                    Key.Back -> { onBack(); true }
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
                    SettingsCategory.SOURCES -> SourcesContent(
                        rows = state.sources,
                        counts = counts,
                        listState = contentListState,
                        focusEnabled = pane == SettingsPane.CONTENT,
                        contentFocus = contentFocus,
                        onToggle = onToggleSource,
                    )
                    SettingsCategory.HIDDEN -> HiddenContent(
                        rows = state.hiddenRows,
                        listState = contentListState,
                        focusEnabled = pane == SettingsPane.CONTENT,
                        contentFocus = contentFocus,
                        onToggle = onToggleHidden,
                        onUnhideAll = onUnhideAll,
                    )
                    SettingsCategory.ABOUT -> AboutContent(
                        version = state.version,
                        listState = contentListState,
                        focusEnabled = pane == SettingsPane.CONTENT,
                        contentFocus = contentFocus,
                    )
                }
            }
        }
    }
}
@Composable
private fun SettingsCategoryRow(
    label: String,
    selected: Boolean,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused, selected)
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
    }
}

private val CategoryPaneWidth = 340.dp
private val PaneTop = 48.dp
private val PaneGap = 24.dp
private val ContentEnd = 60.dp
private val CategoryGap = 6.dp
private val TitleGap = 14.dp
private val CategoryHeight = 38.dp
internal val RowPadding = 14.dp
