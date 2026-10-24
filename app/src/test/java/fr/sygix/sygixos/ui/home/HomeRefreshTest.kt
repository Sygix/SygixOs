/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import fr.sygix.sygixos.installLeanbackActivity

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
import fr.sygix.sygixos.testDataStore
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.cancelAndJoin
import androidx.lifecycle.viewModelScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeRefreshTest {
    @get:Rule val folder = org.junit.rules.TemporaryFolder()

    @Test fun cachedFailureRestoresFallbackAfterRevalidationWithoutAnotherCheckedImageEmission() = runTest(main.dispatcher) {
        val item = heroItem("preview-1", "Fiction", "content://fixture/failed", "fixture.app")
        var feed = listOf(item)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val home = home(provider { feed }) { _, attempt ->
            if (attempt == 2) { entered.complete(Unit); release.await() }
            IOException("Unavailable fixture")
        }
        val events = MutableSharedFlow<fr.sygix.sygixos.data.ArtworkEvent>(extraBufferCapacity = 1)
        home.observeArtwork(events)
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items?.singleOrNull()?.artworkFailed == true }
        events.emit(fr.sygix.sygixos.data.ArtworkEvent.ImageExpired(requireNotNull(item.imageUrl)))
        withTimeout(5_000) { entered.await() }
        feed = listOf(item.copy(positionMillis = 1_000, durationMillis = 4_000))
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items.isNullOrEmpty() }
        release.complete(Unit)
        home.state.await { (it as? HomeState.Ready)?.hero?.items?.singleOrNull()?.let { shown -> shown.artworkFailed && shown.positionMillis == 1_000L } == true }
        assertEquals(2, attempts[item.imageUrl])
    }

    @Test fun rejectedDisplayedCandidateCanSwitchToItsAlreadyValidAlternative() = runTest(main.dispatcher) {
        val bytes = fr.sygix.sygixos.data.artworkBytes()
        fr.sygix.sygixos.data.ArtworkFixture(folder.newFolder(), fr.sygix.sygixos.data.ImageBytesReader { uri, _ ->
            if (uri.path == "/alternative") fr.sygix.sygixos.data.artworkBytes(1000, 600) else bytes
        }, dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)).use { fixture ->
            val item = heroItem("preview-1", "Fiction", "content://fixture/main", "fixture.app").copy(thumbnailUrl = "content://fixture/alternative", contentId = "first")
            fixture.repository.confirm("preview", listOf(item.artwork to item.fullFingerprint))
            val home = home(provider { listOf(item) }, artworkRepository = fixture.repository) { _, _ -> null }
            try {
                home.refreshHero()
                home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(item.imageUrl) }
                home.prepareShelf("fixture.app")
                home.state.await { (it as? HomeState.Ready)?.hero?.shelfValidated == setOf(item.imageUrl) }
                fixture.repository.resolve(item.artwork.withCandidates(listOf(requireNotNull(item.thumbnailUrl))), fr.sygix.sygixos.domain.ArtworkPolicy.Hero)
                val others = (2..3).map { n -> item.copy(id = "preview-$n", title = "Fiction $n", imageUrl = "content://fixture/image$n", posterUrl = "content://fixture/image$n", thumbnailUrl = null, contentId = "film$n") }
                fixture.repository.confirm("preview", (listOf(item) + others).map { it.artwork to it.fullFingerprint })
                for (other in others) fixture.repository.resolve(other.artwork, fr.sygix.sygixos.domain.ArtworkPolicy.Hero)
                home.state.await { (it as? HomeState.Ready)?.hero?.let { hero -> hero.validated == setOf(item.thumbnailUrl) && hero.shelfValidated == setOf(item.thumbnailUrl) } == true }
                assertEquals(item.thumbnailUrl, home.hero()?.items?.single()?.imageUrl)
            } finally { home.viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin() }
        }
    }

    @Test fun imagesBecomingGenericWithdrawPreviouslyAcceptedHeroAndShelfResults() = runTest(main.dispatcher) {
        val bytes = fr.sygix.sygixos.data.artworkBytes()
        fr.sygix.sygixos.data.ArtworkFixture(folder.newFolder(), fr.sygix.sygixos.data.ImageBytesReader { _, _ -> bytes }, dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)).use { fixture ->
            val items = (1..3).map { n -> heroItem("preview-$n", "Fiction $n", "content://fixture/image$n", "fixture.app").copy(contentId = "film$n") }
            var feed = items.take(2)
            fixture.repository.confirm("preview", feed.map { it.artwork to it.fullFingerprint })
            val home = home(provider { feed }, artworkRepository = fixture.repository) { _, _ -> null }
            try {
            home.refreshHero()
            home.state.await { (it as? HomeState.Ready)?.hero?.validated?.size == 2 }
            home.prepareShelf("fixture.app")
            home.state.await { (it as? HomeState.Ready)?.hero?.shelfValidated?.size == 2 }
            feed = items
            fixture.repository.confirm("preview", feed.map { it.artwork to it.fullFingerprint })
            home.refreshHero()
            home.state.await { (it as? HomeState.Ready)?.hero?.let { hero -> hero.validated.isEmpty() && hero.shelfValidated.isNullOrEmpty() && hero.items.none { item -> item.sourcePackage == "fixture.app" } } == true }
            assertEquals(3, fixture.db.artwork().images().count { it.state == "generic" })
            } finally { home.viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin() }
        }
    }

    @Test fun aRefreshedPortraitRemainsCardCompatibleButLeavesHeroAndShelf() = runTest(main.dispatcher) {
        val bytes = java.util.concurrent.atomic.AtomicReference(fr.sygix.sygixos.data.artworkBytes())
        fr.sygix.sygixos.data.ArtworkFixture(folder.newFolder(), fr.sygix.sygixos.data.ImageBytesReader { _, _ -> bytes.get() }, dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)).use { fixture ->
            val item = heroItem("preview-1", "Fiction", "content://fixture/image", "fixture.app").copy(contentId = "film")
            fixture.repository.confirm("preview", listOf(item.artwork to item.fullFingerprint))
            val home = home(provider { listOf(item) }, artworkRepository = fixture.repository) { _, _ -> null }
            try {
            home.refreshHero()
            home.state.await { (it as? HomeState.Ready)?.hero?.validated?.isNotEmpty() == true }
            home.prepareShelf("fixture.app")
            home.state.await { (it as? HomeState.Ready)?.hero?.shelfValidated?.isNotEmpty() == true }
            fixture.repository.watch(item.artwork, fr.sygix.sygixos.domain.ArtworkPolicy.Hero).use {
                bytes.set(fr.sygix.sygixos.data.artworkBytes(800, 1200))
                fixture.now.addAndGet(fr.sygix.sygixos.domain.ArtworkAge.FRESH_MS)
                fixture.repository.confirm("preview", listOf(item.artwork to item.fullFingerprint))
                fixture.repository.checkExpiry()
                home.state.await { (it as? HomeState.Ready)?.hero?.let { hero -> hero.validated.isEmpty() && hero.shelfValidated.isNullOrEmpty() && hero.items.isEmpty() } == true }
                assertNotNull(fixture.repository.resolve(item.artwork, fr.sygix.sygixos.domain.ArtworkPolicy.Card(396)).first)
            }
            } finally { home.viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin() }
        }
    }
    @Test fun aChangedImageKeepsItsPreviousPosterUntilTheReplacementIsReady() = runTest(main.dispatcher) {
        val initial = heroItem("preview-1", "Fiction", imageUrl = "https://example.invalid/old.jpg", sourcePackage = "fixture.app")
        var feed = listOf(initial)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val replacement = "https://example.invalid/new.jpg"
        val home = home(provider { feed }) { uri, _ ->
            if (uri == replacement) { entered.complete(Unit); release.await() }
            null
        }
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(initial.imageUrl) }
        feed = listOf(initial.copy(imageUrl = replacement, posterUrl = replacement))
        home.refreshHero()
        withTimeout(5_000) { entered.await() }
        assertEquals(initial.imageUrl, home.hero()?.items?.single()?.imageUrl)
        release.complete(Unit)
        home.state.await { (it as? HomeState.Ready)?.hero?.items?.singleOrNull()?.imageUrl == replacement }
        assertEquals(1, attempts[initial.imageUrl])
        assertEquals(1, attempts[replacement])
    }

    @Test fun republishedRowsAndProgressUpdatesDoNotRevalidateImages() = runTest(main.dispatcher) {
        val initial = heroItem("preview-1", "Fiction", imageUrl = "https://example.invalid/one.jpg", sourcePackage = "fixture.app")
        var feed = listOf(initial)
        val home = home(provider { feed }) { _, _ -> null }
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(initial.imageUrl) }
        feed = listOf(initial.copy(id = "preview-22", positionMillis = 1_000, durationMillis = 4_000))
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items?.singleOrNull()?.id == "preview-22" }
        home.refreshHero()
        assertEquals(1, attempts[initial.imageUrl])
    }

    @Test fun lateImagesCannotRepopulateWithdrawnExpiredReplacedOrReusedProgramGenerations() = runTest(main.dispatcher) {
        val testScheduler = programScheduler
        for (event in listOf("withdraw", "expire", "replace", "reuse")) {
            val uri = "https://example.invalid/generation-$event.jpg"
            val initial = heroItem("row", "First fiction", imageUrl = uri).copy(
                expiresAt = testScheduler.currentTime + fr.sygix.sygixos.domain.ArtworkAge.FRESH_MS,
                contentId = "original",
            )
            var feed = listOf(initial)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val late = CompletableDeferred<Unit>()
            val home = home(provider { feed }, programClock = { testScheduler.currentTime }) { _, attempt ->
                if (attempt == 1) {
                    entered.complete(Unit)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { release.await() }
                    late.complete(Unit)
                    null
                } else broken
            }
            try {
                home.refreshHero()
                entered.await()
                when (event) {
                    "withdraw" -> { feed = emptyList(); home.refreshHero() }
                    "expire" -> testScheduler.advanceTimeBy(fr.sygix.sygixos.domain.ArtworkAge.FRESH_MS)
                    "replace" -> { feed = listOf(initial.copy(title = "Replacement fiction")); home.refreshHero() }
                    "reuse" -> { feed = listOf(initial.copy(contentId = "replacement")); home.refreshHero() }
                }
                testScheduler.runCurrent()
                release.complete(Unit)
                late.await()
                testScheduler.runCurrent()
                assertEquals(event, emptySet<String>(), home.hero()?.validated)
                if (event == "withdraw" || event == "expire") assertTrue(event, home.hero()?.items.isNullOrEmpty())
                else assertEquals(event, feed.single().contentId, home.hero()?.shelfItems?.single()?.contentId)
            } finally { release.complete(Unit) }
        }
    }

    @Test fun lateValidationCannotApproveAReplacementWithTheSameRowAndSharedEpisodeArtwork() = runTest(main.dispatcher) {
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
            assertEquals("Replacement fiction", home.hero()?.shelfItems?.single()?.title)
            assertTrue(home.hero()?.items.isNullOrEmpty())
            assertEquals(emptySet<String>(), home.hero()?.validated)
            assertEquals(2, attempts[uri])
        } finally { release.complete(Unit) }
    }

    private val programScheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
    @get:Rule val main = MainDispatcherRule(kotlinx.coroutines.test.UnconfinedTestDispatcher(programScheduler))
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

    private fun viewModel(programs: HeroContentProvider, apps: AppCatalogRepository, programClock: () -> Long = System::currentTimeMillis, artworkRepository: fr.sygix.sygixos.data.ArtworkRepository? = null, upNext: fr.sygix.sygixos.domain.UpNextSource? = null, respond: suspend (String, Int) -> Throwable?) = main.track(HomeViewModel(
        apps, HeroRepository(programs, provider { emptyList() }), AppArtworkSource(context.packageManager, main.dispatcher),
        VisualValidator(context, Loader(respond), main.dispatcher).also { it.repository = artworkRepository }, object : ClockSource {
            override fun current() = ""
            override fun time() = emptyFlow<String>()
        }, FakeUpdateController(), StartupSession().apply { claimColdStart() }, FakeMotionSource(), FakeMascotSource(),
        order = object : fr.sygix.sygixos.domain.HeroOrder {
            override var displayedId: String? = null
            override fun arrange(items: List<HeroItem>) = items
        },
        programClock = programClock,
        upNextSource = upNext,
        io = main.dispatcher,
    ))

    private suspend fun TestScope.home(programs: HeroContentProvider, programClock: () -> Long = System::currentTimeMillis, artworkRepository: fr.sygix.sygixos.data.ArtworkRepository? = null, respond: suspend (String, Int) -> Throwable? = { _, _ -> broken }): HomeViewModel {
        val apps = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(testDataStore()), main.dispatcher)
        val home = viewModel(programs, apps, programClock, artworkRepository, respond = respond)
        artworkRepository?.let { home.observeArtwork(it.events) }
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

    @Test fun anUnchangedResumePublishesNothingWhilePackageAndProviderChangesArePublished() = runTest(main.dispatcher) {
        var feed = listOf(heroItem("a", "A").copy(videoUrl = "https://example.invalid/a.mp4"))
        var loads = 0
        val home = home(provider { loads++; feed })
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items == feed }
        val published = mutableListOf<HomeState>()
        val watching = launch(Dispatchers.Unconfined) { home.state.collect { published += it } }
        home.refreshHero()
        assertEquals(2, loads)
        assertEquals(1, published.size)
        context.packageManager.installLeanbackActivity(
            ResolveInfo().apply {
                nonLocalizedLabel = "New"
                activityInfo = ActivityInfo().apply { packageName = "com.new"; name = "com.new.Main"; applicationInfo = ApplicationInfo().apply { packageName = "com.new" } }
            },
        )
        home.artwork.invalidate("com.new")
        home.state.await { (it as? HomeState.Ready)?.catalog?.grid?.map(TvApp::packageName) == listOf("com.new") }
        feed = listOf(heroItem("b", "B").copy(videoUrl = "https://example.invalid/b.mp4"))
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items == feed }
        watching.cancel()
    }

    @Test fun theHeroIsRereadOnProviderSignalsAndAfterTheTtlButOnReturnOnlyWhenSomethingChangedOrTheTtlIsDue() = runTest(main.dispatcher) {
        val signals = MutableSharedFlow<Unit>()
        var loads = 0
        val home = viewModel(object : HeroContentProvider {
            override suspend fun load(): List<HeroItem> { loads++; return listOf(heroItem("a", "A")) }
            override fun changes() = signals.onStart { emit(Unit) }
        }, AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(testDataStore()), main.dispatcher), programClock = { testScheduler.currentTime }) { _, _ -> broken }
        advanceTimeBy(HERO_TTL_MS * 2)
        assertEquals(0, loads)
        var watching = backgroundScope.launch { home.heroUpdates.collect() }
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
        advanceTimeBy(HERO_TTL_MS / 2)
        runCurrent()
        watching = backgroundScope.launch { home.heroUpdates.collect() }
        runCurrent()
        assertEquals(3, loads)
        watching.cancel()
        signals.emit(Unit)
        signals.emit(Unit)
        runCurrent()
        assertEquals(3, loads)
        watching = backgroundScope.launch { home.heroUpdates.collect() }
        runCurrent()
        assertEquals(4, loads)
        watching.cancel()
        advanceTimeBy(HERO_TTL_MS)
        runCurrent()
        assertEquals(4, loads)
        watching = backgroundScope.launch { home.heroUpdates.collect() }
        runCurrent()
        assertEquals(5, loads)
        watching.cancel()
    }

    @Test fun aReturnWithoutChangeReadsNothingWhileAChangeDuringTheStopIsReadOncePerZone() = runTest(main.dispatcher) {
        val previews = MutableSharedFlow<Unit>()
        val watchNext = MutableSharedFlow<Unit>()
        var heroReads = 0
        var upNextReads = 0
        val upNext = object : fr.sygix.sygixos.domain.UpNextSource {
            override suspend fun load(): Result<List<fr.sygix.sygixos.model.UpNextItem>> { upNextReads++; return Result.success(emptyList()) }
            override fun changes() = watchNext.onStart { emit(Unit) }
        }
        val home = viewModel(object : HeroContentProvider {
            override suspend fun load(): List<HeroItem> { heroReads++; return listOf(heroItem("a", "A")) }
            override fun changes() = kotlinx.coroutines.flow.merge(previews, watchNext).onStart { emit(Unit) }
        }, AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(testDataStore()), main.dispatcher), programClock = { testScheduler.currentTime }, upNext = upNext) { _, _ -> broken }
        fun start() = listOf(backgroundScope.launch { home.heroUpdates.collect() }, backgroundScope.launch { home.upNextUpdates.collect() })
        fun stop(observers: List<kotlinx.coroutines.Job>) { observers.forEach { it.cancel() }; home.onBackground() }
        var observers = start()
        runCurrent()
        assertEquals(1 to 1, heroReads to upNextReads)
        watchNext.emit(Unit)
        runCurrent()
        assertEquals(2 to 2, heroReads to upNextReads)
        previews.emit(Unit)
        runCurrent()
        assertEquals(3 to 2, heroReads to upNextReads)
        stop(observers)
        advanceTimeBy(60_000)
        home.onForeground()
        observers = start()
        runCurrent()
        assertEquals(3 to 2, heroReads to upNextReads)
        stop(observers)
        watchNext.emit(Unit)
        watchNext.emit(Unit)
        runCurrent()
        assertEquals(3 to 2, heroReads to upNextReads)
        home.onForeground()
        observers = start()
        runCurrent()
        assertEquals(4 to 3, heroReads to upNextReads)
        stop(observers)
    }

    @Test fun unpublishedProgramsLeaveTheHeroAndItsValidatedSetsWhileModifiedOnesAreUpdated() = runTest(main.dispatcher) {
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

    @Test fun obsoleteHeroLoadIsCancelledWhenRefreshingAgain() = runTest(main.dispatcher) {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        var calls = 0
        val latest = listOf(heroItem("new", "Latest").copy(videoUrl = "https://example.invalid/new.mp4"))
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

    @Test fun unreachableVisualsAreRetriedOnRefreshButRejectedOnesAreNot() = runTest(main.dispatcher) {
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

    @Test fun acceptedYouTubeVariantReplacesThePublishedThumbnail() = runTest(main.dispatcher) {
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

    @Test fun interruptedShelfValidationResumesWhenReturningToTheApp() = runTest(main.dispatcher) {
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

    @Test fun shelfValidationChecksTheHeaderWithoutDecodingAtThePanelSize() = runTest(main.dispatcher) {
        val uri = "https://example.invalid/a.jpg"
        val home = home(provider { shelfFeed(heroItem("a", "A", imageUrl = uri, sourcePackage = "com.a")) }) { _, _ -> null }
        home.awaitHeroChecked()
        home.prepareShelf("com.a")
        home.state.await { (it as? HomeState.Ready)?.hero?.shelfValidated?.contains(uri) == true }
        assertEquals(1, attempts[uri])
    }

    @Test fun theShelfOfTheFocusedAppFollowsTheRepublishedPrograms() = runTest(main.dispatcher) {
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
        home.state.await { it is HomeState.Ready && it.hero.shelfItems == feed }
        assertTrue(fresh in home.hero()?.checked.orEmpty())
    }

    private companion object {
        val broken = IllegalStateException("broken")
    }
}
