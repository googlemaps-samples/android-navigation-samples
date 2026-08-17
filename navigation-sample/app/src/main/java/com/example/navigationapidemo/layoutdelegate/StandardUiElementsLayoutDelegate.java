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

package com.example.navigationapidemo.layoutdelegate;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;
import static com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton.ButtonKnownType.COMPASS;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import com.google.android.libraries.navigation.layoutcustomization.ActiveGuidanceUiState;
import com.google.android.libraries.navigation.layoutcustomization.AutoHidingVerticalLayout;
import com.google.android.libraries.navigation.layoutcustomization.NavigationLayoutDelegate;
import com.google.android.libraries.navigation.layoutcustomization.NavigationReadyUiState;
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton;
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiParent;
import com.google.android.libraries.navigation.layoutcustomization.StyleValues;

/**
 * A sample implementation of {@link NavigationLayoutDelegate} demonstrating a basic,
 * portrait-optimized layout using {@link ConstraintLayout}.
 *
 * <p><b>Understanding the Layout Delegate State Machine:</b> Navigation SDK transitions through
 * distinct states, each calling corresponding lifecycle methods on this delegate:
 *
 * <ul>
 *   <li><b>Navigation Ready:</b> Initiated by {@link #onEnterNavigationReady}. We initialize the
 *       layout here and add non-guidance views, then pass it to {@link NavigationUiParent} as the
 *       navigation layout.
 *   <li><b>Active Guidance (Turn-by-Turn Mode):</b> Initiated by {@link #onEnterActiveGuidance}. We
 *       set up the layout for Active Guidance, adding elements such as the turn card and ETA card.
 *   <li><b>Prompts:</b> Prompts (e.g., incident alerts) may be triggered during Active Guidance
 *       mode and can be added to the layout via {@link #onShowPrompt}.
 * </ul>
 *
 * This class caches its {@link ConstraintSet}s to ensure smooth transitions without needing to
 * recreate or inflate layouts continuously.
 */
public class StandardUiElementsLayoutDelegate extends NavigationLayoutDelegate {
  private final int layoutId;
  private final int buttonsContainerId;

  private ConstraintLayout layout;
  private AutoHidingVerticalLayout buttonsContainer;

  // We cache our ConstraintSet definitions to avoid cloning or rebuilding
  // constraint configurations programmatically on every transition. This optimization
  // keeps UI state switches (such as entering active guidance or popping up prompts) highly
  // performant.
  private ConstraintSet navigationReadyConstraintSet;
  private ConstraintSet activeGuidanceConstraintSet;
  private ConstraintSet activeGuidanceWithPromptConstraintSet;

  private ActiveGuidanceUiState activeGuidanceUiState;

  public StandardUiElementsLayoutDelegate() {
    layoutId = View.generateViewId();
    buttonsContainerId = View.generateViewId();
  }

  @Override
  public void onEnterNavigationReady(
      NavigationUiParent navigationUiParent, NavigationReadyUiState newState) {
    Context context = navigationUiParent.getViewContext();

    // Implementation Tip: For simplicity, this sample instantiates views and constraints
    // programmatically. In a production application, you can safely inflate standard XML
    // layout templates to build your layout hierarchies and define base UI constraints.

    // Create the root layout
    if (layout == null) {
      layout = new ConstraintLayout(context);
      LayoutParams layoutParams = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
      layout.setLayoutParams(layoutParams);
      layout.setId(layoutId);
    }

    // Add the Viewport (REQUIRED):
    // The viewport is an invisible bounding box used by Nav SDK to frame the vehicle
    // chevron and the upcoming route line. We want to position this view such that it avoids
    // being obscured by fully-opaque UI elements (like the turn card or the ETA card).
    removeFromParentView(newState.getViewport());
    LayoutParams viewportLayoutParams = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
    layout.addView(newState.getViewport(), viewportLayoutParams);

    // Add the Google Logo / Re-center Button (REQUIRED):
    // This view displays the Google logo during guidance and may transition into a
    // "Re-center" button if the user scrolls away from the vehicle chevron. It must
    // be added to the view hierarchy in all states.
    removeFromParentView(newState.getGoogleLogo());
    LayoutParams googleLogoLayoutParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    layout.addView(newState.getGoogleLogo(), googleLogoLayoutParams);

    // Add the container for UI buttons
    if (buttonsContainer == null) {
      // We use AutoHidingVerticalLayout to create an adaptive vertical button container that
      // automatically hides or shows child views based on available screen height.
      buttonsContainer = new AutoHidingVerticalLayout(context);
      buttonsContainer.setId(buttonsContainerId);
    }
    removeFromParentView(buttonsContainer);
    LayoutParams buttonsContainerLayoutParams = new LayoutParams(WRAP_CONTENT, MATCH_CONSTRAINT);
    layout.addView(buttonsContainer, buttonsContainerLayoutParams);

    // Add UI buttons to the container
    for (NavigationUiButton button : newState.getNavigationReadyButtons()) {
      removeFromParentView(button.getView());
      buttonsContainer.addView(button.getView());
    }

    // Build constraint set for Navigation Ready state
    if (navigationReadyConstraintSet == null) {
      navigationReadyConstraintSet = buildNavigationReadyConstraintSet(newState);
    }

    // Apply the constraints
    navigationReadyConstraintSet.applyTo(layout);

    // Set the layout in NavigationUiParent
    navigationUiParent.removeNavigationLayout(layout);
    navigationUiParent.setNavigationLayout(layout);
  }

  private ConstraintSet buildNavigationReadyConstraintSet(NavigationReadyUiState uiState) {
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(layout);

    // Constrain viewport to the edges of its parent
    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);
    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP);
    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END);
    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM);

    // Constrain the logo to the bottom start corner
    constraintSet.connect(
        uiState.getGoogleLogo().getId(),
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM);
    constraintSet.connect(
        uiState.getGoogleLogo().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);

    constrainButtonsToBottomEnd(constraintSet);

    return constraintSet;
  }

  @Override
  public void onLeaveNavigationReady(
      NavigationUiParent navigationUiParent, NavigationReadyUiState oldState) {
    buttonsContainer.removeAllViews();
    layout.removeAllViews();
    navigationUiParent.removeNavigationLayout(layout);
  }

  @Override
  public void onEnterActiveGuidance(
      NavigationUiParent navigationUiParent,
      NavigationReadyUiState oldState,
      ActiveGuidanceUiState newState) {
    activeGuidanceUiState = newState;

    Context context = navigationUiParent.getViewContext();

    // Sizing Guideline: The turn card and ETA card are internally configured to adapt and size
    // themselves dynamically based on the layout width (non-wideMode vs. wideMode). Forcing fixed
    // widths or heights on these elements via layouts is unsupported. Always use WRAP_CONTENT to
    // let the elements determine their optimal proportions.

    // Add the turn card
    removeFromParentView(newState.getTurnCard());
    LayoutParams turnCardLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    layout.addView(newState.getTurnCard(), turnCardLayoutParams);

    // Add the ETA card
    removeFromParentView(newState.getEtaCard());
    LayoutParams etaCardLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    layout.addView(newState.getEtaCard(), etaCardLayoutParams);

    // Remove the Navigation Ready UI buttons
    for (NavigationUiButton button : oldState.getNavigationReadyButtons()) {
      removeFromParentView(button.getView());
    }

    // By adding all buttons to the AutoHidingVerticalLayout, we can easily incorporate the latest
    // set of buttons when upgrading without any code changes required
    for (NavigationUiButton button : newState.getActiveGuidanceButtons()) {
      AutoHidingVerticalLayout.LayoutParams buttonLayoutParams =
          new AutoHidingVerticalLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      // Mark critical buttons (such as the compass) as high priority so they are the last to be
      // hidden by AutoHidingVerticalLayout when layout space is limited.
      if (button.getType() == COMPASS) {
        buttonLayoutParams.isHighPriority = true;
      }

      removeFromParentView(button.getView());
      buttonsContainer.addView(button.getView(), buttonLayoutParams);
    }

    // Build constraint set for Active Guidance state
    if (activeGuidanceConstraintSet == null) {
      activeGuidanceConstraintSet = buildActiveGuidanceConstraintSet(context, newState);
    }

    // Apply the constraints
    activeGuidanceConstraintSet.applyTo(layout);
  }

  @Override
  public void onLeaveActiveGuidance(
      NavigationUiParent navigationUiParent,
      ActiveGuidanceUiState oldState,
      NavigationReadyUiState newState) {

    // Remove Active Guidance UI elements
    removeFromParentView(oldState.getEtaCard());
    removeFromParentView(oldState.getTurnCard());
    buttonsContainer.removeAllViews();

    // Add Navigation Ready UI buttons
    for (NavigationUiButton button : newState.getNavigationReadyButtons()) {
      LayoutParams buttonLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
      buttonsContainer.addView(button.getView(), buttonLayoutParams);
    }

    navigationReadyConstraintSet.applyTo(layout);
  }

  private ConstraintSet buildActiveGuidanceConstraintSet(
      Context context, ActiveGuidanceUiState uiState) {
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(layout);

    // Constrain turn card to top start corner
    constraintSet.connect(
        uiState.getTurnCard().getId(),
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP);
    constraintSet.connect(
        uiState.getTurnCard().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);

    // Constrain viewport to top of ETA card
    constraintSet.clear(uiState.getViewport().getId());
    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);
    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP);

    // Instead of constraining the viewport's top directly to the bottom of the turn card
    // (which varies in height and would trigger jumpy camera framing updates), we use a fixed
    // nominal height to estimate the height of the turncard.
    constraintSet.setMargin(
        uiState.getViewport().getId(),
        ConstraintSet.TOP,
        dpToPx(StyleValues.headerNominalHeightDp(), context));

    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END);
    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.BOTTOM,
        uiState.getEtaCard().getId(),
        ConstraintSet.TOP);

    constraintSet.clear(uiState.getGoogleLogo().getId(), ConstraintSet.BOTTOM);
    constrainLogoToTopOfEtaCard(uiState, constraintSet);

    constrainEtaCardToBottomStart(uiState, constraintSet);

    constrainButtonsToTopOfEtaCard(context, uiState, constraintSet);

    return constraintSet;
  }

  private static void constrainEtaCardToBottomStart(
      ActiveGuidanceUiState uiState, ConstraintSet constraintSet) {
    constraintSet.connect(
        uiState.getEtaCard().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);
    constraintSet.connect(
        uiState.getEtaCard().getId(),
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM);
  }

  private static void constrainLogoToTopOfEtaCard(
      ActiveGuidanceUiState uiState, ConstraintSet constraintSet) {
    constraintSet.connect(
        uiState.getGoogleLogo().getId(),
        ConstraintSet.BOTTOM,
        uiState.getEtaCard().getId(),
        ConstraintSet.TOP);
  }

  private void constrainButtonsToBottomEnd(ConstraintSet constraintSet) {
    constraintSet.clear(buttonsContainerId, ConstraintSet.BOTTOM);
    constraintSet.clear(buttonsContainerId, ConstraintSet.TOP);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
  }

  private void constrainButtonsToTopOfEtaCard(
      Context context, ActiveGuidanceUiState uiState, ConstraintSet constraintSet) {
    constraintSet.clear(buttonsContainerId, ConstraintSet.BOTTOM);
    constraintSet.clear(buttonsContainerId, ConstraintSet.TOP);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.BOTTOM, uiState.getEtaCard().getId(), ConstraintSet.TOP);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
    constraintSet.setMargin(
        buttonsContainerId,
        ConstraintSet.TOP,
        dpToPx(StyleValues.headerNominalHeightDp(), context));
  }

  @Override
  public void onShowPrompt(NavigationUiParent navigationUiParent, View newPrompt) {
    Context context = navigationUiParent.getViewContext();

    layout.addView(newPrompt);

    // When a prompt is displayed at the bottom of the screen, we update our active constraints so
    // that the invisible Viewport sits entirely above the prompt. This automatically forces the
    // Nav SDK camera to adjust its zoom and framing so that the route chevron is always visible to
    // the driver.
    activeGuidanceWithPromptConstraintSet =
        buildActiveGuidanceWithPromptConstraintSet(context, newPrompt);
    activeGuidanceWithPromptConstraintSet.applyTo(layout);
  }

  @Override
  public void onChangePrompt(
      NavigationUiParent navigationUiParent, View oldPrompt, View newPrompt) {
    Context context = navigationUiParent.getViewContext();

    activeGuidanceWithPromptConstraintSet.clear(oldPrompt.getId());

    layout.removeView(oldPrompt);
    layout.addView(newPrompt);

    // When a prompt is displayed at the bottom of the screen, we update our active constraints so
    // that the invisible Viewport sits entirely above the prompt. This automatically forces the
    // Nav SDK camera to adjust its zoom and framing so that the route chevron is always visible to
    // the driver.
    activeGuidanceWithPromptConstraintSet =
        buildActiveGuidanceWithPromptConstraintSet(context, newPrompt);
    activeGuidanceWithPromptConstraintSet.applyTo(layout);
  }

  @Override
  public void onHidePrompt(NavigationUiParent navigationUiParent, View oldPrompt) {
    activeGuidanceWithPromptConstraintSet.clear(oldPrompt.getId());

    layout.removeView(oldPrompt);

    activeGuidanceConstraintSet.applyTo(layout);
  }

  private ConstraintSet buildActiveGuidanceWithPromptConstraintSet(Context context, View prompt) {
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(layout);

    // Constrain viewport to top of prompt
    constraintSet.clear(activeGuidanceUiState.getViewport().getId());
    constraintSet.connect(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);
    constraintSet.connect(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP);
    constraintSet.setMargin(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.TOP,
        dpToPx(StyleValues.headerNominalHeightDp(), context));
    constraintSet.connect(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END);
    constraintSet.connect(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.BOTTOM,
        prompt.getId(),
        ConstraintSet.TOP);

    // Constrain prompt to bottom start corner
    constraintSet.connect(
        prompt.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
    constraintSet.connect(
        prompt.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

    return constraintSet;
  }

  private void removeFromParentView(View view) {
    if (view != null && view.getParent() != null) {
      ((ViewGroup) view.getParent()).removeView(view);
    }
  }

  private static int dpToPx(int dp, Context context) {
    return (int)
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
  }
}
