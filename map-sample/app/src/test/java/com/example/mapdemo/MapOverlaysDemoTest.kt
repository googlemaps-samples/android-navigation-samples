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

import android.graphics.Color
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import com.example.navtesting.NavMapsRobolectricTest
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.robolectric.ktx.circles
import com.google.maps.android.robolectric.ktx.groundOverlays
import com.google.maps.android.robolectric.ktx.markers
import com.google.maps.android.robolectric.ktx.polygons
import com.google.maps.android.robolectric.ktx.polylines
import com.google.maps.android.robolectric.ktx.shadow
import com.google.maps.android.robolectric.ktx.tileOverlays
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.connects
import com.google.maps.android.robolectric.powerassert.contains
import com.google.maps.android.robolectric.powerassert.hasVertexNear
import com.google.maps.android.robolectric.powerassert.isCenteredOn
import com.google.maps.android.robolectric.powerassert.isNear
import com.google.maps.android.robolectric.powerassert.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Headless Robolectric tests for `map-sample` vector and raster overlay demos (circles, polygons,
 * polylines, tile overlays, and z-index management) using Kotlin Power-Assert.
 */
@RunWith(RobolectricTestRunner::class)
class MapOverlaysDemoTest : NavMapsRobolectricTest() {

  @Test
  fun circleDemoActivity_createsDraggableCircleAndHandlesStyleAndDrag() {
    val sydney = LatLng(-33.87365, 151.20689)
    val (activity, map) = launchMapDemo<CircleDemoActivity>(useNavFlavor = true)

    with(map) {
      assertMap(
        circles.size == 1 &&
          markers.size == 2 &&
          polylines.size == 1 &&
          isCenteredOn(sydney, expectedZoom = 4.0f, tolerance = 5.meters)
      )
    }

    val circle = map.circles.first()
    assertGeo(circle.center.isNear(sydney, 5.meters) && circle.radius == 1_000_000.0 && sydney in circle)

    val widthSeekBar = activity.findViewById<SeekBar>(R.id.widthSeekBar)
    widthSeekBar.progress = 35
    activity.onProgressChanged(widthSeekBar, 35, true)
    assertGeo(circle.strokeWidth == 35f)

    val clickCheckbox = activity.findViewById<CheckBox>(R.id.toggleClickability)
    clickCheckbox.isChecked = true
    activity.toggleClickability(clickCheckbox)
    val colorBeforeClick = circle.strokeColor
    map.shadow.clickCircle(circle)
    assertGeo(circle.isClickable && circle.strokeColor == (colorBeforeClick xor 0x00ffffff))

    val centerMarker = map.markers.first { it.position.isNear(sydney, 5.meters) }
    val newCenter = LatLng(-30.0, 150.0)
    centerMarker.position = newCenter
    activity.onMarkerDragEnd(centerMarker)
    activity.onMapLongClick(LatLng(-25.0, 135.0))
    assertGeo(circle.center.isNear(newCenter, 5.meters) && map.circles.size == 2)
  }

  @Test
  fun dayNightCircleDemoActivity_createsRotatingTerminatorCircle() {
    val (activity, map) = launchMapDemo<DayNightCircleDemoActivity>(useNavFlavor = false)
    assertGeo(map.circles.size == 1)

    val terminatorCircle = map.circles.first()
    assertGeo(terminatorCircle.radius > 10_000_000.0 && terminatorCircle.center.longitude > 0.0)
    activity.onDestroy()
  }

  @Test
  fun polygonDemoActivity_addsFourPolygonsAndFlipsStrokeOnClick() {
    val sydney = LatLng(-33.87365, 151.20689)
    val (activity, map) = launchMapDemo<PolygonDemoActivity>(useNavFlavor = false)

    with(map) {
      assertMap(polygons.size == 4 && isCenteredOn(sydney, tolerance = 5.meters))
    }

    val northwestPolygon = map.polygons[0]
    assertGeo(
      northwestPolygon.holes.size == 2 &&
        northwestPolygon.fillColor == Color.CYAN &&
        northwestPolygon.strokeColor == Color.BLUE &&
        northwestPolygon.isClickable &&
        northwestPolygon.hasVertexNear(LatLng(-25.0, 125.0), 10.meters)
    )

    val originalStroke = northwestPolygon.strokeColor
    activity.onPolygonClick(northwestPolygon)
    assertGeo(northwestPolygon.strokeColor == (originalStroke xor 0x00ffffff))

    val radioGroup = activity.findViewById<RadioGroup>(R.id.polygon_radio)
    activity.onCheckedChanged(radioGroup, R.id.polygon_radio_southeast)
    activity.onCheckedChanged(radioGroup, R.id.polygon_radio_world_wrapped)
  }

  @Test
  fun polylineDemoActivity_addsFourPolylinesAndFlipsColorOnClick() {
    val perth = LatLng(-31.95285, 115.85734)
    val melbourne = LatLng(-37.81319, 144.96298)
    val london = LatLng(51.471547, -0.460052)
    val (activity, map) = launchMapDemo<PolylineDemoActivity>(useNavFlavor = true)

    with(map) { assertMap(polylines.size == 4) }

    val australiaPolyline = map.polylines[0]
    val worldPolyline = map.polylines[1]
    assertGeo(
      australiaPolyline.connects(perth, melbourne, tolerance = 5.meters) &&
        australiaPolyline.color == Color.MAGENTA &&
        australiaPolyline.zIndex == 1.0f &&
        worldPolyline.connects(london, london, tolerance = 5.meters) &&
        worldPolyline.isGeodesic &&
        worldPolyline.isClickable
    )

    val initialColor = worldPolyline.color
    activity.onPolylineClick(worldPolyline)
    assertGeo(worldPolyline.color == (initialColor xor 0x00ffffff))
  }

  @Test
  fun tileOverlayDemoActivity_configuresMoonTilesFadeInAndTransparency() {
    val (activity, map) = launchMapDemo<TileOverlayDemoActivity>(useNavFlavor = false)
    with(map) { assertMap(mapType == GoogleMap.MAP_TYPE_NONE && tileOverlays.size == 1) }

    val moonOverlay = map.tileOverlays.first()
    val fadeInBox = CheckBox(activity).apply { isChecked = false }
    activity.setFadeIn(fadeInBox)
    assertGeo(!moonOverlay.fadeIn)

    fadeInBox.isChecked = true
    activity.setFadeIn(fadeInBox)
    val transparencyBar = activity.findViewById<SeekBar>(R.id.transparencySeekBar)
    activity.onProgressChanged(transparencyBar, 50, true)
    assertGeo(moonOverlay.fadeIn && moonOverlay.transparency == 0.5f)
    activity.clearCache(null)
  }

  @Test
  fun tileOverlayOnStyledMapDemoActivity_appliesStyleAndMoonOverlayControls() {
    val (activity, map) = launchMapDemo<TileOverlayOnStyledMapDemoActivity>(useNavFlavor = true)
    assertGeo(map.shadow.mapStyle != null && map.tileOverlays.size == 1)

    val moonOverlay = map.tileOverlays.first()
    val transparencySeek = activity.findViewById<SeekBar>(R.id.transparencySeekBar)
    activity.onProgressChanged(transparencySeek, 40, true)
    val fadeCheck = CheckBox(activity).apply { isChecked = false }
    activity.setFadeIn(fadeCheck)
    assertGeo(moonOverlay.transparency == 0.4f && !moonOverlay.fadeIn)
    activity.clearCache(null)
  }

  @Test
  fun zIndexDemoActivity_updatesZIndexVisibilityClickabilityAndTapLog() {
    val (activity, map) = launchMapDemo<ZIndexDemoActivity>(useNavFlavor = false)
    with(map) {
      assertMap(tileOverlays.size == 2 && circles.size == 1 && groundOverlays.size == 1)
      assertMap(markers.size == 2 && polygons.size == 1 && polylines.size == 1)
    }

    val zIndexPicker = activity.findViewById<EditText>(R.id.z_index_picker)
    val visibilityBox = activity.findViewById<CheckBox>(R.id.visibility_box)
    val clickabilityBox = activity.findViewById<CheckBox>(R.id.clickability_box)
    val tapLog = activity.findViewById<TextView>(R.id.tap_log)

    zIndexPicker.setText("42.5")
    activity.onEditorAction(zIndexPicker, 0, null)
    assertGeo(map.circles.first().zIndex == 42.5f)

    zIndexPicker.setText("not-a-float")
    activity.onEditorAction(zIndexPicker, 0, null)
    assertGeo(tapLog.text.toString().contains("invalid float"))

    visibilityBox.isChecked = false
    activity.onVisibilityCheckboxClicked(visibilityBox)
    clickabilityBox.isChecked = false
    activity.onClickabilityCheckboxClicked(clickabilityBox)
    assertGeo(!map.circles.first().isVisible && !map.circles.first().isClickable)

    activity.onCircleClick(map.circles.first())
    activity.onGroundOverlayClick(map.groundOverlays.first())
    activity.onMarkerClick(map.markers.first())
    activity.onPolygonClick(map.polygons.first())
    activity.onPolylineClick(map.polylines.first())
    val logContent = tapLog.text.toString()
    assertGeo(
      logContent.contains("Circle was clicked") &&
        logContent.contains("GroundOverlay was clicked") &&
        logContent.contains("Marker was clicked") &&
        logContent.contains("Polygon was clicked") &&
        logContent.contains("Polyline was clicked")
    )
  }
}
