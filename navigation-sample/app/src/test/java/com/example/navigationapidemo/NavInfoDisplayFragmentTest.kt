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

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.mapsplatform.turnbyturn.model.DrivingSide
import com.google.android.libraries.mapsplatform.turnbyturn.model.Maneuver
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavInfo
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavState
import com.google.android.libraries.mapsplatform.turnbyturn.model.StepInfo
import com.google.android.libraries.navigation.Navigator
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.powerassert.assertGeo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import org.robolectric.util.ReflectionHelpers

/**
 * Headless JVM Robolectric unit tests for [NavInfoDisplayFragment] and [NavInfoReceivingService].
 *
 * Exercises turn-by-turn navigation header state transitions (`STOPPED`, `REROUTING`, `ENROUTE`),
 * imperial distance and ETA formatting edge cases, step preview traversal (`Next`, `Prev`,
 * `Current`), and LiveData updates from [NavInfoReceivingService].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class NavInfoDisplayFragmentTest {

  private lateinit var hostActivity: AppCompatActivity
  private lateinit var fragment: NavInfoDisplayFragment
  private lateinit var liveData: MutableLiveData<NavInfo?>

  @Before
  fun setUp() {
    hostActivity = Robolectric.buildActivity(AppCompatActivity::class.java).create().start().resume().get()
    val container = FrameLayout(hostActivity).apply { id = R.id.nav_info_frame }
    hostActivity.setContentView(container)

    liveData =
      ReflectionHelpers.getStaticField(
        NavInfoReceivingService::class.java,
        "navInfoMutableLiveData",
      )
    liveData.value = null

    fragment = NavInfoDisplayFragment()
    hostActivity
      .supportFragmentManager
      .beginTransaction()
      .add(R.id.nav_info_frame, fragment)
      .commitNow()
  }

  @After
  fun tearDown() {
    liveData.value = null
  }

  private fun createStep(
    stepNumber: Int,
    roadName: String,
    instruction: String,
    maneuver: Int = Maneuver.TURN_RIGHT,
    drivingSide: Int = DrivingSide.RIGHT,
    roundaboutTurn: Int = 2,
    distanceFromPrevMeters: Int = 800,
    timeFromPrevSeconds: Int = 120,
  ): StepInfo =
    StepInfo.builder()
      .setStepNumber(stepNumber)
      .setFullRoadName(roadName)
      .setSimpleRoadName(roadName)
      .setFullInstructionText(instruction)
      .setManeuver(maneuver)
      .setDrivingSide(drivingSide)
      .setRoundaboutTurnNumber(roundaboutTurn)
      .setDistanceFromPrevStepMeters(distanceFromPrevMeters)
      .setTimeFromPrevStepSeconds(timeFromPrevSeconds)
      .build()

  @Test
  fun initialState_showsAwaitingNavigationAndDisablesStepButtons() {
    val root = requireNotNull(fragment.view)
    val primaryText = root.findViewById<TextView>(R.id.tv_primary_text)
    val btnCurrent = root.findViewById<Button>(R.id.btn_current_step)
    val btnPrev = root.findViewById<Button>(R.id.btn_prev_step)
    val btnNext = root.findViewById<Button>(R.id.btn_next_step)

    assertGeo(
      primaryText.text.toString() == "Awaiting navigation..." &&
        !btnCurrent.isEnabled &&
        !btnPrev.isEnabled &&
        !btnNext.isEnabled
    )
  }

  @Test
  fun enrouteState_formatsDistancesAndTimesAndSupportsStepPreviewNavigation() {
    val step0 =
      createStep(
        stepNumber = 0,
        roadName = "Shoreline Blvd",
        instruction = "Head north on Shoreline Blvd",
        maneuver = Maneuver.DEPART,
        drivingSide = DrivingSide.RIGHT,
        distanceFromPrevMeters = 100,
        timeFromPrevSeconds = 45,
      )
    val step1 =
      createStep(
        stepNumber = 1,
        roadName = "US-101 N",
        instruction = "Merge onto US-101 N toward San Francisco",
        maneuver = Maneuver.MERGE_LEFT,
        drivingSide = DrivingSide.RIGHT,
        distanceFromPrevMeters = 1610, // ~1.0 mi
        timeFromPrevSeconds = 180, // 3 min
      )
    val step2 =
      createStep(
        stepNumber = 2,
        roadName = "Market St",
        instruction = "Exit onto Market St",
        maneuver = Maneuver.OFF_RAMP_RIGHT,
        drivingSide = DrivingSide.LEFT,
        distanceFromPrevMeters = 25000, // ~15.5 mi -> rounded integer "16 mi"
        timeFromPrevSeconds = 3900, // 1 hr 5 min
      )

    val navInfo =
      NavInfo.builder()
        .setNavState(NavState.ENROUTE)
        .setCurrentStep(step0)
        .setRemainingSteps(arrayOf(step1, step2))
        .setRouteChanged(false)
        .setDistanceToCurrentStepMeters(100) // 328 ft (< 0.25 mi)
        .setTimeToCurrentStepSeconds(45) // 45 sec (< 60 sec)
        .setDistanceToNextDestinationMeters(1610) // 1.0 mi
        .setTimeToNextDestinationSeconds(180) // 3 min
        .setDistanceToFinalDestinationMeters(25000) // 16 mi (>= 10 mi)
        .setTimeToFinalDestinationSeconds(3900) // 1 hr 5 min
        .build()

    liveData.value = navInfo
    ShadowLooper.idleMainLooper()

    val root = requireNotNull(fragment.view)
    val primaryText = root.findViewById<TextView>(R.id.tv_primary_text)
    val distanceText = root.findViewById<TextView>(R.id.tv_distance_to_step)
    val timeText = root.findViewById<TextView>(R.id.tv_time_to_step)
    val maneuverText = root.findViewById<TextView>(R.id.tv_maneuver_type)
    val drivingSideText = root.findViewById<TextView>(R.id.tv_driving_side)
    val nextEtaText = root.findViewById<TextView>(R.id.tv_next_destination_eta)
    val nextDistText = root.findViewById<TextView>(R.id.tv_next_destination_remaining_distance)
    val finalEtaText = root.findViewById<TextView>(R.id.tv_final_destination_eta)
    val finalDistText = root.findViewById<TextView>(R.id.tv_final_destination_remaining_distance)
    val btnCurrent = root.findViewById<Button>(R.id.btn_current_step)
    val btnPrev = root.findViewById<Button>(R.id.btn_prev_step)
    val btnNext = root.findViewById<Button>(R.id.btn_next_step)

    // Verify current step (step #0) display and formatting
    assertGeo(primaryText.text.toString() == "Shoreline Blvd")
    assertGeo(distanceText.text.toString() == "328 ft")
    assertGeo(timeText.text.toString() == "45 sec to step #0")
    assertGeo(maneuverText.text.toString() == "DEPART" && drivingSideText.text.toString() == "RIGHT")
    assertGeo(nextDistText.text.toString() == "1.0 mi" && nextEtaText.text.toString().trim() == "3 min")
    assertGeo(finalDistText.text.toString() == "16 mi" && finalEtaText.text.toString().trim() == "1 hr 5 min")
    assertGeo(!btnCurrent.isEnabled && !btnPrev.isEnabled && btnNext.isEnabled)
    assertGeo((root.background as ColorDrawable).color == Color.parseColor("#4285F4"))

    // Click "Next step" -> previews step #1
    btnNext.performClick()
    assertGeo(primaryText.text.toString() == "US-101 N")
    assertGeo(distanceText.text.toString() == "1.0 mi" && timeText.text.toString() == "3 min to step #1")
    assertGeo(maneuverText.text.toString() == "MERGE_LEFT")
    assertGeo(btnCurrent.isEnabled && btnPrev.isEnabled && btnNext.isEnabled)
    assertGeo((root.background as ColorDrawable).color == Color.parseColor("#617BA6"))

    // Click "Next step" again -> previews final step #2 (disables Next button)
    btnNext.performClick()
    assertGeo(primaryText.text.toString() == "Market St")
    assertGeo(distanceText.text.toString() == "16 mi" && timeText.text.toString() == "1 hr 5 min to step #2")
    assertGeo(maneuverText.text.toString() == "OFF_RAMP_RIGHT" && drivingSideText.text.toString() == "LEFT")
    assertGeo(btnCurrent.isEnabled && btnPrev.isEnabled && !btnNext.isEnabled)

    // Click "Prev step" -> returns to step #1
    btnPrev.performClick()
    assertGeo(primaryText.text.toString() == "US-101 N" && btnNext.isEnabled)

    // Click "Current step" -> jumps directly back to current step #0
    btnCurrent.performClick()
    assertGeo(primaryText.text.toString() == "Shoreline Blvd")
    assertGeo(!btnCurrent.isEnabled && !btnPrev.isEnabled && btnNext.isEnabled)
    assertGeo((root.background as ColorDrawable).color == Color.parseColor("#4285F4"))
  }

  @Test
  fun reroutingAndStoppedStates_clearHeaderAndShowStatusText() {
    val root = requireNotNull(fragment.view)
    val primaryText = root.findViewById<TextView>(R.id.tv_primary_text)
    val instructionText = root.findViewById<TextView>(R.id.tv_full_instruction)

    // First enter ENROUTE so header fields are populated
    val step0 = createStep(0, "Main St", "Continue straight")
    liveData.value =
      NavInfo.builder()
        .setNavState(NavState.ENROUTE)
        .setCurrentStep(step0)
        .setRemainingSteps(emptyArray())
        .setDistanceToCurrentStepMeters(50)
        .setTimeToCurrentStepSeconds(10)
        .build()
    ShadowLooper.idleMainLooper()
    assertGeo(primaryText.text.toString() == "Main St")

    // Transition to REROUTING
    liveData.value =
      NavInfo.builder().setNavState(NavState.REROUTING).setRemainingSteps(emptyArray()).build()
    ShadowLooper.idleMainLooper()
    assertGeo(
      primaryText.text.toString() == "Rerouting..." && instructionText.text.toString().isEmpty()
    )

    // Transition to STOPPED
    liveData.value =
      NavInfo.builder().setNavState(NavState.STOPPED).setRemainingSteps(emptyArray()).build()
    ShadowLooper.idleMainLooper()
    assertGeo(
      primaryText.text.toString() == "Awaiting navigation..." &&
        instructionText.text.toString().isEmpty()
    )
  }

  @Test
  fun navInfoReceivingService_bindsMessengerAndClearsLiveDataOnUnbind() {
    val serviceController = Robolectric.buildService(NavInfoReceivingService::class.java).create()
    val service = serviceController.get()
    val intent = Intent(hostActivity, NavInfoReceivingService::class.java)

    val binder = service.onBind(intent)
    assertThat(binder).isNotNull()

    // Populate LiveData, then unbind and verify LiveData resets to null
    liveData.value =
      NavInfo.builder().setNavState(NavState.STOPPED).setRemainingSteps(emptyArray()).build()
    assertGeo(NavInfoReceivingService.navInfoLiveData.value != null)

    service.onUnbind(intent)
    ShadowLooper.idleMainLooper()
    assertGeo(NavInfoReceivingService.navInfoLiveData.value == null)
  }

  @Test
  fun navForwardingManager_startsAndStopsServiceRegistrationAndAttachesDisplayFragment() {
    val mockNavigator = mock(Navigator::class.java)
    `when`(
        mockNavigator.registerServiceForNavUpdates(
          hostActivity.packageName,
          NavInfoReceivingService::class.java.name,
          Int.MAX_VALUE,
        )
      )
      .thenReturn(true)
    `when`(mockNavigator.unregisterServiceForNavUpdates()).thenReturn(true)

    val addedFragment =
      NavForwardingManager.startNavForwarding(
        navigator = mockNavigator,
        context = hostActivity,
        fragmentManager = hostActivity.supportFragmentManager,
      )
    ShadowLooper.idleMainLooper()

    assertGeo(addedFragment is NavInfoDisplayFragment && addedFragment.isAdded)
    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo("Successfully registered service for nav updates")
    verify(mockNavigator)
      .registerServiceForNavUpdates(
        hostActivity.packageName,
        NavInfoReceivingService::class.java.name,
        Int.MAX_VALUE,
      )

    NavForwardingManager.stopNavForwarding(
      navigator = mockNavigator,
      context = hostActivity,
      fragmentManager = hostActivity.supportFragmentManager,
      navInfoFragment = addedFragment,
    )
    ShadowLooper.idleMainLooper()

    assertGeo(!addedFragment.isAdded)
    assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Unregistered service for nav updates")
    verify(mockNavigator).unregisterServiceForNavUpdates()
  }

  @Test
  fun navForwardingManager_showsFailureToastsWhenServiceRegistrationOrUnregistrationFails() {
    val mockNavigator = mock(Navigator::class.java)
    `when`(
        mockNavigator.registerServiceForNavUpdates(
          hostActivity.packageName,
          NavInfoReceivingService::class.java.name,
          Int.MAX_VALUE,
        )
      )
      .thenReturn(false)
    `when`(mockNavigator.unregisterServiceForNavUpdates()).thenReturn(false)

    val addedFragment =
      NavForwardingManager.startNavForwarding(
        navigator = mockNavigator,
        context = hostActivity,
        fragmentManager = hostActivity.supportFragmentManager,
      )
    ShadowLooper.idleMainLooper()
    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo("Failed to register service for nav updates")

    NavForwardingManager.stopNavForwarding(
      navigator = mockNavigator,
      context = hostActivity,
      fragmentManager = hostActivity.supportFragmentManager,
      navInfoFragment = addedFragment,
    )
    ShadowLooper.idleMainLooper()
    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo("Failed to unregister service for nav updates")
  }
}
