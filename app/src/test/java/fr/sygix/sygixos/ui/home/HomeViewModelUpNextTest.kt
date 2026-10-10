/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import androidx.datastore.preferences.core.edit
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
import fr.sygix.sygixos.data.dataStore
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.StartupSession
import fr.sygix.sygixos.domain.UpNextContent
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
        context.dataStore.edit { it.clear() }
        repo = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(context))
        vm = viewModel(source, "home")
        repo.refreshApps()
        vm.state.await { it is HomeState.Ready }
        Unit
    }

    @After
    fun clear() = store.clear()

    private fun viewModel(upNext: UpNextSource, key: String): HomeViewModel {
        val empty = object : HeroContentProvider { override suspend fun load() = emptyList<HeroItem>() }
        return HomeViewModel(
            repo, HeroRepository(empty, empty), AppArtworkSource(context.packageManager), VisualValidator(context),
            object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            }, FakeUpdateController(), StartupSession(), FakeMotionSource(), FakeMascotSource(),
            upNextSource = upNext,
        ).also { store.put(key, it) }
    }

    private suspend fun awaitItems(expected: List<UpNextItem>, model: HomeViewModel = vm) = model.state.await {
        ((it as? HomeState.Ready)?.upNext?.content as? UpNextContent.Items)?.items == expected
    } as HomeState.Ready

    @Test
    fun `refresh and provider signal update home without losing content on transient failure`() = runBlocking {
        val watching = launch { vm.upNextUpdates.collect() }
        source.notifications.subscriptionCount.await { it == 1 }
        source.result = Result.success(listOf(item))
        vm.refresh()
        awaitItems(listOf(item))
        val changed = item.copy(title = "Updated")
        source.result = Result.success(listOf(changed))
        source.notifications.emit(Unit)
        awaitItems(listOf(changed))
        source.result = Result.failure(IllegalStateException("unavailable"))
        val previousLoads = source.loads.value
        vm.refresh()
        source.loads.await { it > previousLoads }
        assertEquals(listOf(changed), (awaitItems(listOf(changed)).upNext!!.content as UpNextContent.Items).items)
        watching.cancel()
        source.notifications.subscriptionCount.await { it == 0 }
        Unit
    }

    @Test
    fun `visibility placement and source preferences reach home immediately`() = runBlocking {
        source.result = Result.success(listOf(item))
        vm.refresh()
        awaitItems(listOf(item))
        repo.setUpNextVisible(false)
        assertNull((vm.state.await { it is HomeState.Ready && it.upNext == null } as HomeState.Ready).upNext)
        repo.setUpNextPosition(UpNextPosition.AFTER_APPS)
        repo.setUpNextVisible(true)
        vm.state.await { it is HomeState.Ready && it.upNext?.position == UpNextPosition.AFTER_APPS }
        repo.setSourceEnabled(item.source.packageName, false)
        vm.state.await { it is HomeState.Ready && it.upNext == null }
        repo.setSourceEnabled(item.source.packageName, true)
        awaitItems(listOf(item))
        Unit
    }

    @Test
    fun `permission denial removes row without preventing home readiness`() = runBlocking {
        source.result = Result.failure(SecurityException("denied"))
        vm.refresh()
        source.loads.await { it > 0 }
        vm.state.await { it is HomeState.Ready && it.upNext == null }
        source.result = Result.success(listOf(item))
        vm.refresh()
        awaitItems(listOf(item))
        Unit
    }

    @Test
    fun `refused provider observation neither crashes nor prevents the row from loading`() = runBlocking {
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
