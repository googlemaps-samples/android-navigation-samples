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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.navigationapidemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Navigator.RouteStatus
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.navigation.Waypoint.UnsupportedPlaceIdException
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import java.lang.ref.WeakReference

/**
 * This activity showcases a common use-case for customers onboarding to Android Navigation SDK v4+
 * from v1.
 *
 * Customers of the v1 Navigation SDK historically have integrated the Google Play Services Maps
 * client into their builds side-by-side with the Navigation SDK. In that usage pattern, the
 * adopting app would show a SupportMapFragment while the driver is out-of-navigation, and switch
 * over to a SupportNavigationFragment once the driver has selected a destination. At the end of the
 * trip, or upon user cancellation, the customer would return to showing a SupportMapFragment.
 *
 * This activity mimics that behavior to support any customers who want to temporarily continue this
 * usage pattern. We recommend that v1 customers eventually transition to exclusively using a
 * NavigationView or SupportNavigationFragment, since these classes support the GoogleMap interface.
 */
class SwappingMapAndNavActivity : AppCompatActivity() {
  private var navigator: Navigator? = null
  private lateinit var mapFragment: SupportMapFragment
  private lateinit var navigationFragment: SupportNavigationFragment
  private var arrivalListener: Navigator.ArrivalListener? = null
  private var navigationSessionListener: Navigator.NavigationSessionListener? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_swapping_map_and_nav)

    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
      listOf(EdgeToEdgeMarginConfig(view = findViewById(R.id.container)))
    )

    mapFragment = SupportMapFragment()
    navigationFragment = SupportNavigationFragment()

    // Start with a basic map on the screen.
    if (savedInstanceState == null) {
      // This is the first instance of the Activity, so the Fragment will not exist yet. If the
      // savedInstance state were not null, child Fragments that already exist will be implicitly
      // reattached.
      //
      // In the case of a new Activity, in onCreate(), it is safe to commit a Fragment into an
      // otherwise empty root container. This will overlay the Fragment over the entire Activity's
      // View hierarchy, and is a great way to commit the root Fragment.
      supportFragmentManager
        .beginTransaction()
        .add(R.id.container, mapFragment, MAP_FRAGMENT_TAG)
        .commitNow()
    }

    // Ensure the screen stays on during nav.
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    // Initialize Navigation
    NavigationApi.getNavigator(/* activity= */ this, NavigatorListenerImpl(/* activity= */ this))
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.menu_swapping_map_and_nav, menu)
    return true
  }

  /**
   * Uses the Google Places API Place Picker to choose a destination to navigate to.
   *
   * This method is referenced by the "Set Destination" item in menu_swapping_map_and_nav.xml.
   */
  fun showPlacePickerForDestination(v: MenuItem?): Boolean {
    try {
      startActivityForResult(Intent(this, PlacePickerActivity::class.java), PLACE_PICKER_REQUEST)
    } catch (e: Exception) {
      showToast(
        "Could not display Place Picker. Check your API key has the Google" + "Places API enabled."
      )
    }
    return true
  }

  /** If the Place Picker activity returns a destination, starts navigation to that place. */
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PLACE_PICKER_REQUEST) {
      if (resultCode == RESULT_OK) {
        data?.let {
          val place: Place = PlacePickerActivity.getPlace(it)
          navigateToPlace(place)
        }
      }
    }
  }

  /**
   * Requests directions from the user's current location to a specific place (provided by the
   * Google Places API).
   */
  private fun navigateToPlace(place: Place) {
    val waypoint: Waypoint? =
      if (place.placeTypes?.contains(PlaceTypes.GEOCODE) == true) {
        // Show an example of setting a destination Lat-Lng
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
    val pendingRoute = navigator?.setDestination(waypoint)

    // Set an action to perform when a route is determined to the destination
    pendingRoute?.setOnResultListener { code ->
      when (code) {
        RouteStatus.OK -> {
          // Hide the toolbar to maximize the navigation UI
          actionBar?.hide()

          // Simulate vehicle progress along the route (for demo/debug builds)
          if (BuildConfig.DEBUG) {
            navigator
              ?.simulator
              ?.simulateLocationsAlongExistingRoute(SimulationOptions().speedMultiplier(5f))
          }

          // Start turn-by-turn guidance along the current route
          navigator?.startGuidance()
          // And show the NavFragment to the user.
          if (supportFragmentManager.findFragmentById(R.id.container) is SupportMapFragment) {
            detachOldFragmentAndAddOrAttachNewFragment(
              /*fragmentToDetach=*/ mapFragment,
              /*fragmentToAttach=*/ navigationFragment,
              NAVIGATION_FRAGMENT_TAG,
            )
          }
        }
        RouteStatus.ROUTE_CANCELED -> // Return to top-down perspective
        showToast("Route guidance cancelled.")
        RouteStatus.NO_ROUTE_FOUND,
        RouteStatus
          .NETWORK_ERROR -> // TODO: Add logic to handle when a route could not be determined
        showToast("Error starting guidance: $code")
        else -> showToast("Error starting guidance: $code")
      }
    }
  }

  /**
   * Ends in-progress guided navigation (if any) and displays the SupportMapFragment.
   *
   * This method is referenced by the "Stop trip and Show MapFragment" item in
   * menu_swapping_map_and_nav.xml.
   */
  fun stopTripAndShowMapFragment(unused: MenuItem?) {
    navigator?.clearDestinations()
    if (supportFragmentManager.findFragmentById(R.id.container) is SupportNavigationFragment) {
      detachOldFragmentAndAddOrAttachNewFragment(
        /*fragmentToDetach=*/ navigationFragment,
        /*fragmentToAttach=*/ mapFragment,
        MAP_FRAGMENT_TAG,
      )
    }
  }

  private fun registerListeners() {
    arrivalListener =
      Navigator.ArrivalListener {
        showToast("User has arrived at the destination!")
        navigator?.stopGuidance()

        // Stop simulating vehicle movement.
        if (BuildConfig.DEBUG) {
          navigator?.simulator?.unsetUserLocation()
        }

        // Switch back to the MapView.
        stopTripAndShowMapFragment(/* unused= */ null)
      }
    navigator?.addArrivalListener(arrivalListener)

    navigationSessionListener =
      Navigator.NavigationSessionListener {
        // Enable voice audio guidance (through the device speaker)
        navigator?.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
      }

    navigator?.addNavigationSessionListener(navigationSessionListener)
  }

  // Detaches old fragment and adds a new fragment to the activity if it's not added otherwise
  // attaches the new fragment.
  private fun detachOldFragmentAndAddOrAttachNewFragment(
    fragmentToDetach: Fragment,
    fragmentToAttach: Fragment,
    addFragmentTag: String,
  ) {
    val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
    fragmentTransaction.detach(fragmentToDetach)
    if (fragmentToAttach.isDetached()) {
      // Attach the fragment if the fragment is already detached.
      fragmentTransaction.attach(fragmentToAttach).commitNow()
    } else if (!fragmentToAttach.isAdded()) {
      // Add the fragment if it's not added.
      fragmentTransaction.add(R.id.container, fragmentToAttach, addFragmentTag).commitNow()
    }
  }

  private fun showToast(errorMessage: String) {
    Toast.makeText(this@SwappingMapAndNavActivity, errorMessage, Toast.LENGTH_LONG).show()
  }

  override fun onDestroy() {
    if (arrivalListener != null) {
      navigator?.removeArrivalListener(arrivalListener)
    }
    if (navigationSessionListener != null) {
      navigator?.removeNavigationSessionListener(navigationSessionListener)
    }

    navigator?.simulator?.unsetUserLocation()
    navigator?.cleanup()
    super.onDestroy()
  }

  private companion object {
    const val PLACE_PICKER_REQUEST = 1
    const val MAP_FRAGMENT_TAG = "map_fragment"
    const val NAVIGATION_FRAGMENT_TAG = "navigation_fragment"

    /**
     * Implements [com.google.android.libraries.navigation.NavigationApi.NavigatorListener]
     *
     * Instances of the listener do not hold an implicit reference to the Activity.
     */
    class NavigatorListenerImpl(activity: SwappingMapAndNavActivity) : NavigatorListener {
      private val activity: WeakReference<SwappingMapAndNavActivity>

      init {
        this.activity = WeakReference(activity)
      }

      override fun onNavigatorReady(navigator: Navigator) {
        val swappingMapAndNavActivity: SwappingMapAndNavActivity? = activity.get()
        if (swappingMapAndNavActivity != null) {
          swappingMapAndNavActivity.navigator = navigator
          // Register an arrival listener that returns back to a top-down map once the trip is over.
          swappingMapAndNavActivity.registerListeners()
        }
      }

      override fun onError(errorCode: Int) {
        // Show Toast is the activity is not destroyed.
        val toastMessage =
          when (errorCode) {
            NavigationApi.ErrorCode
              .NOT_AUTHORIZED -> // Note: If this message is displayed, you may need to check that
              // your API_KEY is specified correctly in AndroidManifest.xml
              // and has been enabled to access the Navigation API
              "Error loading Navigation API: Your API key is " +
                "invalid or not authorized to use Navigation."
            NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED ->
              "Error loading Navigation API: User did not " + "accept the Navigation Terms of Use."
            else -> "Error loading Navigation API: $errorCode"
          }

        activity.get()?.showToast(toastMessage)
      }
    }
  }
}
