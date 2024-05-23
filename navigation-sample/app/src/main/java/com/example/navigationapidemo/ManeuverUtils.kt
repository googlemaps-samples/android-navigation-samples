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

import com.google.android.libraries.mapsplatform.turnbyturn.model.Maneuver
import com.google.android.libraries.mapsplatform.turnbyturn.model.StepInfo

/** Utility class that returns the drawable icon and string name of a given maneuver. */
object ManeuverUtils {
  private val maneuverInfoMap: Map<Int, ManeuverInfo> =
    mapOf(
      Maneuver.UNKNOWN to ManeuverInfo("UNKNOWN", R.drawable.ic_road),
      Maneuver.DEPART to ManeuverInfo("DEPART", R.drawable.ic_depart),
      Maneuver.DESTINATION to ManeuverInfo("DESTINATION", R.drawable.ic_destination),
      Maneuver.DESTINATION_LEFT to ManeuverInfo("DESTINATION_LEFT", R.drawable.ic_destination_left),
      Maneuver.DESTINATION_RIGHT to
        ManeuverInfo("DESTINATION_RIGHT", R.drawable.ic_destination_right),
      Maneuver.STRAIGHT to ManeuverInfo("STRAIGHT", R.drawable.ic_straight),
      Maneuver.TURN_LEFT to ManeuverInfo("TURN_LEFT", R.drawable.ic_turn_left),
      Maneuver.TURN_RIGHT to ManeuverInfo("TURN_RIGHT", R.drawable.ic_turn_right),
      Maneuver.TURN_KEEP_LEFT to ManeuverInfo("TURN_KEEP_LEFT", R.drawable.ic_fork_left),
      Maneuver.TURN_KEEP_RIGHT to ManeuverInfo("TURN_KEEP_RIGHT", R.drawable.ic_fork_right),
      Maneuver.TURN_SLIGHT_LEFT to ManeuverInfo("TURN_SLIGHT_LEFT", R.drawable.ic_turn_slight_left),
      Maneuver.TURN_SLIGHT_RIGHT to
        ManeuverInfo("TURN_SLIGHT_RIGHT", R.drawable.ic_turn_slight_right),
      Maneuver.TURN_SHARP_LEFT to ManeuverInfo("TURN_SHARP_LEFT", R.drawable.ic_turn_sharp_left),
      Maneuver.TURN_SHARP_RIGHT to ManeuverInfo("TURN_SHARP_RIGHT", R.drawable.ic_turn_sharp_right),
      Maneuver.TURN_U_TURN_CLOCKWISE to
        ManeuverInfo("TURN_U_TURN_CLOCKWISE", R.drawable.ic_turn_u_turn_clockwise),
      Maneuver.TURN_U_TURN_COUNTERCLOCKWISE to
        ManeuverInfo("TURN_U_TURN_COUNTERCLOCKWISE", R.drawable.ic_turn_u_turn_counterclockwise),
      Maneuver.MERGE_UNSPECIFIED to ManeuverInfo("MERGE_UNSPECIFIED", R.drawable.ic_merge),
      Maneuver.MERGE_LEFT to ManeuverInfo("MERGE_LEFT", R.drawable.ic_merge_left),
      Maneuver.MERGE_RIGHT to ManeuverInfo("MERGE_RIGHT", R.drawable.ic_merge_right),
      Maneuver.FORK_LEFT to ManeuverInfo("FORK_LEFT", R.drawable.ic_fork_left),
      Maneuver.FORK_RIGHT to ManeuverInfo("FORK_RIGHT", R.drawable.ic_fork_right),
      Maneuver.ON_RAMP_UNSPECIFIED to ManeuverInfo("ON_RAMP_UNSPECIFIED", R.drawable.ic_straight),
      Maneuver.ON_RAMP_LEFT to ManeuverInfo("ON_RAMP_LEFT", R.drawable.ic_turn_left),
      Maneuver.ON_RAMP_RIGHT to ManeuverInfo("ON_RAMP_RIGHT", R.drawable.ic_turn_right),
      Maneuver.ON_RAMP_KEEP_LEFT to ManeuverInfo("ON_RAMP_KEEP_LEFT", R.drawable.ic_fork_left),
      Maneuver.ON_RAMP_KEEP_RIGHT to ManeuverInfo("ON_RAMP_KEEP_RIGHT", R.drawable.ic_fork_right),
      Maneuver.ON_RAMP_SLIGHT_LEFT to
        ManeuverInfo("ON_RAMP_SLIGHT_LEFT", R.drawable.ic_turn_slight_left),
      Maneuver.ON_RAMP_SLIGHT_RIGHT to
        ManeuverInfo("ON_RAMP_SLIGHT_RIGHT", R.drawable.ic_turn_slight_right),
      Maneuver.ON_RAMP_SHARP_LEFT to
        ManeuverInfo("ON_RAMP_SHARP_LEFT", R.drawable.ic_turn_sharp_left),
      Maneuver.ON_RAMP_SHARP_RIGHT to
        ManeuverInfo("ON_RAMP_SHARP_RIGHT", R.drawable.ic_turn_sharp_right),
      Maneuver.ON_RAMP_U_TURN_CLOCKWISE to
        ManeuverInfo("ON_RAMP_U_TURN_CLOCKWISE", R.drawable.ic_turn_u_turn_clockwise),
      Maneuver.ON_RAMP_U_TURN_COUNTERCLOCKWISE to
        ManeuverInfo("ON_RAMP_U_TURN_COUNTERCLOCKWISE", R.drawable.ic_turn_u_turn_counterclockwise),
      Maneuver.OFF_RAMP_UNSPECIFIED to ManeuverInfo("OFF_RAMP_UNSPECIFIED", R.drawable.ic_straight),
      Maneuver.OFF_RAMP_LEFT to ManeuverInfo("OFF_RAMP_LEFT", R.drawable.ic_merge_left),
      Maneuver.OFF_RAMP_RIGHT to ManeuverInfo("OFF_RAMP_RIGHT", R.drawable.ic_merge_right),
      Maneuver.OFF_RAMP_KEEP_LEFT to ManeuverInfo("OFF_RAMP_KEEP_LEFT", R.drawable.ic_fork_left),
      Maneuver.OFF_RAMP_KEEP_RIGHT to ManeuverInfo("OFF_RAMP_KEEP_RIGHT", R.drawable.ic_fork_right),
      Maneuver.OFF_RAMP_SLIGHT_LEFT to
        ManeuverInfo("OFF_RAMP_SLIGHT_LEFT", R.drawable.ic_turn_slight_left),
      Maneuver.OFF_RAMP_SLIGHT_RIGHT to
        ManeuverInfo("OFF_RAMP_SLIGHT_RIGHT", R.drawable.ic_turn_slight_right),
      Maneuver.OFF_RAMP_SHARP_LEFT to
        ManeuverInfo("OFF_RAMP_SHARP_LEFT", R.drawable.ic_turn_sharp_left),
      Maneuver.OFF_RAMP_SHARP_RIGHT to
        ManeuverInfo("OFF_RAMP_SHARP_RIGHT", R.drawable.ic_turn_sharp_right),
      Maneuver.OFF_RAMP_U_TURN_CLOCKWISE to
        ManeuverInfo("OFF_RAMP_U_TURN_CLOCKWISE", R.drawable.ic_turn_u_turn_clockwise),
      Maneuver.OFF_RAMP_U_TURN_COUNTERCLOCKWISE to
        ManeuverInfo(
          "OFF_RAMP_U_TURN_COUNTERCLOCKWISE",
          R.drawable.ic_turn_u_turn_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_CLOCKWISE to
        ManeuverInfo("ROUNDABOUT_CLOCKWISE", R.drawable.ic_roundabout_clockwise),
      Maneuver.ROUNDABOUT_COUNTERCLOCKWISE to
        ManeuverInfo("ROUNDABOUT_COUNTERCLOCKWISE", R.drawable.ic_roundabout_counterclockwise),
      Maneuver.ROUNDABOUT_STRAIGHT_CLOCKWISE to
        ManeuverInfo("ROUNDABOUT_STRAIGHT_CLOCKWISE", R.drawable.ic_roundabout_straight_clockwise),
      Maneuver.ROUNDABOUT_STRAIGHT_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_STRAIGHT_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_straight_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_LEFT_CLOCKWISE to
        ManeuverInfo("ROUNDABOUT_LEFT_CLOCKWISE", R.drawable.ic_roundabout_left_clockwise),
      Maneuver.ROUNDABOUT_LEFT_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_LEFT_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_left_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_RIGHT_CLOCKWISE to
        ManeuverInfo("ROUNDABOUT_RIGHT_CLOCKWISE", R.drawable.ic_roundabout_right_clockwise),
      Maneuver.ROUNDABOUT_RIGHT_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_RIGHT_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_right_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_SLIGHT_LEFT_CLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_SLIGHT_LEFT_CLOCKWISE",
          R.drawable.ic_roundabout_slight_left_clockwise,
        ),
      Maneuver.ROUNDABOUT_SLIGHT_LEFT_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_SLIGHT_LEFT_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_slight_left_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_SLIGHT_RIGHT_CLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_SLIGHT_RIGHT_CLOCKWISE",
          R.drawable.ic_roundabout_slight_right_clockwise,
        ),
      Maneuver.ROUNDABOUT_SLIGHT_RIGHT_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_SLIGHT_RIGHT_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_slight_right_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_SHARP_LEFT_CLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_SHARP_LEFT_CLOCKWISE",
          R.drawable.ic_roundabout_sharp_left_clockwise,
        ),
      Maneuver.ROUNDABOUT_SHARP_LEFT_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_SHARP_LEFT_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_sharp_left_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_SHARP_RIGHT_CLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_SHARP_RIGHT_CLOCKWISE",
          R.drawable.ic_roundabout_sharp_right_clockwise,
        ),
      Maneuver.ROUNDABOUT_SHARP_RIGHT_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_SHARP_RIGHT_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_sharp_right_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_U_TURN_CLOCKWISE to
        ManeuverInfo("ROUNDABOUT_U_TURN_CLOCKWISE", R.drawable.ic_roundabout_u_turn_clockwise),
      Maneuver.ROUNDABOUT_U_TURN_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_U_TURN_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_u_turn_counterclockwise,
        ),
      Maneuver.ROUNDABOUT_EXIT_CLOCKWISE to
        ManeuverInfo("ROUNDABOUT_EXIT_CLOCKWISE", R.drawable.ic_roundabout_exit_clockwise),
      Maneuver.ROUNDABOUT_EXIT_COUNTERCLOCKWISE to
        ManeuverInfo(
          "ROUNDABOUT_EXIT_COUNTERCLOCKWISE",
          R.drawable.ic_roundabout_exit_counterclockwise,
        ),
      Maneuver.FERRY_BOAT to ManeuverInfo("FERRY_BOAT", R.drawable.ic_ferry_boat),
      Maneuver.FERRY_TRAIN to ManeuverInfo("FERRY_TRAIN", R.drawable.ic_ferry_train),
      Maneuver.NAME_CHANGE to ManeuverInfo("NAME_CHANGE", R.drawable.ic_straight),
    )

  /**
   * Returns the string name of the step's maneuver.
   *
   * @param stepInfo the given step
   * @return the string name of the step's maneuver
   */
  fun getManeuverName(stepInfo: StepInfo): String? = maneuverInfoMap[stepInfo.maneuver]?.name

  /**
   * Selects an appropriate icon for the maneuver of a given step.
   *
   * @param stepInfo the step
   * @return the resource id of the selected maneuver icon
   */
  fun getManeuverIconResId(stepInfo: StepInfo): Int =
    maneuverInfoMap.getValue(stepInfo.maneuver).iconResId

  /** Stores the maneuver's name and icon resource id. */
  private class ManeuverInfo(val name: String, val iconResId: Int)
}
