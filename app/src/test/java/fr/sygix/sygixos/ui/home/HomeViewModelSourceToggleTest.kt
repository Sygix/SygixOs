/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.HeroRepository
import fr.sygix.sygixos.data.NatureFallbackProvider
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

private class StaticFeedProvider(private val items: List<HeroItem>) : HeroContentProvider {
    override suspend fun load(): List<HeroItem> = items
}

@RunWith(RobolectricTestRunner::class)
class HomeViewModelSourceToggleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetDataStore() = runBlocking {
        // Le DataStore est un singleton par process : réinitialiser pour rendre chaque
        // test indépendant des autres classes de tests.
        val prefs = LauncherPrefs(context)
        prefs.setDisabledSources(emptySet())
        prefs.setHidden(emptySet())
        prefs.setPinned(emptySet())
        prefs.setGridOrder(emptyList())
        prefs.setCachedApps(emptyList())
    }

    private val feedItems = listOf(
        heroItem("1", "From A", sourcePackage = "com.a"),
        heroItem("2", "From B", sourcePackage = "com.b"),
    )

    // Le viewModelScope tourne sur Dispatchers.Main (looper Robolectric en pause) :
    // pomper le looper laisse les coroutines du ViewModel se terminer.
    private fun pumpMain() {
        val shadow = shadowOf(Looper.getMainLooper())
        repeat(20) {
            shadow.idle()
            Thread.sleep(10)
        }
    }

    private fun buildViewModel(): Pair<HomeViewModel, AppCatalogRepository> {
        val prefs = LauncherPrefs(context)
        val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)
        // Aucun imageUrl : la validation visuelle ne démarre pas (pas de réseau en test).
        val vm = HomeViewModel(
            apps = repo,
            hero = HeroRepository(StaticFeedProvider(feedItems), NatureFallbackProvider()),
            artwork = AppArtworkSource(context.packageManager),
            validator = VisualValidator(context),
            fallback = StaticFeedProvider(emptyList()),
        )
        return vm to repo
    }

    private fun readyItems(vm: HomeViewModel): List<String> {
        pumpMain()
        return (vm.state.value as? HomeState.Ready)?.hero?.items?.map { it.title } ?: emptyList()
    }

    @Test
    fun `disabling a source filters its hero items reactively`() = runBlocking {
        val (vm, repo) = buildViewModel()
        repo.refreshApps()
        vm.refreshHero()
        // Le héro est alimenté par les apps sources : les deux items sont là.
        assertEquals(listOf("From A", "From B"), readyItems(vm))

        // Bascule réactive sans redémarrage : désactiver com.a retire « From A ».
        repo.setSourceEnabled("com.a", false)
        assertEquals(listOf("From B"), readyItems(vm))

        repo.setSourceEnabled("com.a", true)
        assertEquals(listOf("From A", "From B"), readyItems(vm))
    }

    @Test
    fun `disabling every source falls back to a non-app hero instead of an empty one`() = runBlocking {
        val (vm, repo) = buildViewModel()
        repo.refreshApps()
        vm.refreshHero()
        assertEquals(listOf("From A", "From B"), readyItems(vm))
        repo.setSourceEnabled("com.a", false)
        repo.setSourceEnabled("com.b", false)
        pumpMain()
        val fallback = vm.state.value as? HomeState.Ready ?: error("state not ready")
        // Repli nature : fromApps est false (pas de héro vide).
        assertFalse(fallback.hero.fromApps)
    }
}
