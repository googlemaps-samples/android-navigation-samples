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

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.example.navigationapidemo.testing.NavMapsRobolectricTest
import com.example.navigationapidemo.testing.NavShadowGoogleMap
import com.example.navigationapidemo.testing.NavShadowNavigationApi
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.ForceNightMode
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.OnNavigationUiChangedListener
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.ktx.cameraBearing
import com.google.maps.android.robolectric.ktx.cameraTarget
import com.google.maps.android.robolectric.ktx.cameraZoom
import com.google.maps.android.robolectric.ktx.createTestMap
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.distanceTo
import com.google.maps.android.robolectric.powerassert.isCenteredOn
import com.google.maps.android.robolectric.powerassert.meters
import com.google.maps.android.robolectric.truth.assertThat as assertThatMap
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

/**
 * Headless JVM Robolectric unit tests for [CustomizationPanelsDelegate].
 *
 * Verifies mutual exclusion of control panels, spinner callbacks for night mode and camera
 * perspectives, bidirectional synchronization with [GoogleMap] location-following state, and
 * camera movement to Melbourne using both **Kotlin Power-Assert** (`assertMap`, `assertGeo`) and
 * **Google Truth** (`assertThatMap`).
 */
@RunWith(RobolectricTestRunner::class)
class CustomizationPanelsDelegateTest : NavMapsRobolectricTest() {

  private lateinit var activity: AppCompatActivity
  private lateinit var googleMap: GoogleMap

  @Before
  fun setUp() {
    NavShadowNavigationApi.reset()
    NavShadowNavigationApi.autoDeliverResult = false
    activity = Robolectric.buildActivity(NavViewActivity::class.java).create().get()
    googleMap = createTestMap(width = 400, height = 400)
  }

  @After
  fun tearDown() {
    NavShadowNavigationApi.reset()
  }

  @Test
  fun initializeCustomizationPanels_enforcesMutualExclusionAcrossThreePanels() {
    val navToggle = activity.findViewById<Button>(R.id.controls_toggle_nav)
    val mapToggle = activity.findViewById<Button>(R.id.controls_toggle_map)
    val viewToggle = activity.findViewById<Button>(R.id.controls_toggle_view)

    val navPanel = activity.findViewById<LinearLayout>(R.id.controls_navigator)
    val mapPanel = activity.findViewById<LinearLayout>(R.id.controls_map)
    val viewPanel = activity.findViewById<LinearLayout>(R.id.controls_navview)

    // Initially all three customization panels are GONE.
    assertGeo(
      navPanel.visibility == View.GONE &&
        mapPanel.visibility == View.GONE &&
        viewPanel.visibility == View.GONE
    )

    // 1. Clicking "Nav methods" shows controls_navigator and hides others.
    navToggle.performClick()
    assertGeo(
      navPanel.visibility == View.VISIBLE &&
        mapPanel.visibility == View.GONE &&
        viewPanel.visibility == View.GONE
    )

    // 2. Clicking "Map methods" hides controls_navigator and shows controls_map.
    mapToggle.performClick()
    assertGeo(
      navPanel.visibility == View.GONE &&
        mapPanel.visibility == View.VISIBLE &&
        viewPanel.visibility == View.GONE
    )

    // 3. Clicking "View methods" hides controls_map and shows controls_navview.
    viewToggle.performClick()
    assertGeo(
      navPanel.visibility == View.GONE &&
        mapPanel.visibility == View.GONE &&
        viewPanel.visibility == View.VISIBLE
    )

    // 4. Clicking "View methods" a second time collapses controls_navview.
    viewToggle.performClick()
    assertGeo(
      navPanel.visibility == View.GONE &&
        mapPanel.visibility == View.GONE &&
        viewPanel.visibility == View.GONE
    )
  }

  @Test
  fun switchCustomizationUiVisibility_togglesBothButtonsAndPanelsScrollContainer() {
    val togglesContainer = activity.findViewById<LinearLayout>(R.id.control_toggles_container)
    val panelsScroll = activity.findViewById<ScrollView>(R.id.control_panels_scroll)

    assertGeo(
      togglesContainer.visibility == View.VISIBLE && panelsScroll.visibility == View.VISIBLE
    )

    CustomizationPanelsDelegate.switchCustomizationUiVisibility(activity)
    assertGeo(togglesContainer.visibility == View.GONE && panelsScroll.visibility == View.GONE)

    CustomizationPanelsDelegate.switchCustomizationUiVisibility(activity)
    assertGeo(
      togglesContainer.visibility == View.VISIBLE && panelsScroll.visibility == View.VISIBLE
    )
  }

  @Test
  fun setUpNightModeSpinner_mapsSpinnerSelectionsToForceNightModeConstants() {
    val selectedModes = mutableListOf<Int>()
    CustomizationPanelsDelegate.setUpNightModeSpinner(activity) { mode -> selectedModes.add(mode) }

    val spinner = activity.findViewById<Spinner>(R.id.night_mode_spinner)
    val listener = checkNotNull(spinner.onItemSelectedListener)
    val dummyItemView = View(activity)

    assertGeo(spinner.adapter.count == 4)

    // Position 0 is the header prompt ("Night Mode Options") and should be a no-op.
    listener.onItemSelected(spinner, dummyItemView, 0, 0L)
    assertGeo(selectedModes.isEmpty())

    listener.onItemSelected(spinner, dummyItemView, 1, 1L)
    listener.onItemSelected(spinner, dummyItemView, 2, 2L)
    listener.onItemSelected(spinner, dummyItemView, 3, 3L)

    assertThat(selectedModes)
      .containsExactly(
        ForceNightMode.AUTO,
        ForceNightMode.FORCE_DAY,
        ForceNightMode.FORCE_NIGHT,
      )
      .inOrder()
  }

  @Test
  fun cameraPerspectiveSpinner_andFollowLocationCallback_synchronizePerspectiveState() {
    val selectedPerspectives = mutableListOf<Int>()
    CustomizationPanelsDelegate.setUpCameraPerspectiveSpinner(activity) { perspective ->
      selectedPerspectives.add(perspective)
      googleMap.followMyLocation(perspective)
    }
    CustomizationPanelsDelegate.registerOnCameraFollowLocationCallback(activity, googleMap)

    val spinner = activity.findViewById<Spinner>(R.id.follow_my_location_spinner)
    val dummyItemView = View(activity)
    val shadowMap = Shadow.extract<NavShadowGoogleMap>(googleMap)

    // Select "Following: North up" (position 2). Note: setSelection(2) on Robolectric's
    // ShadowAbsSpinner automatically dispatches onItemSelected to onItemSelectedListener.
    spinner.setSelection(2)

    assertGeo(
      selectedPerspectives.last() == GoogleMap.CameraPerspective.TOP_DOWN_NORTH_UP &&
        shadowMap.lastCameraPerspective == GoogleMap.CameraPerspective.TOP_DOWN_NORTH_UP &&
        googleMap.isCameraFollowingMyLocation
    )

    // Simulate camera stopping following location (e.g. user pans map or moves to Melbourne).
    shadowMap.simulateCameraStoppedFollowingLocation()
    assertGeo(spinner.selectedItemPosition == 0 && !googleMap.isCameraFollowingMyLocation)

    // Simulate camera starting to follow location again: spinner should restore position 2
    // ("Following: North up") without re-firing the selection callback.
    val callbackCountBeforeRestore = selectedPerspectives.size
    shadowMap.simulateCameraStartedFollowingLocation()
    assertGeo(
      spinner.selectedItemPosition == 2 && selectedPerspectives.size == callbackCountBeforeRestore
    )
  }

  @Test
  fun moveCameraToMelbourne_centersMapOnMelbourneAtZoom10_powerAssertAndTruth() {
    val melbourne = LatLng(-37.813, 144.962)

    CustomizationPanelsDelegate.moveCameraToMelbourne(activity, googleMap)

    // 1. Verify using android-maps-testing Kotlin Power-Assert helpers
    with(googleMap) {
      assertMap(
        isCenteredOn(melbourne, expectedZoom = 10f, tolerance = 5.meters) &&
          (cameraTarget distanceTo melbourne) <= 5.meters &&
          cameraZoom == 10f &&
          cameraBearing == 0f
      )
    }

    // 2. Verify using android-maps-testing Google Truth subject for Java interop parity
    assertThatMap(googleMap).isCenteredOn(melbourne, 5.meters).hasZoom(10f)
  }

  @Test
  fun toggleSetMyLocationEnabled_andViewToggles_updateMapAndCallbacks() {
    val myLocationToggle = activity.findViewById<ToggleButton>(R.id.btn_set_my_location_enabled)
    myLocationToggle.isChecked = true
    CustomizationPanelsDelegate.toggleSetMyLocationEnabled(activity, googleMap)
    assertGeo(googleMap.isMyLocationEnabled)

    myLocationToggle.isChecked = false
    CustomizationPanelsDelegate.toggleSetMyLocationEnabled(activity, googleMap)
    assertGeo(!googleMap.isMyLocationEnabled)

    // Verify Navigation UI toggle & listener registration
    val navUiToggle = activity.findViewById<ToggleButton>(R.id.btn_navigation_ui_mode)
    var capturedListener: OnNavigationUiChangedListener? = null
    CustomizationPanelsDelegate.registerOnNavigationUiChangedListener(activity) { listener ->
      capturedListener = listener
    }
    val nonNullListener = checkNotNull(capturedListener)
    nonNullListener.onNavigationUiChanged(false)
    assertGeo(!navUiToggle.isChecked)

    nonNullListener.onNavigationUiChanged(true)
    assertGeo(navUiToggle.isChecked)

    var lastTripProgressEnabled: Boolean? = null
    val progressToggle = activity.findViewById<ToggleButton>(R.id.btn_progress_bar)
    progressToggle.isChecked = true
    CustomizationPanelsDelegate.toggleTripProgressBarUI(activity) { enabled ->
      lastTripProgressEnabled = enabled
    }
    assertGeo(lastTripProgressEnabled == true)
  }

  @Test
  fun toggleNavForwarding_startsAndStopsNavForwardingFragmentLifecycle() {
    val mockNavigator = mock(Navigator::class.java)
    `when`(
        mockNavigator.registerServiceForNavUpdates(
          activity.packageName,
          NavInfoReceivingService::class.java.name,
          Int.MAX_VALUE,
        )
      )
      .thenReturn(true)
    `when`(mockNavigator.unregisterServiceForNavUpdates()).thenReturn(true)

    // 1. First toggle starts nav forwarding and attaches NavInfoDisplayFragment
    val activeFragment =
      CustomizationPanelsDelegate.toggleNavForwarding(
        activity = activity,
        navigator = mockNavigator,
        existingFragment = null,
      )
    assertGeo(activeFragment is NavInfoDisplayFragment)
    verify(mockNavigator)
      .registerServiceForNavUpdates(
        activity.packageName,
        NavInfoReceivingService::class.java.name,
        Int.MAX_VALUE,
      )

    // 2. Second toggle stops nav forwarding and returns null
    val afterStop =
      CustomizationPanelsDelegate.toggleNavForwarding(
        activity = activity,
        navigator = mockNavigator,
        existingFragment = activeFragment,
      )
    assertGeo(afterStop == null)
    verify(mockNavigator).unregisterServiceForNavUpdates()
  }
}
