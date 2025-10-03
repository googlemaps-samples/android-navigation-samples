/*
 * Copyright 2024 Google LLC
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

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.navigationapidemo.CustomizationPanelsDelegate.logDebugInfo
import com.example.navigationapidemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Navigator.RouteStatus
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.navigation.Waypoint.UnsupportedPlaceIdException
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import java.lang.Exception

/**
 * This activity shows a simple Navigation API implementation using a Navigation view and using the
 * Google Places API for destination selection.
 */
private const val TAG = "NavViewActivity"
private const val PLACE_PICKER_REQUEST = 1

class NavViewActivity : AppCompatActivity() {
  private lateinit var navView: NavigationView
  var navigatorScope: InitializedNavScope? = null
  var pendingNavActions = mutableListOf<InitializedNavRunnable>()
  private var arrivalListener: Navigator.ArrivalListener? = null
  private var routeChangedListener: Navigator.RouteChangedListener? = null

  // Only used to demo the turn-by-turn nav forwarding feature.
  var navInfoDisplayFragment: Fragment? = null

  @SuppressLint("MissingPermission") // TODO: requestPermissions(...) in here or earlier
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_nav_view)

    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
      listOf(EdgeToEdgeMarginConfig(view = findViewById(R.id.nav_view_layout_container)))
    )

    navView = findViewById(R.id.navigation_view)

    navView.onCreate(savedInstanceState)

    // Set up the UI that allows the user to control some NavSDK behaviors in the demo app.
    // These panels set up all the users' selectable options, like whether to show the trip
    // progress bar, whether to force night mode, etc.
    CustomizationPanelsDelegate.initializeCustomizationPanels(this)
    CustomizationPanelsDelegate.setUpNightModeSpinner(this, navView::setForceNightMode)

    // Ensure the screen stays on during nav.
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Register some example listeners for navigation events.
    registerNavigationListeners()

    initializeNavigationApi()
  }

  /**
   * Runs [block] once map is initialized. Block is ignored if map is never initialized.
   *
   * This ensures that calls using the map before the map is initialized gets executed after the map
   * has been initialized.
   */
  private fun withMapAsync(block: InitializedMapScope.() -> Unit) {
    navView.getMapAsync { map ->
      object : InitializedMapScope {
          override val map = map
        }
        .block()
    }
  }

  /**
   * Runs [block] once navigator is initialized. Block is ignored if the navigator is never
   * initialized (error, etc.).
   *
   * This ensures that calls using the navigator before the navigator is initialized gets executed
   * after the navigator has been initialized.
   */
  private fun withNavigatorAsync(block: InitializedNavRunnable) {
    val navigatorScope = navigatorScope
    if (navigatorScope != null) {
      navigatorScope.block()
    } else {
      pendingNavActions.add(block)
    }
  }

  /** Starts the Navigation API, capturing a reference when ready. */
  private fun initializeNavigationApi() {
    NavigationApi.getNavigator(
      this,
      object : NavigatorListener {
        override fun onNavigatorReady(navigator: Navigator) {
          val scope = InitializedNavScope(navigator)
          navigatorScope = scope
          pendingNavActions.forEach { block -> scope.block() }
          pendingNavActions.clear()

          // Disables the guidance notifications and shuts down the app and background service
          // when the user dismisses/swipes away the app from Android's recent tasks.
          navigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)
        }

        override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
          when (errorCode) {
            NavigationApi.ErrorCode.NOT_AUTHORIZED -> {
              // Note: If this message is displayed, you may need to check that
              // your API_KEY is specified correctly in AndroidManifest.xml
              // and is been enabled to access the Navigation API
              showToast(
                "Error loading Navigation API: Your API key is " +
                  "invalid or not authorized to use Navigation."
              )
            }
            NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> {
              showToast(
                "Error loading Navigation API: User did not " +
                  "accept the Navigation Terms of Use."
              )
            }
            else -> showToast("Error loading Navigation API: $errorCode")
          }
        }
      },
    )

    withMapAsync {
      CustomizationPanelsDelegate.setUpCameraPerspectiveSpinner(
        this@NavViewActivity,
        map::followMyLocation,
      )
      // The logic below simply helps keep the UI in tune with the underlying SDK state.
      CustomizationPanelsDelegate.registerOnCameraFollowLocationCallback(this@NavViewActivity, map)

      CustomizationPanelsDelegate.registerOnNavigationUiChangedListener(
        this@NavViewActivity,
        navView::addOnNavigationUiChangedListener,
      )
    }
  }

  /**
   * Registers a number of example event listeners that show an on screen message when certain
   * navigation events occur (e.g. the driver's route changes or the destination is reached).
   */
  private fun registerNavigationListeners() {
    withNavigatorAsync {
      arrivalListener =
        Navigator.ArrivalListener { // Show an onscreen message
          showToast("User has arrived at the destination!")
          navigator.clearDestinations()

          // Stop simulating vehicle movement.
          if (BuildConfig.DEBUG) {
            navigator.simulator?.unsetUserLocation()
          }
        }
      navigator.addArrivalListener(arrivalListener)

      routeChangedListener =
        Navigator.RouteChangedListener { // Show an onscreen message when the route changes
          showToast("onRouteChanged: the driver's route changed")
        }
      navigator.addRouteChangedListener(routeChangedListener)
    }
  }

  /**
   * Requests directions from the user's current location to a specific place (provided by the
   * Google Places API).
   */
  private fun navigateToPlace(place: Place) {
    val waypoint: Waypoint? =
      if (place.getPlaceTypes()?.contains(PlaceTypes.GEOCODE) == true) {
        // An example of setting a destination via Lat-Lng.
        // Note: Setting LatLng destinations can result in poor routing quality/ETA calculation.
        // Wherever possible you should use a Place ID to describe the destination accurately.
        place.latLng?.let { Waypoint.builder().setLatLng(it.latitude, it.longitude).build() }
      } else {
        // Set a destination by using a Place ID (the recommended method)
        try {
          Waypoint.builder().setPlaceIdString(place.id).build()
        } catch (e: UnsupportedPlaceIdException) {
          showToast("Place ID was unsupported.")
          return
        }
      }

    withNavigatorAsync {
      val pendingRoute = navigator.setDestination(waypoint)

      // Set an action to perform when a route is determined to the destination
      pendingRoute?.setOnResultListener { code ->
        when (code) {
          RouteStatus.OK -> {
            // Hide the toolbar to maximize the navigation UI
            actionBar?.hide()

            // Enable voice audio guidance (through the device speaker)
            navigator.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)

            // Simulate vehicle progress along the route (for demo/debug builds)
            if (BuildConfig.DEBUG) {
              navigator.simulator.simulateLocationsAlongExistingRoute(
                SimulationOptions().speedMultiplier(5f)
              )
            }

            // Start turn-by-turn guidance along the current route
            navigator.startGuidance()
          }
          RouteStatus.ROUTE_CANCELED -> showToast("Route guidance cancelled.")
          RouteStatus.NO_ROUTE_FOUND,
          RouteStatus.NETWORK_ERROR ->
            // TODO: Add logic to handle when a route could not be determined
            showToast("Error starting guidance: $code")
          else -> showToast("Error starting guidance: $code")
        }
      }
    }
  }

  override fun onSaveInstanceState(savedInstanceState: Bundle) {
    super.onSaveInstanceState(savedInstanceState)

    navView.onSaveInstanceState(savedInstanceState)
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    navView.onTrimMemory(level)
  }

  override fun onStart() {
    super.onStart()
    navView.onStart()
  }

  override fun onResume() {
    super.onResume()
    navView.onResume()
  }

  override fun onPause() {
    navView.onPause()
    super.onPause()
  }

  override fun onConfigurationChanged(configuration: Configuration) {
    super.onConfigurationChanged(configuration)
    navView.onConfigurationChanged(configuration)
  }

  override fun onStop() {
    navView.onStop()
    super.onStop()
  }

  override fun onDestroy() {
    navView.onDestroy()
    withNavigatorAsync {
      // Unregister event listeners to avoid memory leaks.
      if (arrivalListener != null) {
        navigator.removeArrivalListener(arrivalListener)
      }
      if (routeChangedListener != null) {
        navigator.removeRouteChangedListener(routeChangedListener)
      }

      navigator.simulator?.unsetUserLocation()
      navigator.cleanup()
    }
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.menu_default, menu)
    return true
  }

  /** If the Place Picker activity returns a destination, starts navigation to that place. */
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
      data?.let {
        val place: Place = PlacePickerActivity.getPlace(it)
        navigateToPlace(place)
      }
    }
  }

  /**
   * Uses the Google Places API Place Picker to choose a destination to navigate to.
   *
   * This method is referenced by the "Set Destination" item in menu_default.xml
   */
  fun showPlacePickerForDestination(v: MenuItem?): Boolean {
    try {
      startActivityForResult(Intent(this, PlacePickerActivity::class.java), PLACE_PICKER_REQUEST)
    } catch (e: Exception) {
      showToast(
        "Could not display Place Picker. Check your API key has the Google" + "Places API enabled."
      )
      Log.e(TAG, Log.getStackTraceString(e))
    }
    return true
  }

  /**
   * Switches the visibility of the UI of the customization panels and the toggle buttons.
   *
   * This method is referenced by the "Switch Customizations UI On/Off" item in menu_default.xml.
   */
  fun switchCustomizationUIVisibility(unused: MenuItem?) {
    CustomizationPanelsDelegate.switchCustomizationUiVisibility(this)
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // OnClick listeners for various buttons in the customization panels.
  //
  ////////////////////////////////////////////////////////////////////////////////////////////////
  /** Toggles whether the Navigation UI is enabled. */
  fun toggleNavigationUiEnabled(unused: View?) {
    CustomizationPanelsDelegate.toggleNavigationUiEnabled(this, navView::setNavigationUiEnabled)
  }

  /** Toggles navigation forwarding (e.g. for 2-wheeler projection). */
  fun toggleNavFwding(unused: View?) {
    withNavigatorAsync {
      navInfoDisplayFragment =
        CustomizationPanelsDelegate.toggleNavForwarding(
          this@NavViewActivity,
          navigator,
          navInfoDisplayFragment,
        )
    }
  }

  /** Moves the position of the camera to hover over Melbourne. */
  fun moveCameraToMelbourne(unused: View?) {
    withMapAsync { CustomizationPanelsDelegate.moveCameraToMelbourne(this@NavViewActivity, map) }
  }

  /** Toggles whether the location marker is enabled. */
  fun toggleSetMyLocationEnabled(unused: View?) {
    withMapAsync {
      CustomizationPanelsDelegate.toggleSetMyLocationEnabled(this@NavViewActivity, map)
    }
  }

  /** Toggles the visibility of the Trip Progress Bar UI. This is an EXPERIMENTAL FEATURE. */
  fun toggleTripProgressBarUi(unused: View?) {
    CustomizationPanelsDelegate.toggleTripProgressBarUI(this, navView::setTripProgressBarEnabled)
  }

  /** Logs some debug information to the logcat from the Navigator, upon user request. */
  fun logDebugInfo(unused: View?) {
    withNavigatorAsync {
      navigator.logDebugInfo()
      showToast("Check the logcat for some information about your trip!")
    }
  }

  private fun showToast(errorMessage: String) {
    Toast.makeText(this@NavViewActivity, errorMessage, Toast.LENGTH_LONG).show()
  }
}
