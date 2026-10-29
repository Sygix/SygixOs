/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.json.JSONObject

class TraktDeviceProtocol(
    private val api: TraktApi,
    private val monotonicClock: () -> Long = { System.nanoTime() / 1_000_000 },
    private val wallClock: () -> Long = System::currentTimeMillis,
) : TrackingDeviceProtocol {
    override suspend fun prepare(): DeviceAssociation {
        val configuration = api.configuration()
        val result = try {
            api.request("oauth/device/code", method = "POST", oauth = true, clientOverride = configuration.clientId,
                body = JSONObject().put("client_id", configuration.clientId))
        } catch (error: TraktResponseException) {
            if (error.errorCode == "invalid_client") api.invalidateConfiguration(configuration)
            throw error
        }
        val code = result.text("device_code") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val user = result.text("user_code") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val url = result.text("verification_url")?.takeIf { officialTrackingUrl(it) }
            ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)
        if (uri.host !in setOf("trakt.tv", "auth.trakt.tv") || uri.path.isNullOrBlank() || uri.path == "/") throw TrackingException(TrackingFailure.PROTOCOL)
        val expiry = result.optLong("expires_in", -1).takeIf { it in 1..3600 } ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val interval = result.optLong("interval", -1).takeIf { it in 1..300 } ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val wallExpiry = wallClock() + expiry * 1000
        val monotonicExpiry = monotonicClock() + (wallExpiry - wallClock()).coerceAtLeast(0)
        return DeviceAssociation(code, user, url, monotonicExpiry, interval * 1000, configuration.clientId)
    }

    override suspend fun poll(association: DeviceAssociation): TrackingDevicePoll {
        if (monotonicClock() >= association.expiresAt) return TrackingDevicePoll.Expired
        return try {
            val client = association.clientId ?: throw TrackingException(TrackingFailure.CONFIGURATION)
            val session = api.exchange(association.deviceCode, client)
            TrackingDevicePoll.Authorized(session.encode())
        } catch (error: TraktResponseException) {
            when (error.errorCode) {
                "authorization_pending" -> TrackingDevicePoll.Pending
                "slow_down" -> TrackingDevicePoll.SlowDown(association.intervalMillis + 5_000)
                "expired_token" -> TrackingDevicePoll.Expired
                "access_denied" -> throw TrackingException(TrackingFailure.ACCESS)
                else -> throw error
            }
        }
    }
}

internal suspend fun TraktApi.configuration(): TraktConfiguration = configured()
