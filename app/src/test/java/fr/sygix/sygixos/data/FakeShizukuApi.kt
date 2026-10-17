/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

class FakeShizukuApi : ShizukuApi {
    var binder = true
    var apiVersion = 13
    var permission = true
    var denied = false
    var connectOnBind = true
    var reply: (String) -> String? = { "0\nok" }
    val requests = mutableListOf<Int>()
    val sent = mutableListOf<String>()
    var binds = 0
    var unbinds = 0
    val received = mutableListOf<() -> Unit>()
    val dead = mutableListOf<() -> Unit>()
    val results = mutableListOf<(Int, Boolean) -> Unit>()
    var connected: ((PrivilegedOpsRemote) -> Unit)? = null
    var lost: (() -> Unit)? = null

    override fun pingBinder() = binder
    override fun version(): Int { check(binder); return apiVersion }
    override fun granted() = binder && permission
    override fun permanentlyDenied() = binder && denied
    override fun request(requestCode: Int) { requests += requestCode }

    override fun onBinderReceived(listener: () -> Unit): AutoCloseable {
        received += listener
        if (binder) listener()
        return AutoCloseable { received -= listener }
    }

    override fun onBinderDead(listener: () -> Unit): AutoCloseable {
        dead += listener
        return AutoCloseable { dead -= listener }
    }

    override fun onPermissionResult(listener: (Int, Boolean) -> Unit): AutoCloseable {
        results += listener
        return AutoCloseable { results -= listener }
    }

    override fun bindOps(connected: (PrivilegedOpsRemote) -> Unit, lost: () -> Unit): AutoCloseable {
        check(binder)
        binds++
        this.connected = connected
        this.lost = lost
        if (connectOnBind) connect()
        return AutoCloseable { unbinds++ }
    }

    fun connect() = connected?.invoke(PrivilegedOpsRemote { sent += it; reply(it) })

    fun receive() { binder = true; received.toList().forEach { it() } }
    fun die() { binder = false; dead.toList().forEach { it() } }
    fun answer(code: Int, granted: Boolean) = results.toList().forEach { it(code, granted) }
}
