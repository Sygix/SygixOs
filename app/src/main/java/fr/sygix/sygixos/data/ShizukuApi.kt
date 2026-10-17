/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import fr.sygix.sygixos.IPrivilegedOps
import rikka.shizuku.Shizuku

fun interface PrivilegedOpsRemote {
    fun run(command: String): String?
}

interface ShizukuApi {
    fun pingBinder(): Boolean
    fun version(): Int
    fun granted(): Boolean
    fun permanentlyDenied(): Boolean
    fun request(requestCode: Int)
    fun onBinderReceived(listener: () -> Unit): AutoCloseable
    fun onBinderDead(listener: () -> Unit): AutoCloseable
    fun onPermissionResult(listener: (requestCode: Int, granted: Boolean) -> Unit): AutoCloseable
    fun bindOps(connected: (PrivilegedOpsRemote) -> Unit, lost: () -> Unit): AutoCloseable
}

class AndroidShizukuApi(private val context: Context) : ShizukuApi {
    override fun pingBinder(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    override fun version(): Int = Shizuku.getVersion()
    override fun granted(): Boolean = pingBinder() && runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
    override fun permanentlyDenied(): Boolean = pingBinder() && runCatching { Shizuku.shouldShowRequestPermissionRationale() }.getOrDefault(false)
    override fun request(requestCode: Int) = Shizuku.requestPermission(requestCode)

    override fun onBinderReceived(listener: () -> Unit): AutoCloseable {
        val received = Shizuku.OnBinderReceivedListener { listener() }
        Shizuku.addBinderReceivedListenerSticky(received)
        return AutoCloseable { Shizuku.removeBinderReceivedListener(received) }
    }

    override fun onBinderDead(listener: () -> Unit): AutoCloseable {
        val dead = Shizuku.OnBinderDeadListener { listener() }
        Shizuku.addBinderDeadListener(dead)
        return AutoCloseable { Shizuku.removeBinderDeadListener(dead) }
    }

    override fun onPermissionResult(listener: (Int, Boolean) -> Unit): AutoCloseable {
        val result = Shizuku.OnRequestPermissionResultListener { code, grant -> listener(code, grant == PackageManager.PERMISSION_GRANTED) }
        Shizuku.addRequestPermissionResultListener(result)
        return AutoCloseable { Shizuku.removeRequestPermissionResultListener(result) }
    }

    override fun bindOps(connected: (PrivilegedOpsRemote) -> Unit, lost: () -> Unit): AutoCloseable {
        val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, PrivilegedOpsService::class.java.name))
            .daemon(false)
            .processNameSuffix("privileged")
            .debuggable(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
            .version(versionCode())
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service == null || !service.pingBinder()) { lost(); return }
                runCatching { service.linkToDeath({ lost() }, 0) }.onFailure { lost(); return }
                val ops = IPrivilegedOps.Stub.asInterface(service)
                connected(PrivilegedOpsRemote { ops.run(it) })
            }

            override fun onServiceDisconnected(name: ComponentName?) = lost()
        }
        Shizuku.bindUserService(args, connection)
        return AutoCloseable { runCatching { Shizuku.unbindUserService(args, connection, true) } }
    }

    private fun versionCode(): Int = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
    }.getOrDefault(1)
}
