/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.StockHomeState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenStateReaderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun install(packageName: String) = shadowOf(context.packageManager).installPackage(PackageInfo().apply {
        this.packageName = packageName
        applicationInfo = ApplicationInfo().apply { this.packageName = packageName; enabled = true }
    })

    @Test fun missingStockPackagesAreAbsenceNotFailedReading() = runTest {
        val state = HomeScreenStateReader(context).read().getOrThrow()
        assertTrue(state.stock.values.all { it == StockHomeState.ABSENT })
        assertFalse(state.canDisable)
    }

    @Test fun disabledUserDiffersFromSystemDisabledAndCanBeRestored() = runTest {
        StockHomePackages.forEach(::install)
        context.packageManager.setApplicationEnabledSetting(StockHomePackages.first(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0)
        context.packageManager.setApplicationEnabledSetting(StockHomePackages.last(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0)
        val state = HomeScreenStateReader(context).read().getOrThrow()
        assertEquals(StockHomeState.DISABLED_USER, state.stock[StockHomePackages.first()])
        assertEquals(StockHomeState.OUT_OF_REACH, state.stock[StockHomePackages.last()])
        assertTrue(state.canRestore)
        assertFalse(state.canDisable)
    }

    @Test fun failedPackageReadIsNotEmptyOrActive() = runTest {
        val denied = object : ContextWrapper(context) {
            override fun getPackageManager(): PackageManager = throw SecurityException()
        }
        val result = HomeScreenStateReader(denied).read()
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test fun disabledOwnHomeCannotPassDisableGuardsEvenWhenProvisioned() = runTest {
        StockHomePackages.forEach(::install)
        context.packageManager.setComponentEnabledSetting(ComponentName(context.packageName, "fr.sygix.sygixos.ui.MainActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0)
        Settings.Global.putInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
        Settings.Secure.putInt(context.contentResolver, "user_setup_complete", 1)
        val state = HomeScreenStateReader(context).read().getOrThrow()
        assertTrue(state.provisioned)
        assertFalse(state.ownHomeEnabled)
        assertFalse(state.canDisable)
    }

    @Test fun higherPriorityStockPreventsEffectiveHomeEvenWithOwnResolution() = runTest {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        fun home(packageName: String, priority: Int) = ResolveInfo().apply {
            this.priority = priority
            isDefault = true
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.Home"
                enabled = true
                applicationInfo = ApplicationInfo().apply { this.packageName = packageName; enabled = true }
            }
        }
        shadowOf(context.packageManager).setResolveInfosForIntent(intent, listOf(home(context.packageName, 0), home(StockHomePackages.last(), 2)))
        assertFalse(HomeScreenStateReader(context).read().getOrThrow().effective)
    }

    @Test fun packageReadsRunOffTheMainThread() = runTest {
        var mainThread: Boolean? = null
        val observed = object : ContextWrapper(context) {
            override fun getPackageManager(): PackageManager {
                mainThread = Looper.myLooper() == Looper.getMainLooper()
                return super.getPackageManager()
            }
        }
        HomeScreenStateReader(observed).read().getOrThrow()
        assertEquals(false, mainThread)
    }
}
