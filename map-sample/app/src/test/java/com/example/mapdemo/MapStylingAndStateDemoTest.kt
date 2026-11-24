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
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Spinner
import androidx.test.core.app.ApplicationProvider
import com.example.navtesting.NavDelegateReflection
import com.example.navtesting.NavMapsRobolectricTest
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.robolectric.ktx.markers
import com.google.maps.android.robolectric.ktx.shadow
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.hasMarker
import com.google.maps.android.robolectric.powerassert.isCenteredOn
import com.google.maps.android.robolectric.powerassert.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Headless Robolectric tests for `map-sample` styling, background color, layers, and saved-state
 * demos using Kotlin Power-Assert.
 */
@RunWith(RobolectricTestRunner::class)
class MapStylingAndStateDemoTest : NavMapsRobolectricTest() {

  @Test
  fun backgroundColorCustomizationDemoActivity_togglesMapTypeBetweenNoneAndNormal() {
    for (useNavFlavor in listOf(false, true)) {
      val (activity, map) = launchMapDemo<BackgroundColorCustomizationDemoActivity>(useNavFlavor)
      val checkbox = activity.findViewById<CheckBox>(R.id.map_type_toggle)

      with(map) {
        assertMap(
          mapType == GoogleMap.MAP_TYPE_NONE &&
            !checkbox.isChecked &&
            hasMarker(title = "Marker", near = LatLng(0.0, 0.0))
        )
      }

      checkbox.isChecked = true
      assertGeo(map.mapType == GoogleMap.MAP_TYPE_NORMAL)

      checkbox.isChecked = false
      assertGeo(map.mapType == GoogleMap.MAP_TYPE_NONE)
    }
  }

  @Test
  fun layersDemoActivity_togglesLayersAndSavesInstanceState() {
    val intent =
      Intent(ApplicationProvider.getApplicationContext(), LayersDemoActivity::class.java).apply {
        putExtra(ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO, true)
      }
    val controller = Robolectric.buildActivity(LayersDemoActivity::class.java, intent).setup()
    val activity = controller.get()
    val map =
      NavDelegateReflection.extractActivityMap(activity.supportFragmentManager.findFragmentById(R.id.map))

    val trafficBox = activity.findViewById<CheckBox>(R.id.traffic)
    val buildingsBox = activity.findViewById<CheckBox>(R.id.buildings)
    val styledBox = activity.findViewById<CheckBox>(R.id.styled_map)

    trafficBox.isChecked = true
    activity.onTrafficToggled(trafficBox)
    buildingsBox.isChecked = true
    activity.onBuildingsToggled(buildingsBox)
    styledBox.isChecked = true
    activity.onStyleMapToggled(styledBox)

    assertGeo(map.isTrafficEnabled && map.isBuildingsEnabled && map.shadow.mapStyle != null)

    val savedState = Bundle()
    controller.saveInstanceState(savedState)
    assertGeo(
      savedState.getBoolean("traffic") &&
        savedState.getBoolean("building") &&
        savedState.getBoolean("style_map")
    )
  }

  @Test
  fun mapStylingDemoActivity_switchesRawStylesAndMovesCamera() {
    val (activity, map) = launchMapDemo<MapStylingDemoActivity>(useNavFlavor = false)
    assertGeo(map.shadow.mapStyle != null)

    activity.showAdelaide(null)
    with(map) { assertMap(isCenteredOn(LatLng(-34.92873, 138.59995), expectedZoom = 10f, tolerance = 5.meters)) }

    activity.showAustralia(null)
    with(map) { assertMap(isCenteredOn(LatLng(-26.0, 134.0), expectedZoom = 4f, tolerance = 5.meters)) }

    activity.showSouthChinaSea(null)
    with(map) { assertMap(isCenteredOn(LatLng(13.870237, 111.470760), expectedZoom = 4f, tolerance = 5.meters)) }

    activity.setMapStyleTextOutline(null)
    assertGeo(map.shadow.mapStyle != null)
    activity.setMapStyleNoStyle(null)
    assertGeo(map.shadow.mapStyle == null)
  }

  @Test
  fun topStylesDemoActivity_appliesSelectedStyleAndTrafficToggle() {
    val (activity, map) = launchMapDemo<TopStylesDemoActivity>(useNavFlavor = false)
    val stylesSpinner = activity.findViewById<Spinner>(R.id.styles_spinner)
    stylesSpinner.setSelection(1)
    activity.onItemSelected(stylesSpinner, null, 1, 1L)
    assertGeo(map.shadow.mapStyle != null)

    val trafficBox = activity.findViewById<CheckBox>(R.id.traffic)
    trafficBox.isChecked = true
    activity.onTrafficToggled(trafficBox)
    assertGeo(map.isTrafficEnabled)
  }

  @Suppress("DEPRECATION")
  @Test
  fun saveStateDemoActivity_persistsDraggedMarkerPositionInSavedState() {
    val (activity, map) = launchMapDemo<SaveStateDemoActivity>(useNavFlavor = true)
    val paris = LatLng(48.858179, 2.294576)
    with(map) { assertMap(markers.size == 1 && isCenteredOn(paris, tolerance = 5.meters)) }

    val navSaveFragment =
      activity.supportFragmentManager.findFragmentById(R.id.map)
        as SaveStateDemoActivity.SaveStateNavFragment
    val draggedPos = LatLng(48.860, 2.300)
    val saveMarker = map.markers.first()
    saveMarker.position = draggedPos
    navSaveFragment.onMarkerDragEnd(saveMarker)
    navSaveFragment.onMarkerClick(saveMarker)

    val fragmentOut = Bundle()
    navSaveFragment.onSaveInstanceState(fragmentOut)
    assertGeo(fragmentOut.getParcelable<LatLng>("markerPosition") == draggedPos)
  }
}
