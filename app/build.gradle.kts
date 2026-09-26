/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
}

// versionCode dérivé du tag SemVer : major·10⁸ + minor·10⁵ + patch·10² + suffixe
// (alpha.N → N, beta.N → 30+N, rc.N → 60+N, finale → 99) pour garantir alpha < beta < rc < finale.
// Plafond Android de 2 100 000 000 : major ≤ 20, minor et patch ≤ 999, N ≤ 29.
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
        // Signature release fournie par l'environnement (CI ou local.properties), sinon clé de debug :
        // indispensable pour tester les performances réelles (R8 + AOT) sur la TV sans secret dans le repo.
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures { compose = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.haze)
    implementation(libs.haze.glass)
    implementation(libs.aboutlibraries.compose.m3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.compose.ui.test.junit4)
}

// Robolectric accède aux internes de FileDescriptor : le JDK 17+ exige une ouverture explicite.
tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
    jvmArgs(
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
    )
}

