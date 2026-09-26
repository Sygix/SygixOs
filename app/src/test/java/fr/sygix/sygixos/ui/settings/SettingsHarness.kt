/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

internal class SettingsHarness(counts: Flow<Map<String, Int>> = flowOf(emptyMap())) {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var now = 1_000L
    val prefs = LauncherPrefs(context) { now }
    private val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)
    val unhid = mutableListOf<String>()
    val hid = mutableListOf<String>()
    var unhideAllCalls = 0
    var backs = 0
    var open by mutableStateOf(true)

    init {
        runBlocking {
            prefs.setDisabledSources(emptySet())
            prefs.setHidden(emptySet())
            prefs.setPinned(emptySet())
            prefs.setGridOrder(emptyList())
            prefs.setCachedApps(emptyList())
        }
    }

    val viewModel: SettingsViewModel by lazy {
        SettingsViewModel(
            apps = repo,
            programCounts = counts,
            pm = context.packageManager,
            selfPackage = context.packageName,
        )
    }

    fun seed(vararg labels: String) = runBlocking {
        prefs.setCachedApps(labels.map { TvApp("com.${it.lowercase()}", it) })
        repo.refreshApps()
    }

    fun hideInOrder(vararg labels: String) = runBlocking {
        labels.forEach {
            now += 1_000
            repo.hideApp("com.${it.lowercase()}")
        }
    }

    fun hiddenNow(): Set<String> = runBlocking { prefs.hidden.first() }

    @Composable
    fun Content() {
        if (!open) return
        val state by viewModel.state.collectAsState()
        val counts = viewModel.counts.collectAsState()
        MaterialTheme {
            SettingsScreen(
                state = state,
                counts = counts,
                onToggleSource = viewModel::toggleSource,
                onHide = { hid += it; viewModel.hide(it) },
                onUnhide = { unhid += it; viewModel.unhide(it) },
                onUnhideAll = { unhideAllCalls++; viewModel.unhideAll() },
                onCategoryEntered = viewModel::enterCategory,
                onBack = { backs++; open = false },
            )
        }
    }
}

internal fun ComposeContentTestRule.waitForToggle(tag: String, on: Boolean) = waitUntil(5_000) {
    onAllNodesWithTag(tag).fetchSemanticsNodes().singleOrNull()
        ?.config?.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState(on)
}
