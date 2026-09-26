/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.sygix.sygixos.core.designsystem.tvFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester

// Sous-écran plein écran des applications cachées : réactivation par ligne ou globale.
@Composable
fun HiddenAppsScreen(
    hiddenApps: List<fr.sygix.sygixos.model.TvApp>,
    onUnhide: (String) -> Unit,
    onUnhideAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    var pendingFocusIndex by remember { mutableStateOf<Int?>(0) }
    LaunchedEffect(hiddenApps) {
        val index = pendingFocusIndex ?: return@LaunchedEffect
        pendingFocusIndex = null
        if (hiddenApps.isEmpty()) return@LaunchedEffect
        val target = hiddenApps[index.coerceIn(0, hiddenApps.lastIndex)]
        withFrameNanos { }
        rowFocus.getValue(target.packageName).tryRequestFocus()
    }
    Box(
        modifier
            .testTag("hidden-apps-screen")
            .fillMaxSize()
            .zIndex(22f)
            .background(Color(0xFF101014)),
    ) {
        if (hiddenApps.isEmpty()) {
            // État vide navigable : un nœud focalisable neutre garde le focus dans l'arbre,
            // les touches (dont Retour) restent donc délivrées au sous-écran.
            val emptyFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { emptyFocus.tryRequestFocus() }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aucune application cachée",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.testTag("hidden-empty"),
                )
            }
            Box(
                Modifier
                    .size(1.dp)
                    .focusRequester(emptyFocus)
                    .focusable()
                    .testTag("hidden-empty-focus"),
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 64.dp, vertical = 64.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Applications cachées", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(24.dp))
                hiddenApps.forEachIndexed { index, app ->
                    key(app.packageName) {
                    var focused by remember { mutableStateOf(false) }
                    Row(
                        Modifier
                            .testTag("hidden-row-${app.packageName}")
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (focused) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
                            .tvFocus(
                                focusRequester = rowFocus.getOrPut(app.packageName) { FocusRequester() },
                                onFocused = { focused = it },
                            )
                            .tvClickable(onClick = { pendingFocusIndex = index; onUnhide(app.packageName) })
                            .semantics(mergeDescendants = true) {
                                role = Role.Switch
                                toggleableState = ToggleableState.On
                                stateDescription = "Cachée"
                                onClick { onUnhide(app.packageName); true }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.packageName)
                        Spacer(Modifier.width(18.dp))
                        Text(
                            app.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        AppleSwitch(checked = true, tag = "hidden-switch-${app.packageName}")
                    }
                    Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
                var unhideAllFocused by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .testTag("unhide-all")
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (unhideAllFocused) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.14f))
                        .tvFocus(onFocused = { unhideAllFocused = it })
                        .tvClickable(onClick = onUnhideAll)
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Text("Tout réactiver", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}
