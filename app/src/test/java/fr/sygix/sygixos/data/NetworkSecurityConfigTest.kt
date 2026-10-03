/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkSecurityConfigTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun cleartextPermitted(host: String): Boolean {
        val info = ApplicationInfo(context.applicationInfo).apply { targetSdkVersion = 37 }
        val source = Class.forName("android.security.net.config.XmlConfigSource")
            .getConstructor(Context::class.java, Int::class.javaPrimitiveType, ApplicationInfo::class.java)
            .newInstance(context, R.xml.network_security_config, info)
        val configSource = Class.forName("android.security.net.config.ConfigSource")
        val config = Class.forName("android.security.net.config.ApplicationConfig")
            .getConstructor(configSource)
            .newInstance(source)
        return config.javaClass.getMethod("isCleartextTrafficPermitted", String::class.java).invoke(config, host) as Boolean
    }

    @Test
    fun `cleartext is refused for the github domains`() {
        listOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com").forEach {
            assertFalse(it, cleartextPermitted(it))
        }
    }

    @Test
    fun `cleartext stays allowed for posters on the local network`() {
        assertTrue(cleartextPermitted("nas.local"))
        assertTrue(cleartextPermitted("jellyfin.local"))
    }
}
