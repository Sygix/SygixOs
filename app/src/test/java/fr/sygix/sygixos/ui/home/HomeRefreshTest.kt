/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import androidx.lifecycle.viewModelScope
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
import fr.sygix.sygixos.data.ClockSource
import fr.sygix.sygixos.data.FakeUpdateController
import fr.sygix.sygixos.data.HeroRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.data.dataStore
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.StartupSession
import fr.sygix.sygixos.domain.VisualQuality
import fr.sygix.sygixos.model.HeroItem
import androidx.datastore.preferences.core.edit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeRefreshTest {
    @get:Rule val main = MainDispatcherRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun provider(block: suspend () -> List<HeroItem>) = object : HeroContentProvider {
        override suspend fun load(): List<HeroItem> = block()
    }

    private class Loader(val load: suspend (String) -> Unit = {}) : ImageLoader {
        override val defaults = DefaultRequestOptions()
        override val components = ComponentRegistry()
        override val memoryCache: MemoryCache? = null
        override val diskCache: DiskCache? = null
        override fun enqueue(request: ImageRequest): Disposable = error("unused")
        override suspend fun execute(request: ImageRequest): ImageResult {
            load(request.data.toString())
            return ErrorResult(null, request, IllegalStateException("offline"))
        }
        override fun shutdown() = Unit
        override fun newBuilder(): ImageLoader.Builder = error("unused")
    }

    private suspend fun home(programs: HeroContentProvider, loader: ImageLoader = Loader()): HomeViewModel {
        context.dataStore.edit { it.clear() }
        val apps = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(context))
        val home = main.track(HomeViewModel(
            apps, HeroRepository(programs, provider { emptyList() }), AppArtworkSource(context.packageManager),
            VisualValidator(context, loader), object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            }, FakeUpdateController(), StartupSession().apply { claimColdStart() }, FakeMotionSource(), FakeMascotSource(),
        ))
        apps.refreshApps()
        home.state.await { it is HomeState.Ready }
        return home
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
        try {
            home.refreshHero()
            withTimeout(5_000) { started.await() }
            home.refreshHero()
            home.state.await { (it as? HomeState.Ready)?.hero?.items == latest }
            withTimeout(5_000) { cancelled.await() }
            assertEquals(2, calls)
        } finally { home.viewModelScope.cancel() }
    }

    @Test fun interruptedShelfValidationResumesWhenReturningToTheApp() = runBlocking {
        val uri = "https://example.invalid/a.jpg"
        val first = CompletableDeferred<Unit>()
        val resumed = CompletableDeferred<Unit>()
        val attempts = AtomicInteger()
        val images = Loader { image ->
            if (image == uri) {
                if (attempts.incrementAndGet() == 1) {
                    first.complete(Unit)
                    awaitCancellation()
                } else resumed.complete(Unit)
            }
        }
        val items = (1..25).map { heroItem("b$it", "B$it", imageUrl = "https://example.invalid/b$it.jpg", sourcePackage = "com.b") } +
            heroItem("a", "A", imageUrl = uri, sourcePackage = "com.a")
        val home = home(provider { items }, images)
        try {
            home.refreshHero()
            home.state.await { (it as? HomeState.Ready)?.hero?.checked?.size == VisualQuality.HERO_VALIDATED }
            home.prepareShelf("com.a")
            withTimeout(5_000) { first.await() }
            home.prepareShelf("com.b")
            home.prepareShelf("com.a")
            withTimeout(5_000) { resumed.await() }
            home.state.await { uri in (it as? HomeState.Ready)?.hero?.checked.orEmpty() }
            home.prepareShelf("com.a")
            assertEquals(2, attempts.get())
        } finally { home.viewModelScope.cancel() }
    }
}
