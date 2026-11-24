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

import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import com.example.navtesting.NavMapsRobolectricTest
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.robolectric.ktx.circles
import com.google.maps.android.robolectric.ktx.findMarker
import com.google.maps.android.robolectric.ktx.groundOverlays
import com.google.maps.android.robolectric.ktx.markers
import com.google.maps.android.robolectric.ktx.polygons
import com.google.maps.android.robolectric.ktx.polylines
import com.google.maps.android.robolectric.ktx.shadow
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.assertMarker
import com.google.maps.android.robolectric.powerassert.hasMarker
import com.google.maps.android.robolectric.powerassert.isNear
import com.google.maps.android.robolectric.powerassert.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Headless Robolectric tests for `map-sample` marker, info-window retap, anchor, z-index, and tag
 * demos using Kotlin Power-Assert.
 */
@RunWith(RobolectricTestRunner::class)
class MapMarkersDemoTest : NavMapsRobolectricTest() {

  @Test
  fun markerDemoActivity_addsCityAndRainbowMarkersAndHandlesClicks() {
    val brisbane = LatLng(-27.47093, 153.0235)
    val sydney = LatLng(-33.87365, 151.20689)
    val perth = LatLng(-31.952854, 115.857342)
    val (activity, map) = launchMapDemo<MarkerDemoActivity>(useNavFlavor = false)

    with(map) {
      assertMap(
        markers.size == 17 &&
          hasMarker(title = "Brisbane", near = brisbane) &&
          hasMarker(title = "Sydney", near = sydney) &&
          hasMarker(title = "Perth", near = perth)
      )
    }

    val brisbaneMarker = checkNotNull(map.findMarker(title = "Brisbane"))
    activity.onMarkerClick(brisbaneMarker)
    brisbaneMarker.assertMarker(
      brisbaneMarker.position.isNear(LatLng(brisbane.latitude + 2.0, brisbane.longitude), 5.meters)
    )

    val sydneyMarker = checkNotNull(map.findMarker(title = "Sydney"))
    activity.onShowInfoWindow(null)
    sydneyMarker.assertMarker(sydneyMarker.isInfoWindowShown)
    activity.onHideInfoWindow(null)
    sydneyMarker.assertMarker(!sydneyMarker.isInfoWindowShown)

    activity.onMarkerDrag(sydneyMarker)
    assertGeo(activity.findViewById<TextView>(R.id.top_text).text.toString().isNotEmpty())
  }

  @Test
  fun markerDemoActivity_updatesRainbowPropertiesAndSupportsClearReset() {
    val (activity, map) = launchMapDemo<MarkerDemoActivity>(useNavFlavor = false)

    val flatBox = activity.findViewById<CheckBox>(R.id.flat)
    flatBox.isChecked = true
    activity.onToggleFlat(flatBox)
    assertGeo(map.markers.count { it.isFlat } == 12)

    val rotationBar = activity.findViewById<SeekBar>(R.id.rotationSeekBar)
    rotationBar.progress = 90
    activity.onProgressChanged(rotationBar, 90, true)
    assertGeo(map.markers.count { it.rotation == 90f } == 12)

    activity.onClearMap(null)
    assertGeo(map.markers.isEmpty())

    activity.onResetMap(null)
    assertGeo(map.markers.size == 17)
  }

  @Test
  fun markerCloseInfoWindowOnRetapDemoActivity_consumesRetapOnSelectedMarker() {
    val (_, map) = launchMapDemo<MarkerCloseInfoWindowOnRetapDemoActivity>(useNavFlavor = true)
    with(map) { assertMap(markers.size == 5 && !uiSettings.isZoomControlsEnabled) }

    val sydneyMarker = checkNotNull(map.findMarker(title = "Sydney"))
    val brisbaneMarker = checkNotNull(map.findMarker(title = "Brisbane"))

    assertGeo(!map.shadow.clickMarker(sydneyMarker) && sydneyMarker.isInfoWindowShown)
    assertGeo(map.shadow.clickMarker(sydneyMarker))

    assertGeo(!map.shadow.clickMarker(brisbaneMarker))
    map.shadow.clickMap(LatLng(-30.0, 140.0))
    assertGeo(!map.shadow.clickMarker(brisbaneMarker))
  }

  @Test
  fun markerAnchorsDemoActivity_rotatesAdelaideAndSydneyMarkers() {
    val (activity, map) = launchMapDemo<MarkerAnchorsDemoActivity>(useNavFlavor = false)
    with(map) {
      assertMap(
        markers.size == 2 &&
          hasMarker(title = "Adelaide", near = LatLng(-34.92873, 138.59995)) &&
          hasMarker(title = "Sydney", near = LatLng(-33.87365, 151.20689))
      )
    }

    val rotationSeek = activity.findViewById<SeekBar>(R.id.rotationSeekBar)
    rotationSeek.progress = 135
    activity.onProgressChanged(rotationSeek, 135, true)
    assertGeo(map.markers.all { it.rotation == 135f })
  }

  @Test
  fun markerZIndexDemoActivity_assignsZIndicesAndTogglesOverlayTransparency() {
    val (activity, map) = launchMapDemo<MarkerZIndexDemoActivity>(useNavFlavor = false)
    with(map) { assertMap(markers.size == 4 && groundOverlays.size == 1) }

    val z1 = checkNotNull(map.findMarker(title = "Marker z1"))
    val z2 = checkNotNull(map.findMarker(title = "Marker z2"))
    val z3 = checkNotNull(map.findMarker(title = "Marker z3"))
    val z4 = checkNotNull(map.findMarker(title = "Marker z4"))
    assertGeo(z1.zIndex == 1.0f && z2.zIndex == 2.0f && z3.zIndex == 3.0f && z4.zIndex == 4.0f)

    val overlay = map.groundOverlays.first()
    assertGeo(overlay.zIndex == 5.0f && overlay.isClickable && overlay.transparency == 0.0f)

    activity.onGroundOverlayClick(overlay)
    assertGeo(overlay.transparency == 0.5f)
    activity.onGroundOverlayClick(overlay)
    assertGeo(overlay.transparency == 0.0f)
  }

  @Test
  fun tagsDemoActivity_attachesCustomTagsAndCountsClicksPerOverlay() {
    val (activity, map) = launchMapDemo<TagsDemoActivity>(useNavFlavor = true)
    val tagText = activity.findViewById<TextView>(R.id.tag_text)

    with(map) {
      assertMap(!uiSettings.isZoomControlsEnabled && !uiSettings.isScrollGesturesEnabled)
      assertMap(circles.size == 1 && groundOverlays.size == 1 && markers.size == 1)
      assertMap(polygons.size == 1 && polylines.size == 1)
    }

    activity.onCircleClick(map.circles.first())
    activity.onCircleClick(map.circles.first())
    assertGeo(tagText.text.toString() == "The Adelaide circle has been clicked 2 times.")

    activity.onGroundOverlayClick(map.groundOverlays.first())
    assertGeo(tagText.text.toString() == "The Sydney ground overlay has been clicked 1 times.")

    assertGeo(activity.onMarkerClick(map.markers.first()))
    assertGeo(tagText.text.toString() == "The Hobart marker has been clicked 1 times.")

    activity.onPolygonClick(map.polygons.first())
    assertGeo(tagText.text.toString() == "The Darwin polygon has been clicked 1 times.")

    activity.onPolylineClick(map.polylines.first())
    assertGeo(tagText.text.toString() == "The Perth to Brisbane polyline has been clicked 1 times.")
  }
}
