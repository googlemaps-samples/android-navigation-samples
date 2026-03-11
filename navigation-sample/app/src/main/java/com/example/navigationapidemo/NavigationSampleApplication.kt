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

package com.example.navigationapidemo

import android.app.Application
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import com.google.android.libraries.places.api.Places

class NavigationSampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Places.initializeWithNewPlacesApiEnabled(applicationContext, getApiKeyFromMetaData())
    }

    /**
     * Gets the Google Maps Api Key for the Places API from Metadata.
     *
     * @return The API key from AndroidManifest.
     * @throws RuntimeException if metadata com.google.android.geo.API_KEY doesn't exist.
     */
    private fun getApiKeyFromMetaData(): String {
        return try {
            val packageInfo: PackageItemInfo =
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            packageInfo.metaData.getString("com.google.android.geo.API_KEY")!!
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException("com.google.android.geo.API_KEY not defined in Manifest")
        }
    }
}
