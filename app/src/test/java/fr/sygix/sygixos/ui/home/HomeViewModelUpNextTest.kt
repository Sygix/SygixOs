/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import fr.sygix.sygixos.installLeanbackActivity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
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
import fr.sygix.sygixos.domain.InMemoryUpNextMemory
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextContentKey
import fr.sygix.sygixos.domain.UpNextMemory
import fr.sygix.sygixos.domain.UpNextOpener
import fr.sygix.sygixos.domain.UpNextSource
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import fr.sygix.sygixos.inlineDataStore
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class HomeViewModelUpNextTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = ViewModelStore()
    private val source = Source()
    private lateinit var repo: AppCatalogRepository
    private lateinit var vm: HomeViewModel
    private val item = UpNextItem(1, UpNextSourceEntry("com.source"), UpNextContentType.MOVIE, "Movie")

    private class Source : UpNextSource {
        val notifications = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val loads = MutableStateFlow(0)
        var result: Result<List<UpNextItem>> = Result.success(emptyList())
        override suspend fun load(): Result<List<UpNextItem>> {
            loads.value += 1
            return result
        }
        override fun changes() = notifications
    }

    @Before
    fun setup() = runBlocking {
        repo = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(inlineDataStore()), Dispatchers.Unconfined)
        vm = viewModel(source, "home")
        repo.refreshApps()
        vm.state.await { it is HomeState.Ready }
        Unit
    }

    @After
    fun clear() = store.clear()

    private fun viewModel(
        upNext: UpNextSource,
        key: String,
        memory: UpNextMemory = InMemoryUpNextMemory(),
        opener: UpNextOpener = UpNextOpener { _, _ -> false },
    ): HomeViewModel {
        val empty = object : HeroContentProvider { override suspend fun load() = emptyList<HeroItem>() }
        return mainDispatcher.track(HomeViewModel(
            repo, HeroRepository(empty, empty), AppArtworkSource(context.packageManager, mainDispatcher.dispatcher), VisualValidator(context, dispatcher = mainDispatcher.dispatcher),
            object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            }, FakeUpdateController(), StartupSession(), FakeMotionSource(), FakeMascotSource(),
            upNextSource = upNext,
            upNextMemory = memory,
            upNextOpener = opener,
        )).also { store.put(key, it) }
    }

    private suspend fun awaitItems(expected: List<UpNextItem>, model: HomeViewModel = vm) = model.state.await {
        ((it as? HomeState.Ready)?.upNext?.content as? UpNextContent.Items)?.items == expected
    } as HomeState.Ready

    @Test
    fun `an opening is recorded on the published intent or the fallback and never on a double failure`() = runTest(mainDispatcher.dispatcher) {
        val memory = InMemoryUpNextMemory()
        var refuseIntent = false
        var refuseFallback = false
        val launcher = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                if (if (intent.data != null) refuseIntent else refuseFallback) throw ActivityNotFoundException()
            }
        }
        val entry = UpNextSourceEntry("com.source", "Source", intentUri = "https://example.org/play")
        context.packageManager.installLeanbackActivity(
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = entry.packageName
                    name = "com.source.MainActivity"
                    applicationInfo = ApplicationInfo().apply { packageName = entry.packageName; enabled = true }
                    enabled = true
                }
            },
        )
        val home = viewModel(source, "routing", memory) { opened, chosen ->
            if (chosen == null) AppLauncher.open(launcher, opened) else AppLauncher.openWith(launcher, opened, chosen)
        }
        fun movie(title: String) = UpNextItem(title.length.toLong(), entry, UpNextContentType.MOVIE, title)
        val viaIntent = movie("Par intent")
        home.openUpNext(viaIntent)
        memory.opened.await { UpNextContentKey.of(viaIntent) in it }
        refuseIntent = true
        val viaFallback = movie("Repli")
        home.openUpNext(viaFallback, entry)
        memory.opened.await { UpNextContentKey.of(viaFallback) in it }
        refuseFallback = true
        val refused = movie("Refusé")
        home.openUpNext(refused)
        home.openUpNext(refused, entry)
        assertEquals(setOf(UpNextContentKey.of(viaIntent), UpNextContentKey.of(viaFallback)), memory.opened.value.keys)
    }

    @Test
    fun `refresh and provider signal update home without losing content on transient failure`() = runTest(mainDispatcher.dispatcher) {
        val watching = launch { vm.upNextUpdates.collect() }
        source.notifications.subscriptionCount.await { it == 1 }
        source.result = Result.success(listOf(item))
        vm.refreshUpNext()
        awaitItems(listOf(item))
        val changed = item.copy(title = "Updated")
        source.result = Result.success(listOf(changed))
        source.notifications.emit(Unit)
        awaitItems(listOf(changed))
        source.result = Result.failure(IllegalStateException("unavailable"))
        val previousLoads = source.loads.value
        vm.refreshUpNext()
        source.loads.await { it > previousLoads }
        assertEquals(listOf(changed), (awaitItems(listOf(changed)).upNext!!.content as UpNextContent.Items).items)
        watching.cancel()
        source.notifications.subscriptionCount.await { it == 0 }
        Unit
    }

    @Test
    fun `visibility placement and source preferences reach home immediately`() = runTest(mainDispatcher.dispatcher) {
        source.result = Result.success(listOf(item))
        vm.refreshUpNext()
        awaitItems(listOf(item))
        repo.setUpNextVisible(false)
        assertNull((vm.state.await { it is HomeState.Ready && it.upNext == null } as HomeState.Ready).upNext)
        repo.setUpNextPosition(UpNextPosition.AFTER_APPS)
        repo.setUpNextVisible(true)
        vm.state.await { it is HomeState.Ready && it.upNext?.position == UpNextPosition.AFTER_APPS }
        repo.toggleSource(item.source.packageName)
        vm.state.await { it is HomeState.Ready && it.upNext == null }
        repo.toggleSource(item.source.packageName)
        awaitItems(listOf(item))
        Unit
    }

    @Test
    fun `permission denial removes row without preventing home readiness`() = runTest(mainDispatcher.dispatcher) {
        source.result = Result.failure(SecurityException("denied"))
        vm.refreshUpNext()
        source.loads.await { it > 0 }
        vm.state.await { it is HomeState.Ready && it.upNext == null }
        source.result = Result.success(listOf(item))
        vm.refreshUpNext()
        awaitItems(listOf(item))
        Unit
    }

    @Test
    fun `refused provider observation neither crashes nor prevents the row from loading`() = runTest(mainDispatcher.dispatcher) {
        val refused = object : UpNextSource {
            override suspend fun load() = Result.success(listOf(item))
            override fun changes() = flowOf(Unit)
        }
        val home = viewModel(refused, "refused")
        withTimeout(5_000) { home.upNextUpdates.collect() }
        awaitItems(listOf(item), home)
        Unit
    }
}
