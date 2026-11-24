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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.google.maps.android.robolectric.powerassert.assertGeo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.util.ReflectionHelpers

/**
 * Headless JVM Robolectric unit tests for [NavInfoDisplayFragment].
 *
 * Exercises turn-by-turn navigation header state transitions (`STOPPED`, `REROUTING`, `ENROUTE`),
 * imperial distance and ETA formatting edge cases, and step preview traversal (`Next`, `Prev`,
 * `Current`).
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
    hostActivity.setContentView(FrameLayout(hostActivity).apply { id = R.id.nav_info_frame })

    liveData = ReflectionHelpers.getStaticField(NavInfoReceivingService::class.java, "navInfoMutableLiveData")
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
      .setRoundaboutTurnNumber(2)
      .setDistanceFromPrevStepMeters(distanceFromPrevMeters)
      .setTimeFromPrevStepSeconds(timeFromPrevSeconds)
      .build()

  private fun createThreeStepEnrouteNavInfo(): NavInfo {
    val step0 =
      createStep(0, "Shoreline Blvd", "Head north on Shoreline Blvd", Maneuver.DEPART, DrivingSide.RIGHT, 100, 45)
    val step1 =
      createStep(1, "US-101 N", "Merge onto US-101 N", Maneuver.MERGE_LEFT, DrivingSide.RIGHT, 1610, 180)
    val step2 =
      createStep(2, "Market St", "Exit onto Market St", Maneuver.OFF_RAMP_RIGHT, DrivingSide.LEFT, 25000, 3900)
    return NavInfo.builder()
      .setNavState(NavState.ENROUTE)
      .setCurrentStep(step0)
      .setRemainingSteps(arrayOf(step1, step2))
      .setRouteChanged(false)
      .setDistanceToCurrentStepMeters(100)
      .setTimeToCurrentStepSeconds(45)
      .setDistanceToNextDestinationMeters(1610)
      .setTimeToNextDestinationSeconds(180)
      .setDistanceToFinalDestinationMeters(25000)
      .setTimeToFinalDestinationSeconds(3900)
      .build()
  }

  @Test
  fun initialState_showsAwaitingNavigationAndDisablesStepButtons() {
    val root = requireNotNull(fragment.view)
    assertGeo(
      root.findViewById<TextView>(R.id.tv_primary_text).text.toString() == "Awaiting navigation..." &&
        !root.findViewById<Button>(R.id.btn_current_step).isEnabled &&
        !root.findViewById<Button>(R.id.btn_prev_step).isEnabled &&
        !root.findViewById<Button>(R.id.btn_next_step).isEnabled
    )
  }

  @Test
  fun enrouteState_formatsImperialDistancesAndTimesForCurrentStep() {
    liveData.value = createThreeStepEnrouteNavInfo()
    ShadowLooper.idleMainLooper()

    val root = requireNotNull(fragment.view)
    assertGeo(root.findViewById<TextView>(R.id.tv_primary_text).text.toString() == "Shoreline Blvd")
    assertGeo(root.findViewById<TextView>(R.id.tv_distance_to_step).text.toString() == "328 ft")
    assertGeo(root.findViewById<TextView>(R.id.tv_time_to_step).text.toString() == "45 sec to step #0")
    assertGeo(
      root.findViewById<TextView>(R.id.tv_maneuver_type).text.toString() == "DEPART" &&
        root.findViewById<TextView>(R.id.tv_driving_side).text.toString() == "RIGHT"
    )
    assertGeo(
      root.findViewById<TextView>(R.id.tv_next_destination_remaining_distance).text.toString() == "1.0 mi" &&
        root.findViewById<TextView>(R.id.tv_next_destination_eta).text.toString().trim() == "3 min"
    )
    assertGeo(
      root.findViewById<TextView>(R.id.tv_final_destination_remaining_distance).text.toString() == "16 mi" &&
        root.findViewById<TextView>(R.id.tv_final_destination_eta).text.toString().trim() == "1 hr 5 min"
    )
    assertGeo((root.background as ColorDrawable).color == Color.parseColor("#4285F4"))
  }

  @Test
  fun enrouteState_stepPreviewButtonsTraverseStepsAndUpdateBackgroundColor() {
    liveData.value = createThreeStepEnrouteNavInfo()
    ShadowLooper.idleMainLooper()

    val root = requireNotNull(fragment.view)
    val primaryText = root.findViewById<TextView>(R.id.tv_primary_text)
    val distanceText = root.findViewById<TextView>(R.id.tv_distance_to_step)
    val timeText = root.findViewById<TextView>(R.id.tv_time_to_step)
    val btnCurrent = root.findViewById<Button>(R.id.btn_current_step)
    val btnPrev = root.findViewById<Button>(R.id.btn_prev_step)
    val btnNext = root.findViewById<Button>(R.id.btn_next_step)

    btnNext.performClick()
    assertGeo(primaryText.text.toString() == "US-101 N" && distanceText.text.toString() == "1.0 mi")
    assertGeo(timeText.text.toString() == "3 min to step #1" && btnCurrent.isEnabled && btnPrev.isEnabled && btnNext.isEnabled)
    assertGeo((root.background as ColorDrawable).color == Color.parseColor("#617BA6"))

    btnNext.performClick()
    assertGeo(primaryText.text.toString() == "Market St" && distanceText.text.toString() == "16 mi")
    assertGeo(timeText.text.toString() == "1 hr 5 min to step #2" && !btnNext.isEnabled)

    btnPrev.performClick()
    assertGeo(primaryText.text.toString() == "US-101 N" && btnNext.isEnabled)

    btnCurrent.performClick()
    assertGeo(primaryText.text.toString() == "Shoreline Blvd" && !btnCurrent.isEnabled && !btnPrev.isEnabled)
    assertGeo((root.background as ColorDrawable).color == Color.parseColor("#4285F4"))
  }

  @Test
  fun reroutingAndStoppedStates_clearHeaderAndShowStatusText() {
    val root = requireNotNull(fragment.view)
    val primaryText = root.findViewById<TextView>(R.id.tv_primary_text)
    val instructionText = root.findViewById<TextView>(R.id.tv_full_instruction)

    liveData.value =
      NavInfo.builder()
        .setNavState(NavState.ENROUTE)
        .setCurrentStep(createStep(0, "Main St", "Continue straight"))
        .setRemainingSteps(emptyArray())
        .setDistanceToCurrentStepMeters(50)
        .setTimeToCurrentStepSeconds(10)
        .build()
    ShadowLooper.idleMainLooper()
    assertGeo(primaryText.text.toString() == "Main St")

    liveData.value = NavInfo.builder().setNavState(NavState.REROUTING).setRemainingSteps(emptyArray()).build()
    ShadowLooper.idleMainLooper()
    assertGeo(primaryText.text.toString() == "Rerouting..." && instructionText.text.toString().isEmpty())

    liveData.value = NavInfo.builder().setNavState(NavState.STOPPED).setRemainingSteps(emptyArray()).build()
    ShadowLooper.idleMainLooper()
    assertGeo(primaryText.text.toString() == "Awaiting navigation..." && instructionText.text.toString().isEmpty())
  }
}
