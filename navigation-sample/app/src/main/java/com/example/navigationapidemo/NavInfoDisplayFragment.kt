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

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.libraries.mapsplatform.turnbyturn.model.DrivingSide
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavInfo
import com.google.android.libraries.mapsplatform.turnbyturn.model.NavState
import com.google.android.libraries.mapsplatform.turnbyturn.model.StepInfo
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Shows navigation information from the receiving service in a separate header fragment above the
 * base navigation fragment.
 */
class NavInfoDisplayFragment : Fragment() {
  private val timestampFormat = SimpleDateFormat("HH:mm:ss.SSS z", Locale.US)
  private lateinit var displayHeader: View
  private var selectedStepNumber = -1
  private var headerNavInfo: NavInfo? = null
  private var showingCurrentStep = true

  /** Returns whether the displayed step is the current step rather than a future step preview. */
  private val isDisplayedStepCurrentStep: Boolean
    get() =
      headerNavInfo?.currentStep != null &&
        headerNavInfo?.currentStep?.stepNumber == selectedStepNumber &&
        headerNavInfo?.distanceToCurrentStepMeters != null &&
        headerNavInfo?.timeToCurrentStepSeconds != null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    return inflater.inflate(R.layout.fragment_nav_info_display, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    displayHeader = view
    displayHeader.findViewById<View>(R.id.btn_next_step).setOnClickListener {
      headerNavInfo?.let { showNextStep(it) }
    }
    displayHeader.findViewById<View>(R.id.btn_prev_step).setOnClickListener {
      headerNavInfo?.let { showPrevStep(it) }
    }
    displayHeader.findViewById<View>(R.id.btn_current_step).setOnClickListener {
      headerNavInfo?.let { showCurrentStep(it) }
    }
    showAwaitingNavigationText()
    // Observe live data for nav info updates.
    val navInfoObserver = Observer { navInfo: NavInfo? ->
      headerNavInfo = navInfo
      headerNavInfo?.let { showNavInfo(it) }
    }

    NavInfoReceivingService.navInfoLiveData.observe(this.viewLifecycleOwner, navInfoObserver)
  }

  private fun showNavInfo(navInfo: NavInfo) {
    if (navInfo.navState == NavState.REROUTING) {
      // Rerouting: Clear the header and indicate that we're rerouting.
      clearHeader()
      displayHeader.findViewById<TextView>(R.id.tv_primary_text).text = "Rerouting..."
    } else if (navInfo.navState == NavState.STOPPED) {
      // Stopped: Nav has stopped, so clear the header and indicate that we're awaiting
      // navigation.
      clearHeader()
      showAwaitingNavigationText()
    } else if (
      navInfo.navState == NavState.ENROUTE &&
        navInfo.currentStep != null &&
        navInfo.remainingSteps != null
    ) {
      // Enroute:
      // Show the latest current step if
      //  1) The last shown step was the current step.
      //  2) This is the first step to be shown.
      //  3) If the route has changed since the last message.
      // Otherwise, continue to show whichever step is currently being shown, which may be
      // a step preview.
      if (
        navInfo.routeChanged ||
          selectedStepNumber < 0 ||
          showingCurrentStep ||
          !isStepNumberAvailable(navInfo, selectedStepNumber)
      ) {
        selectedStepNumber = navInfo.currentStep.stepNumber
      }
      showSelectedStep(navInfo)
    } else {
      // Error.
      showToast("Received unknown NavInfo.")
    }
  }

  /**
   * Checks if a step number is part of the route. This includes the current step and remaining
   * steps.
   */
  private fun isStepNumberAvailable(navInfo: NavInfo?, stepNumber: Int): Boolean {
    if (navInfo == null || navInfo.currentStep == null) {
      return false
    }
    val currentStepNumber = navInfo.currentStep.stepNumber
    if (navInfo.remainingSteps.isEmpty()) {
      return stepNumber == currentStepNumber
    }
    val lastAvailableStepNumber = navInfo.remainingSteps[navInfo.remainingSteps.size - 1].stepNumber
    return stepNumber in currentStepNumber..lastAvailableStepNumber
  }

  /** Shows the step selected by the user. This could be a current or remaining step. */
  private fun showSelectedStep(navInfo: NavInfo) {
    if (navInfo.currentStep == null || navInfo.remainingSteps == null) {
      return
    }

    val currentStepNumber = navInfo.currentStep.stepNumber
    var selectedStep = navInfo.currentStep
    if (selectedStepNumber != currentStepNumber) {
      // If the selected step is not the current step, then it must be a step preview.
      // Subtract the current step number from the selected step number to get the index
      // of the selected step in the array of remaining steps.
      selectedStep = navInfo.remainingSteps[selectedStepNumber - currentStepNumber - 1]
    }
    showingCurrentStep = selectedStep.stepNumber == currentStepNumber

    // Show the full road name, maneuver icon, time and distance to step, and further details.
    displayHeader.findViewById<TextView>(R.id.tv_primary_text).text = selectedStep.fullRoadName
    setManeuverIcon(selectedStep)
    setTimeAndDistanceToSelectedStepTexts(selectedStep, navInfo)
    setHeaderDetailTexts(selectedStep, navInfo)

    // Enable or disable the current, previous, and next step buttons.
    setStepButtonsStates(navInfo)
  }

  private fun setTimeAndDistanceToSelectedStepTexts(selectedStep: StepInfo, navInfo: NavInfo) {
    // Get the estimated remaining time and distance to the current step.
    var distanceToStepMeters = navInfo.distanceToCurrentStepMeters
    var timeToStepSeconds = navInfo.timeToCurrentStepSeconds
    if (!isDisplayedStepCurrentStep) {
      // If the displayed step is a future step preview rather than the current step, show
      // the entire time and distance for the step maneuver rather than the estimated
      // remaining time and distance to the current step.
      distanceToStepMeters = selectedStep.distanceFromPrevStepMeters
      timeToStepSeconds = selectedStep.timeFromPrevStepSeconds
    }

    // Show the time and distance to the selected step.
    displayHeader.findViewById<TextView>(R.id.tv_distance_to_step).text =
      getDistanceFormatted(distanceToStepMeters)
    val timeToStep =
      getTimeFormatted(timeToStepSeconds).append("to step #").append(selectedStepNumber).toString()
    displayHeader.findViewById<TextView>(R.id.tv_time_to_step).text = timeToStep
  }

  /**
   * Enable or disable the current, previous, and next step buttons based on whether those steps are
   * available.
   */
  private fun setStepButtonsStates(navInfo: NavInfo) {
    displayHeader.findViewById<View>(R.id.btn_next_step).isEnabled = canShowNextStep(navInfo)
    displayHeader.findViewById<View>(R.id.btn_prev_step).isEnabled =
      selectedStepNumber > navInfo.currentStep.stepNumber
    displayHeader.findViewById<View>(R.id.btn_current_step).isEnabled = !showingCurrentStep
    displayHeader.setBackgroundColor(
      if (showingCurrentStep) CURRENT_STEP_COLOR else STEP_PREVIEW_COLOR
    )
    displayHeader.visibility = View.VISIBLE
  }

  /** Displays the current step when the current step button is pressed. */
  private fun showCurrentStep(navInfo: NavInfo) {
    if (navInfo.currentStep == null || navInfo.remainingSteps.isEmpty()) {
      return
    }

    selectedStepNumber = navInfo.currentStep.stepNumber
    showSelectedStep(navInfo)
  }

  /** Returns whether the next step is available. */
  private fun canShowNextStep(navInfo: NavInfo): Boolean {
    val nextSteps = navInfo.remainingSteps
    if (nextSteps == null || nextSteps.isEmpty()) {
      return false
    }

    val lastAvailableStepNumber = nextSteps[nextSteps.size - 1].stepNumber
    return selectedStepNumber < lastAvailableStepNumber
  }

  /** Displays the next step when the next step button is pressed. */
  private fun showNextStep(navInfo: NavInfo) {
    if (
      navInfo.remainingSteps == null ||
        navInfo.remainingSteps?.isEmpty() == true ||
        selectedStepNumber < 0 ||
        !canShowNextStep(navInfo)
    ) {
      return
    }

    selectedStepNumber++
    showSelectedStep(navInfo)
  }

  /** Displays the previous step when the previous step button is pressed. */
  private fun showPrevStep(navInfo: NavInfo) {
    if (navInfo.remainingSteps?.isEmpty() == true || selectedStepNumber <= 0) {
      return
    }
    selectedStepNumber--
    showSelectedStep(navInfo)
  }

  /** Shows the maneuver icon for the step. */
  private fun setManeuverIcon(stepInfo: StepInfo) {
    displayHeader
      .findViewById<ImageView>(R.id.iv_maneuver_icon)
      .setImageDrawable(
        requireActivity().resources.getDrawable(ManeuverUtils.getManeuverIconResId(stepInfo))
      )
  }

  /**
   * Returns the distance in the format of "mi" or "ft". Only shows ft if remaining distance is less
   * than 0.25 miles.
   *
   * @param distanceMeters the distance in meters.
   * @return the distance in the format of "mi" or "ft".
   */
  private fun getDistanceFormatted(distanceMeters: Int): String {
    // Distance can be negative so set the min distance to 0.
    // Only show the tenths place digit if the distance is less than 10 miles.
    // Only show feet if the distance is less than 0.25 miles.
    val remainingFeet = (distanceMeters * FEET_PER_METER).coerceAtLeast(0.0).toInt()
    val remainingMiles = remainingFeet.toDouble() / FEET_PER_MILE
    val distance: String =
      if (remainingMiles >= MIN_MILES_TO_SHOW_INTEGER) {
        remainingMiles.roundToInt().toString() + " mi"
      } else if (remainingMiles >= 0.25) {
        DecimalFormat("0.0").format(remainingMiles) + " mi"
      } else {
        "$remainingFeet ft"
      }
    return distance
  }

  /**
   * Returns the time in the format of "hr min sec". Only shows hr if remaining minutes > 60. Only
   * shows min if remaining minutes % 60 != 0. Only shows sec if remaining minutes < 1.
   *
   * @param timeSeconds the time in seconds
   * @return the time in the format of "hr min sec".
   */
  private fun getTimeFormatted(timeSeconds: Int): StringBuilder {
    val remainingSeconds = timeSeconds.coerceAtLeast(0)
    val remainingHours = remainingSeconds / 3600
    val remainingMinutesRounded = (remainingSeconds % 3600.0 / 60).roundToInt()
    val timeBuilder = StringBuilder()
    if (remainingHours > 0) {
      timeBuilder.append(remainingHours).append(" hr ")
    }
    if (remainingMinutesRounded > 0 && timeSeconds >= 60) {
      timeBuilder.append(remainingMinutesRounded).append(" min ")
    }
    if (remainingSeconds < 60) {
      timeBuilder.append(remainingSeconds).append(" sec ")
    }
    return timeBuilder
  }

  /** Shows detailed navigation information. */
  private fun setHeaderDetailTexts(stepInfo: StepInfo, navInfo: NavInfo) {
    displayHeader.findViewById<TextView>(R.id.tv_full_instruction).text =
      stepInfo.fullInstructionText
    displayHeader.findViewById<TextView>(R.id.tv_timestamp).text =
      timestampFormat.format(System.currentTimeMillis())
    displayHeader.findViewById<TextView>(R.id.tv_roundabout_turn_number).text =
      stepInfo.roundaboutTurnNumber.toString()
    displayHeader.findViewById<TextView>(R.id.tv_next_destination_eta).text =
      getTimeFormatted(navInfo.timeToNextDestinationSeconds)
    displayHeader.findViewById<TextView>(R.id.tv_next_destination_remaining_distance).text =
      getDistanceFormatted(navInfo.distanceToNextDestinationMeters)
    displayHeader.findViewById<TextView>(R.id.tv_final_destination_eta).text =
      getTimeFormatted(navInfo.timeToFinalDestinationSeconds)
    displayHeader.findViewById<TextView>(R.id.tv_final_destination_remaining_distance).text =
      getDistanceFormatted(navInfo.distanceToFinalDestinationMeters)
    setManeuverNameText(stepInfo)
    setDrivingSideText(stepInfo)
  }

  /** Shows the textual name of the maneuver. */
  private fun setManeuverNameText(stepInfo: StepInfo) {
    val maneuverName = ManeuverUtils.getManeuverName(stepInfo)
    if (maneuverName == null) {
      val error = "Error! Maneuver not found: " + stepInfo.maneuver
      showToast(error)
      Log.e(TAG, error)
    } else {
      displayHeader.findViewById<TextView>(R.id.tv_maneuver_type).text = maneuverName
    }
  }

  /** Shows whether the step is in left-hand-traffic or right-hand-traffic. */
  private fun setDrivingSideText(stepInfo: StepInfo) {
    if (!mDrivingSideStrings.containsKey(stepInfo.drivingSide)) {
      val error = "Error! DrivingSide not found: " + stepInfo.drivingSide
      showToast(error)
      Log.e(TAG, error)
    } else {
      displayHeader.findViewById<TextView>(R.id.tv_driving_side).text =
        mDrivingSideStrings[stepInfo.drivingSide]
    }
  }

  private fun clearHeader() {
    displayHeader.findViewById<ImageView>(R.id.iv_maneuver_icon).setImageDrawable(null)
    for (tvId in HEADER_TEXTVIEWS) {
      displayHeader.findViewById<TextView>(tvId).text = ""
    }
    displayHeader.findViewById<View>(R.id.btn_next_step).isEnabled = false
    displayHeader.findViewById<View>(R.id.btn_prev_step).isEnabled = false
    displayHeader.findViewById<View>(R.id.btn_current_step).isEnabled = false
    showingCurrentStep = true
    selectedStepNumber = -1
    displayHeader.setBackgroundColor(CURRENT_STEP_COLOR)
  }

  private fun showAwaitingNavigationText() {
    displayHeader.findViewById<TextView>(R.id.tv_primary_text).text = "Awaiting navigation..."
  }

  private fun showToast(text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
  }

  companion object {
    private const val TAG = "NavInfoDisplay"

    /**
     * Conversion values for imperial measurement units. This sample app simply shows imperial
     * units. In your real app, you may want to use locale settings to determine whether to display
     * metric or imperial units.
     */
    private const val MIN_MILES_TO_SHOW_INTEGER = 10
    private const val FEET_PER_MILE = 5280
    private const val FEET_PER_METER = 3.28

    private val mDrivingSideStrings: Map<Int, String> =
      mapOf(DrivingSide.NONE to "NONE", DrivingSide.LEFT to "LEFT", DrivingSide.RIGHT to "RIGHT")

    private val HEADER_TEXTVIEWS =
      intArrayOf(
        R.id.tv_primary_text,
        R.id.tv_time_to_step,
        R.id.tv_distance_to_step,
        R.id.tv_maneuver_type,
        R.id.tv_full_instruction,
        R.id.tv_timestamp,
        R.id.tv_driving_side,
        R.id.tv_roundabout_turn_number,
        R.id.tv_next_destination_eta,
        R.id.tv_next_destination_remaining_distance,
        R.id.tv_final_destination_eta,
        R.id.tv_final_destination_remaining_distance,
      )

    /** Set the header to blue for the current step. */
    private val CURRENT_STEP_COLOR = Color.parseColor("#4285F4")

    /** Set the header to blue-grey for step previews. */
    private val STEP_PREVIEW_COLOR = Color.parseColor("#617BA6")
  }
}
