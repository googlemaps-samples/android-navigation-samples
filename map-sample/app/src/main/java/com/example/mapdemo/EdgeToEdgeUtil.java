/*
 * Copyright 2025 Google LLC
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

package com.example.mapdemo;

import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Utility to support Edge-To-Edge mode for devices running Android V+. */
public final class EdgeToEdgeUtil {

  private EdgeToEdgeUtil() {}

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
  public static void setMarginForEdgeToEdgeSupport(EdgeToEdgeMarginConfig edgeToEdgeMarginConfig) {

    applyMargins(ImmutableList.of(edgeToEdgeMarginConfig));
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
  public static void setMarginForEdgeToEdgeSupport(
      List<EdgeToEdgeMarginConfig> edgeToEdgeMarginConfigs) {

    applyMargins(edgeToEdgeMarginConfigs);
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
  private static void applyMargins(List<EdgeToEdgeMarginConfig> edgeToEdgeMarginConfigs) {
    for (EdgeToEdgeMarginConfig edgeToEdgeMarginConfig : edgeToEdgeMarginConfigs) {
      ViewCompat.setOnApplyWindowInsetsListener(
          edgeToEdgeMarginConfig.view(),
          (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the insets as a margin to the view.
            MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
            if (edgeToEdgeMarginConfig.shouldSetLeftMargin()) {
              mlp.leftMargin = insets.left;
            }
            if (edgeToEdgeMarginConfig.shouldSetRightMargin()) {
              mlp.rightMargin = insets.right;
            }
            if (edgeToEdgeMarginConfig.shouldSetTopMargin()) {
              mlp.topMargin = insets.top;
            }
            if (edgeToEdgeMarginConfig.shouldSetBottomMargin()) {
              mlp.bottomMargin = insets.bottom;
            }
            v.setLayoutParams(mlp);

            // Return the window insets to keep passing down to descendant views.
            return windowInsets;
          });
    }
  }

  /**
   * Configuration to set up margins for {@link View}s supporting Edge-To-Edge mode with Android V+.
   */
  @AutoValue
  public abstract static class EdgeToEdgeMarginConfig {

    /** Returns the {@link View} the margins need to be applied to. */
    public abstract View view();

    /** Returns {@code true} if the top margin should be applied, {@code false} otherwise. */
    public abstract boolean shouldSetTopMargin();

    /** Returns {@code true} if the bottom margin should be applied, {@code false} otherwise. */
    public abstract boolean shouldSetBottomMargin();

    /** Returns {@code true} if the left margin should be applied, {@code false} otherwise. */
    public abstract boolean shouldSetLeftMargin();

    /** Returns {@code true} if the right margin should be applied, {@code false} otherwise. */
    public abstract boolean shouldSetRightMargin();

    /**
     * Creates and returns {@link Builder} to create an instance of {@link EdgeToEdgeMarginConfig}.
     *
     * <p>Default {@code true} is set for property, to avoid supplying values while using the {@link
     * Builder}.
     */
    public static Builder builder() {
      return new AutoValue_EdgeToEdgeUtil_EdgeToEdgeMarginConfig.Builder()
          .setShouldSetTopMargin(true)
          .setShouldSetBottomMargin(true)
          .setShouldSetLeftMargin(true)
          .setShouldSetRightMargin(true);
    }

    /** Provides a generalized {@code Builder} to create the {@link EdgeToEdgeMarginConfig}. */
    @AutoValue.Builder
    public abstract static class Builder {

      /** Sets the view to which the inset margins are applied. */
      public abstract Builder setView(View view);

      /**
       * Indicates whether the top margin should be applied to the {@link View} passed to {@link
       * #setView(View)}.
       *
       * <p>The default is {@code true}, inset changes will be applied by default for the top
       * margin.
       */
      public abstract Builder setShouldSetTopMargin(boolean shouldSetTopMargin);

      /**
       * Indicates whether the bottom margin should be applied to the {@link View} passed to {@link
       * #setView(View)}.
       *
       * <p>The default is {@code true}, inset changes will be applied by default for the bottom
       * margin.
       */
      public abstract Builder setShouldSetBottomMargin(boolean shouldSetBottomMargin);

      /**
       * Indicates whether the left margin should be applied to the {@link View} passed to {@link
       * #setView(View)}.
       *
       * <p>The default is {@code true}, inset changes will be applied by default for the left
       * margin.
       */
      public abstract Builder setShouldSetLeftMargin(boolean shouldSetLeftMargin);

      /**
       * Indicates whether the right margin should be applied to the {@link View} passed to {@link
       * #setView(View)}.
       *
       * <p>The default is {@code true}, inset changes will be applied by default for the right
       * margin.
       */
      public abstract Builder setShouldSetRightMargin(boolean shouldSetRightMargin);

      /** Creates and returns {@link EdgeToEdgeMarginConfig}. */
      public abstract EdgeToEdgeMarginConfig build();
    }
  }
}
