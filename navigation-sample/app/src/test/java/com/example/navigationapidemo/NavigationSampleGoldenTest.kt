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

import android.graphics.Color
import android.view.View
import com.example.navtesting.NavMapsRobolectricTest
import com.example.navtesting.NavShadowNavigationApi
import com.example.navtesting.NavShadowSupportNavigationFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Simulator
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.maps.android.robolectric.ktx.createTestMap
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMarker
import com.google.maps.android.robolectric.powerassert.connects
import com.google.maps.android.robolectric.powerassert.meters
import com.google.maps.android.testing.golden.GoldenTileStrategy
import com.google.maps.android.testing.golden.assertMatchesGolden
import com.google.maps.android.testing.golden.enableGoldenTesting
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

/**
 * Deterministic offline golden screenshot tests for `navigation-sample` using
 * `com.google.maps.android:maps-testing-golden` against committed baselines under
 * `src/test/resources/goldens/`.
 */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class NavigationSampleGoldenTest : NavMapsRobolectricTest() {

  @Before
  fun setUp() {
    NavShadowNavigationApi.reset()
  }

  @After
  fun tearDown() {
    NavShadowNavigationApi.reset()
  }

  @Test
  fun navigationRouteOverlay_matchesCommittedGoldenBaseline() = runTest {
    val map = createTestMap(width = 200, height = 200)
    map.enableGoldenTesting(strategy = GoldenTileStrategy.SolidColorHash())

    val melbourne = LatLng(-37.813, 144.962)
    val destination = LatLng(-37.805, 144.970)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(melbourne, 13f))

    val originMarker =
      checkNotNull(map.addMarker(MarkerOptions().position(melbourne).title("Origin: Melbourne")))
    val routeLine =
      checkNotNull(
        map.addPolyline(PolylineOptions().add(melbourne, destination).color(Color.BLUE).width(6f))
      )
    map.addCircle(
      CircleOptions()
        .center(destination)
        .radius(300.0)
        .fillColor(Color.GREEN)
        .strokeWidth(2f)
    )

    originMarker.assertMarker(originMarker.title == "Origin: Melbourne")
    assertGeo(routeLine.connects(melbourne, destination, tolerance = 5.meters))

    map.assertMatchesGolden(
      goldenFile = File("src/test/resources/goldens/navigation_route_overlay.png"),
      maxDiffPercentage = 0.0,
      message = "Navigation route overlay must match committed golden baseline PNG",
    )
  }

  @Test
  fun navFragmentActivity_melbourneCameraViewMatchesCommittedGoldenBaseline() = runTest {
    val mockNavigator = mock(Navigator::class.java)
    `when`(mockNavigator.simulator).thenReturn(mock(Simulator::class.java))
    NavShadowNavigationApi.configuredNavigator = mockNavigator

    val activity =
      Robolectric.buildActivity(NavFragmentActivity::class.java).create().start().resume().get()
    ShadowLooper.idleMainLooper()

    val navFragment =
      activity.supportFragmentManager.findFragmentById(R.id.navigation_fragment)
        as SupportNavigationFragment
    val shadowNavFragment = Shadow.extract<NavShadowSupportNavigationFragment>(navFragment)
    val mapView = checkNotNull(shadowNavFragment.mapView)
    mapView.measure(
      View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 200, 200)

    var readyMap: GoogleMap? = null
    mapView.getMapAsync { readyMap = it }
    ShadowLooper.idleMainLooper()
    val map = checkNotNull(readyMap)

    map.enableGoldenTesting(strategy = GoldenTileStrategy.CoordinateGrid())
    activity.moveCameraToMelbourne(null)
    ShadowLooper.idleMainLooper()
    map.addMarker(MarkerOptions().position(LatLng(-37.813, 144.962)).title("Melbourne"))

    map.assertMatchesGolden(
      goldenFile = File("src/test/resources/goldens/nav_fragment_melbourne_grid.png"),
      maxDiffPercentage = 0.0,
      message = "NavFragmentActivity Melbourne camera view must match committed golden baseline PNG",
    )
  }
}
