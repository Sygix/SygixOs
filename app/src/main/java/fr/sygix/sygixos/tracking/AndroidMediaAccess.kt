/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import fr.sygix.sygixos.data.AdbAccess
import fr.sygix.sygixos.data.HomeScreenStateReader
import fr.sygix.sygixos.data.PrivilegedCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrackingNotificationService : NotificationListenerService()

class AndroidMediaAccess(private val context: Context, private val access: AdbAccess?) : MediaAccessGateway {
    private val component = ComponentName(context, TrackingNotificationService::class.java)
    override suspend fun granted(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(component) &&
                context.getSystemService(MediaSessionManager::class.java).getActiveSessions(component).let { true }
        }.getOrDefault(false)
    }
    override fun privilegedAvailable(): Boolean = access?.previouslyAuthorized() == true
    override suspend fun activate() {
        val gateway = access ?: throw TrackingException(TrackingFailure.ACCESS)
        gateway.connect().getOrThrow()
        gateway.execute(PrivilegedCommands.trackingListener(HomeScreenStateReader.currentUser())).getOrThrow()
    }

    private fun manualIntent(): Intent? {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        return listOf(detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)).firstOrNull { it.resolveActivity(context.packageManager) != null }
    }
    override fun manualAvailable(): Boolean = runCatching { manualIntent() != null }.getOrDefault(false)
    override fun openManual(): Boolean = runCatching { val intent = manualIntent() ?: return false; context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
}
