/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
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
import fr.sygix.sygixos.data.VisualCheck
import fr.sygix.sygixos.data.VisualChecker
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.HeroOrder
import fr.sygix.sygixos.domain.HeroRead
import fr.sygix.sygixos.domain.StartupSession
import fr.sygix.sygixos.domain.VisualQuality
import fr.sygix.sygixos.inlineDataStore
import fr.sygix.sygixos.model.HeroItem
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HeroLoadingTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val nature = HeroItem("nature-0", "", videoUrl = "https://example.invalid/nature.mp4")
    private val visibility = ConcurrentHashMap<String, Boolean>()
    private lateinit var apps: AppCatalogRepository

    private class Programs(
        private val cache: List<HeroItem>? = null,
        private val gate: CompletableDeferred<Unit>? = null,
        private val items: () -> List<HeroItem>,
    ) : HeroContentProvider {
        override suspend fun load(): List<HeroItem> { gate?.await(); return items() }
        override suspend fun cached(): HeroRead? = cache?.let { HeroRead(it) }
    }

    private fun checker(respond: suspend (String) -> VisualCheck) = VisualChecker { source, _, visible ->
        val uri = source.candidates.first()
        visibility[uri] = visible
        respond(uri)
    }

    private fun TestScope.home(programs: HeroContentProvider, checker: VisualChecker): HomeViewModel {
        apps = AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(inlineDataStore()), main.dispatcher)
        return main.track(HomeViewModel(
            apps, HeroRepository(programs, object : HeroContentProvider { override suspend fun load() = listOf(nature) }),
            AppArtworkSource(context.packageManager, main.dispatcher), checker,
            object : ClockSource {
                override fun current() = ""
                override fun time() = emptyFlow<String>()
            }, FakeUpdateController(), StartupSession().apply { claimColdStart() }, FakeMotionSource(), FakeMascotSource(),
            order = object : HeroOrder {
                override var displayedId: String? = null
                override fun arrange(items: List<HeroItem>) = items
            },
            io = main.dispatcher,
        )).also { kotlinx.coroutines.runBlocking { apps.refreshApps() } }
    }

    private fun HomeViewModel.hero() = (state.value as? HomeState.Ready)?.hero

    @Test
    fun `cached programs show before the provider read completes`() = runTest(main.dispatcher) {
        val cached = heroItem("preview-1", "Cached fiction", "https://example.invalid/cached.jpg", "com.example")
        val home = home(Programs(cache = listOf(cached), gate = CompletableDeferred()) { emptyList() }, checker { VisualCheck(it) })
        home.refreshHero()
        val shown = home.state.await { (it as? HomeState.Ready)?.hero?.items?.singleOrNull()?.imageUrl == cached.imageUrl }
        assertEquals(listOf("Cached fiction"), (shown as HomeState.Ready).hero.items.map { it.title })
    }

    @Test
    fun `a cold start shows the program texts while its image is pending and never the fallback`() = runTest(main.dispatcher) {
        val program = heroItem("preview-1", "Pending fiction", "https://example.invalid/pending.jpg", "com.example")
        val release = CompletableDeferred<Unit>()
        val home = home(Programs { listOf(program) }, checker { release.await(); VisualCheck(it) })
        home.refreshHero()
        val pending = home.state.await { (it as? HomeState.Ready)?.hero?.items?.isNotEmpty() == true } as HomeState.Ready
        assertEquals(listOf("Pending fiction"), pending.hero.items.map { it.title })
        assertTrue(pending.hero.items.single().artworkPending)
        assertFalse(pending.hero.items.single().artworkFailed)
        release.complete(Unit)
        home.state.await { (it as? HomeState.Ready)?.hero?.items?.singleOrNull()?.artworkPending == false }
    }

    @Test
    fun `a program without any image is kept for the nature fallback`() = runTest(main.dispatcher) {
        val plain = heroItem("preview-1", "Plain fiction", sourcePackage = "com.example")
        val home = home(Programs { listOf(plain) }, checker { error("nothing to check") })
        home.refreshHero()
        val shown = home.state.await { (it as? HomeState.Ready)?.hero?.items?.isNotEmpty() == true } as HomeState.Ready
        assertEquals(listOf("preview-1"), shown.hero.items.map { it.id })
        assertFalse(shown.hero.items.single().artworkPending)
        assertTrue(shown.hero.fromApps)
    }

    @Test
    fun `an image already verified in the index is ready without waiting behind a network check of the same app`() = runTest(main.dispatcher) {
        val first = heroItem("preview-1", "Unverified fiction", "https://example.invalid/first.jpg", "com.example")
        val second = heroItem("preview-2", "Verified fiction", "https://example.invalid/second.jpg", "com.example")
        val network = ConcurrentHashMap<String, Int>()
        val checker = object : VisualChecker {
            override suspend fun validate(source: fr.sygix.sygixos.model.ProgramArtwork, policy: fr.sygix.sygixos.domain.ArtworkPolicy, visible: Boolean): VisualCheck {
                network.merge(source.candidates.first(), 1, Int::plus)
                return awaitCancellation()
            }
            override suspend fun indexed(source: fr.sygix.sygixos.model.ProgramArtwork, policy: fr.sygix.sygixos.domain.ArtworkPolicy) =
                source.candidates.first().takeIf { it == second.imageUrl }
        }
        val home = home(Programs { listOf(first, second) }, checker)
        home.refreshHero()
        val shown = home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(second.imageUrl) } as HomeState.Ready
        assertEquals(listOf(true, false), shown.hero.items.map { it.artworkPending })
        assertEquals(mapOf(first.imageUrl to 1), network.toMap())
    }

    @Test
    fun `a busy artwork queue keeps the program pending instead of failing it`() = runTest(main.dispatcher) {
        val program = heroItem("preview-1", "Busy fiction", "https://example.invalid/busy.jpg", "com.example")
        var calls = 0
        val home = home(Programs { listOf(program) }, checker { uri -> if (++calls <= 2) VisualCheck(null, busy = true) else VisualCheck(uri) })
        home.refreshHero()
        runCurrent()
        val busy = home.hero()?.items?.single()
        assertTrue(busy?.artworkPending == true)
        assertFalse(busy?.artworkFailed == true)
        advanceTimeBy(10_000)
        home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(program.imageUrl) }
        assertEquals(3, calls)
    }

    @Test
    fun `a real image failure shows the program on the fallback while a quality rejection excludes it`() = runTest(main.dispatcher) {
        val failed = heroItem("preview-1", "Failed fiction", "https://example.invalid/failed.jpg", "com.example")
        val rejected = heroItem("preview-2", "Rejected fiction", "https://example.invalid/portrait.jpg", "com.other")
        val home = home(Programs { listOf(failed, rejected) }, checker { uri -> VisualCheck(null, retryable = uri == failed.imageUrl) })
        home.refreshHero()
        val shown = home.state.await { (it as? HomeState.Ready)?.hero?.checked == setOf(failed.imageUrl, rejected.imageUrl) } as HomeState.Ready
        assertEquals(listOf("preview-1"), shown.hero.items.map { it.id })
        assertTrue(shown.hero.items.single().artworkFailed)
    }

    @Test
    fun `the global fallback waits for a read that confirms no program`() = runTest(main.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val home = home(Programs(gate = gate) { emptyList() }, checker { VisualCheck(it) })
        home.refreshHero()
        apps.toggleSource("com.unrelated")
        runCurrent()
        advanceTimeBy(60_000)
        assertEquals(emptyList<HeroItem>(), home.hero()?.items)
        gate.complete(Unit)
        val fallback = home.state.await { (it as? HomeState.Ready)?.hero?.items?.isNotEmpty() == true } as HomeState.Ready
        assertEquals(listOf("nature-0"), fallback.hero.items.map { it.id })
        assertFalse(fallback.hero.fromApps)
    }

    @Test
    fun `the first hero images are checked at the visible priority and the rest in the background`() = runTest(main.dispatcher) {
        val programs = (1..5).map { heroItem("preview-$it", "Fiction $it", "https://example.invalid/$it.jpg", "com.app$it") }
        val home = home(Programs { programs }, checker { VisualCheck(it) })
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.validated?.size == programs.size }
        val visible = programs.take(VisualQuality.HERO_VISIBLE).map { it.imageUrl!! }
        assertEquals(programs.associate { it.imageUrl!! to (it.imageUrl in visible) }, visibility.toMap())
    }

    @Test
    fun `a provider read replacing the cache revalidates only the programs whose artwork changed`() = runTest(main.dispatcher) {
        val kept = heroItem("preview-1", "Kept fiction", "https://example.invalid/kept.jpg", "com.example")
        val old = heroItem("preview-2", "Old fiction", "https://example.invalid/old.jpg", "com.other")
        val replaced = old.copy(imageUrl = "https://example.invalid/new.jpg", posterUrl = "https://example.invalid/new.jpg")
        val gate = CompletableDeferred<Unit>()
        val checks = ConcurrentHashMap<String, Int>()
        val home = home(Programs(cache = listOf(kept, old), gate = gate) { listOf(kept, replaced) }, checker { uri -> checks.merge(uri, 1, Int::plus); VisualCheck(uri) })
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(kept.imageUrl, old.imageUrl) }
        gate.complete(Unit)
        home.state.await { (it as? HomeState.Ready)?.hero?.validated == setOf(kept.imageUrl, replaced.imageUrl) }
        assertEquals(mapOf(kept.imageUrl to 1, old.imageUrl to 1, replaced.imageUrl to 1), checks.toMap())
    }

    @Test
    fun `a stale cache never replaces a confirmed read`() = runTest(main.dispatcher) {
        val fresh = heroItem("preview-1", "Fresh fiction", "https://example.invalid/fresh.jpg", "com.example")
        val stale = heroItem("preview-9", "Stale fiction", "https://example.invalid/stale.jpg", "com.example")
        val cacheGate = CompletableDeferred<Unit>()
        val programs = object : HeroContentProvider {
            override suspend fun load() = listOf(fresh)
            override suspend fun cached(): HeroRead { cacheGate.await(); return HeroRead(listOf(stale)) }
        }
        val home = home(programs, checker { VisualCheck(it) })
        home.refreshHero()
        home.state.await { (it as? HomeState.Ready)?.hero?.items?.map { item -> item.id } == listOf("preview-1") }
        cacheGate.complete(Unit)
        runCurrent()
        assertEquals(listOf("preview-1"), home.hero()?.items?.map { it.id })
    }
}
