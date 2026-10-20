/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.ksp)
}

fun versionCodeOf(version: String): Int {
    val match = requireNotNull(
        Regex("""(\d{1,2})\.(\d{1,3})\.(\d{1,3})(?:-(alpha|beta|rc)\.?(\d{1,2}))?""").matchEntire(version)
    ) { "SYGIXOS_VERSION doit être vX.Y.Z ou vX.Y.Z-(alpha|beta|rc).N, reçu : $version" }
    val (major, minor, patch, stage, n) = match.destructured
    require(major.toInt() <= 20) { "major doit être ≤ 20 (plafond du versionCode Android), reçu : $version" }
    val suffix = if (stage.isEmpty()) 99 else {
        require(n.toInt() <= 29) { "le numéro de pré-release doit être ≤ 29, reçu : $version" }
        mapOf("alpha" to 0, "beta" to 30, "rc" to 60).getValue(stage) + n.toInt()
    }
    return major.toInt() * 100_000_000 + minor.toInt() * 100_000 + patch.toInt() * 100 + suffix
}

android {
    namespace = "fr.sygix.sygixos"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.sygix.sygixos"
        minSdk = 34
        targetSdk = 37
        val ciVersion = System.getenv("SYGIXOS_VERSION")?.removePrefix("v")
        versionCode = ciVersion?.let(::versionCodeOf) ?: 1
        versionName = ciVersion ?: "0.1.0"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("SYGIXOS_STORE_FILE")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = System.getenv("SYGIXOS_STORE_PASSWORD")
                keyAlias = System.getenv("SYGIXOS_KEY_ALIAS")
                keyPassword = System.getenv("SYGIXOS_KEY_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("release")
        }
        create("perf") {
            initWith(getByName("release"))
            applicationIdSuffix = ".perf"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures {
        compose = true
        aidl = true
    }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

abstract class DexMetadataTask : DefaultTask() {
    @get:InputFiles
    abstract val apkFolder: DirectoryProperty

    @get:Input
    abstract val minSdk: Property<Int>

    @get:OutputDirectory
    abstract val outputFolder: DirectoryProperty

    @TaskAction
    fun run() {
        val folder = apkFolder.get().asFile
        @Suppress("UNCHECKED_CAST")
        val metadata = groovy.json.JsonSlurper().parse(File(folder, "output-metadata.json")) as Map<String, Any?>
        val apk = ((metadata["elements"] as List<Map<String, Any?>>).single()["outputFile"] as String)
        val api = minSdk.get()
        @Suppress("UNCHECKED_CAST")
        val profiles = metadata["baselineProfiles"] as? List<Map<String, Any?>> ?: emptyList()
        val match = profiles.firstOrNull { (it["minApi"] as Number).toInt() <= api && api <= (it["maxApi"] as Number).toInt() }
        val dm = (match?.get("baselineProfiles") as? List<*>)?.singleOrNull() as? String
            ?: error("Aucun fichier .dm produit par AGP pour l'API $api dans ${folder.name}")
        val out = outputFolder.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        File(folder, dm).copyTo(File(out, apk.removeSuffix(".apk") + ".dm"))
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        if (variant.buildType != "release" && variant.buildType != "perf") return@onVariants
        val suffix = variant.name.replaceFirstChar { it.uppercase() }
        tasks.register<DexMetadataTask>("dexMetadata$suffix") {
            apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
            minSdk.set(variant.minSdk.apiLevel)
            outputFolder.set(layout.buildDirectory.dir("outputs/dexmetadata/${variant.name}"))
        }
    }
}

aboutLibraries {
    collect {
        configPath = file("config")
    }
}

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    testImplementation(libs.room.testing)
    implementation(libs.dadb)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.haze)
    implementation(libs.haze.glass)
    implementation(libs.aboutlibraries.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.compose.ui.test.junit4)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

android.sourceSets.getByName("debug").assets.directories.add("schemas")

tasks.configureEach {
    if (name == "mergeDebugAssets") dependsOn("kspDebugKotlin")
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
    jvmArgs(
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
    )
}
