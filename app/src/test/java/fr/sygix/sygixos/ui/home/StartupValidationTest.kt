/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
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
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.StartupSession
import fr.sygix.sygixos.model.HeroItem
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class WidePicture : ColorDrawable(android.graphics.Color.WHITE) {
    override fun getIntrinsicWidth() = 1920
    override fun getIntrinsicHeight() = 1080
}

private class CountingLoader : ImageLoader {
    val requests = AtomicInteger()
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult {
        requests.incrementAndGet()
        return SuccessResult(WidePicture(), request, DataSource.MEMORY)
    }
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

private class Programs(private val items: List<HeroItem>) : HeroContentProvider {
    override suspend fun load(): List<HeroItem> = items
}

@RunWith(RobolectricTestRunner::class)
class StartupValidationTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val uris = (1..3).map { "https://example.invalid/p$it.jpg" }

    @Before
    fun resetPrefs() = runBlocking {
        val prefs = LauncherPrefs(context)
        prefs.setDisabledSources(emptySet())
        prefs.setHidden(emptySet())
        prefs.setPinned(emptySet())
        prefs.setGridOrder(emptyList())
        prefs.setCachedApps(emptyList())
    }

    private fun viewModel(session: StartupSession, loader: CountingLoader): HomeViewModel {
        val repo = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(context))
        val items = uris.mapIndexed { i, uri -> heroItem("p$i", "Titre fictif $i", imageUrl = uri, sourcePackage = "com.example.player") }
        return HomeViewModel(
            apps = repo,
            hero = HeroRepository(Programs(items), Programs(emptyList())),
            artwork = AppArtworkSource(context.packageManager),
            validator = VisualValidator(context, loader),
            clockSource = object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            },
            updates = FakeUpdateController(),
            session = session,
            motion = FakeMotionSource(),
            mascot = FakeMascotSource(),
        ).also { vm ->
            runBlocking { repo.refreshApps() }
            vm.refreshHero()
        }
    }

    private fun HomeViewModel.checked(): Set<String> = (state.value as? HomeState.Ready)?.hero?.checked.orEmpty()

    @Test
    fun `on a cold start only the first usable hero visual is validated during the startup screen`() = runBlocking {
        val loader = CountingLoader()
        val vm = viewModel(StartupSession(), loader)
        vm.state.await { (it as? HomeState.Ready)?.hero?.checked?.isNotEmpty() == true }
        delay(300)
        assertEquals(setOf(uris[0]), vm.checked())
        assertEquals(1, loader.requests.get())
    }

    @Test
    fun `without a startup screen every hero visual is validated at once`() = runBlocking {
        val session = StartupSession().apply { claimColdStart() }
        val loader = CountingLoader()
        val vm = viewModel(session, loader)
        vm.state.await { (it as? HomeState.Ready)?.hero?.checked?.size == uris.size }
        assertEquals(uris.toSet(), vm.checked())
    }
}
