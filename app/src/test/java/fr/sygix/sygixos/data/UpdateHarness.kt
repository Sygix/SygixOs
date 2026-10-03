/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.data.FakeTransport.Reply
import fr.sygix.sygixos.domain.Release
import fr.sygix.sygixos.domain.ReleaseVersion
import fr.sygix.sygixos.domain.asset
import fr.sygix.sygixos.domain.release
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

internal class UpdateHarness(
    val scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    val updatesDir: File,
    val store: UpdateStore = MemoryUpdateStore(),
    installedCode: Long = 199,
    val transport: FakeTransport = FakeTransport(),
) {
    var now = 100L * DAY
    var freeSpace = Long.MAX_VALUE
    val inspector = FakeInspector(ApkIdentity(PACKAGE, installedCode, setOf(CERT)), PACKAGE, CERT)
    val gateway = FakeGateway()
    val foreground = FakeForeground()
    val network = FakeNetwork()
    val screens = FakeScreens()
    private val apks = mutableMapOf<String, ByteArray>()
    private val source = GitHubReleaseSource(transport, { "SygixOs/test" }, clock = { now }, io = dispatcher)
    val installer = UpdateInstaller(
        source = source,
        transport = transport,
        inspector = inspector,
        gateway = gateway,
        store = store,
        foreground = foreground,
        updatesDir = updatesDir,
        freeSpace = { freeSpace },
        selfPackage = PACKAGE,
        userAgent = { "SygixOs/test" },
        io = dispatcher,
    )
    val repository = UpdateRepository(
        store = store,
        source = source,
        installer = installer,
        gateway = gateway,
        network = network,
        foreground = foreground,
        screens = screens,
        installedVersionCode = { inspector.installed?.versionCode ?: 0L },
        scope = scope,
        clock = { now },
        io = dispatcher,
    )

    fun apkUrl(tag: String) = "https://github.com/Sygix/SygixOs/releases/download/$tag/app-release.apk"

    fun apkBytes(tag: String): ByteArray = apks.getOrPut(tag) { ByteArray(APK_SIZE) { (it * 31 + tag.hashCode()).toByte() } }

    fun releaseOf(tag: String, bytes: ByteArray = apkBytes(tag), url: String = apkUrl(tag)): Release =
        release(tag, assets = listOf(asset(size = bytes.size.toLong(), digest = "sha256:" + sha256(bytes), url = url)))

    fun publish(vararg tags: String) {
        publishReleases(*tags.map { releaseOf(it) }.toTypedArray())
    }

    fun publishReleases(vararg releases: Release) {
        transport.on(LIST_URL, Reply.Body(body = releasesJson(*releases).toByteArray()))
        releases.forEach { release ->
            transport.on(tagUrl(release.tag), Reply.Body(body = releaseJson(release).toString().toByteArray()))
            release.assets.firstOrNull()?.let { asset ->
                transport.on(asset.downloadUrl, Reply.Body(body = apkBytes(release.tag)))
            }
        }
    }

    fun archiveAs(packageName: String = PACKAGE, versionCode: Long? = null, certificates: Set<String> = setOf(CERT)) {
        inspector.identify = { file ->
            val version = requireNotNull(ReleaseVersion.parse(file.name.removeSuffix(".apk")))
            ApkIdentity(packageName, versionCode ?: version.versionCode, certificates)
        }
    }

    fun listCalls() = transport.calls.count { it.first == LIST_URL }

    fun apkCalls(tag: String) = transport.calls.count { it.first == apkUrl(tag) }

    fun residualFiles(): List<File> = updatesDir.walkTopDown().filter { it.isFile }.toList()

    companion object {
        const val PACKAGE = "fr.sygix.sygixos"
        const val CERT = "release-cert"
        const val APK_SIZE = 4_096
        const val DAY = 24L * 60 * 60 * 1000
        const val HOUR = 60L * 60 * 1000
        val LIST_URL = "${GitHubReleaseSource.API_BASE}/releases?per_page=100"
        fun tagUrl(tag: String) = "${GitHubReleaseSource.API_BASE}/releases/tags/$tag"
        fun sha256(bytes: ByteArray): String = UpdateInstaller.hex(MessageDigest.getInstance("SHA-256").digest(bytes))
    }
}
