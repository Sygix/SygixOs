/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.core.designsystem.tvFocus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalAppIcons = staticCompositionLocalOf<AppIconCache?> { null }

// Libellés au pluriel correct (chaînes en dur jusqu'à la migration vers strings.xml).
internal fun programCountLabel(count: Int): String = when (count) {
    0 -> "Aucun programme publié"
    1 -> "1 programme publié"
    else -> "$count programmes publiés"
}

internal fun hiddenCountLabel(count: Int): String = when (count) {
    0 -> "Aucune application cachée"
    1 -> "1 application cachée"
    else -> "$count applications cachées"
}

// Catégorie « Apps sources » : une ligne par app TV installée, switch de contribution au héro / Top Shelf.
@Composable
internal fun SourcesContent(
    rows: List<SourceRow>,
    counts: State<Map<String, Int>>,
    listState: LazyListState,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("settings-sources")
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        item {
            Text("Apps sources", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                "Choisissez les applications qui alimentent le héro et le Top Shelf.",
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
                onToggle = { onToggle(row.app.packageName) },
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SourceRowLine(
    row: SourceRow,
    counts: State<Map<String, Int>>,
    focusEnabled: Boolean,
    focusRequester: FocusRequester?,
    onToggle: () -> Unit,
) {
    // Compte lu au niveau de la ligne : seul le rang dont le compte change se recompose.
    val programCount by remember(row.app.packageName) {
        derivedStateOf { counts.value[row.app.packageName] ?: 0 }
    }
    Row(
        Modifier
            .testTag("source-row-${row.app.packageName}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .tvFocus(
                focusRequester = focusRequester,
                enabled = focusEnabled,
                onFocused = { },
            )
            .tvClickable(onClick = onToggle)
            // Accessibilité : un seul nœud (libellé + état + action) ; le switch visuel est
            // piloté par la ligne.
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(row.enabled)
                onClick { onToggle(); true }
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(row.app.packageName)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(row.app.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                programCountLabel(programCount),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
        AppleSwitch(checked = row.enabled, tag = "source-switch-${row.app.packageName}")
    }
}

// Catégorie « Applications cachées » : entrée du volet droit ouvrant le sous-écran.
@Composable
internal fun HiddenCategoryContent(
    hiddenApps: List<fr.sygix.sygixos.model.TvApp>,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().fillMaxHeight()) {
        Text("Applications cachées", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(
            "Les applications cachées n'apparaissent plus dans la grille ni dans le dock.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(20.dp))
        val count = hiddenApps.size
        SettingsEntryButton(
            label = hiddenCountLabel(count),
            focusEnabled = focusEnabled,
            focusRequester = contentFocus,
            onClick = onOpen,
            modifier = Modifier.testTag("open-hidden"),
        )
    }
}

@Composable
internal fun SettingsEntryButton(
    label: String,
    focusEnabled: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
            .tvFocus(focusRequester = focusRequester, enabled = focusEnabled, onFocused = { focused = it })
            .tvClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
internal fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val provided = LocalAppIcons.current
    val icons = remember(provided) { provided ?: AppIconCache.forPackageManager(context.packageManager) }
    // Icône carrée (et non la bannière 16:9 du héro, qui serait rognée) : lue dans le cache
    // pendant la composition, chargée sur IO seulement la première fois.
    val icon by produceState(initialValue = icons.cached(packageName), packageName, icons) {
        if (value == null) value = withContext(Dispatchers.IO) { icons.get(packageName) }
    }
    val bitmap = icon
    Box(
        modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().testTag("app-icon-$packageName"),
            )
        }
    }
}
