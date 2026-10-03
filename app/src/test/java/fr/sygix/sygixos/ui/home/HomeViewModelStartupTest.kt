/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

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
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.ClockSource
import fr.sygix.sygixos.data.FakeUpdateController
import fr.sygix.sygixos.data.HeroRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.StartupPhase
import fr.sygix.sygixos.domain.StartupSession
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class EmptyFeed : HeroContentProvider {
    override suspend fun load(): List<HeroItem> = emptyList()
}

private class NoImageLoader : ImageLoader {
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult = ErrorResult(null, request, IllegalStateException("offline"))
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelStartupTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repo = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(context))

    private fun TestScope.viewModel(
        session: StartupSession,
        motion: FakeMotionSource = FakeMotionSource(),
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
    ) = HomeViewModel(
        apps = repo,
        hero = HeroRepository(EmptyFeed(), EmptyFeed()),
        artwork = AppArtworkSource(context.packageManager),
        validator = VisualValidator(context, NoImageLoader()),
        clockSource = object : ClockSource {
            override fun current() = ""
            override fun time() = emptyFlow<String>()
        },
        updates = FakeUpdateController(),
        session = session,
        motion = motion,
        mascot = FakeMascotSource(),
        startupClock = { testScheduler.currentTime },
        startupDispatcher = dispatcher,
    )

    @Test
    fun `first home of the process starts on the splash and a second one does not`() = runTest {
        val session = StartupSession()
        val first = viewModel(session)
        val second = viewModel(session)
        assertEquals(StartupPhase.Splash, first.startup.value)
        assertEquals(StartupPhase.Done, second.startup.value)
    }

    @Test
    fun `a process started in the background without a home still gets the splash`() = runTest {
        val untouched = StartupSession()
        assertEquals(StartupPhase.Splash, viewModel(untouched).startup.value)
    }

    @Test
    fun `system motion is read on a cold start only`() = runTest {
        val session = StartupSession()
        val coldMotion = FakeMotionSource(enabled = false)
        val warmMotion = FakeMotionSource()
        val cold = viewModel(session, coldMotion)
        val warm = viewModel(session, warmMotion)
        assertEquals(1, coldMotion.reads)
        assertFalse(cold.startupAnimated)
        assertEquals(0, warmMotion.reads)
        assertFalse(warm.startupAnimated)
        assertTrue(viewModel(StartupSession()).startupAnimated)
    }

    @Test
    fun `loaded catalog and hero visual end the splash after the minimum duration`() = runTest {
        val vm = viewModel(StartupSession())
        repo.refreshApps()
        vm.state.first { it is HomeState.Ready }
        runCurrent()
        vm.onSplashShown()
        vm.onHeroVisualReady()
        advanceTimeBy(599)
        assertEquals(StartupPhase.Splash, vm.startup.value)
        advanceTimeBy(2)
        assertEquals(StartupPhase.FadingOut, vm.startup.value)
        advanceTimeBy(10_000)
        assertEquals(StartupPhase.FadingOut, vm.startup.value)
        vm.onSplashFadeFinished()
        assertEquals(StartupPhase.Done, vm.startup.value)
    }

    @Test
    fun `without a catalog the splash ends at the global cap`() = runTest {
        val vm = viewModel(StartupSession())
        vm.onSplashShown()
        vm.onHeroVisualReady()
        advanceTimeBy(4_999)
        assertEquals(StartupPhase.Splash, vm.startup.value)
        advanceTimeBy(2)
        assertEquals(StartupPhase.FadingOut, vm.startup.value)
    }
}
