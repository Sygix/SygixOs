/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpNextControllerTest {
    private val item = UpNextItem(1, UpNextSourceEntry("a"), UpNextContentType.MOVIE, "Film")
    private class Source(var result: Result<List<UpNextItem>>) : UpNextSource {
        val signals = MutableSharedFlow<Unit>()
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun load(): Result<List<UpNextItem>> { calls++; gate?.await(); return result }
        override fun changes(): Flow<Unit> = signals
    }

    @Test
    fun `signals reload retaining previous on failure and successful empty removes row`() = runTest {
        val source = Source(Result.success(listOf(item)))
        val controller = UpNextController(source, MutableStateFlow(true), MutableStateFlow(UpNextPosition.BEFORE_APPS), MutableStateFlow(emptySet()), backgroundScope)
        backgroundScope.launch { controller.updates.collect() }
        runCurrent()
        source.signals.emit(Unit)
        runCurrent()
        val previous = controller.state.value
        assertTrue(previous?.content is UpNextContent.Items)
        source.result = Result.failure(IllegalStateException())
        source.signals.emit(Unit)
        runCurrent()
        assertEquals(2, source.calls)
        assertEquals(previous, controller.state.value)
        source.result = Result.success(emptyList())
        controller.refresh()
        runCurrent()
        assertNull(controller.state.value)
    }

    @Test
    fun `provider changes are observed only while updates are collected`() = runTest {
        val source = Source(Result.success(listOf(item)))
        val controller = UpNextController(source, MutableStateFlow(true), MutableStateFlow(UpNextPosition.BEFORE_APPS), MutableStateFlow(emptySet()), backgroundScope)
        runCurrent()
        assertEquals(0, source.signals.subscriptionCount.value)
        val watching = backgroundScope.launch { controller.updates.collect() }
        runCurrent()
        assertEquals(1, source.signals.subscriptionCount.value)
        source.signals.emit(Unit)
        runCurrent()
        assertEquals(1, source.calls)
        watching.cancel()
        runCurrent()
        assertEquals(0, source.signals.subscriptionCount.value)
        source.signals.emit(Unit)
        runCurrent()
        assertEquals(1, source.calls)
    }

    @Test
    fun `visibility and sources are reactive position remains selected while hidden`() = runTest {
        val source = Source(Result.success(listOf(item, item.copy(id = 2, source = UpNextSourceEntry("b"), sources = listOf(UpNextSourceEntry("b"))))))
        val visible = MutableStateFlow(true)
        val position = MutableStateFlow(UpNextPosition.BEFORE_APPS)
        val disabled = MutableStateFlow(emptySet<String>())
        val controller = UpNextController(source, visible, position, disabled, backgroundScope)
        controller.refresh()
        runCurrent()
        assertEquals(2, (controller.state.value!!.content as UpNextContent.Items).items.single().sources.size)
        disabled.value = setOf("a")
        runCurrent()
        assertEquals("b", (controller.state.value!!.content as UpNextContent.Items).items.single().source.packageName)
        visible.value = false
        position.value = UpNextPosition.AFTER_APPS
        runCurrent()
        assertNull(controller.state.value)
        visible.value = true
        runCurrent()
        assertEquals(UpNextPosition.AFTER_APPS, controller.state.value?.position)
        disabled.value = setOf("a", "b")
        runCurrent()
        assertNull(controller.state.value)
    }

    @Test
    fun `denied permission hides row without error and a later grant reloads`() = runTest {
        val source = Source(Result.failure(SecurityException()))
        val controller = UpNextController(source, MutableStateFlow(true), MutableStateFlow(UpNextPosition.BEFORE_APPS), MutableStateFlow(emptySet()), backgroundScope)
        controller.refresh()
        runCurrent()
        assertNull(controller.state.value)
        source.result = Result.success(listOf(item))
        controller.refresh()
        runCurrent()
        assertTrue(controller.state.value?.content is UpNextContent.Items)
    }

    @Test
    fun `first load skeleton only appears after 300ms and initial failure is retryable`() = runTest {
        val source = Source(Result.failure(IllegalStateException())).apply { gate = CompletableDeferred() }
        val controller = UpNextController(source, MutableStateFlow(true), MutableStateFlow(UpNextPosition.BEFORE_APPS), MutableStateFlow(emptySet()), backgroundScope)
        controller.refresh()
        runCurrent()
        assertEquals(UpNextContent.Pending, controller.state.value?.content)
        advanceTimeBy(299)
        runCurrent()
        assertEquals(UpNextContent.Pending, controller.state.value?.content)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(UpNextContent.Skeleton, controller.state.value?.content)
        source.gate!!.complete(Unit)
        runCurrent()
        assertEquals(UpNextContent.Error, controller.state.value?.content)
        source.result = Result.success(listOf(item))
        controller.refresh()
        runCurrent()
        assertTrue(controller.state.value?.content is UpNextContent.Items)
    }
}
