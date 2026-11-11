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

package com.example.navigationapidemo

import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.ListView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Connected-device instrumentation smoke tests for the `navigation-sample` application.
 *
 * Sitting near the top of the testing pyramid, this compact suite verifies that:
 * 1. All 5 navigation sample activities are properly declared in `AndroidManifest.xml` and
 *    resolvable by the Android `PackageManager` on a real device or emulator.
 * 2. The launcher (`MainActivity`) inflates its layout and populates the chooser list with all 3
 *    navigation demo entries cleanly without crashing.
 */
@RunWith(AndroidJUnit4::class)
class NavigationSampleSmokeTest {

  @Test
  fun manifest_declaresAllNavigationSampleActivities() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo =
      context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
    val declaredClassNames =
      packageInfo.activities?.map { activityInfo -> activityInfo.name }.orEmpty()

    assertThat(declaredClassNames)
      .containsAtLeast(
        MainActivity::class.java.name,
        SplashScreenActivity::class.java.name,
        NavViewActivity::class.java.name,
        NavFragmentActivity::class.java.name,
        SwappingMapAndNavActivity::class.java.name,
      )
  }

  @Test
  fun mainActivity_launchesAndDisplaysChooserList() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val listView = activity.findViewById<ListView>(R.id.list_view)
        assertThat(listView).isNotNull()
        assertThat(listView.visibility).isEqualTo(View.VISIBLE)
        assertThat(listView.adapter.count).isEqualTo(3)
        assertThat(listView.adapter.getItem(0)).isEqualTo("NavViewActivity")
        assertThat(listView.adapter.getItem(1)).isEqualTo("NavFragmentActivity")
        assertThat(listView.adapter.getItem(2)).isEqualTo("SwappingMapAndNavActivity")
      }
    }
  }
}
