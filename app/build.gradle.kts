plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "fr.sygix.sygixos"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.sygix.sygixos"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
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
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
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

tasks.withType<Test>().matching { it.name.contains("Release") }.configureEach {
    filter { excludeTestsMatching("*HomeScreenScreenshotTest*") }
}
