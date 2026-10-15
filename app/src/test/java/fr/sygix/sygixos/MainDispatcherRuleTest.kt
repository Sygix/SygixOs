/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MainDispatcherRuleTest {
    @Test fun `suspending cleanup completes before Main is reset`() {
        val rule = MainDispatcherRule()
        var cleaned = false
        val statement = object : Statement() {
            override fun evaluate() {
                rule.track(object : ViewModel() {}).viewModelScope.launch {
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) {
                            delay(1)
                            cleaned = true
                        }
                    }
                }
            }
        }
        rule.apply(statement, Description.createTestDescription(javaClass, "cleanup")).evaluate()
        assertTrue(cleaned)
    }
}
