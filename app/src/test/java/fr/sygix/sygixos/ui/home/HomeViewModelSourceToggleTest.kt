/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import fr.sygix.sygixos.data.FakeUpdateController
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ComponentRegistry
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import fr.sygix.sygixos.MainDispatcherRule
import fr.sygix.sygixos.await
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.HeroRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.data.ClockSource
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class StaticFeedProvider(private val items: List<HeroItem>) : HeroContentProvider {
    override suspend fun load(): List<HeroItem> = items
}

private class OfflineImageLoader : ImageLoader {
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult = ErrorResult(null, request, IllegalStateException("offline"))
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

@RunWith(RobolectricTestRunner::class)
class HomeViewModelSourceToggleTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

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

    private val natureClip = HeroItem(id = "nature-0", title = "", videoUrl = "https://x/clip.mp4")

    private fun buildViewModel(items: List<HeroItem> = feedItems): Pair<HomeViewModel, AppCatalogRepository> {
        val prefs = LauncherPrefs(context)
        val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)
        val vm = HomeViewModel(
            apps = repo,
            hero = HeroRepository(StaticFeedProvider(items), StaticFeedProvider(listOf(natureClip))),
            artwork = AppArtworkSource(context.packageManager),
            validator = VisualValidator(context, OfflineImageLoader()),
            clockSource = object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            },
            updates = FakeUpdateController(),
        )
        return vm to repo
    }

    private suspend fun awaitTitles(vm: HomeViewModel, expected: List<String>): List<String> =
        vm.state.await { (it as? HomeState.Ready)?.hero?.items?.map { item -> item.title } == expected }
            .let { (it as HomeState.Ready).hero.items.map { item -> item.title } }

    @Test
    fun `disabling a source filters its hero items reactively`() = runBlocking {
        val (vm, repo) = buildViewModel()
        repo.refreshApps()
        vm.refreshHero()
        // Le héro est alimenté par les apps sources : les deux items sont là.
        assertEquals(listOf("From A", "From B"), awaitTitles(vm, listOf("From A", "From B")))

        // Bascule réactive sans redémarrage : désactiver com.a retire « From A ».
        repo.setSourceEnabled("com.a", false)
        assertEquals(listOf("From B"), awaitTitles(vm, listOf("From B")))

        repo.setSourceEnabled("com.a", true)
        assertEquals(listOf("From A", "From B"), awaitTitles(vm, listOf("From A", "From B")))
    }

    @Test
    fun `disabling every source falls back to the repository fallback instead of an empty hero`() = runBlocking {
        val (vm, repo) = buildViewModel()
        repo.refreshApps()
        vm.refreshHero()
        awaitTitles(vm, listOf("From A", "From B"))
        repo.setSourceEnabled("com.a", false)
        repo.setSourceEnabled("com.b", false)
        val fallback = vm.state.await { (it as? HomeState.Ready)?.hero?.fromApps == false } as HomeState.Ready
        assertFalse(fallback.hero.fromApps)
        assertEquals(listOf("nature-0"), fallback.hero.items.map { it.id })
    }

    @Test
    fun `re-enabling a source re-prepares the shelf of the focused app on the recomputed hero`() = runBlocking {
        val items = (1..25).map { heroItem("b$it", "B$it", imageUrl = "https://x/b$it.jpg", sourcePackage = "com.b") } +
            heroItem("a", "A", imageUrl = "https://x/a.jpg", sourcePackage = "com.a")
        LauncherPrefs(context).setDisabledSources(setOf("com.a"))
        val (vm, repo) = buildViewModel(items)
        repo.refreshApps()
        vm.refreshHero()
        vm.state.await { (it as? HomeState.Ready)?.hero?.items?.size == 25 }

        vm.prepareShelf("com.a")
        repo.setSourceEnabled("com.a", true)
        vm.state.await { (it as? HomeState.Ready)?.hero?.items?.size == 26 }

        vm.state.await { "https://x/a.jpg" in ((it as? HomeState.Ready)?.hero?.checked ?: emptySet()) }
        Unit
    }
}
