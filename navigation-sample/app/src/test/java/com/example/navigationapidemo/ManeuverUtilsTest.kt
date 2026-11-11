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

import com.google.android.libraries.mapsplatform.turnbyturn.model.DrivingSide
import com.google.android.libraries.mapsplatform.turnbyturn.model.Maneuver
import com.google.android.libraries.mapsplatform.turnbyturn.model.StepInfo
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.powerassert.assertGeo
import java.lang.reflect.Modifier
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Fast, pure JVM unit tests for [ManeuverUtils].
 *
 * Demonstrates combining **Kotlin Power-Assert** (`assertGeo` / `assert`) for expressive boolean
 * invariant diagrams with **Google Truth** (`assertThat`) when inspecting Java reflection metadata
 * on the Navigation SDK's `Maneuver` annotation constants.
 */
class ManeuverUtilsTest {

  private fun stepWithManeuver(maneuver: Int): StepInfo =
    StepInfo.builder()
      .setManeuver(maneuver)
      .setDrivingSide(DrivingSide.RIGHT)
      .setFullRoadName(" Amphitheatre Pkwy ")
      .setSimpleRoadName("Amphitheatre Pkwy")
      .setFullInstructionText("Turn onto Amphitheatre Pkwy")
      .setStepNumber(1)
      .setRoundaboutTurnNumber(0)
      .setDistanceFromPrevStepMeters(250)
      .setTimeFromPrevStepSeconds(30)
      .build()

  @Test
  fun allDeclaredManeuverConstants_haveMatchingNameAndValidDrawableResource() {
    val maneuverFields =
      Maneuver::class
        .java
        .declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }

    // Ensure our reflection discovery found the full set of Turn-by-Turn Maneuver constants.
    assertThat(maneuverFields.size).isAtLeast(65)

    for (field in maneuverFields) {
      val maneuverValue = field.getInt(null)
      val expectedName = field.name
      val stepInfo = stepWithManeuver(maneuverValue)

      val actualName = ManeuverUtils.getManeuverName(stepInfo)
      val iconResId = ManeuverUtils.getManeuverIconResId(stepInfo)

      // Kotlin Power-Assert: if any maneuver mapping is missing or mismatched, the failure diagram
      // prints field.name, maneuverValue, actualName, and iconResId.
      assertGeo(actualName == expectedName && iconResId != 0)
    }
  }

  @Test
  fun directionalManeuvers_mapToDistinctLeftAndRightIcons() {
    val turnLeft = stepWithManeuver(Maneuver.TURN_LEFT)
    val turnRight = stepWithManeuver(Maneuver.TURN_RIGHT)
    val forkLeft = stepWithManeuver(Maneuver.FORK_LEFT)
    val forkRight = stepWithManeuver(Maneuver.FORK_RIGHT)
    val mergeLeft = stepWithManeuver(Maneuver.MERGE_LEFT)
    val mergeRight = stepWithManeuver(Maneuver.MERGE_RIGHT)
    val destLeft = stepWithManeuver(Maneuver.DESTINATION_LEFT)
    val destRight = stepWithManeuver(Maneuver.DESTINATION_RIGHT)

    assertGeo(
      ManeuverUtils.getManeuverIconResId(turnLeft) == R.drawable.ic_turn_left &&
        ManeuverUtils.getManeuverIconResId(turnRight) == R.drawable.ic_turn_right &&
        ManeuverUtils.getManeuverIconResId(turnLeft) !=
          ManeuverUtils.getManeuverIconResId(turnRight)
    )

    assertGeo(
      ManeuverUtils.getManeuverIconResId(forkLeft) == R.drawable.ic_fork_left &&
        ManeuverUtils.getManeuverIconResId(forkRight) == R.drawable.ic_fork_right &&
        ManeuverUtils.getManeuverIconResId(mergeLeft) == R.drawable.ic_merge_left &&
        ManeuverUtils.getManeuverIconResId(mergeRight) == R.drawable.ic_merge_right &&
        ManeuverUtils.getManeuverIconResId(destLeft) == R.drawable.ic_destination_left &&
        ManeuverUtils.getManeuverIconResId(destRight) == R.drawable.ic_destination_right
    )
  }

  @Test
  fun roundaboutAndFerryManeuvers_mapToExpectedNamesAndDrawables() {
    val roundaboutCw = stepWithManeuver(Maneuver.ROUNDABOUT_CLOCKWISE)
    val roundaboutCcw = stepWithManeuver(Maneuver.ROUNDABOUT_COUNTERCLOCKWISE)
    val ferryBoat = stepWithManeuver(Maneuver.FERRY_BOAT)
    val ferryTrain = stepWithManeuver(Maneuver.FERRY_TRAIN)

    assertGeo(
      ManeuverUtils.getManeuverName(roundaboutCw) == "ROUNDABOUT_CLOCKWISE" &&
        ManeuverUtils.getManeuverIconResId(roundaboutCw) == R.drawable.ic_roundabout_clockwise &&
        ManeuverUtils.getManeuverName(roundaboutCcw) == "ROUNDABOUT_COUNTERCLOCKWISE" &&
        ManeuverUtils.getManeuverIconResId(roundaboutCcw) ==
          R.drawable.ic_roundabout_counterclockwise &&
        ManeuverUtils.getManeuverIconResId(ferryBoat) == R.drawable.ic_ferry_boat &&
        ManeuverUtils.getManeuverIconResId(ferryTrain) == R.drawable.ic_ferry_train
    )
  }

  @Test
  fun unrecognizedManeuverCode_returnsNullNameAndThrowsOnIconLookup() {
    val invalidStep = stepWithManeuver(-9999)

    assertGeo(ManeuverUtils.getManeuverName(invalidStep) == null)
    assertThrows(NoSuchElementException::class.java) {
      ManeuverUtils.getManeuverIconResId(invalidStep)
    }
  }
}
