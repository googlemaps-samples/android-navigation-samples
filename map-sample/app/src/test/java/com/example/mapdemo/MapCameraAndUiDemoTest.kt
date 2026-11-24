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
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.test.core.app.ApplicationProvider
import com.example.navtesting.NavMapsRobolectricTest
import com.example.navtesting.NavShadowSupportNavigationFragment
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
import com.google.maps.android.robolectric.ktx.findMarker
import com.google.maps.android.robolectric.ktx.markers
import com.google.maps.android.robolectric.ktx.polygons
import com.google.maps.android.robolectric.ktx.polylines
import com.google.maps.android.robolectric.ktx.shadow
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.assertMarker
import com.google.maps.android.robolectric.powerassert.contains
import com.google.maps.android.robolectric.powerassert.distanceTo
import com.google.maps.android.robolectric.powerassert.hasMarker
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
 * Headless Robolectric tests for `map-sample` launcher, camera, UI settings, events, POI click,
 * toolbar, programmatic fragment, and snapshot demos using Kotlin Power-Assert.
 */
@RunWith(RobolectricTestRunner::class)
class MapCameraAndUiDemoTest : NavMapsRobolectricTest() {

  @Test
  fun mainActivity_togglesFlavorAndLaunchesSelectedDemo() {
    val mainActivity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
    val flavorToggle = mainActivity.findViewById<ToggleButton>(R.id.view_source_toggle_btn)
    val demoListView = mainActivity.findViewById<ListView>(R.id.demoListView)
    assertGeo(!flavorToggle.isChecked && demoListView.adapter.count == DemoDetailsList.DEMOS.size)

    flavorToggle.isChecked = true
    demoListView.performItemClick(demoListView.adapter.getView(0, null, demoListView), 0, 0L)
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
  fun basicMapDemoActivity_addsMarkerAtOriginInBothFlavors() {
    for (useNavFlavor in listOf(false, true)) {
      val (activity, map) = launchMapDemo<BasicMapDemoActivity>(useNavFlavor)
      val fragment = activity.supportFragmentManager.findFragmentById(R.id.map)
      assertGeo(if (useNavFlavor) fragment is SupportNavigationFragment else fragment is SupportMapFragment)

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
  fun cameraDemoActivity_movesBetweenBondiAndSydney() {
    val bondi = LatLng(-33.891614, 151.276417)
    val sydney = LatLng(-33.87365, 151.20689)
    val (activity, map) = launchMapDemo<CameraDemoActivity>(useNavFlavor = true)

    with(map) {
      assertMap(isCenteredOn(sydney, expectedZoom = 10f, tolerance = 5.meters))
    }

    activity.onGoToBondi(null)
    with(map) {
      assertMap(
        isCenteredOn(bondi, expectedZoom = 15.5f, tolerance = 5.meters) &&
          cameraBearing == 300f &&
          cameraTilt == 50f
      )
    }

    activity.onGoToSydney(null)
    with(map) {
      assertMap(
        isCenteredOn(sydney, expectedZoom = 15.5f, tolerance = 5.meters) &&
          cameraBearing == 0f &&
          cameraTilt == 25f
      )
    }
  }

  @Test
  fun cameraDemoActivity_zoomsScrollsAndTracesCameraPath() {
    val (activity, map) = launchMapDemo<CameraDemoActivity>(useNavFlavor = false)

    val zoomBefore = map.cameraZoom
    activity.onZoomIn(null)
    assertGeo(map.cameraZoom == zoomBefore + 1.0f)
    activity.onZoomOut(null)
    assertGeo(map.cameraZoom == zoomBefore)

    val targetBeforeScroll = map.cameraTarget
    activity.onScrollUp(null)
    assertGeo(map.cameraTarget.latitude > targetBeforeScroll.latitude)
    activity.onScrollDown(null)
    activity.onScrollLeft(null)
    assertGeo(map.cameraTarget.longitude < targetBeforeScroll.longitude)
    activity.onScrollRight(null)

    activity.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE)
    activity.onCameraMove()
    activity.onCameraIdle()
    assertGeo(map.polylines.isNotEmpty())
  }

  @Test
  fun cameraClampingDemoActivity_clampsBoundsAndZoomPreferences() {
    val adelaide = LatLng(-34.92873, 138.59995)
    val (activity, map) = launchMapDemo<CameraClampingDemoActivity>(useNavFlavor = false)

    activity.onClampToAdelaide(null)
    with(map) {
      assertMap(
        isCenteredOn(adelaide, expectedZoom = 20.0f, tolerance = 5.meters) &&
          adelaide in checkNotNull(shadow.latLngBoundsForCameraTarget)
      )
    }

    activity.onClampToPacific(null)
    assertGeo(map.cameraZoom == 4.0f && map.shadow.latLngBoundsForCameraTarget != null)

    activity.onLatLngClampReset(null)
    assertGeo(map.shadow.latLngBoundsForCameraTarget == null)

    activity.onSetMinZoomClamp(null)
    activity.onSetMaxZoomClamp(null)
    assertGeo(map.minZoomLevel == 4.0f && map.maxZoomLevel == 20.0f)

    activity.onMinMaxZoomClampReset(null)
    assertGeo(map.minZoomLevel == 2.0f && map.maxZoomLevel == 21.0f)

    activity.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE)
    val cameraText = activity.findViewById<TextView>(R.id.camera_text).text.toString()
    assertGeo(cameraText.contains("REASON_GESTURE"))
  }

  @Test
  fun uiSettingsDemoActivity_synchronizesGestureAndControlToggles() {
    val (activity, map) = launchMapDemo<UiSettingsDemoActivity>(useNavFlavor = true)
    val uiSettings = map.uiSettings
    val unchecked = CheckBox(activity).apply { isChecked = false }

    activity.setZoomButtonsEnabled(unchecked)
    activity.setCompassEnabled(unchecked)
    activity.setScrollGesturesEnabled(unchecked)
    activity.setZoomGesturesEnabled(unchecked)
    activity.setTiltGesturesEnabled(unchecked)
    activity.setRotateGesturesEnabled(unchecked)
    activity.setScrollGesturesEnabledDuringRotateOrZoom(unchecked)

    assertGeo(
      !uiSettings.isZoomControlsEnabled &&
        !uiSettings.isCompassEnabled &&
        !uiSettings.isScrollGesturesEnabled &&
        !uiSettings.isZoomGesturesEnabled &&
        !uiSettings.isTiltGesturesEnabled &&
        !uiSettings.isRotateGesturesEnabled &&
        !uiSettings.isScrollGesturesEnabledDuringRotateOrZoom
    )
  }

  @Test
  fun visibleRegionDemoActivity_disablesNavCardsAndMovesBetweenLandmarks() {
    val sfo = LatLng(37.614631, -122.385153)
    val operaHouse = LatLng(-33.85704, 151.21522)
    val (activity, map) = launchMapDemo<VisibleRegionDemoActivity>(useNavFlavor = true)

    val navFragment =
      activity.supportFragmentManager.findFragmentById(R.id.map) as SupportNavigationFragment
    val shadowNav = Shadow.extract<NavShadowSupportNavigationFragment>(navFragment)
    assertGeo(!shadowNav.isHeaderEnabledState && !shadowNav.isEtaCardEnabledState)

    with(map) {
      assertMap(
        isCenteredOn(sfo, expectedZoom = 18f, tolerance = 5.meters) &&
          hasMarker(title = "Sydney Opera House", near = operaHouse) &&
          shadow.paddingLeft == 150
      )
    }

    activity.moveToOperaHouse(null)
    with(map) { assertMap(isCenteredOn(operaHouse, expectedZoom = 16f, tolerance = 5.meters)) }

    activity.moveToAUS(null)
    assertGeo(map.cameraTarget.distanceTo(operaHouse) < 4000.kilometers)

    activity.moveToSFO(null)
    with(map) { assertMap(isCenteredOn(sfo, expectedZoom = 18f, tolerance = 5.meters)) }
  }

  @Test
  fun eventsDemoActivity_recordsMapClickAndLongClickCoordinates() {
    val (activity, _) = launchMapDemo<EventsDemoActivity>(useNavFlavor = false)
    val clickPoint = LatLng(37.422, -122.084)
    val tapText = activity.findViewById<TextView>(R.id.tap_text)

    activity.onMapClick(clickPoint)
    assertGeo(tapText.text.toString().contains(clickPoint.toString()))

    activity.onMapLongClick(clickPoint)
    assertGeo(tapText.text.toString().contains("long pressed"))
  }

  @Test
  fun onPoiClickDemoActivity_centersOnSydneyAndShowsPoiToast() {
    val (activity, map) = launchMapDemo<OnPoiClickDemoActivity>(useNavFlavor = true)
    with(map) {
      assertMap(isCenteredOn(LatLng(-33.87365, 151.20689), expectedZoom = 16f, tolerance = 5.meters))
    }

    val poi = PointOfInterest(LatLng(-33.87365, 151.20689), "place_id_123", "Queen Victoria Building")
    activity.onPoiClick(poi)
    assertThat(ShadowToast.getTextOfLatestToast()).contains("Queen Victoria Building")
  }

  @Test
  fun programmaticDemoActivity_attachesFragmentAndAddsMarkerInBothFlavors() {
    val (mapsActivity, mapsMap) = launchMapDemo<ProgrammaticDemoActivity>(useNavFlavor = false)
    assertGeo(
      mapsActivity.supportFragmentManager.findFragmentByTag("map") is SupportMapFragment &&
        mapsMap.markers.size == 1
    )

    val (navActivity, navMap) = launchMapDemo<ProgrammaticDemoActivity>(useNavFlavor = true)
    assertGeo(
      navActivity.supportFragmentManager.findFragmentByTag("map") is SupportNavigationFragment &&
        navMap.markers.size == 1
    )
  }

  @Test
  fun toolbarDemoActivity_addsOverlaysAndCreatesMarkerOnLongClick() {
    val (activity, map) = launchMapDemo<ToolbarDemoActivity>(useNavFlavor = false)
    with(map) {
      assertMap(markers.isEmpty() && polygons.size == 1 && polylines.size == 1)
    }

    val toolbarToggle = activity.findViewById<CheckBox>(R.id.enable_toolbar_globally)
    toolbarToggle.isChecked = false
    toolbarToggle.performClick()
    activity.onMapLongClick(LatLng(-30.0, 140.0))
    assertGeo(map.markers.size == 1)
  }

  @Test
  fun snapshotDemoActivity_capturesAndClearsSnapshotBitmap() {
    val intent = Intent(ApplicationProvider.getApplicationContext(), SnapshotDemoActivity::class.java)
    val activity = Robolectric.buildActivity(SnapshotDemoActivity::class.java, intent).setup().get()
    ShadowLooper.idleMainLooper()
    val snapshotHolder = activity.findViewById<ImageView>(R.id.snapshot_holder)

    assertGeo(snapshotHolder.drawable == null)
    activity.onScreenshot(null)
    ShadowLooper.idleMainLooper()
    assertGeo(snapshotHolder.drawable != null)
    activity.onClearScreenshot(null)
    assertGeo(snapshotHolder.drawable == null)
  }
}
