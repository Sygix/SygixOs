/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tvFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tryRequestFocus

enum class SettingsPane { CATEGORIES, CONTENT }

internal enum class SettingsCategory(val label: String) {
    SOURCES("Apps sources"),
    HIDDEN("Applications cachées"),
    ABOUT("À propos"),
}

// Page réglages tvOS : volet catégories à gauche, contenu à droite, DPAD uniquement.
@Composable
fun SettingsScreen(
    state: SettingsState,
    counts: State<Map<String, Int>> = remember { mutableStateOf(emptyMap<String, Int>()) },
    onToggleSource: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onUnhideAll: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var pane by rememberSaveable { mutableStateOf(SettingsPane.CATEGORIES) }
    var hiddenSubScreen by rememberSaveable { mutableStateOf(false) }
    val categoryFocusers = remember { SettingsCategory.entries.map { FocusRequester() } }
    val contentFocus = remember { FocusRequester() }
    val contentListState = remember(categoryIndex) { LazyListState() }
    val category = SettingsCategory.entries[categoryIndex]

    fun enterContent() {
        val target = SettingsCategory.entries[categoryIndex]
        if (target == SettingsCategory.SOURCES && state.sources.isEmpty()) return
        pane = SettingsPane.CONTENT
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
    // Retour du sous-écran : le focus repart sur la catégorie active.
    LaunchedEffect(hiddenSubScreen) {
        if (!hiddenSubScreen) categoryFocusers[categoryIndex].tryRequestFocus()
    }

    val backdrop = Color(0xFF101014)
    Box(
        modifier
            .testTag("settings-screen")
            .fillMaxSize()
            .zIndex(20f)
            .background(backdrop)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (hiddenSubScreen) {
                    return@onPreviewKeyEvent if (e.key == Key.Back) {
                        // Retour du sous-écran : on repart du volet catégories (les lignes
                        // redeviennent focalisables et reprennent le focus).
                        hiddenSubScreen = false
                        pane = SettingsPane.CATEGORIES
                        true
                    } else false
                }
                when (e.key) {
                    Key.DirectionUp, Key.DirectionDown -> if (pane == SettingsPane.CATEGORIES) {
                        val delta = if (e.key == Key.DirectionUp) -1 else 1
                        categoryIndex = (categoryIndex + delta).coerceIn(0, SettingsCategory.entries.lastIndex)
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
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(420.dp)
                    .fillMaxHeight()
                    .padding(start = 64.dp, top = 64.dp),
            ) {
                Text("Réglages", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(32.dp))
                SettingsCategory.entries.forEachIndexed { index, entry ->
                    SettingsCategoryRow(
                        label = entry.label,
                        // État « sélectionné » visible même quand le focus est dans le volet droit.
                        selected = index == categoryIndex,
                        focusEnabled = pane == SettingsPane.CATEGORIES && !hiddenSubScreen,
                        focusRequester = categoryFocusers[index],
                        onClick = {
                            categoryIndex = index
                            enterContent()
                        },
                        modifier = Modifier.testTag("settings-category-${entry.name}"),
                    )
                    // Espacement constant : la sélection ne décale plus la mise en page.
                    if (index < SettingsCategory.entries.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight().padding(end = 64.dp, top = 64.dp)) {
                when (category) {
                    SettingsCategory.SOURCES -> SourcesContent(
                        rows = state.sources,
                        counts = counts,
                        listState = contentListState,
                        focusEnabled = pane == SettingsPane.CONTENT && !hiddenSubScreen,
                        contentFocus = contentFocus,
                        onToggle = onToggleSource,
                    )
                    SettingsCategory.HIDDEN -> HiddenCategoryContent(
                        hiddenApps = state.hiddenApps,
                        focusEnabled = pane == SettingsPane.CONTENT && !hiddenSubScreen,
                        contentFocus = contentFocus,
                        onOpen = { hiddenSubScreen = true },
                    )
                    SettingsCategory.ABOUT -> AboutContent(
                        version = state.version,
                        listState = contentListState,
                        focusEnabled = pane == SettingsPane.CONTENT && !hiddenSubScreen,
                        contentFocus = contentFocus,
                    )
                }
            }
        }
        if (hiddenSubScreen) {
            HiddenAppsScreen(
                hiddenApps = state.hiddenApps,
                onUnhide = onUnhide,
                onUnhideAll = onUnhideAll,
                modifier = Modifier.zIndex(2f),
            )
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
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, tween(Motion.FOCUS_MS, easing = AppleEasing), label = "categoryScale")
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .tvFocus(
                focusRequester = focusRequester,
                enabled = focusEnabled,
                onFocused = { focused = it },
            )
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.14f)
                    // Catégorie active : visible même quand le focus est à droite.
                    selected -> Color.White.copy(alpha = 0.08f)
                    else -> Color.Transparent
                },
            )
            .tvClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                focused || selected -> Color.White
                else -> Color.White.copy(alpha = 0.75f)
            },
        )
    }
}
