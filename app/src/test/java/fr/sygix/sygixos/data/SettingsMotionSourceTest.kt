/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.animation.ValueAnimator
import android.content.ContentResolver
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsMotionSourceTest {

    private val resolver: ContentResolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver

    private fun setAnimatorScale(scale: Float) {
        ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType).invoke(null, scale)
    }

    @After
    fun restore() = setAnimatorScale(1f)

    @Test
    fun `animations stay enabled when only the app animator scale is forced to zero`() {
        setAnimatorScale(0f)
        assertFalse(ValueAnimator.areAnimatorsEnabled())
        assertTrue(SettingsMotionSource(resolver).animationsEnabled())
    }

    @Test
    fun `animations follow the system animator duration scale`() {
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertFalse(SettingsMotionSource(resolver).animationsEnabled())
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.5f)
        assertTrue(SettingsMotionSource(resolver).animationsEnabled())
    }
}
