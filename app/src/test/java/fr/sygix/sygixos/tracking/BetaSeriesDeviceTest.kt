/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BetaSeriesDeviceTest {
    @Test fun transientHtmlAndNetworkPollsKeepAssociationUntilSuccessOrExpiry() = runTest {
        for (success in listOf(true, false)) {
            var polls = 0
            val api = trackingApi(TrackingTransport { request -> when {
                request.url.endsWith("/") -> TrackingResponse(200, """{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}""")
                request.url.endsWith("oauth/device") -> TrackingResponse(200, """{"device_code":"fixture-device","user_code":"FIXTURE","verification_url":"https://www.betaseries.com/device","expires_in":8,"interval":1}""")
                else -> when (++polls) {
                    1 -> TrackingResponse(200, "<html>Temporary response</html>")
                    2 -> throw TrackingException(TrackingFailure.NETWORK)
                    else -> if (success) TrackingResponse(200, """{"access_token":"fixture-token"}""") else throw TrackingException(TrackingFailure.NETWORK)
                }
            } })
            val device = BetaSeriesDevice(api, controller(backgroundScope), backgroundScope) { testScheduler.currentTime }
            device.open(); runCurrent()
            repeat(2) { advanceTimeBy(1000); runCurrent(); assertTrue(device.state.value is DeviceAssociationState.Waiting) }
            advanceTimeBy(6000); runCurrent()
            assertEquals(if (success) DeviceAssociationState.Connected else DeviceAssociationState.Expired, device.state.value)
            assertEquals(if (success) 3 else 7, polls)
        }
    }

    @Test fun pollingWaitsServerIntervalSuspendsInBackgroundAndClosesPermanently() = runTest {
        var polls = 0
        val transport = TrackingTransport { request ->
            when {
                request.url.endsWith("/") -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
                request.url.endsWith("oauth/device") -> TrackingResponse(200, "{\"device_code\":\"fixture-device\",\"user_code\":\"FIXTURE\",\"verification_url\":\"https://www.betaseries.com/device\",\"expires_in\":10,\"interval\":2}")
                else -> { ++polls; TrackingResponse(400, "{\"errors\":[{\"code\":2001}]}") }
            }
        }
        val api = trackingApi(transport)
        val device = BetaSeriesDevice(api, controller(backgroundScope), backgroundScope) { testScheduler.currentTime }
        device.open(); runCurrent()
        val waiting = device.state.value as DeviceAssociationState.Waiting
        assertEquals("https://www.betaseries.com/device", waiting.association.verificationUrl)
        assertEquals("FIXTURE", waiting.association.userCode)
        advanceTimeBy(1999); runCurrent(); assertEquals(0, polls)
        advanceTimeBy(1); runCurrent(); assertEquals(1, polls)
        device.setVisible(false)
        advanceTimeBy(4000); runCurrent(); assertEquals(1, polls)
        device.setVisible(true)
        advanceTimeBy(2000); runCurrent(); assertEquals(2, polls)
        device.close()
        advanceTimeBy(20_000); runCurrent()
        assertEquals(2, polls)
        assertEquals(DeviceAssociationState.Closed, device.state.value)
    }

    @Test fun expiredCodeStopsPollingAndInvalidVerificationUrlNeverFallsBackToDomain() = runTest {
        var polls = 0
        var valid = true
        val transport = TrackingTransport { request ->
            when {
                request.url.endsWith("/") -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
                request.url.endsWith("oauth/device") -> TrackingResponse(200, "{\"device_code\":\"fixture-device\",\"user_code\":\"FIXTURE\",\"verification_url\":\"https://www.betaseries.com/${if (valid) "device" else ""}\",\"expires_in\":2,\"interval\":1}")
                else -> { ++polls; TrackingResponse(400, "{\"errors\":[{\"code\":2001}]}") }
            }
        }
        val device = BetaSeriesDevice(trackingApi(transport), controller(backgroundScope), backgroundScope) { testScheduler.currentTime }
        device.open(); runCurrent(); advanceTimeBy(5000); runCurrent()
        assertEquals(1, polls)
        assertEquals(DeviceAssociationState.Expired, device.state.value)
        valid = false
        device.open(); runCurrent()
        assertEquals(DeviceAssociationState.Error(TrackingFailure.PROTOCOL), device.state.value)
        assertEquals(1, polls)
    }

    private fun controller(scope: kotlinx.coroutines.CoroutineScope) = TrackingController(object : WatchTrackingProvider {
        override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
        override suspend fun snapshot(token: String) = TrackingSnapshot(emptyList())
        override suspend fun revoke(token: String) {}
    }, object : TrackingSessionStore {
        override suspend fun read(): String? = null
        override suspend fun write(token: String?) {}
    }, scope)

    @Test fun confirmedAccountFinishesAssociationAndStopsPolling() = runTest {
        var polls = 0
        val transport = TrackingTransport { request -> when {
            request.url.endsWith("/") -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
            request.url.endsWith("oauth/device") -> TrackingResponse(200, "{\"device_code\":\"fixture-device\",\"user_code\":\"FIXTURE\",\"verification_url\":\"https://www.betaseries.com/device\",\"expires_in\":10,\"interval\":1}")
            else -> { ++polls; assertEquals("fixture-device", request.fields["code"]); assertFalse(request.fields.containsKey("device_code")); TrackingResponse(200, "{\"access_token\":\"fixture-token\",\"token_type\":\"Bearer\"}") }
        } }
        val controller = controller(backgroundScope)
        val device = BetaSeriesDevice(trackingApi(transport), controller, backgroundScope) { testScheduler.currentTime }
        device.open(); runCurrent()
        assertNull(controller.state.value.account)
        advanceTimeBy(1000); runCurrent()
        assertEquals(DeviceAssociationState.Connected, device.state.value)
        assertEquals("fixture", controller.state.value.account?.id)
        advanceTimeBy(20_000); runCurrent()
        assertEquals(1, polls)
    }

    @Test fun consumedCodeStopsPollingAndRepeatedRetryKeepsOnlyOneAttempt() = runTest {
        var attempts = 0
        var polls = 0
        val transport = TrackingTransport { request -> when {
            request.url.endsWith("/") -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
            request.url.endsWith("oauth/device") -> { ++attempts; TrackingResponse(200, "{\"device_code\":\"fixture-device-$attempts\",\"user_code\":\"FIXTURE-$attempts\",\"verification_url\":\"https://www.betaseries.com/device\",\"expires_in\":30,\"interval\":1}") }
            else -> { ++polls; TrackingResponse(400, "{\"errors\":[{\"code\":2002}]}") }
        } }
        val device = BetaSeriesDevice(trackingApi(transport), controller(backgroundScope), backgroundScope) { testScheduler.currentTime }
        device.open(); runCurrent(); advanceTimeBy(1000); runCurrent()
        assertEquals(DeviceAssociationState.Error(TrackingFailure.ACCESS), device.state.value)
        advanceTimeBy(10_000); runCurrent(); assertEquals(1, polls)
        device.open(); device.open(); runCurrent()
        assertEquals(2, attempts)
        assertEquals("FIXTURE-2", (device.state.value as DeviceAssociationState.Waiting).association.userCode)
        advanceTimeBy(1000); runCurrent(); assertEquals(2, polls)
    }
}
