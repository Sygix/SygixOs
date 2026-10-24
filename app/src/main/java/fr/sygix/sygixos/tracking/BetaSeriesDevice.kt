/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceAssociation(
    internal val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val expiresAt: Long,
    val intervalMillis: Long,
) {
    override fun toString() = "DeviceAssociation(redacted)"
}

sealed interface DeviceAssociationState {
    data object Closed : DeviceAssociationState
    data object Preparing : DeviceAssociationState
    class Waiting(val association: DeviceAssociation) : DeviceAssociationState
    data object Expired : DeviceAssociationState
    data object Connected : DeviceAssociationState
    data class Error(val failure: TrackingFailure) : DeviceAssociationState
}

class BetaSeriesDevice(
    private val api: BetaSeriesApi,
    private val controller: TrackingController,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val mutable = MutableStateFlow<DeviceAssociationState>(DeviceAssociationState.Closed)
    val state = mutable.asStateFlow()
    private var work: Job? = null
    private var attempt = 0L
    private var visible = true

    fun open() {
        close()
        val current = attempt
        mutable.value = DeviceAssociationState.Preparing
        work = scope.launch {
            try {
                val response = api.request("oauth/device", method = "POST")
                val device = response.text("device_code") ?: throw TrackingException(TrackingFailure.PROTOCOL)
                val user = response.text("user_code") ?: throw TrackingException(TrackingFailure.PROTOCOL)
                val url = response.text("verification_url")?.takeIf(::officialTrackingUrl) ?: throw TrackingException(TrackingFailure.PROTOCOL)
                if (java.net.URI(url).path.isNullOrBlank() || java.net.URI(url).path == "/") throw TrackingException(TrackingFailure.PROTOCOL)
                val expiry = response.optLong("expires_in").takeIf { it in 1..3600 } ?: throw TrackingException(TrackingFailure.PROTOCOL)
                val interval = response.optLong("interval").takeIf { it in 1..300 } ?: throw TrackingException(TrackingFailure.PROTOCOL)
                val association = DeviceAssociation(device, user, url, clock() + expiry * 1000, interval * 1000)
                if (attempt != current) return@launch
                mutable.value = DeviceAssociationState.Waiting(association)
                while (current == attempt && clock() < association.expiresAt) {
                    delay(minOf(association.intervalMillis, (association.expiresAt - clock()).coerceAtLeast(1)))
                    if (!visible || clock() >= association.expiresAt) continue
                    try {
                        val access = api.request("oauth/access_token", method = "POST", fields = mapOf("code" to association.deviceCode))
                        val token = access.text("access_token") ?: throw TrackingException(TrackingFailure.PROTOCOL)
                        if (attempt != current) return@launch
                        controller.connect(token) { attempt == current }
                        if (attempt == current) mutable.value = if (controller.state.value.account != null) DeviceAssociationState.Connected else DeviceAssociationState.Error(controller.state.value.failure ?: TrackingFailure.ACCESS)
                        return@launch
                    } catch (error: BetaSeriesApiException) { if (error.code != 2001) throw error }
                    catch (error: TrackingException) { if (error.failure !in setOf(TrackingFailure.NETWORK, TrackingFailure.PROTOCOL)) throw error }
                }
                if (current == attempt) mutable.value = DeviceAssociationState.Expired
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (attempt == current) mutable.value = DeviceAssociationState.Error(error.trackingFailure()) }
        }
    }

    fun setVisible(value: Boolean) { visible = value }
    fun close() { ++attempt; work?.cancel(); work = null; mutable.value = DeviceAssociationState.Closed }
}
