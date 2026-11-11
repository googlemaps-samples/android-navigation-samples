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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.example.mapdemo.testing.NavDelegateReflection
import com.example.mapdemo.testing.NavMapsRobolectricTest
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.hasOverlayPixelAt
import com.google.maps.android.testing.golden.GoldenTileStrategy
import com.google.maps.android.testing.golden.assertMatchesGolden
import com.google.maps.android.testing.golden.enableGoldenTesting
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Deterministic offline golden screenshot tests for `map-sample` demo activities using
 * `com.google.maps.android:maps-testing-golden` (`enableGoldenTesting`, `assertMatchesGolden`) and
 * committed `.png` baseline images under `src/test/resources/goldens/`.
 *
 * Sits above fast unit/Robolectric tests in the testing pyramid to verify pixel-deterministic
 * rendering of actual sample activities ([CircleDemoActivity], [PolygonDemoActivity], and
 * [TileCoordinateDemoActivity]). Pass `-Dmaps.golden.record=true` to record or update baselines.
 */
@RunWith(RobolectricTestRunner::class)
class MapSampleGoldenTest : NavMapsRobolectricTest() {

  private inline fun <reified T : AppCompatActivity> launchActivityWithSizedMap(): GoogleMap {
    val intent =
      Intent(ApplicationProvider.getApplicationContext(), T::class.java).apply {
        putExtra(ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO, false)
      }
    val activity = Robolectric.buildActivity(T::class.java, intent).setup().get()
    val fragment = activity.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    val fragmentView = checkNotNull(fragment.view)
    fragmentView.measure(
      View.MeasureSpec.makeMeasureSpec(256, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(256, View.MeasureSpec.EXACTLY),
    )
    fragmentView.layout(0, 0, 256, 256)
    return NavDelegateReflection.extractActivityMap(fragment)
  }

  @Test
  fun circleDemoActivity_rendersSydneyDraggableCircleMatchingCommittedGolden() = runTest {
    val sydney = LatLng(-33.87365, 151.20689)
    val map = launchActivityWithSizedMap<CircleDemoActivity>()
    map.enableGoldenTesting(strategy = GoldenTileStrategy.CoordinateGrid())

    assertGeo(map.hasOverlayPixelAt(sydney))

    map.assertMatchesGolden(
      goldenFile = File("src/test/resources/goldens/circle_demo_sydney.png"),
      maxDiffPercentage = 0.0,
      message = "CircleDemoActivity rendered map must match committed golden baseline PNG",
    )
  }

  @Test
  fun polygonDemoActivity_rendersAustralianPolygonsMatchingCommittedGolden() = runTest {
    val map = launchActivityWithSizedMap<PolygonDemoActivity>()
    map.enableGoldenTesting(strategy = GoldenTileStrategy.SolidColorHash())

    map.assertMatchesGolden(
      goldenFile = File("src/test/resources/goldens/polygon_demo_australia.png"),
      maxDiffPercentage = 0.0,
      message = "PolygonDemoActivity rendered map must match committed golden baseline PNG",
    )
  }

  @Test
  fun tileCoordinateDemoActivity_rendersCustomTileOverlayMatchingCommittedGolden() = runTest {
    val map = launchActivityWithSizedMap<TileCoordinateDemoActivity>()

    map.assertMatchesGolden(
      goldenFile = File("src/test/resources/goldens/tile_coordinate_demo.png"),
      maxDiffPercentage = 0.0,
      message = "TileCoordinateDemoActivity custom tile overlay must match committed golden PNG",
    )
  }
}
