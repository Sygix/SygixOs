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
    internal val clientId: String? = null,
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

sealed interface TrackingDevicePoll {
    data object Pending : TrackingDevicePoll
    data object Expired : TrackingDevicePoll
    data class SlowDown(val intervalMillis: Long) : TrackingDevicePoll
    class Authorized(val token: String) : TrackingDevicePoll {
        override fun toString() = "Authorized(redacted)"
    }
}

interface TrackingDeviceProtocol {
    suspend fun prepare(): DeviceAssociation
    suspend fun poll(association: DeviceAssociation): TrackingDevicePoll
}

class BetaSeriesDeviceProtocol(private val api: BetaSeriesApi, private val clock: () -> Long) : TrackingDeviceProtocol {
    override suspend fun prepare(): DeviceAssociation {
        val response = api.request("oauth/device", method = "POST")
        val device = response.text("device_code") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val user = response.text("user_code") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val url = response.text("verification_url")?.takeIf(::officialTrackingUrl) ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val uri = java.net.URI(url)
        if (uri.host !in setOf("www.betaseries.com", "betaseries.com") || uri.path.isNullOrBlank() || uri.path == "/") throw TrackingException(TrackingFailure.PROTOCOL)
        val expiry = response.optLong("expires_in").takeIf { it in 1..3600 } ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val interval = response.optLong("interval").takeIf { it in 1..300 } ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return DeviceAssociation(device, user, url, clock() + expiry * 1000, interval * 1000)
    }

    override suspend fun poll(association: DeviceAssociation): TrackingDevicePoll = try {
        val access = api.request("oauth/access_token", method = "POST", fields = mapOf("code" to association.deviceCode))
        TrackingDevicePoll.Authorized(access.text("access_token") ?: throw TrackingException(TrackingFailure.PROTOCOL))
    } catch (error: BetaSeriesApiException) {
        if (error.code == 2001) TrackingDevicePoll.Pending else throw error
    }
}

class BetaSeriesDevice(api: BetaSeriesApi, controller: TrackingController, scope: CoroutineScope,
    clock: () -> Long = { System.nanoTime() / 1_000_000 },
) : TrackingDeviceFlow({ BetaSeriesDeviceProtocol(api, clock) }, controller, scope, clock)

open class TrackingDeviceFlow(
    private val protocol: () -> TrackingDeviceProtocol,
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
                val source = protocol()
                val association = source.prepare()
                var interval = association.intervalMillis
                if (attempt != current) return@launch
                mutable.value = DeviceAssociationState.Waiting(association)
                while (current == attempt && clock() < association.expiresAt) {
                    delay(minOf(interval, (association.expiresAt - clock()).coerceAtLeast(1)))
                    if (!visible || clock() >= association.expiresAt) continue
                    try {
                        when (val result = source.poll(association)) {
                            TrackingDevicePoll.Pending -> Unit
                            TrackingDevicePoll.Expired -> {
                                if (attempt == current) mutable.value = DeviceAssociationState.Expired
                                return@launch
                            }
                            is TrackingDevicePoll.SlowDown -> interval = maxOf(interval + 5_000, result.intervalMillis)
                            is TrackingDevicePoll.Authorized -> {
                                controller.connect(result.token) { attempt == current }
                                if (attempt == current) mutable.value = if (controller.state.value.account != null) DeviceAssociationState.Connected else DeviceAssociationState.Error(controller.state.value.failure ?: TrackingFailure.ACCESS)
                                return@launch
                            }
                        }
                    } catch (error: TrackingException) { if (error.failure !in setOf(TrackingFailure.NETWORK, TrackingFailure.PROTOCOL)) throw error }
                }
                if (current == attempt) mutable.value = DeviceAssociationState.Expired
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (attempt == current) mutable.value = DeviceAssociationState.Error(error.trackingFailure()) }
        }
    }

    fun setVisible(value: Boolean) { visible = value }
    fun close() { ++attempt; work?.cancel(); work = null; mutable.value = DeviceAssociationState.Closed }
}
