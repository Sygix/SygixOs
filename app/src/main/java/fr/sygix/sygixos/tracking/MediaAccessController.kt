/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

enum class MediaAccessState { DISABLED, ACTIVATING, GRANTED, MANUAL, UNAVAILABLE, ERROR }

interface MediaAccessGateway {
    suspend fun granted(): Boolean
    fun privilegedAvailable(): Boolean
    suspend fun activate()
    fun manualAvailable(): Boolean
    fun openManual(): Boolean
}

class MediaAccessController(private val gateway: MediaAccessGateway, private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow(MediaAccessState.DISABLED)
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = -1L
    private var attempted = false
    private var active = false

    fun refresh(accountGeneration: Long, connected: Boolean, enabled: Boolean, retry: Boolean = false) {
        if (job?.isActive == true && generation == accountGeneration && active && connected && enabled && !retry) return
        if (generation != accountGeneration) { generation = accountGeneration; attempted = false }
        active = connected && enabled
        job?.cancel()
        if (!active) { mutable.value = MediaAccessState.DISABLED; return }
        val current = generation
        job = scope.launch {
            try {
                withTimeout(10_000) {
                    if (gateway.granted()) { mutable.value = MediaAccessState.GRANTED; return@withTimeout }
                    if ((!attempted || retry) && gateway.privilegedAvailable()) {
                        attempted = true
                        mutable.value = MediaAccessState.ACTIVATING
                        gateway.activate()
                        if (generation != current || !active) return@withTimeout
                        mutable.value = if (gateway.granted()) MediaAccessState.GRANTED else MediaAccessState.ERROR
                    } else mutable.value = if (gateway.manualAvailable()) MediaAccessState.MANUAL else MediaAccessState.UNAVAILABLE
                }
            } catch (_: TimeoutCancellationException) { if (generation == current && active) mutable.value = MediaAccessState.ERROR }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (generation == current && active) mutable.value = MediaAccessState.ERROR }
        }
    }

    fun openManual(): Boolean = active && gateway.openManual()
}

data class PlaybackEvidence(val content: TrackingContent, val generation: Long, val playbackId: Long, val adapter: String, val exactIdentity: Boolean, val completed: Boolean, val packageName: String = "")

interface PlaybackEvidenceSource {
    val evidence: kotlinx.coroutines.flow.Flow<PlaybackEvidence>
    fun start(request: PlaybackRequest, packageName: String, generation: Long, playbackId: Long) {}
    fun stop()
}

class PlaybackEvidenceGate(private val verifiedAdapters: Set<String>) {
    private var current: Triple<TrackingIdentity, Long, Long>? = null
    private var consumed = false
    private var packageName: String? = null
    fun start(content: TrackingIdentity, generation: Long, playbackId: Long, packageName: String? = null) { current = Triple(content, generation, playbackId); this.packageName = packageName; consumed = false }
    fun stop() { current = null; packageName = null; consumed = false }
    fun accept(evidence: PlaybackEvidence): Boolean {
        if (consumed || evidence.adapter !in verifiedAdapters || !evidence.exactIdentity || !evidence.completed || packageName?.let { it != evidence.packageName } == true || current != Triple(evidence.content.identity, evidence.generation, evidence.playbackId)) return false
        consumed = true
        return true
    }
}
