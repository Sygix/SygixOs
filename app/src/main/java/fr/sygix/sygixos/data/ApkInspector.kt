/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.io.File
import java.security.MessageDigest

data class ApkIdentity(val packageName: String, val versionCode: Long, val certificates: Set<String>)

interface ApkInspector {
    fun archive(file: File): ApkIdentity?
    fun installed(): ApkIdentity?
}

class PackageManagerApkInspector(
    private val pm: PackageManager,
    private val selfPackage: String,
) : ApkInspector {

    override fun archive(file: File): ApkIdentity? = runCatching {
        pm.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNING_CERTIFICATES)?.let(::identity)
    }.getOrNull()

    override fun installed(): ApkIdentity? = runCatching {
        identity(pm.getPackageInfo(selfPackage, PackageManager.GET_SIGNING_CERTIFICATES))
    }.getOrNull()

    private fun identity(info: PackageInfo): ApkIdentity = ApkIdentity(
        packageName = info.packageName,
        versionCode = info.longVersionCode,
        certificates = info.signingInfo?.apkContentsSigners.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet(),
    )
}
