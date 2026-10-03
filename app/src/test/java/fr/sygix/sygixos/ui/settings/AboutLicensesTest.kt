/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.R
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AboutLicensesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `zxing core is listed with its apache licence`() {
        val json = JSONObject(context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() })
        val libraries = json.getJSONArray("libraries")
        val zxing = (0 until libraries.length()).map { libraries.getJSONObject(it) }
            .firstOrNull { it.optString("uniqueId") == "com.google.zxing:core" }
        assertNotNull(zxing)
        assertEquals(listOf("Apache-2.0"), (0 until zxing!!.getJSONArray("licenses").length()).map { zxing.getJSONArray("licenses").getString(it) })
    }
}
