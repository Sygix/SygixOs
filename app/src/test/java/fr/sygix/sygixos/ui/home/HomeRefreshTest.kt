/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.ColorDrawable
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import fr.sygix.sygixos.MainDispatcherRule
import fr.sygix.sygixos.await
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.ClockSource
import fr.sygix.sygixos.data.FakeUpdateController
import fr.sygix.sygixos.data.HeroRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.data.dataStore
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.StartupSession
import fr.sygix.sygixos.domain.ShelfPosters
import fr.sygix.sygixos.domain.VisualQuality
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeRefreshTest {
    @Test fun lateValidationCannotApproveAReplacementWithTheSameRowAndSharedEpisodeArtwork() = runBlocking {
        val uri = "https://example.invalid/shared.jpg"
        val original = heroItem("same-row", "First fiction", imageUrl = uri).copy(episodeArtwork = true)
        var feed = listOf(original)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val home = home(provider { feed }) { _, attempt ->
            if (attempt == 1) {
                entered.complete(Unit)
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { release.await() }
                finished.complete(Unit)
                null
            } else broken
        }
        try {
            home.refreshHero()
            withTimeout(5_000) { entered.await() }
            feed = listOf(original.copy(title = "Replacement fiction"))
            home.refreshHero()
            home.state.await { (it as? HomeState.Ready)?.hero?.checked == setOf(uri) }
            release.complete(Unit)
            withTimeout(5_000) { finished.await() }
            assertEquals("Replacement fiction", home.hero()?.items?.single()?.title)
            assertEquals(emptySet<String>(), home.hero()?.validated)
            assertEquals(2, attempts[uri])
        } finally { release.complete(Unit) }
    }

    @get:Rule val main = MainDispatcherRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val attempts = ConcurrentHashMap<String, Int>()

    private fun provider(block: suspend () -> List<HeroItem>) = object : HeroContentProvider {
        override suspend fun load(): List<HeroItem> = block()
    }

    private class Wide : ColorDrawable() {
        override fun getIntrinsicWidth() = VisualQuality.MIN_WIDTH_PX
        override fun getIntrinsicHeight() = 360
    }

    private inner class Loader(val respond: suspend (String, Int) -> Throwable?) : ImageLoader {
        override val defaults = DefaultRequestOptions()
        override val components = ComponentRegistry()
        override val memoryCache: MemoryCache? = null
        override val diskCache: DiskCache? = null
        override fun enqueue(request: ImageRequest): Disposable = error("unused")
        override suspend fun execute(request: ImageRequest): ImageResult {
            val uri = (request.data as? fr.sygix.sygixos.model.ProgramArtwork)?.candidates?.firstOrNull() ?: request.data.toString()
            val error = respond(uri, attempts.merge(uri, 1) { a, b -> a + b }!!)
            return if (error == null) SuccessResult(Wide(), request, DataSource.NETWORK) else ErrorResult(null, request, error)
        }
        override fun shutdown() = Unit
        override fun newBuilder(): ImageLoader.Builder = error("unused")
    }

    private fun viewModel(programs: HeroContentProvider, apps: AppCatalogRepository, respond: suspend (String, Int) -> Throwable?) = main.track(HomeViewModel(
        apps, HeroRepository(programs, provider { emptyList() }), AppArtworkSource(context.packageManager),
        VisualValidator(context, Loader(respond), Dispatchers.Unconfined), object : ClockSource {
            override fun current() = ""
            override fun time() = emptyFlow<String>()
        }, FakeUpdateController(), StartupSession().apply { claimColdStart() }, FakeMotionSource(), FakeMascotSource(),
        order = object : fr.sygix.sygixos.domain.HeroOrder {
            override var displayedId: String? = null
            override fun arrange(items: List<HeroItem>) = items
        },
    ))

    private suspend fun home(programs: HeroContentProvider, respond: suspend (String, Int) -> Throwable? = { _, _ -> broken }): HomeViewModel {
        context.dataStore.edit { it.clear() }
        val apps = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(context))
        val home = viewModel(programs, apps, respond)
        apps.refreshApps()
        home.state.await { it is HomeState.Ready }
        return home
    }

    private fun HomeViewModel.hero() = (state.value as? HomeState.Ready)?.hero

    private fun shelfFeed(vararg shelf: HeroItem) = (1..VisualQuality.HERO_VALIDATED).map {
        heroItem("b$it", "B$it", imageUrl = "https://example.invalid/b$it.jpg", sourcePackage = "com.b")
    } + shelf

    private suspend fun HomeViewModel.awaitHeroChecked() {
        refreshHero()
        state.await { (it as? HomeState.Ready)?.hero?.checked?.size == VisualQuality.HERO_VALIDATED }
    }

    @Test fun anUnchangedResumePublishesNothingWhilePackageAndProviderChangesArePublished() = runBlocking {
        var feed = listOf(heroItem("a", "A"))
        var loads = 0
        val home = home(provider { loads++; feed })
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items == feed }
        val published = mutableListOf<HomeState>()
        val watching = launch(Dispatchers.Unconfined) { home.state.collect { published += it } }
        home.refreshHero()
        assertEquals(2, loads)
        assertEquals(1, published.size)
        shadowOf(context.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            ResolveInfo().apply {
                nonLocalizedLabel = "New"
                activityInfo = ActivityInfo().apply { packageName = "com.new"; name = "com.new.Main"; applicationInfo = ApplicationInfo().apply { packageName = "com.new" } }
            },
        )
        home.artwork.invalidate("com.new")
        home.state.await { (it as? HomeState.Ready)?.catalog?.grid?.map(TvApp::packageName) == listOf("com.new") }
        feed = listOf(heroItem("b", "B"))
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items == feed }
        watching.cancel()
    }

    @Test fun theHeroIsRereadOnProviderSignalsAndAfterTheTtlOnlyWhileObserved() = runTest {
        val signals = MutableSharedFlow<Unit>()
        var loads = 0
        val home = viewModel(object : HeroContentProvider {
            override suspend fun load(): List<HeroItem> { loads++; return listOf(heroItem("a", "A")) }
            override fun changes() = signals.onStart { emit(Unit) }
        }, AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(context))) { _, _ -> broken }
        advanceTimeBy(HERO_TTL_MS * 2)
        assertEquals(0, loads)
        val watching = backgroundScope.launch { home.heroUpdates.collect() }
        runCurrent()
        assertEquals(1, loads)
        signals.emit(Unit)
        runCurrent()
        assertEquals(2, loads)
        advanceTimeBy(HERO_TTL_MS - 1)
        runCurrent()
        assertEquals(2, loads)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(3, loads)
        watching.cancel()
        advanceTimeBy(HERO_TTL_MS * 2)
        runCurrent()
        assertEquals(3, loads)
    }

    @Test fun unpublishedProgramsLeaveTheHeroAndItsValidatedSetsWhileModifiedOnesAreUpdated() = runBlocking {
        val kept = "https://example.invalid/kept.jpg"
        val gone = "https://example.invalid/gone.jpg"
        val replaced = "https://example.invalid/replaced.jpg"
        var feed = listOf(heroItem("k", "K", imageUrl = kept), heroItem("g", "G", imageUrl = gone))
        val home = home(provider { feed }) { _, _ -> null }
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(kept, gone) }
        feed = listOf(heroItem("k", "K2", imageUrl = replaced))
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.let { hero -> hero.items == feed && hero.validated == setOf(replaced) } == true }
        assertEquals(setOf(replaced), home.hero()?.checked)
    }

    @Test fun obsoleteHeroLoadIsCancelledWhenRefreshingAgain() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        var calls = 0
        val latest = listOf(heroItem("new", "Latest"))
        val home = home(provider {
            if (++calls == 1) {
                started.complete(Unit)
                try { awaitCancellation() } finally { cancelled.complete(Unit) }
            }
            latest
        })
        home.refreshHero()
        withTimeout(5_000) { started.await() }
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items == latest }
        withTimeout(5_000) { cancelled.await() }
        assertEquals(2, calls)
    }

    @Test fun unreachableVisualsAreRetriedOnRefreshButRejectedOnesAreNot() = runBlocking {
        val offline = "https://example.invalid/offline.jpg"
        val rejected = "https://example.invalid/rejected.jpg"
        val home = home(provider { listOf(heroItem("o", "O", imageUrl = offline), heroItem("r", "R", imageUrl = rejected)) }) { uri, attempt ->
            when {
                uri == rejected -> broken
                attempt == 1 -> IOException("offline")
                else -> null
            }
        }
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.checked == setOf(offline, rejected) }
        assertEquals(emptySet<String>(), home.hero()?.validated)
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(offline) }
        assertEquals(mapOf(offline to 2, rejected to 1), attempts)
    }

    @Test fun acceptedYouTubeVariantReplacesThePublishedThumbnail() = runBlocking {
        val published = "https://i.ytimg.com/vi/AAAAAAAAAAA/sddefault.jpg"
        val maxres = "https://i.ytimg.com/vi/AAAAAAAAAAA/maxresdefault.jpg"
        val home = home(provider { listOf(heroItem("y", "Y", imageUrl = published, sourcePackage = "com.y")) }) { uri, _ ->
            if (uri == maxres) null else broken
        }
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(maxres) }
        val hero = home.hero()!!
        assertEquals(listOf(maxres), hero.items.map { it.imageUrl })
        assertEquals(listOf(maxres), ShelfPosters.forPackage(hero.items, hero.validated, "com.y"))
        assertEquals(setOf(published, maxres), hero.checked)
    }

    @Test fun interruptedShelfValidationResumesWhenReturningToTheApp() = runBlocking {
        val uri = "https://example.invalid/a.jpg"
        val home = home(provider { shelfFeed(heroItem("a", "A", imageUrl = uri, sourcePackage = "com.a")) }) { image, attempt ->
            if (image == uri && attempt == 1) awaitCancellation()
            broken
        }
        home.awaitHeroChecked()
        home.prepareShelf("com.a")
        home.prepareShelf("com.b")
        home.prepareShelf("com.a")
        home.prepareShelf("com.a")
        assertTrue(uri in home.hero()?.checked.orEmpty())
        assertEquals(2, attempts[uri])
    }

    @Test fun theShelfOfTheFocusedAppFollowsTheRepublishedPrograms() = runBlocking {
        val stale = "https://example.invalid/a1.jpg"
        val fresh = "https://example.invalid/a2.jpg"
        var feed = shelfFeed(heroItem("a1", "A1", imageUrl = stale, sourcePackage = "com.a"))
        val home = home(provider { feed }) { image, _ ->
            if (image == stale) awaitCancellation()
            broken
        }
        home.awaitHeroChecked()
        home.prepareShelf("com.a")
        feed = shelfFeed(heroItem("a2", "A2", imageUrl = fresh, sourcePackage = "com.a"))
        home.refreshHero()
        home.state.await { it is HomeState.Ready && it.hero.items == feed }
        assertTrue(fresh in home.hero()?.checked.orEmpty())
    }

    private companion object {
        val broken = IllegalStateException("broken")
    }
}
