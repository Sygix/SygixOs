/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.TsuyuLink
import fr.sygix.sygixos.model.AdbAccessState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

class SwitchingAccess(
    private val link: TsuyuLink,
    private val tsuyu: AdbAccess,
    private val adb: AdbAccess,
) : AdbAccess {
    private val chosen = MutableStateFlow(adb)

    @Volatile var usingTsuyu: Boolean = false
        private set

    @OptIn(ExperimentalForInheritanceCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    override val state: StateFlow<AdbAccessState> = object : StateFlow<AdbAccessState> {
        override val value: AdbAccessState get() = chosen.value.state.value
        override val replayCache: List<AdbAccessState> get() = listOf(value)
        override suspend fun collect(collector: FlowCollector<AdbAccessState>): Nothing {
            chosen.flatMapLatest { it.state }.distinctUntilChanged().collect(collector)
            awaitCancellation()
        }
    }

    fun tsuyuActive(): Boolean = runCatching { link.probe().active }.getOrDefault(false)

    override fun debugEnabled(): Boolean = tsuyuActive() || adb.debugEnabled()
    override fun previouslyAuthorized(): Boolean = tsuyuActive() || adb.previouslyAuthorized()

    override suspend fun connect(): Result<Unit> {
        val useTsuyu = tsuyuActive()
        val next = if (useTsuyu) tsuyu else adb
        val previous = chosen.value
        usingTsuyu = useTsuyu
        chosen.value = next
        if (previous !== next) previous.close()
        return next.connect()
    }

    override suspend fun execute(command: String): Result<String> = chosen.value.execute(command)

    override fun close() = chosen.value.close()
}
