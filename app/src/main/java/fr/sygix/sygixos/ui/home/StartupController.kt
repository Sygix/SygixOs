/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import fr.sygix.sygixos.domain.StartupGate
import fr.sygix.sygixos.domain.StartupPhase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartupController(
    coldStart: Boolean,
    private val gate: StartupGate,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    private val _phase = MutableStateFlow(if (coldStart) StartupPhase.Splash else StartupPhase.Done)
    val phase: StateFlow<StartupPhase> = _phase.asStateFlow()
    private var timer: Job? = null

    fun splashShown() = record(gate::splashShown)

    fun catalogReady() = record(gate::catalogReady)

    fun heroVisualReady() = record(gate::heroVisualReady)

    private fun record(event: () -> Unit) {
        if (_phase.value == StartupPhase.Done) return
        event()
        evaluate()
    }

    private fun evaluate() {
        timer?.cancel()
        _phase.value = gate.phase()
        val wait = gate.millisUntilChange() ?: return
        timer = scope.launch(dispatcher) {
            delay(wait)
            evaluate()
        }
    }
}
