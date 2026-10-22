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
import fr.sygix.sygixos.testDataStore
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.HeroRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.StartupSession
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.data.ClockSource
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val feedItems = listOf(
        heroItem("1", "From A", sourcePackage = "com.a").copy(videoUrl = "https://example.invalid/a.mp4"),
        heroItem("2", "From B", sourcePackage = "com.b").copy(videoUrl = "https://example.invalid/b.mp4"),
    )

    private val natureClip = HeroItem(id = "nature-0", title = "", videoUrl = "https://x/clip.mp4")

    private fun TestScope.buildViewModel(items: List<HeroItem> = feedItems, prefs: LauncherPrefs = LauncherPrefs(testDataStore())): Pair<HomeViewModel, AppCatalogRepository> {
        val repo = AppCatalogRepository(InstalledAppsSource(context), prefs, mainDispatcher.dispatcher)
        val vm = mainDispatcher.track(HomeViewModel(
            apps = repo,
            hero = HeroRepository(StaticFeedProvider(items), StaticFeedProvider(listOf(natureClip))),
            artwork = AppArtworkSource(context.packageManager, mainDispatcher.dispatcher),
            validator = VisualValidator(context, OfflineImageLoader(), mainDispatcher.dispatcher),
            clockSource = object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            },
            updates = FakeUpdateController(),
            session = StartupSession(),
            motion = FakeMotionSource(),
            mascot = FakeMascotSource(),
            order = object : fr.sygix.sygixos.domain.HeroOrder {
                override var displayedId: String? = null
                override fun arrange(items: List<HeroItem>) = items
            },
        ))
        return vm to repo
    }

    private suspend fun awaitTitles(vm: HomeViewModel, expected: List<String>): List<String> =
        vm.state.await { (it as? HomeState.Ready)?.hero?.items?.map { item -> item.title } == expected }
            .let { (it as HomeState.Ready).hero.items.map { item -> item.title } }

    @Test
    fun `disabling a source filters its hero items reactively`() = runTest(mainDispatcher.dispatcher) {
        val (vm, repo) = buildViewModel()
        repo.refreshApps()
        vm.refreshHero()
        assertEquals(listOf("From A", "From B"), awaitTitles(vm, listOf("From A", "From B")))

        repo.toggleSource("com.a")
        assertEquals(listOf("From B"), awaitTitles(vm, listOf("From B")))

        repo.toggleSource("com.a")
        assertEquals(listOf("From A", "From B"), awaitTitles(vm, listOf("From A", "From B")))
    }

    @Test
    fun `disabling every source falls back to the repository fallback instead of an empty hero`() = runTest(mainDispatcher.dispatcher) {
        val (vm, repo) = buildViewModel()
        repo.refreshApps()
        vm.refreshHero()
        awaitTitles(vm, listOf("From A", "From B"))
        repo.toggleSource("com.a")
        repo.toggleSource("com.b")
        val fallback = vm.state.await { (it as? HomeState.Ready)?.hero?.fromApps == false } as HomeState.Ready
        assertFalse(fallback.hero.fromApps)
        assertEquals(listOf("nature-0"), fallback.hero.items.map { it.id })
    }

    @Test
    fun `re-enabling a source re-prepares the shelf of the focused app on the recomputed hero`() = runTest(mainDispatcher.dispatcher) {
        val items = (1..25).map { heroItem("b$it", "B$it", imageUrl = "https://x/b$it.jpg", sourcePackage = "com.b").copy(videoUrl = "https://example.invalid/b$it.mp4") } +
            heroItem("a", "A", imageUrl = "https://x/a.jpg", sourcePackage = "com.a").copy(videoUrl = "https://example.invalid/a.mp4")
        val prefs = LauncherPrefs(testDataStore())
        prefs.updateDisabledSources { setOf("com.a") }
        val (vm, repo) = buildViewModel(items, prefs)
        repo.refreshApps()
        vm.refreshHero()
        vm.state.await { (it as? HomeState.Ready)?.hero?.items?.size == 25 }

        vm.prepareShelf("com.a")
        repo.toggleSource("com.a")
        vm.state.await { (it as? HomeState.Ready)?.hero?.items?.size == 26 }

        vm.state.await { "https://x/a.jpg" in ((it as? HomeState.Ready)?.hero?.checked ?: emptySet()) }
        Unit
    }
}
