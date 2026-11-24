/*
 * Copyright 2026 Google LLC
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

package com.example.mapdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.example.navtesting.NavDelegateReflection
import com.google.android.gms.maps.GoogleMap
import org.robolectric.Robolectric

/** Launches a `map-sample` demo activity in either Maps (`false`) or Navigation (`true`) flavor. */
internal inline fun <reified T : AppCompatActivity> launchMapDemo(
  useNavFlavor: Boolean
): Pair<T, GoogleMap> = launchMapDemo(T::class.java, useNavFlavor)

internal fun <T : AppCompatActivity> launchMapDemo(
  activityClass: Class<T>,
  useNavFlavor: Boolean,
): Pair<T, GoogleMap> {
  val intent =
    Intent(ApplicationProvider.getApplicationContext(), activityClass).apply {
      putExtra(ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO, useNavFlavor)
    }
  val activity = Robolectric.buildActivity(activityClass, intent).setup().get()
  val fragment =
    activity.supportFragmentManager.findFragmentById(R.id.map)
      ?: activity.supportFragmentManager.findFragmentByTag("map")
  val map = NavDelegateReflection.extractActivityMap(fragment)
  return activity to map
}
