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
import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraFollowLocationCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.ForceNightMode
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.OnNavigationUiChangedListener
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.common.base.MoreObjects

/**
 * Handles the shared setup and behaviors of the customization panels in `NavViewActivity` and
 * `NavFragmentActivity`.
 *
 * Users are initially presented with a simple overlay of toggles that allow them to select which
 * type of runtime behavior to stress. "Nav methods" refer to behaviors on [Navigator]. "View
 * methods" refer to behaviors on [NavigationView] or [SupportNavigationFragment]. And "Map methods"
 * refer to behaviors on [GoogleMap].
 *
 * Clicking one of the toggles will expand a list of corresponding behaviors, presented as a panel
 * of buttons, spinners etc for the user to play with. The user can always hide/show the entire
 * customization UI (toggles and panels) using the drop-down menu.
 *
 * IMPORTANT: Note that the Navigation SDK is a complex product, and so many of the features
 * presented in the panels are interdependent. For example, calling GoogleMap#followMyLocation also
 * (asynchronously) enables the Navigation style of UI (see [NavigationView.isNavigationUiEnabled]).
 *
 * As a result, the control panels will sometimes change without a user interaction, in response to
 * the Navigation SDK changing its internal state. These complexities are explained in in-line
 * comments below, where applicable.
 */
internal object CustomizationPanelsDelegate {
  private const val TAG = "CustomizationPanels"

  /** The location of Melbourne. */
  private val MELBOURNE = LatLng(-37.813, 144.962)

  /**
   * A map from the resource ID of a toggle button to the resource ID of the panel it should
   * show/hide upon toggle.
   */
  private val TOGGLE_BUTTON_TO_LAYOUT_MAP =
    mapOf(
      R.id.controls_toggle_nav to R.id.controls_navigator,
      R.id.controls_toggle_map to R.id.controls_map,
      R.id.controls_toggle_view to R.id.controls_navview,
    )

  /** Initializes the toggles that show/hide customization panels when clicked. */
  fun initializeCustomizationPanels(activity: Activity) {
    for ((toggleButtonId, layoutToToggleId) in TOGGLE_BUTTON_TO_LAYOUT_MAP) {
      val toggleButton = activity.findViewById<Button>(toggleButtonId)
      toggleButton.setOnClickListener {
        val layoutToToggle = activity.findViewById<LinearLayout>(layoutToToggleId)
        val currentlyShown = layoutToToggle.visibility == View.VISIBLE
        layoutToToggle.visibility = if (currentlyShown) View.GONE else View.VISIBLE

        // Unilaterally hide all other layouts, since one may be lingering from
        // an earlier toggle press.
        val allOtherPanels =
          TOGGLE_BUTTON_TO_LAYOUT_MAP.values.toSet() subtract setOf(layoutToToggleId)

        for (resourceIdToHide in allOtherPanels) {
          activity.findViewById<View>(resourceIdToHide).visibility = View.GONE
        }
      }
    }
  }

  /** Switches the visibility of the UI of the customization panels and the toggle buttons. */
  fun switchCustomizationUiVisibility(activity: Activity) {
    val toggleButtons = activity.findViewById<LinearLayout>(R.id.control_toggles_container)
    val panelsWrapper = activity.findViewById<ScrollView>(R.id.control_panels_scroll)

    // Toggle visibility of buttons and the element that wraps all the corresponding customization
    // panels.
    val visibility = if (toggleButtons.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    toggleButtons.visibility = visibility
    panelsWrapper.visibility = visibility
  }

  /**
   * Sets up the data for the night mode spinner.
   *
   * Note that spinners don't support listener declarations in XML - which is why we handle spinners
   * a bit differently compared to button interactions.
   */
  fun setUpNightModeSpinner(activity: Activity, onNightModeOptionSelected: (Int) -> Unit) {
    val nightModeAdapter = ArrayAdapter<CharSequence>(activity, R.layout.spinner_item)
    nightModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    nightModeAdapter.add("Night Mode Options")
    nightModeAdapter.add("Night Mode: auto")
    nightModeAdapter.add("Night mode: off")
    nightModeAdapter.add("Night mode: on")
    val nightModeSpinner = activity.findViewById<Spinner>(R.id.night_mode_spinner)
    nightModeSpinner.adapter = nightModeAdapter
    nightModeSpinner.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
          when (position) {
            0 -> {}
            1 -> onNightModeOptionSelected(ForceNightMode.AUTO)
            2 -> onNightModeOptionSelected(ForceNightMode.FORCE_DAY)
            3 -> onNightModeOptionSelected(ForceNightMode.FORCE_NIGHT)
            else -> onNightModeOptionSelected(ForceNightMode.FORCE_NIGHT)
          }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
          // Do nothing.
        }
      }
  }

  /**
   * Sets up the data for the camera perspective spinner.
   *
   * Note that spinners don't support listener declarations in XML - which is why we handle spinners
   * a bit differently compared to button interactions.
   */
  fun setUpCameraPerspectiveSpinner(
    activity: Activity,
    onCameraPerspectiveOptionChange: (Int) -> Unit,
  ) {
    val cameraPerspectiveAdapter = ArrayAdapter<CharSequence>(activity, R.layout.spinner_item)
    cameraPerspectiveAdapter.apply {
      setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      add("Camera Mode Options")
      add("Following: Tilted")
      add("Following: North up")
      add("Following: Heading up")
    }
    val cameraPerspectiveSpinner = activity.findViewById<Spinner>(R.id.follow_my_location_spinner)
    cameraPerspectiveSpinner.adapter = cameraPerspectiveAdapter
    cameraPerspectiveSpinner.onItemSelectedListener =
      object : OnCameraPerspectiveSelectedListener {
        // We keep track of the last camera perspective that was explicitly set by the
        // user. So that if we start automatically following the user's location later,
        // we know which camera perspective to show as being active.
        override var lastSetNonZeroPosition = 1
          private set

        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
          when (position) {
            0 -> {}
            1 -> {
              lastSetNonZeroPosition = 1
              onCameraPerspectiveOptionChange(GoogleMap.CameraPerspective.TILTED)
            }
            2 -> {
              lastSetNonZeroPosition = 2
              onCameraPerspectiveOptionChange(GoogleMap.CameraPerspective.TOP_DOWN_NORTH_UP)
            }
            3 -> {
              lastSetNonZeroPosition = 3
              onCameraPerspectiveOptionChange(GoogleMap.CameraPerspective.TOP_DOWN_HEADING_UP)
            }
            else -> {
              lastSetNonZeroPosition = 3
              onCameraPerspectiveOptionChange(GoogleMap.CameraPerspective.TOP_DOWN_HEADING_UP)
            }
          }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
          // Do nothing.
        }
      }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // Handlers for "View methods" control panel interactions (other than spinners).
  ///////////////////////////////////////////////////////////////////////////////////////
  /** Toggles whether the Navigation UI is enabled. */
  fun toggleNavigationUiEnabled(activity: Activity, setEnabled: (Boolean) -> Unit) {
    val navigationUiEnabled = activity.findViewById<ToggleButton>(R.id.btn_navigation_ui_mode)

    // Note that setting the Navigation UI style to "true" will automatically follow your
    // location with the last-specified camera perspective. Setting the Navigation UI style
    // to "false" will stop following your location all together.
    setEnabled(navigationUiEnabled.isChecked)
  }

  /** Toggles the visibility of the Trip Progress Bar UI. This is an EXPERIMENTAL FEATURE. */
  fun toggleTripProgressBarUI(activity: Activity, toggle: (Boolean) -> Unit) {
    val toggleButton = activity.findViewById<ToggleButton>(R.id.btn_progress_bar)
    toggle(toggleButton.isChecked)
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // Handlers for "Map methods" control panel interactions (other than spinners).
  //
  // An aside: Looking for more customizations you can do using GoogleMap?
  //
  // Check out our "GoogleMap" sample app to see all the different behaviors
  // supported by the GoogleMap interface, like drawing markers, setting up camera bounds,
  // and more!
  ///////////////////////////////////////////////////////////////////////////////////////
  /** Moves the position of the camera to hover over Melbourne. */
  fun moveCameraToMelbourne(activity: Activity, googleMap: GoogleMap) {
    // Moving the camera always exits follow mode until it's enabled programmatically or via
    // clicking the "Recenter" button.
    googleMap.moveCamera(
      CameraUpdateFactory.newCameraPosition(
        CameraPosition.builder().target(MELBOURNE).zoom(10f).bearing(0f).build()
      )
    )
  }

  /** Toggles whether the location marker is enabled. */
  @SuppressLint("MissingPermission") // TODO: requestPermissions(...) in here or earlier
  fun toggleSetMyLocationEnabled(activity: Activity, googleMap: GoogleMap) {
    val toggleButton = activity.findViewById<ToggleButton>(R.id.btn_set_my_location_enabled)
    googleMap.isMyLocationEnabled = toggleButton.isChecked
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // Handlers for "Nav methods" control panel interactions (other than spinners).
  ///////////////////////////////////////////////////////////////////////////////////////
  /**
   * Toggles navigation forwarding (e.g. for 2-wheeler projection).
   *
   * @return the new NavInfoDisplayFragment state to manage, to pass in upon the next user-click
   */
  fun toggleNavForwarding(
    activity: AppCompatActivity,
    navigator: Navigator,
    existingFragment: Fragment?,
  ): Fragment? {
    return if (existingFragment == null) {
      NavForwardingManager.startNavForwarding(navigator, activity, activity.supportFragmentManager)
    } else {
      NavForwardingManager.stopNavForwarding(
        navigator,
        activity,
        activity.supportFragmentManager,
        existingFragment,
      )
      null
    }
  }

  fun Navigator.logDebugInfo() {
    currentRouteSegment?.let {
      Log.i(
        TAG,
        MoreObjects.toStringHelper("RouteSegment")
          .add("Destination LatLng", it.destinationLatLng)
          .add("Destination Waypoint", it.destinationWaypoint)
          .add("Traffic Data", it.trafficData)
          .add("List<LatLng> for segment", it.latLngs)
          .toString(),
      )
    }

    val stringifiedTraveledRoute =
      MoreObjects.toStringHelper(traveledRoute)
        .add("List<LatLng> already traveled", traveledRoute)
        .toString()
    Log.i(TAG, stringifiedTraveledRoute)

    currentTimeAndDistance?.let {
      Log.i(
        TAG,
        MoreObjects.toStringHelper(it)
          .add("Delay severity", it.delaySeverity)
          .add("Meters", it.meters)
          .add("Seconds", it.seconds)
          .toString(),
      )
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // Methods/interfaces to coordinate any implicit state changes in the NavSDK.
  ///////////////////////////////////////////////////////////////////////////////////////
  /**
   * Registers a listener which reflects the latest state of the GoogleMap camera perspective in the
   * corresponding spinner, on an as-needed basis.
   *
   * Remember that, as described in the top-level Javadoc for this class, the camera perspective can
   * sometimes change without explicitly calling [ ][GoogleMap.followMyLocation]. For instance, when
   * you pan or animate the camera to hover over a specified location. In cases where that happens,
   * the user of the demo app should always see the correct camera perspective being displayed.
   */
  fun registerOnCameraFollowLocationCallback(activity: Activity, map: GoogleMap) {
    map.setOnFollowMyLocationCallback(
      object : OnCameraFollowLocationCallback {
        override fun onCameraStartedFollowingLocation() {
          // The camera started following the user's location.
          val cameraPerspectiveSpinner =
            activity.findViewById<Spinner>(R.id.follow_my_location_spinner)
          if (cameraPerspectiveSpinner.selectedItemPosition != 0) {
            // The spinner is already showing the user's last-selected camera perspective.
            // So we don't need to update it.
          } else {
            // The spinner isn't showing the user's last-selected camera perspective. This
            // can be for many reasons. Maybe the user manually reset the spinner, or the
            // user set the "Navigation UI" toggle button to "false". Or they requested to
            // move the camera to Melbourne, thereby disabling the camera from following.
            //
            // At any rate, we want to make sure the last camera perspective is
            // displayed.
            val listener =
              cameraPerspectiveSpinner.onItemSelectedListener
                as OnCameraPerspectiveSelectedListener?
            val currentCameraPerspective =
              if (listener?.lastSetNonZeroPosition == null) {
                1 // The default camera perspective is TILTED.
              } else {
                listener.lastSetNonZeroPosition
              }
            // All we want to do is to update the spinner with the last-set perspective.
            // We don't actually want to trigger the listener itself.
            cameraPerspectiveSpinner.onItemSelectedListener = null
            cameraPerspectiveSpinner.setSelection(currentCameraPerspective)
            cameraPerspectiveSpinner.onItemSelectedListener = listener
          }
        }

        override fun onCameraStoppedFollowingLocation() {
          // The camera stopped following the user's location. Reset the camera perspective
          // spinner accordingly.
          val cameraPerspectiveSpinner =
            activity.findViewById<Spinner>(R.id.follow_my_location_spinner)
          cameraPerspectiveSpinner.setSelection(/* position= */ 0)
        }
      }
    )
  }

  /**
   * Changes the "checked" state of the "Navigation UI" toggle button if we detect that the value
   * has changed.
   *
   * This logic detects any state changes "under the hood" for the Navigation UI mode. It does not
   * itself trigger any API calls... it only assists with the presentation of the button.
   */
  fun registerOnNavigationUiChangedListener(
    activity: Activity,
    addListener: (OnNavigationUiChangedListener) -> Unit,
  ) {
    addListener { isNavigationUiEnabled: Boolean ->
      val navigationUiEnabled = activity.findViewById<ToggleButton>(R.id.btn_navigation_ui_mode)
      // We simply update the checked state. Note that this won't trigger an API
      // call, because the button is configured to only respond to "click" events.
      navigationUiEnabled.isChecked = isNavigationUiEnabled
    }
  }

  /** An item selection listener for use with the camera perspective spinner. */
  private interface OnCameraPerspectiveSelectedListener : AdapterView.OnItemSelectedListener {
    /**
     * Gets the last non-zero position for the Camera Perspective spinner.
     *
     * Note that this will ignore selections of the default (0-positioned) value of the spinner,
     * since that's just a dummy element to explain the spinner's purpose.
     */
    val lastSetNonZeroPosition: Int
  }
}
