/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

interface NetworkStatus {
    fun hasValidatedNetwork(): Boolean
}

class ConnectivityNetworkStatus(private val context: Context) : NetworkStatus {

    override fun hasValidatedNetwork(): Boolean = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)
}
