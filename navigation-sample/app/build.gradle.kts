/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertCompilationFilter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.power.assert)
    alias(libs.plugins.maps.secrets)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    functions = listOf(
        "kotlin.assert",
        "kotlin.require",
        "kotlin.check",
        "com.google.maps.android.robolectric.powerassert.assertGeo",
        "com.google.maps.android.robolectric.powerassert.assertMap",
        "com.google.maps.android.robolectric.powerassert.assertMarker",
    )
    compilationFilter = PowerAssertCompilationFilter.ALL
}

android {
    namespace = "com.example.navigationapidemo"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.navigationapidemo"
        // Navigation SDK supports a minimum of SDK 24.
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["MAPS_API_KEY"] = "DEFAULT_API_KEY"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.systemProperty(
                    "maps.golden.record",
                    System.getProperty("maps.golden.record") ?: "false",
                )
            }
        }
    }

    androidResources {
        // Set this to the languages you actually use, otherwise you'll include resource strings
        // for all Navigation SDK supported languages.
        localeFilters += "en"
    }

    buildTypes {
        // Run proguard. Note that the Navigation SDK includes its own proguard config, and that
        // will be included transitively by depending on the Navigation SDK.
        // If the proguard step takes too long, consider enabling multidex for development work
        // instead.
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Include the Google Navigation SDK.
    // Note: remember to exclude Google Play services Maps SDK from your transitive
    // dependencies to avoid duplicate copies of the Google Maps SDK.
    implementation(libs.google.maps.navigation)

    // Add: android.useDeprecatedNdk=true
    // to local.properties.
    implementation(libs.cronet.fallback)
    // Optional for Cronet users:
    // implementation("org.chromium.net:cronet-api:69.3497.100")

    // Add LeakCanary to debugImplementation because LeakCanary should only run
    // in debug builds.
    debugImplementation(libs.leakcanary)

    // And dependencies.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.customview)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.mediarouter)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.transition)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.window)

    implementation(libs.glide)
    implementation(libs.datatransport.api)
    implementation(libs.datatransport.backend.cct)
    implementation(libs.datatransport.runtime)
    implementation(libs.joda.time)

    // Also include the Google Places SDK, which is used by this example, but
    // isn't required by the Navigation SDK.
    implementation(libs.google.places)
    implementation(libs.google.material)
    // If you include the Maps Utils library, you must exclude the independent
    // play-services-maps dependency, as the Navigation SDK provides its own
    // bundled, compatible Map implementation.
    implementation(libs.google.maps.utils) {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }

    implementation(libs.kotlin.stdlib)
    implementation(libs.guava)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Unit tests: JUnit, Kotlin Power-Assert, Google Truth, Robolectric, and android-maps-testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.maps.testing.robolectric)
    testImplementation(libs.maps.testing.golden)

    // Instrumented & visual tests
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.maps.testing.golden)
    androidTestImplementation(libs.maps.testing.visual)
}

secrets {
    // To add your Maps API key to this project:
    // 1. Create a secrets.properties file in the root project directory
    // 2. Add this line, where YOUR_API_KEY is your API key:
    //        MAPS_API_KEY=YOUR_API_KEY
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

tasks.register("installAndLaunch") {
    description = "Installs and launches the debug build on a connected device or emulator."
    group = "running"
    dependsOn("installDebug")
    doLast {
        ProcessBuilder(
            "adb",
            "shell",
            "am",
            "start",
            "-n",
            "com.example.navigationapidemo/.SplashScreenActivity"
        ).inheritIO().start().waitFor()
    }
}
