/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    private lateinit var dispatcher: TestDispatcher
    private val jobs = mutableListOf<Job>()

    fun <T : ViewModel> track(model: T): T = model.also {
        jobs += requireNotNull(it.viewModelScope.coroutineContext[Job])
    }

    override fun starting(description: Description) {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        try {
            runTest(dispatcher) {
                jobs.forEach { it.cancel() }
                jobs.joinAll()
            }
        } finally {
            jobs.clear()
            Dispatchers.resetMain()
        }
    }
}

suspend fun <T> StateFlow<T>.await(timeoutMillis: Long = 5_000, predicate: (T) -> Boolean): T =
    withTimeout(timeoutMillis) { first(predicate) }
