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
    namespace = "com.example.mapdemo"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.mapdemo"
        // Navigation SDK supports a minimum of SDK 24.
        minSdk = 24
        // This example targets SDK 30 so that there's no need to explicitly include permissions
        // flows in the app.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        // Set this to the languages you actually use, otherwise you'll include resource strings
        // for all Navigation SDK supported languages.
        androidResources.localeFilters += "en"
        multiDexEnabled = true
    }

    buildTypes {
        // Run proguard. Note that the Navigation SDK includes its own proguard config, and that
        // will be included transitively by depending on the Navigation SDK.
        // If the proguard step takes too long, consider enabling multidex for development work
        // instead.
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(libs.google.maps.navigation)

    // Add: android.useDeprecatedNdk=true
    // to local.properties.
    implementation(libs.cronet.fallback)

    // Add LeakCanary to debugImplementation because LeakCanary should only run
    // in debug builds.
    debugImplementation(libs.leakcanary)

    // And dependencies.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.glide)
    implementation(libs.auto.value.annotations)
    annotationProcessor(libs.auto.value.processor)
    implementation(libs.error.prone.annotations)
    implementation(libs.guava)
    implementation(libs.google.material)
    implementation(libs.kotlin.stdlib)

    annotationProcessor(libs.androidx.annotation)
    annotationProcessor(libs.glide.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

secrets {
    // To add your Maps API key to this project:
    // 1. Open the root project's secrets.properties file
    // 2. Add this line, where YOUR_API_KEY is your API key:
    //        MAPS_API_KEY=YOUR_API_KEY
    defaultPropertiesFileName = "local.defaults.properties"
}

// Add a convenience target to build, install, and run the app
tasks.register("installAndLaunch") {
    dependsOn("installDebug")
    group = "execute"
    description = "Installs and launches the debug APK on a connected device."

    doLast {
        project.exec {
            commandLine("adb", "shell", "monkey", "-p", "com.example.mapdemo", "-c", "android.intent.category.LAUNCHER", "1")
        }
    }
}
