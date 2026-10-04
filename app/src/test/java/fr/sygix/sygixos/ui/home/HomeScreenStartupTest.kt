/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
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
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class WidePoster : ColorDrawable(android.graphics.Color.WHITE) {
    override fun getIntrinsicWidth() = 1920
    override fun getIntrinsicHeight() = 1080
}

private class WidePosterLoader : ImageLoader {
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult = SuccessResult(WidePoster(), request, DataSource.MEMORY)
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

private class PosterFeed(private val items: List<HeroItem>) : HeroContentProvider {
    override suspend fun load(): List<HeroItem> = items
}

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HomeScreenStartupTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>(dispatcher)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val poster = "https://x/poster.jpg"
    private var shownAt = 0L

    @After
    fun resetImageLoader() = Coil.reset()

    private fun readyViewModel(): HomeViewModel = runBlocking {
        val prefs = LauncherPrefs(context)
        prefs.setDisabledSources(emptySet())
        prefs.setHidden(emptySet())
        prefs.setPinned(emptySet())
        prefs.setGridOrder(emptyList())
        prefs.setCachedApps(emptyList())
        val loader = WidePosterLoader()
        Coil.setImageLoader(loader)
        val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)
        val vm = HomeViewModel(
            apps = repo,
            hero = HeroRepository(PosterFeed(listOf(heroItem("h1", "Programme", imageUrl = poster))), PosterFeed(emptyList())),
            artwork = AppArtworkSource(context.packageManager),
            validator = VisualValidator(context, loader),
            clockSource = object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            },
            updates = FakeUpdateController(),
            session = StartupSession(),
            motion = FakeMotionSource(),
            mascot = FakeMascotSource(),
            startupClock = { scheduler.currentTime },
            startupDispatcher = dispatcher,
        )
        repo.refreshApps()
        vm.refreshHero()
        vm.state.await { poster in ((it as? HomeState.Ready)?.hero?.validated ?: emptySet()) }
        vm
    }

    private fun frames(count: Int = 3) = repeat(count) { compose.mainClock.advanceTimeByFrame() }

    private fun advanceTo(elapsed: Long) {
        val delta = shownAt + elapsed - scheduler.currentTime
        if (delta > 0) compose.mainClock.advanceTimeBy(delta, ignoreFrameDuration = true)
        frames(1)
    }

    @Test
    fun `home screen keeps the home unfocusable under the splash and ends it on the first hero visual`() {
        val vm = readyViewModel()
        compose.mainClock.autoAdvance = false
        compose.setContent { SygixOsTheme { HomeScreen(vm, glassBlur = false) } }
        frames(3)
        shownAt = scheduler.currentTime
        compose.onNodeWithTag("startup-splash").assertExists()
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(0)
        advanceTo(400)
        compose.onNodeWithTag("hero-poster").assertExists()
        advanceTo(500)
        compose.onAllNodes(isFocused()).assertCountEquals(0)
        compose.onNodeWithTag("startup-splash").assertExists()
        advanceTo(700 + Motion.SPLASH_FADE_MS.toLong())
        frames()
        compose.onAllNodesWithTag("startup-splash").assertCountEquals(0)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }
}
