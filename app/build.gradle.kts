plugins {
    alias(libs.plugins.bakamitai.android.application)
    alias(libs.plugins.bakamitai.detekt)
    alias(libs.plugins.bakamitai.kotlin.only)
    alias(libs.plugins.bakamitai.android.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
}

android {
    namespace = "com.zc.bakamitai"

    defaultConfig {
        applicationId = "com.zc.bakamitai"
        // The Play release workflow passes -PversionCode and -PversionName from the release tag;
        // local builds keep these defaults.
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(5)
        versionName = providers.gradleProperty("versionName").getOrElse("2.0.0")
    }

    signingConfigs {
        // The standard Android debug keystore at the repo root: public credentials, never for production.
        create("development") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // A build type's signing config wins over a flavor's, and debug has its own, so this only signs
            // developmentRelease (built by .github/workflows/build-release-apk.yml). productionRelease has no
            // signing config here: it is signed with the Play upload key by whoever builds it, through AGP's
            // android.injected.signing.* properties (the Play release workflow, or Android Studio's Generate
            // Signed App Bundle). Without them it stays unsigned.
            signingConfig = signingConfigs.getByName("development")
        }
        create("production") {
            dimension = "environment"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.koin.bom))

    // AndroidX & lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Compose, Material 3 & Adaptive
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Kotlinx
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)

    // Networking, HTML parsing, images, rich text, persistence
    implementation(libs.bundles.ktor.client)
    implementation(libs.ksoup)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.richtext.ui.material3)
    implementation(libs.richtext.commonmark)
    implementation(libs.androidx.datastore.preferences)

    // Background work
    implementation(libs.androidx.work.runtime)

    // DI
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.koin.annotations)

    testImplementation(libs.bundles.test.unit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
