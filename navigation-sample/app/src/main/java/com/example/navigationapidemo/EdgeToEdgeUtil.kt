
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

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.auto.value.AutoValue

/** Utility to support Edge-To-Edge mode for devices running Android V+. */
object EdgeToEdgeUtil {
  /**
   * Configuration to set up margins for {@link View}s supporting Edge-To-Edge mode with Android V+.
   */
  data class EdgeToEdgeMarginConfig(
    /** The {@link View} the margins need to be applied to. */
    val view: View,

    /** If the top margin should be applied, {@code false} otherwise. */
    val shouldSetTopMargin: Boolean = true,

    /** If the bottom margin should be applied, {@code false} otherwise. */
    val shouldSetBottomMargin: Boolean = true,

    /** If the left margin should be applied, {@code false} otherwise. */
    val shouldSetLeftMargin: Boolean = true,

    /** If the right margin should be applied, {@code false} otherwise. */
    val shouldSetRightMargin: Boolean = true,
  )

  /**
   * Edge-To-Edge mode is enabled by default on devices running Android V and above. As a result,
   * margins should be set for devices targeting Android V and higher so that they are not hidden
   * behind the system bars and remain accessible.
   *
   * <p>Note: The margins are applied when the edge-to-edge is enabled and because we are not
   * manually enabling it, they'll be applied only for Android V+ devices.
   *
   * @param edgeToEdgeMarginConfig, the configuration that requires to set margins.
   */
  fun setMarginForEdgeToEdgeSupport(edgeToEdgeMarginConfig: EdgeToEdgeMarginConfig): Unit {
    applyMargins(listOf(edgeToEdgeMarginConfig))
  }

  /**
   * Edge-To-Edge mode is enabled by default on devices running Android V and above. As a result,
   * margins should be set for devices targeting Android V and higher so that they are not hidden
   * behind the system bars and remain accessible.
   *
   * <p>Note: The margins are applied when the edge-to-edge is enabled and because we are not
   * manually enabling it, they'll be applied only for Android V+ devices.
   *
   * @param edgeToEdgeMarginConfigs, the list of configurations that requires to set margins.
   */
  fun setMarginForEdgeToEdgeSupport(edgeToEdgeMarginConfigs: List<EdgeToEdgeMarginConfig>): Unit {
    applyMargins(edgeToEdgeMarginConfigs)
  }

  /**
   * Applies margins for the provided configs.
   *
   * <p>A callback is provided to the window inset listener to adjust margins, ensuring that views
   * are not obscured by the system bars.
   *
   * <p>Note: The callbacks are only invoked when the edge-to-edge is enabled and because we are not
   * manually enabling it, they'll be invoked for Android V+ devices.
   *
   * @param edgeToEdgeMarginConfigs, the list of configurations that requires to set margins.
   */
  fun applyMargins(edgeToEdgeMarginConfigs: List<EdgeToEdgeMarginConfig>): Unit {
    for (edgeToEdgeMarginConfig in edgeToEdgeMarginConfigs) {
      ViewCompat.setOnApplyWindowInsetsListener(
        edgeToEdgeMarginConfig.view,
        { v, windowInsets ->
          val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
          // Apply the insets as a margin to the view.
          val mlp = v.layoutParams as MarginLayoutParams
          if (edgeToEdgeMarginConfig.shouldSetLeftMargin) {
            mlp.leftMargin = insets.left
          }
          if (edgeToEdgeMarginConfig.shouldSetRightMargin) {
            mlp.rightMargin = insets.right
          }
          if (edgeToEdgeMarginConfig.shouldSetTopMargin) {
            mlp.topMargin = insets.top
          }
          if (edgeToEdgeMarginConfig.shouldSetBottomMargin) {
            mlp.bottomMargin = insets.bottom
          }
          v.layoutParams = mlp

          // Return the window insets to keep passing down to descendant views.
          windowInsets
        },
      )
    }
  }
}
