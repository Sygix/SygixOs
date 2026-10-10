/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.FakeUpdateController
import fr.sygix.sygixos.data.UpdateController
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows

internal class SettingsHarness(
    private val counts: Flow<Map<String, Int>> = flowOf(emptyMap()),
    val updates: UpdateController = FakeUpdateController(),
) {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    private val store = ViewModelStore()
    var now = 1_000L
    val prefs = LauncherPrefs(context) { now }
    val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)
    val toggled = mutableListOf<String>()
    var unhideAllCalls = 0
    var backs = 0
    var open by mutableStateOf(false)
        private set

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
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(
                apps = repo,
                programCounts = counts,
                pm = context.packageManager,
                selfPackage = context.packageName,
                updates = updates,
            ) as T
        }
        ViewModelProvider(store, factory)[SettingsViewModel::class.java]
    }

    fun packageOf(label: String) = "com.${label.lowercase()}"

    fun install(vararg labels: String) = runBlocking {
        val pm = Shadows.shadowOf(context.packageManager)
        labels.forEach { label ->
            val pkg = packageOf(label)
            pm.addResolveInfoForIntent(
                leanback,
                ResolveInfo().apply {
                    nonLocalizedLabel = label
                    activityInfo = ActivityInfo().apply {
                        packageName = pkg
                        name = "$pkg.Main"
                        applicationInfo = ApplicationInfo().apply { packageName = pkg }
                    }
                },
            )
        }
        repo.refreshApps()
    }

    fun uninstall(vararg labels: String) = runBlocking {
        val pm = Shadows.shadowOf(context.packageManager)
        labels.forEach { pm.removeResolveInfosForIntent(leanback, packageOf(it)) }
        repo.refreshApps()
    }

    fun hideInOrder(vararg labels: String) = runBlocking {
        labels.forEach {
            now += 1_000
            repo.hideApp(packageOf(it))
        }
    }

    fun hiddenNow(): Set<String> = runBlocking { prefs.hidden.first() }

    fun openSettings() {
        viewModel.enterCategory(SettingsCategory.Initial)
        open = true
    }

    fun closeSettings() {
        open = false
    }

    fun clear() = store.clear()

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
                onToggleHidden = { toggled += it; viewModel.toggleHidden(it) },
                onUnhideAll = { unhideAllCalls++; viewModel.unhideAll() },
                onCategoryEntered = viewModel::enterCategory,
                update = viewModel.updateActions,
                onBack = { backs++; open = false },
                homeScreenActions = viewModel.homeScreenActions,
            )
        }
    }
}

internal fun ComposeContentTestRule.showSettings(harness: SettingsHarness) {
    harness.openSettings()
    setContent { harness.Content() }
    waitForIdle()
}

internal fun ComposeContentTestRule.waitForToggle(tag: String, on: Boolean) = waitUntil(5_000) {
    onAllNodesWithTag(tag).fetchSemanticsNodes().singleOrNull()
        ?.config?.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState(on)
}
