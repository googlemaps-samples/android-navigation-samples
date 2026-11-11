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

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.example.mapdemo.testing.NavDelegateReflection
import com.example.mapdemo.testing.NavMapsRobolectricTest
import com.example.mapdemo.testing.ShadowSupportNavigationFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.ktx.cameraBearing
import com.google.maps.android.robolectric.ktx.cameraTarget
import com.google.maps.android.robolectric.ktx.cameraTilt
import com.google.maps.android.robolectric.ktx.cameraZoom
import com.google.maps.android.robolectric.ktx.circles
import com.google.maps.android.robolectric.ktx.findMarker
import com.google.maps.android.robolectric.ktx.groundOverlays
import com.google.maps.android.robolectric.ktx.markers
import com.google.maps.android.robolectric.ktx.polygons
import com.google.maps.android.robolectric.ktx.polylines
import com.google.maps.android.robolectric.ktx.shadow
import com.google.maps.android.robolectric.ktx.tileOverlays
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.assertMarker
import com.google.maps.android.robolectric.powerassert.connects
import com.google.maps.android.robolectric.powerassert.contains
import com.google.maps.android.robolectric.powerassert.distanceTo
import com.google.maps.android.robolectric.powerassert.hasMarker
import com.google.maps.android.robolectric.powerassert.hasVertexNear
import com.google.maps.android.robolectric.powerassert.isCenteredOn
import com.google.maps.android.robolectric.powerassert.isNear
import com.google.maps.android.robolectric.powerassert.kilometers
import com.google.maps.android.robolectric.powerassert.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

/**
 * Middle-tier headless Robolectric JVM tests for `map-sample` demo activities using **Kotlin
 * Power-Assert** (`assert`, `assertGeo`, `assertMap`, `assertMarker`) and `android-maps-testing`
 * extensions.
 *
 * Every demo activity in `map-sample` supports two rendering flavors controlled by
 * [ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO]:
 * - **Maps flavor** (`false`): inflates a [SupportMapFragment].
 * - **Navigation flavor** (`true`): inflates a [SupportNavigationFragment].
 *
 * This test suite verifies both flavors as well as interactive user journeys across camera
 * movement, camera clamping, circles, polygons, polylines, markers, tags, tile overlays, UI
 * settings, visible regions, Z-index ordering, map styling, layers, events, and launcher routing.
 */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class MapActivitiesPowerAssertTest : NavMapsRobolectricTest() {

  private inline fun <reified T : AppCompatActivity> launchDemo(
    useNavFlavor: Boolean = false
  ): Pair<T, GoogleMap> {
    val intent =
      Intent(ApplicationProvider.getApplicationContext(), T::class.java).apply {
        putExtra(ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO, useNavFlavor)
      }
    val activity = Robolectric.buildActivity(T::class.java, intent).setup().get()
    val fragment =
      activity.supportFragmentManager.findFragmentById(R.id.map)
        ?: activity.supportFragmentManager.findFragmentByTag("map")
    val map = NavDelegateReflection.extractActivityMap(fragment)
    return activity to map
  }

  @Test
  fun basicMapDemoActivity_addsMarkerAtOriginInBothMapsAndNavigationFlavors() {
    for (useNavFlavor in listOf(false, true)) {
      val (activity, map) = launchDemo<BasicMapDemoActivity>(useNavFlavor = useNavFlavor)
      val fragment = activity.supportFragmentManager.findFragmentById(R.id.map)
      if (useNavFlavor) {
        assertGeo(fragment is SupportNavigationFragment)
      } else {
        assertGeo(fragment is SupportMapFragment)
      }

      with(map) {
        assertMap(markers.size == 1 && hasMarker(title = "Marker", near = LatLng(0.0, 0.0)))
      }
      val marker = checkNotNull(map.findMarker(title = "Marker"))
      marker.assertMarker(
        marker.title == "Marker" && marker.position.isNear(LatLng(0.0, 0.0), 1.meters)
      )
    }
  }

  @Test
  fun backgroundColorCustomizationDemoActivity_togglesMapTypeBetweenNoneAndNormal() {
    for (useNavFlavor in listOf(false, true)) {
      val (activity, map) =
        launchDemo<BackgroundColorCustomizationDemoActivity>(useNavFlavor = useNavFlavor)
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
  fun cameraDemoActivity_animatesBetweenBondiAndSydneyAndScrollsAndZooms() {
    val bondi = LatLng(-33.891614, 151.276417)
    val sydney = LatLng(-33.87365, 151.20689)

    val (activity, map) = launchDemo<CameraDemoActivity>(useNavFlavor = true)

    // Initial onMapReady moves camera to Sydney at zoom 10.
    with(map) {
      assertMap(isCenteredOn(sydney, expectedZoom = 10f, tolerance = 5.meters))
    }

    // Go to Bondi (zoom 15.5, bearing 300, tilt 50)
    activity.onGoToBondi(null)
    with(map) {
      assertMap(
        isCenteredOn(bondi, expectedZoom = 15.5f, tolerance = 5.meters) &&
          cameraBearing == 300f &&
          cameraTilt == 50f
      )
    }

    // Go to Sydney (zoom 15.5, bearing 0, tilt 25)
    activity.onGoToSydney(null)
    with(map) {
      assertMap(
        isCenteredOn(sydney, expectedZoom = 15.5f, tolerance = 5.meters) &&
          cameraTilt == 25f
      )
    }

    // Zoom in & zoom out
    val zoomBefore = map.cameraZoom
    activity.onZoomIn(null)
    assertGeo(map.cameraZoom == zoomBefore + 1.0f)
    activity.onZoomOut(null)
    assertGeo(map.cameraZoom == zoomBefore)

    // Scroll directions displace camera target
    val targetBeforeScroll = map.cameraTarget
    activity.onScrollUp(null)
    assertGeo(map.cameraTarget.latitude > targetBeforeScroll.latitude)
    activity.onScrollDown(null)
    activity.onScrollLeft(null)
    assertGeo(map.cameraTarget.longitude < targetBeforeScroll.longitude)
    activity.onScrollRight(null)

    // Go back to Bondi
    activity.onGoToBondi(null)
    with(map) {
      assertMap(isCenteredOn(bondi, expectedZoom = 15.5f, tolerance = 5.meters))
    }

    // Camera move lifecycle callbacks trace a polyline across the camera path
    activity.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE)
    activity.onCameraMove()
    activity.onCameraIdle()
    assertGeo(map.polylines.isNotEmpty())
  }

  @Test
  fun cameraClampingDemoActivity_clampsBoundsAndMinMaxZoomPreferences() {
    val adelaide = LatLng(-34.92873, 138.59995)
    val pacificTarget = LatLng(0.0, -180.0)

    val (activity, map) = launchDemo<CameraClampingDemoActivity>(useNavFlavor = false)

    activity.onClampToAdelaide(null)
    with(map) {
      assertMap(
        isCenteredOn(adelaide, expectedZoom = 20.0f, tolerance = 5.meters) &&
          shadow.latLngBoundsForCameraTarget != null &&
          adelaide in checkNotNull(shadow.latLngBoundsForCameraTarget)
      )
    }

    activity.onClampToPacific(null)
    with(map) {
      assertMap(
        cameraZoom == 4.0f &&
          shadow.latLngBoundsForCameraTarget != null &&
          pacificTarget.latitude == cameraTarget.latitude
      )
    }

    activity.onLatLngClampReset(null)
    assertGeo(map.shadow.latLngBoundsForCameraTarget == null)

    // Min/max zoom preference clamping (default min=2.0, max=22.0, delta=2.0)
    activity.onSetMinZoomClamp(null)
    assertGeo(map.minZoomLevel == 4.0f)

    activity.onSetMaxZoomClamp(null)
    assertGeo(map.maxZoomLevel == 20.0f)

    activity.onMinMaxZoomClampReset(null)
    assertGeo(map.minZoomLevel == 2.0f && map.maxZoomLevel == 21.0f)

    // Camera move started updates camera_text with animation reason and target
    activity.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE)
    val cameraText = activity.findViewById<TextView>(R.id.camera_text).text.toString()
    assertGeo(cameraText.contains("REASON_GESTURE"))
  }

  @Test
  fun circleDemoActivity_createsDraggableCircleUpdatesStyleAndHandlesLongClick() {
    val sydney = LatLng(-33.87365, 151.20689)
    val (activity, map) = launchDemo<CircleDemoActivity>(useNavFlavor = true)

    with(map) {
      assertMap(
        circles.size == 1 &&
          markers.size == 2 &&
          polylines.size == 1 &&
          isCenteredOn(sydney, expectedZoom = 4.0f, tolerance = 5.meters)
      )
    }

    val initialCircle = map.circles.first()
    assertGeo(
      initialCircle.center.isNear(sydney, 5.meters) &&
        initialCircle.radius == 1_000_000.0 &&
        sydney in initialCircle
    )

    // Changing widthSeekBar updates circle strokeWidth
    val widthSeekBar = activity.findViewById<SeekBar>(R.id.widthSeekBar)
    widthSeekBar.progress = 35
    activity.onProgressChanged(widthSeekBar, 35, true)
    assertGeo(initialCircle.strokeWidth == 35f)

    // Toggling clickability checkbox updates circle clickability
    val clickCheckbox = activity.findViewById<CheckBox>(R.id.toggleClickability)
    clickCheckbox.isChecked = true
    activity.toggleClickability(clickCheckbox)
    assertGeo(initialCircle.isClickable)

    // Clicking the circle flips the RGB bits of its strokeColor
    val colorBeforeClick = initialCircle.strokeColor
    map.shadow.clickCircle(initialCircle)
    assertGeo(initialCircle.strokeColor == (colorBeforeClick xor 0x00ffffff))

    // Dragging the center marker moves the circle's center
    val centerMarker = map.markers.first { it.position.isNear(sydney, 5.meters) }
    val newCenter = LatLng(-30.0, 150.0)
    centerMarker.position = newCenter
    activity.onMarkerDragEnd(centerMarker)
    assertGeo(initialCircle.center.isNear(newCenter, 5.meters))

    // Long-clicking on the map adds a second DraggableCircle
    activity.onMapLongClick(LatLng(-25.0, 135.0))
    assertGeo(map.circles.size == 2 && map.markers.size == 4 && map.polylines.size == 2)
  }

  @Test
  fun polygonDemoActivity_addsFourPolygonsFlipsStrokeColorOnClickAndSwitchesSelection() {
    val sydney = LatLng(-33.87365, 151.20689)
    val (activity, map) = launchDemo<PolygonDemoActivity>(useNavFlavor = false)

    with(map) {
      assertMap(
        polygons.size == 4 &&
          isCenteredOn(sydney, tolerance = 5.meters)
      )
    }

    val northwestPolygon = map.polygons[0]
    assertGeo(
      northwestPolygon.holes.size == 2 &&
        northwestPolygon.fillColor == Color.CYAN &&
        northwestPolygon.strokeColor == Color.BLUE &&
        northwestPolygon.isClickable &&
        northwestPolygon.hasVertexNear(LatLng(-25.0, 125.0), 10.meters)
    )

    // Clicking polygon flips RGB bits of its strokeColor
    val originalStroke = northwestPolygon.strokeColor
    activity.onPolygonClick(northwestPolygon)
    assertGeo(northwestPolygon.strokeColor == (originalStroke xor 0x00ffffff))

    // Radio group switches selectedPolygon without throwing
    val radioGroup = activity.findViewById<RadioGroup>(R.id.polygon_radio)
    activity.onCheckedChanged(radioGroup, R.id.polygon_radio_northwest)
    activity.onCheckedChanged(radioGroup, R.id.polygon_radio_southeast)
    activity.onCheckedChanged(radioGroup, R.id.polygon_radio_world_wrapped)
    activity.onCheckedChanged(radioGroup, R.id.polygon_radio_central)
  }

  @Test
  fun polylineDemoActivity_addsFourPolylinesVerifiesEndpointsAndFlipsColorOnClick() {
    val perth = LatLng(-31.95285, 115.85734)
    val melbourne = LatLng(-37.81319, 144.96298)
    val london = LatLng(51.471547, -0.460052)

    val (activity, map) = launchDemo<PolylineDemoActivity>(useNavFlavor = true)

    with(map) {
      assertMap(polylines.size == 4)
    }

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

    val radioGroup = activity.findViewById<RadioGroup>(R.id.polyline_radio)
    activity.onCheckedChanged(radioGroup, R.id.polyline_radio_sydney)
    activity.onCheckedChanged(radioGroup, R.id.polyline_radio_melbourne)
    activity.onCheckedChanged(radioGroup, R.id.polyline_radio_world)
  }

  @Test
  fun markerDemoActivity_addsAustralianCityMarkersHandlesClicksClearAndReset() {
    val brisbane = LatLng(-27.47093, 153.0235)
    val sydney = LatLng(-33.87365, 151.20689)
    val perth = LatLng(-31.952854, 115.857342)

    val (activity, map) = launchDemo<MarkerDemoActivity>(useNavFlavor = false)

    // 5 named city markers + Perth + 11 rainbow Victoria markers = 17 markers total
    with(map) {
      assertMap(
        markers.size == 17 &&
          hasMarker(title = "Brisbane", near = brisbane) &&
          hasMarker(title = "Sydney", near = sydney) &&
          hasMarker(near = perth)
      )
    }

    // Clicking Brisbane shifts its latitude northward by +2.0 degrees
    val brisbaneMarker = checkNotNull(map.findMarker(title = "Brisbane"))
    activity.onMarkerClick(brisbaneMarker)
    brisbaneMarker.assertMarker(
      brisbaneMarker.position.isNear(LatLng(brisbane.latitude + 2.0, brisbane.longitude), 5.meters)
    )

    // Showing and hiding Sydney's info window toggles its infoWindow state
    val sydneyMarker = checkNotNull(map.findMarker(title = "Sydney"))
    activity.onShowInfoWindow(null)
    sydneyMarker.assertMarker(sydneyMarker.isInfoWindowShown)
    activity.onHideInfoWindow(null)
    sydneyMarker.assertMarker(!sydneyMarker.isInfoWindowShown)

    // Dragging a marker updates top_text with drag coordinates
    activity.onMarkerDrag(sydneyMarker)
    val topText = activity.findViewById<TextView>(R.id.top_text).text.toString()
    assertGeo(topText.isNotEmpty())

    // Toggling flat checkbox and rotation seekbar updates the 12 rainbow markers
    val flatBox = activity.findViewById<CheckBox>(R.id.flat)
    flatBox.isChecked = true
    activity.onToggleFlat(flatBox)
    assertGeo(map.markers.count { it.isFlat } == 12)

    val rotationBar = activity.findViewById<SeekBar>(R.id.rotationSeekBar)
    rotationBar.progress = 90
    activity.onProgressChanged(rotationBar, 90, true)
    assertGeo(map.markers.count { it.rotation == 90f } == 12)

    // Clear and reset map
    activity.onClearMap(null)
    assertGeo(map.markers.isEmpty())

    activity.onResetMap(null)
    assertGeo(map.markers.size == 17)
  }

  @Test
  fun markerCloseInfoWindowOnRetapDemoActivity_closesInfoWindowWhenSelectedMarkerRetapped() {
    val (_, map) = launchDemo<MarkerCloseInfoWindowOnRetapDemoActivity>(useNavFlavor = true)

    with(map) {
      assertMap(markers.size == 5 && !uiSettings.isZoomControlsEnabled)
    }

    val sydneyMarker = checkNotNull(map.findMarker(title = "Sydney"))
    val brisbaneMarker = checkNotNull(map.findMarker(title = "Brisbane"))

    // First tap on Sydney returns false (default behavior opens info window)
    val firstTapConsumed = map.shadow.clickMarker(sydneyMarker)
    assertGeo(!firstTapConsumed && sydneyMarker.isInfoWindowShown)

    // Second tap (retap) on Sydney returns true (consumes event so info window stays closed)
    val retapConsumed = map.shadow.clickMarker(sydneyMarker)
    assertGeo(retapConsumed)

    // Tapping Brisbane selects Brisbane, then clicking the map clears selection
    assertGeo(!map.shadow.clickMarker(brisbaneMarker))
    map.shadow.clickMap(LatLng(-30.0, 140.0))
    // After map click cleared selectedMarker, tapping Brisbane again is treated as a fresh tap
    assertGeo(!map.shadow.clickMarker(brisbaneMarker))
  }

  @Test
  fun markerZIndexDemoActivity_assignsDistinctZIndicesAndTogglesGroundOverlayTransparency() {
    val (activity, map) = launchDemo<MarkerZIndexDemoActivity>(useNavFlavor = false)

    with(map) {
      assertMap(markers.size == 4 && groundOverlays.size == 1)
    }

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
  fun tagsDemoActivity_disablesGesturesAttachesCustomTagsAndCountsClicksAcrossAllOverlays() {
    val (activity, map) = launchDemo<TagsDemoActivity>(useNavFlavor = true)
    val tagText = activity.findViewById<TextView>(R.id.tag_text)

    with(map) {
      assertMap(!uiSettings.isZoomControlsEnabled && !uiSettings.isScrollGesturesEnabled)
      assertMap(!uiSettings.isZoomGesturesEnabled && !uiSettings.isTiltGesturesEnabled)
      assertMap(!uiSettings.isRotateGesturesEnabled)
      assertMap(circles.size == 1 && groundOverlays.size == 1 && markers.size == 1)
      assertMap(polygons.size == 1 && polylines.size == 1)
    }

    activity.onCircleClick(map.circles.first())
    assertGeo(tagText.text.toString() == "The Adelaide circle has been clicked 1 times.")

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

  @Test
  fun tileOverlayDemoActivity_configuresMoonTilesFadeInAndTransparency() {
    val (activity, map) = launchDemo<TileOverlayDemoActivity>(useNavFlavor = false)

    with(map) {
      assertMap(mapType == GoogleMap.MAP_TYPE_NONE && tileOverlays.size == 1)
    }

    val moonOverlay = map.tileOverlays.first()
    val fadeInBox = CheckBox(activity).apply { isChecked = false }
    activity.setFadeIn(fadeInBox)
    assertGeo(!moonOverlay.fadeIn)

    fadeInBox.isChecked = true
    activity.setFadeIn(fadeInBox)
    assertGeo(moonOverlay.fadeIn)

    val transparencyBar = activity.findViewById<SeekBar>(R.id.transparencySeekBar)
    activity.onProgressChanged(transparencyBar, 50, true)
    assertGeo(moonOverlay.transparency == 0.5f)

    activity.clearCache(null)
  }

  @Test
  fun uiSettingsDemoActivity_synchronizesAllGestureAndControlTogglesWithMapUiSettings() {
    val (activity, map) = launchDemo<UiSettingsDemoActivity>(useNavFlavor = true)
    val uiSettings = map.uiSettings

    val toggleCheckbox = CheckBox(activity).apply { isChecked = false }

    activity.setZoomButtonsEnabled(toggleCheckbox)
    assertGeo(!uiSettings.isZoomControlsEnabled)

    activity.setCompassEnabled(toggleCheckbox)
    assertGeo(!uiSettings.isCompassEnabled)

    activity.setScrollGesturesEnabled(toggleCheckbox)
    assertGeo(!uiSettings.isScrollGesturesEnabled)

    activity.setZoomGesturesEnabled(toggleCheckbox)
    assertGeo(!uiSettings.isZoomGesturesEnabled)

    activity.setTiltGesturesEnabled(toggleCheckbox)
    assertGeo(!uiSettings.isTiltGesturesEnabled)

    activity.setRotateGesturesEnabled(toggleCheckbox)
    assertGeo(!uiSettings.isRotateGesturesEnabled)

    activity.setScrollGesturesEnabledDuringRotateOrZoom(toggleCheckbox)
    assertGeo(!uiSettings.isScrollGesturesEnabledDuringRotateOrZoom)
  }

  @Test
  fun visibleRegionDemoActivity_disablesNavCardsInNavFlavorAndMovesBetweenLandmarks() {
    val sfo = LatLng(37.614631, -122.385153)
    val operaHouse = LatLng(-33.85704, 151.21522)

    val (activity, map) = launchDemo<VisibleRegionDemoActivity>(useNavFlavor = true)
    val navFragment =
      activity.supportFragmentManager.findFragmentById(R.id.map) as SupportNavigationFragment
    val shadowNav = Shadow.extract<ShadowSupportNavigationFragment>(navFragment)

    // In Navigation flavor, VisibleRegionDemoActivity disables header and ETA card so padding works
    assertGeo(!shadowNav.isHeaderEnabledState && !shadowNav.isEtaCardEnabledState)

    with(map) {
      assertMap(
        isCenteredOn(sfo, expectedZoom = 18f, tolerance = 5.meters) &&
          hasMarker(title = "Sydney Opera House", near = operaHouse) &&
          shadow.paddingLeft == 150
      )
    }

    activity.moveToOperaHouse(null)
    with(map) {
      assertMap(isCenteredOn(operaHouse, expectedZoom = 16f, tolerance = 5.meters))
    }

    activity.moveToAUS(null)
    assertGeo(map.cameraTarget.distanceTo(operaHouse) < 4000.kilometers)

    activity.moveToSFO(null)
    with(map) {
      assertMap(isCenteredOn(sfo, expectedZoom = 18f, tolerance = 5.meters))
    }
  }

  @Test
  fun zIndexDemoActivity_managesVisibilityClickabilityZIndexAndTapLoggingForEightOverlays() {
    val (activity, map) = launchDemo<ZIndexDemoActivity>(useNavFlavor = false)

    with(map) {
      assertMap(tileOverlays.size == 2 && circles.size == 1 && groundOverlays.size == 1)
      assertMap(markers.size == 2 && polygons.size == 1 && polylines.size == 1)
    }

    val zIndexPicker = activity.findViewById<EditText>(R.id.z_index_picker)
    val visibilityBox = activity.findViewById<CheckBox>(R.id.visibility_box)
    val clickabilityBox = activity.findViewById<CheckBox>(R.id.clickability_box)
    val tapLog = activity.findViewById<TextView>(R.id.tap_log)

    // Initially Circle (index 0) is selected
    zIndexPicker.setText("42.5")
    activity.onEditorAction(zIndexPicker, 0, null)
    assertGeo(map.circles.first().zIndex == 42.5f)

    // Invalid float in zIndexPicker logs an error message to tapLog
    zIndexPicker.setText("not-a-float")
    activity.onEditorAction(zIndexPicker, 0, null)
    assertGeo(tapLog.text.toString().contains("invalid float"))

    // Toggle visibility and clickability of Circle
    visibilityBox.isChecked = false
    activity.onVisibilityCheckboxClicked(visibilityBox)
    assertGeo(!map.circles.first().isVisible)

    clickabilityBox.isChecked = false
    activity.onClickabilityCheckboxClicked(clickabilityBox)
    assertGeo(!map.circles.first().isClickable)

    // Click events append entries to tapLog
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

  @Test
  fun eventsLayersMapStylingSaveStateAndMainActivity_handleUserInteractionsCorrectly() {
    // 1. EventsDemoActivity
    val (eventsActivity, _) = launchDemo<EventsDemoActivity>(useNavFlavor = false)
    val clickPoint = LatLng(37.422, -122.084)
    eventsActivity.onMapClick(clickPoint)
    val tapText = eventsActivity.findViewById<TextView>(R.id.tap_text)
    assertGeo(tapText.text.toString().contains(clickPoint.toString()))
    eventsActivity.onMapLongClick(clickPoint)
    assertGeo(tapText.text.toString().contains("long pressed"))

    // 2. LayersDemoActivity
    val (layersActivity, layersMap) = launchDemo<LayersDemoActivity>(useNavFlavor = true)
    val trafficBox = layersActivity.findViewById<CheckBox>(R.id.traffic)
    val buildingsBox = layersActivity.findViewById<CheckBox>(R.id.buildings)
    val styledBox = layersActivity.findViewById<CheckBox>(R.id.styled_map)

    trafficBox.isChecked = true
    layersActivity.onTrafficToggled(trafficBox)
    buildingsBox.isChecked = true
    layersActivity.onBuildingsToggled(buildingsBox)
    styledBox.isChecked = true
    layersActivity.onStyleMapToggled(styledBox)

    assertGeo(
      layersMap.isTrafficEnabled &&
        layersMap.isBuildingsEnabled &&
        layersMap.shadow.mapStyle != null
    )

    val savedState = Bundle()
    layersActivity.saveInstanceState(savedState)
    assertGeo(
      savedState.getBoolean("trafic") &&
        savedState.getBoolean("building") &&
        savedState.getBoolean("style_map")
    )

    // 3. MapStylingDemoActivity
    val (stylingActivity, stylingMap) = launchDemo<MapStylingDemoActivity>(useNavFlavor = false)
    assertGeo(stylingMap.shadow.mapStyle != null)

    stylingActivity.showAdelaide(null)
    with(stylingMap) {
      assertMap(isCenteredOn(LatLng(-34.92873, 138.59995), expectedZoom = 10f, tolerance = 5.meters))
    }
    stylingActivity.showAustralia(null)
    with(stylingMap) {
      assertMap(isCenteredOn(LatLng(-26.0, 134.0), expectedZoom = 4f, tolerance = 5.meters))
    }
    stylingActivity.showSouthChinaSea(null)
    with(stylingMap) {
      assertMap(isCenteredOn(LatLng(13.870237, 111.470760), expectedZoom = 4f, tolerance = 5.meters))
    }
    stylingActivity.setMapStyleTextOutline(null)
    assertGeo(stylingMap.shadow.mapStyle != null)
    stylingActivity.setMapStyleNoStyle(null)
    assertGeo(stylingMap.shadow.mapStyle == null)

    // 4. SaveStateDemoActivity (both Maps and Navigation fragments)
    val (saveActivity, saveMap) = launchDemo<SaveStateDemoActivity>(useNavFlavor = true)
    val paris = LatLng(48.858179, 2.294576)
    with(saveMap) {
      assertMap(markers.size == 1 && isCenteredOn(paris, tolerance = 5.meters))
    }
    val navSaveFragment =
      saveActivity.supportFragmentManager.findFragmentById(R.id.map)
        as SaveStateDemoActivity.SaveStateNavFragment
    val draggedPos = LatLng(48.860, 2.300)
    val saveMarker = saveMap.markers.first()
    saveMarker.position = draggedPos
    navSaveFragment.onMarkerDragEnd(saveMarker)
    navSaveFragment.onMarkerClick(saveMarker)
    val fragmentOut = Bundle()
    navSaveFragment.onSaveInstanceState(fragmentOut)
    assertGeo(fragmentOut.getParcelable<LatLng>("markerPosition") == draggedPos)

    // 5. MainActivity launcher flavor toggle button & item click intent
    val mainActivity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
    val flavorToggle = mainActivity.findViewById<ToggleButton>(R.id.view_source_toggle_btn)
    val demoListView = mainActivity.findViewById<ListView>(R.id.demoListView)
    assertGeo(!flavorToggle.isChecked && demoListView.adapter.count == DemoDetailsList.DEMOS.size)

    flavorToggle.isChecked = true
    demoListView.performItemClick(
      demoListView.adapter.getView(0, null, demoListView),
      0,
      0L,
    )
    val launchedIntent = shadowOf(mainActivity as Activity).nextStartedActivity
    assertGeo(
      launchedIntent != null &&
        launchedIntent.component?.className == BasicMapDemoActivity::class.java.name &&
        launchedIntent.getBooleanExtra(
          ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
          false,
        )
    )
  }

  @Test
  fun backgroundColorAndDayNightCircleAndStyledTileOverlayDemos_updateMapTypeCircleAndStyles() {
    // 1. BackgroundColorCustomizationDemoActivity
    val (bgActivity, bgMap) =
      launchDemo<BackgroundColorCustomizationDemoActivity>(useNavFlavor = true)
    with(bgMap) {
      assertMap(
        mapType == GoogleMap.MAP_TYPE_NONE &&
          markers.size == 1 &&
          hasMarker(title = "Marker", near = LatLng(0.0, 0.0))
      )
    }
    val mapTypeToggle = bgActivity.findViewById<CheckBox>(R.id.map_type_toggle)
    mapTypeToggle.isChecked = true
    assertGeo(bgMap.mapType == GoogleMap.MAP_TYPE_NORMAL)
    mapTypeToggle.isChecked = false
    assertGeo(bgMap.mapType == GoogleMap.MAP_TYPE_NONE)

    // 2. DayNightCircleDemoActivity
    val (dayNightActivity, dayNightMap) =
      launchDemo<DayNightCircleDemoActivity>(useNavFlavor = false)
    assertGeo(dayNightMap.circles.size == 1)
    val terminatorCircle = dayNightMap.circles.first()
    assertGeo(terminatorCircle.radius > 10_000_000.0 && terminatorCircle.center.longitude > 0.0)
    dayNightActivity.onDestroy()

    // 3. TopStylesDemoActivity
    val (topStylesActivity, topStylesMap) =
      launchDemo<TopStylesDemoActivity>(useNavFlavor = false)
    val stylesSpinner = topStylesActivity.findViewById<Spinner>(R.id.styles_spinner)
    stylesSpinner.setSelection(1)
    topStylesActivity.onItemSelected(stylesSpinner, null, 1, 1L)
    assertGeo(topStylesMap.shadow.mapStyle != null)

    val topTrafficBox = topStylesActivity.findViewById<CheckBox>(R.id.traffic)
    topTrafficBox.isChecked = true
    topStylesActivity.onTrafficToggled(topTrafficBox)
    assertGeo(topStylesMap.isTrafficEnabled)

    // 4. TileOverlayOnStyledMapDemoActivity
    val (styledTileActivity, styledTileMap) =
      launchDemo<TileOverlayOnStyledMapDemoActivity>(useNavFlavor = true)
    assertGeo(
      styledTileMap.shadow.mapStyle != null && styledTileMap.tileOverlays.size == 1
    )
    val moonOverlay = styledTileMap.tileOverlays.first()
    val transparencySeek = styledTileActivity.findViewById<SeekBar>(R.id.transparencySeekBar)
    styledTileActivity.onProgressChanged(transparencySeek, 40, true)
    assertGeo(moonOverlay.transparency == 0.4f)
    val fadeCheck = CheckBox(styledTileActivity).apply { isChecked = false }
    styledTileActivity.setFadeIn(fadeCheck)
    assertGeo(!moonOverlay.fadeIn)
    styledTileActivity.clearCache(null)
  }

  @Test
  fun markerAnchorsPoiClickProgrammaticToolbarAndSnapshotDemos_handleAllInteractions() {
    // 1. MarkerAnchorsDemoActivity
    val (anchorsActivity, anchorsMap) =
      launchDemo<MarkerAnchorsDemoActivity>(useNavFlavor = false)
    with(anchorsMap) {
      assertMap(
        markers.size == 2 &&
          hasMarker(title = "Adelaide", near = LatLng(-34.92873, 138.59995)) &&
          hasMarker(title = "Sydney", near = LatLng(-33.87365, 151.20689))
      )
    }
    val rotationSeek = anchorsActivity.findViewById<SeekBar>(R.id.rotationSeekBar)
    rotationSeek.progress = 135
    anchorsActivity.onProgressChanged(rotationSeek, 135, true)
    assertGeo(anchorsMap.markers.all { it.rotation == 135f })

    // 2. OnPoiClickDemoActivity
    val (poiActivity, poiMap) = launchDemo<OnPoiClickDemoActivity>(useNavFlavor = true)
    with(poiMap) {
      assertMap(isCenteredOn(LatLng(-33.87365, 151.20689), expectedZoom = 16f, tolerance = 5.meters))
    }
    val poi = PointOfInterest(LatLng(-33.87365, 151.20689), "place_id_123", "Queen Victoria Building")
    poiActivity.onPoiClick(poi)
    assertThat(ShadowToast.getTextOfLatestToast()).contains("Queen Victoria Building")

    // 3. ProgrammaticDemoActivity (Maps & Navigation flavors)
    val (progMapsActivity, progMaps) =
      launchDemo<ProgrammaticDemoActivity>(useNavFlavor = false)
    assertGeo(
      progMapsActivity.supportFragmentManager.findFragmentByTag("map") is SupportMapFragment &&
        progMaps.markers.size == 1
    )
    val (progNavActivity, progNav) =
      launchDemo<ProgrammaticDemoActivity>(useNavFlavor = true)
    assertGeo(
      progNavActivity.supportFragmentManager.findFragmentByTag("map") is SupportNavigationFragment &&
        progNav.markers.size == 1
    )

    // 4. ToolbarDemoActivity
    val (toolbarActivity, toolbarMap) =
      launchDemo<ToolbarDemoActivity>(useNavFlavor = false)
    with(toolbarMap) {
      assertMap(markers.isEmpty() && polygons.size == 1 && polylines.size == 1)
    }
    val toolbarToggle = toolbarActivity.findViewById<CheckBox>(R.id.enable_toolbar_globally)
    toolbarToggle.isChecked = false
    toolbarToggle.performClick()
    toolbarActivity.onMapLongClick(LatLng(-30.0, 140.0))
    assertGeo(toolbarMap.markers.size == 1)

    // 5. SnapshotDemoActivity
    val snapshotIntent =
      Intent(ApplicationProvider.getApplicationContext(), SnapshotDemoActivity::class.java)
    val snapshotActivity =
      Robolectric.buildActivity(SnapshotDemoActivity::class.java, snapshotIntent).setup().get()
    ShadowLooper.idleMainLooper()
    val snapshotHolder = snapshotActivity.findViewById<ImageView>(R.id.snapshot_holder)
    assertGeo(snapshotHolder.drawable == null)
    snapshotActivity.onScreenshot(null)
    ShadowLooper.idleMainLooper()
    assertGeo(snapshotHolder.drawable != null)
    snapshotActivity.onClearScreenshot(null)
    assertGeo(snapshotHolder.drawable == null)
  }

  private fun LayersDemoActivity.saveInstanceState(outState: Bundle) {
    val method =
      LayersDemoActivity::class.java.getDeclaredMethod("onSaveInstanceState", Bundle::class.java)
    method.isAccessible = true
    method.invoke(this, outState)
  }
}
