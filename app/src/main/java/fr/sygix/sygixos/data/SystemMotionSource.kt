/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ContentResolver
import android.provider.Settings

interface SystemMotionSource {
    fun animationsEnabled(): Boolean
}

class SettingsMotionSource(private val resolver: ContentResolver) : SystemMotionSource {
    override fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
}
