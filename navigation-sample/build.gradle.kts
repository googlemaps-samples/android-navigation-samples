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

// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.maps.secrets) apply false
}

allprojects {
    // Required: you must exclude the Google play service Maps SDK from
    // your transitive dependencies. This is to ensure there won't be
    // multiple copies of Google Maps SDK in your binary, as navigation SDK
    // already bundles the Google Maps SDK.
    configurations.all {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
