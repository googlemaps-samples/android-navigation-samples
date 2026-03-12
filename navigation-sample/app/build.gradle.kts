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

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maps.secrets)
}

android {
    namespace = "com.example.navigationapidemo"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.navigationapidemo"
        // Navigation SDK supports a minimum of SDK 24.
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
    jvmToolchain(17)
}

dependencies {
    // Include the Google Navigation SDK.
    // Note: remember to exclude Google play service Maps SDK from your transitive
    // dependencies to avoid duplicate copies of the Google Maps SDK.
    implementation(libs.google.maps.navigation)

    // Add: android.useDeprecatedNdk=true
    // to local.properties.
    implementation(libs.cronet.fallback)
    // Optional for Cronet users:
    // implementation "org.chromium.net:cronet-api:69.3497.100"

    // Add LeakCanary to debugImplementation because LeakCanary should only run
    // in debug builds.
    debugImplementation(libs.leakcanary)

    // And dependencies.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    
    implementation(libs.glide)
    
    // Also include the Google Places SDK, which is used by this example, but
    // isn't required by the Navigation SDK.
    implementation(libs.google.places)
    implementation(libs.google.material)
    implementation(libs.google.maps.utils) {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }

    implementation(libs.kotlin.stdlib)
    implementation(libs.guava)

    annotationProcessor(libs.androidx.annotation)
    annotationProcessor(libs.glide.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
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
        exec {
            commandLine("adb", "shell", "am", "start", "-n", "com.example.navigationapidemo/.SplashScreenActivity")
        }
    }
}
