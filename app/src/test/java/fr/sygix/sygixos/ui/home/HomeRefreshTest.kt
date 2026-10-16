/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
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
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeRefreshTest {
    @get:Rule val main = MainDispatcherRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val attempts = ConcurrentHashMap<String, Int>()

    private fun provider(block: suspend () -> List<HeroItem>) = object : HeroContentProvider {
        override suspend fun load(): List<HeroItem> = block()
    }

    private class Wide : ColorDrawable() {
        override fun getIntrinsicWidth() = VisualQuality.MIN_WIDTH_PX
    }

    private inner class Loader(val respond: suspend (String, Int) -> Throwable?) : ImageLoader {
        override val defaults = DefaultRequestOptions()
        override val components = ComponentRegistry()
        override val memoryCache: MemoryCache? = null
        override val diskCache: DiskCache? = null
        override fun enqueue(request: ImageRequest): Disposable = error("unused")
        override suspend fun execute(request: ImageRequest): ImageResult {
            val uri = request.data.toString()
            val error = respond(uri, attempts.merge(uri, 1) { a, b -> a + b }!!)
            return if (error == null) SuccessResult(Wide(), request, DataSource.NETWORK) else ErrorResult(null, request, error)
        }
        override fun shutdown() = Unit
        override fun newBuilder(): ImageLoader.Builder = error("unused")
    }

    private suspend fun home(programs: HeroContentProvider, respond: suspend (String, Int) -> Throwable? = { _, _ -> broken }): HomeViewModel {
        context.dataStore.edit { it.clear() }
        val apps = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(context))
        val home = main.track(HomeViewModel(
            apps, HeroRepository(programs, provider { emptyList() }), AppArtworkSource(context.packageManager),
            VisualValidator(context, Loader(respond), Dispatchers.Unconfined), object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            }, FakeUpdateController(), StartupSession().apply { claimColdStart() }, FakeMotionSource(), FakeMascotSource(),
        ))
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

    @Test fun newShelfCandidatesAreNotBlockedByAStaleValidation() = runBlocking {
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
        home.prepareShelf("com.a")
        assertTrue(fresh in home.hero()?.checked.orEmpty())
    }

    private companion object {
        val broken = IllegalStateException("broken")
    }
}
