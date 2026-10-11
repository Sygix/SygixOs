/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpNextControllerMemoryTest {
    private class Source(var result: Result<List<UpNextItem>>) : UpNextSource {
        override suspend fun load(): Result<List<UpNextItem>> = result
        override fun changes(): Flow<Unit> = emptyFlow()
    }

    private fun movie(id: Long, title: String, engagement: Long, pkg: String = "app.$id") =
        UpNextItem(id, UpNextSourceEntry(pkg), UpNextContentType.MOVIE, title, watchNextType = ProgramKind.CONTINUE, engagement = engagement)

    private fun episode(id: Long, number: String, pkg: String = "app.series") =
        UpNextItem(id, UpNextSourceEntry(pkg), UpNextContentType.EPISODE, "Ep $number", seriesTitle = "Série", season = "1", episode = number)

    private var now = 1_000L
    private val disabled = MutableStateFlow(emptySet<String>())

    private fun TestScope.controller(source: Source, memory: UpNextMemory) =
        UpNextController(source, MutableStateFlow(true), MutableStateFlow(UpNextPosition.BEFORE_APPS), disabled, backgroundScope, memory) { now }

    private fun UpNextController.ids() = (state.value?.content as UpNextContent.Items).items.map { it.id }

    @Test
    fun `an opened content moves first only at the next load and its card is returned for focus`() = runTest {
        val items = listOf(movie(1, "A", 40), movie(2, "B", 30), movie(3, "C", 20), movie(4, "D", 10))
        val source = Source(Result.success(items))
        val memory = InMemoryUpNextMemory()
        val controller = controller(source, memory)
        controller.refresh()
        runCurrent()
        controller.opened(items[3])
        runCurrent()
        assertEquals(listOf(1L, 2L, 3L, 4L), controller.ids())
        assertEquals(1_000L, memory.opened.value[UpNextContentKey.of(items[3])])
        controller.refresh()
        runCurrent()
        assertEquals(listOf(4L, 1L, 2L, 3L), controller.ids())
        val target = controller.state.value!!.returnFocus!!
        assertEquals(items[3].key, target.key)
        controller.returnHandled(target.id)
        runCurrent()
        assertNull(controller.state.value!!.returnFocus)
        controller.refresh()
        runCurrent()
        assertNull(controller.state.value!!.returnFocus)
        assertEquals(listOf(4L, 1L, 2L, 3L), controller.ids())
    }

    @Test
    fun `the next episode published under another program still returns the series card`() = runTest {
        val source = Source(Result.success(listOf(movie(1, "A", 50), episode(3, "3"))))
        val controller = controller(source, InMemoryUpNextMemory())
        controller.refresh()
        runCurrent()
        controller.opened(episode(3, "3"))
        source.result = Result.success(listOf(movie(1, "A", 50), episode(4, "4")))
        controller.refresh()
        runCurrent()
        assertEquals(listOf(4L, 1L), controller.ids())
        assertEquals(episode(4, "4").key, controller.state.value!!.returnFocus!!.key)
    }

    @Test
    fun `an opened content no longer published returns no card`() = runTest {
        val source = Source(Result.success(listOf(movie(1, "A", 50), movie(2, "B", 40))))
        val controller = controller(source, InMemoryUpNextMemory())
        controller.refresh()
        runCurrent()
        controller.opened(movie(2, "B", 40))
        source.result = Result.success(listOf(movie(1, "A", 50)))
        controller.refresh()
        runCurrent()
        assertNull(controller.state.value!!.returnFocus)
    }

    @Test
    fun `memory is pruned after a successful load only, from every app before the source filter`() = runTest {
        val kept = movie(1, "Gardé", 10, pkg = "app.off")
        val gone = UpNextContentKey(UpNextContentType.MOVIE, "disparu")
        val memory = InMemoryUpNextMemory().apply { opened.value = mapOf(UpNextContentKey.of(kept) to 1L, gone to 2L) }
        disabled.value = setOf("app.off")
        val source = Source(Result.failure(IllegalStateException()))
        val controller = controller(source, memory)
        controller.refresh()
        runCurrent()
        source.result = Result.failure(TimeoutCancellationExceptionFactory.create())
        controller.refresh()
        runCurrent()
        source.result = Result.failure(SecurityException())
        controller.refresh()
        runCurrent()
        assertEquals(2, memory.opened.value.size)
        source.result = Result.success(listOf(kept))
        controller.refresh()
        runCurrent()
        assertEquals(setOf(UpNextContentKey.of(kept)), memory.opened.value.keys)
    }

    @Test
    fun `a persisted memory orders the very first load and an unreadable memory falls back to the groups`() = runTest {
        val items = listOf(movie(1, "A", 50), movie(2, "B", 40))
        val memory = InMemoryUpNextMemory().apply { opened.value = mapOf(UpNextContentKey.of(items[1]) to 5L) }
        val controller = controller(Source(Result.success(items)), memory)
        controller.refresh()
        runCurrent()
        assertEquals(listOf(2L, 1L), controller.ids())
        val broken = object : UpNextMemory by InMemoryUpNextMemory() {
            override val opened: Flow<Map<UpNextContentKey, Long>> = kotlinx.coroutines.flow.flow { throw IllegalStateException() }
        }
        val fallback = controller(Source(Result.success(items)), broken)
        fallback.refresh()
        runCurrent()
        assertEquals(listOf(1L, 2L), fallback.ids())
    }

    @Test
    fun `removing a content hides it at once for every app and persists it with its sources`() = runTest {
        val first = episode(1, "1", pkg = "app.a")
        val other = episode(2, "2", pkg = "app.b").copy(season = "2")
        val film = movie(3, "Film", 5)
        val memory = InMemoryUpNextMemory()
        val controller = controller(Source(Result.success(listOf(first, other, film))), memory)
        controller.refresh()
        runCurrent()
        assertEquals(3, controller.ids().size)
        controller.remove(first)
        runCurrent()
        assertEquals(listOf(3L), controller.ids())
        val removed = memory.removed.value.single()
        assertEquals("Série", removed.title)
        assertEquals(listOf("app.a"), removed.packages)
        assertEquals(1_000L, removed.removedAt)
        memory.restoreRemoved(removed.key)
        runCurrent()
        assertTrue(controller.ids().containsAll(listOf(1L, 2L)))
    }
}

private object TimeoutCancellationExceptionFactory {
    fun create(): Throwable = runCatching {
        kotlinx.coroutines.runBlocking { kotlinx.coroutines.withTimeout(0) { kotlinx.coroutines.awaitCancellation() } }
    }.exceptionOrNull() as TimeoutCancellationException
}
