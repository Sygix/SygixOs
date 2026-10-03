/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Intent
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SystemClockSourceTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val evening = 21L * 3_600_000 + 47L * 60_000
    private val defaultZone = TimeZone.getDefault()
    private val defaultLocale = Locale.getDefault()

    @Before
    fun utc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        Locale.setDefault(Locale.FRANCE)
    }

    @After
    fun restore() {
        TimeZone.setDefault(defaultZone)
        Locale.setDefault(defaultLocale)
    }

    private fun use24Hour(enabled: Boolean) {
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, if (enabled) "24" else "12")
    }

    @Test
    fun `system 24 hour setting gives a 24 hour time`() = runTest {
        use24Hour(true)
        val source = SystemClockSource(context) { evening }
        assertEquals("21:47", source.current())
        assertEquals("21:47", source.time().first())
    }

    @Test
    fun `system 12 hour setting gives a 12 hour time`() = runTest {
        use24Hour(false)
        val time = SystemClockSource(context) { evening }.time().first()
        assertTrue(time, time.startsWith("9:47"))
        assertFalse(time, time.contains("21"))
    }

    @Test
    fun `time is emitted at once then on every time broadcast`() = runTest {
        use24Hour(true)
        var now = evening
        val source = SystemClockSource(context) { now }
        val values = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { source.time().take(3).toList(values) }
        assertEquals(listOf("21:47"), values)
        now += 60_000
        context.sendBroadcast(Intent(Intent.ACTION_TIME_TICK))
        shadowOf(Looper.getMainLooper()).idle()
        now += 3_600_000
        context.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
        shadowOf(Looper.getMainLooper()).idle()
        job.join()
        assertEquals(listOf("21:47", "21:48", "22:48"), values)
    }
}
