/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {
    @Test fun `suspending cleanup on Main and on the shared startup scheduler completes before Main is reset`() {
        val scheduler = TestCoroutineScheduler()
        val rule = MainDispatcherRule(UnconfinedTestDispatcher(scheduler))
        val cleaned = mutableListOf<String>()
        val statement = object : Statement() {
            override fun evaluate() {
                val scope = rule.track(object : ViewModel() {}).viewModelScope
                mapOf("main" to EmptyCoroutineContext, "startup" to StandardTestDispatcher(scheduler)).forEach { (name, context) ->
                    scope.launch(context) {
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) {
                                delay(1)
                                cleaned += name
                            }
                        }
                    }
                }
                scheduler.runCurrent()
            }
        }
        rule.apply(statement, Description.EMPTY).evaluate()
        assertEquals(setOf("main", "startup"), cleaned.toSet())
    }
}
