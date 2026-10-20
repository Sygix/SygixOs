/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import org.robolectric.Shadows.shadowOf

internal fun PackageManager.installLeanbackActivity(info: ResolveInfo) {
    val activity = requireNotNull(info.activityInfo)
    activity.enabled = true
    activity.exported = true
    if (info.nonLocalizedLabel != null) activity.nonLocalizedLabel = info.nonLocalizedLabel
    activity.applicationInfo.apply {
        enabled = true
        flags = flags or ApplicationInfo.FLAG_INSTALLED
        if (processName == null) processName = packageName
    }
    val manager = shadowOf(this)
    manager.addOrUpdateActivity(activity)
    manager.addIntentFilterForActivity(ComponentName(activity.packageName, activity.name), IntentFilter(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        addCategory(Intent.CATEGORY_DEFAULT)
    })
}
