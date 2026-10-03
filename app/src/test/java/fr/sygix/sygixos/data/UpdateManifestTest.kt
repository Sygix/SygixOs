/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.ui.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateManifestTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `main activity is a home screen candidate`() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName)
        val activities = context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
        assertTrue(activities.any { it.activityInfo.name == MainActivity::class.java.name })
    }

    @Test
    fun `install permissions are requested`() {
        val requested = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue("android.permission.REQUEST_INSTALL_PACKAGES" in requested)
        assertTrue("android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION" in requested)
    }

    @Test
    fun `install status receiver is not exported`() {
        val receivers = context.packageManager.queryBroadcastReceivers(Intent(context, InstallStatusReceiver::class.java), 0)
        assertTrue(receivers.isNotEmpty())
        assertTrue(receivers.none { it.activityInfo.exported })
    }
}
