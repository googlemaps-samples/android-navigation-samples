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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import androidx.annotation.ColorInt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import com.google.android.libraries.navigation.layoutcustomization.ActiveGuidanceUiState;
import com.google.android.libraries.navigation.layoutcustomization.AutoHidingVerticalLayout;
import com.google.android.libraries.navigation.layoutcustomization.NavigationLayoutDelegate;
import com.google.android.libraries.navigation.layoutcustomization.NavigationReadyUiState;
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton;
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiParent;
import com.google.android.libraries.navigation.layoutcustomization.StyleValues;
import com.google.android.libraries.navigation.layoutcustomization.UiState;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * A robust implementation of {@link NavigationLayoutDelegate} demonstrating a navigation user
 * interface optimized for both portrait and landscape (wide mode).
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
 *
 * @see NavigationLayoutDelegate
 * @see UiState
 */
public class AllUiElementsLayoutDelegate extends NavigationLayoutDelegate {
  private static final int CUSTOM_BUTTON_SIZE_DP = 56;

  @ColorInt private static final int CUSTOM_UI_ELEMENT_COLOR = Color.parseColor("#FFA500");

  private final int layoutId;
  private final int exampleCustomButtonId;
  private final int buttonsContainerId;
  private final int startUiControlsContainerId;

  private ConstraintLayout layout;
  private View exampleCustomButton;
  private AutoHidingVerticalLayout buttonsContainer;
  private AutoHidingVerticalLayout startUiControlsContainer;

  // We cache our ConstraintSet definitions to avoid cloning or rebuilding
  // constraint configurations programmatically on every transition. This optimization
  // keeps UI state switches (such as entering active guidance or popping up prompts) highly
  // performant.
  private ConstraintSet navigationReadyConstraintSet;
  private ConstraintSet activeGuidanceConstraintSet;
  private ConstraintSet activeGuidanceWideModeConstraintSet;
  private ConstraintSet activeGuidanceWithPromptConstraintSet;

  private boolean isActiveGuidance = false;
  private ActiveGuidanceUiState activeGuidanceUiState;

  public AllUiElementsLayoutDelegate() {
    layoutId = View.generateViewId();
    exampleCustomButtonId = View.generateViewId();
    buttonsContainerId = View.generateViewId();
    startUiControlsContainerId = View.generateViewId();
  }

  @Override
  public void onEnterNavigationReady(
      NavigationUiParent navigationUiParent, NavigationReadyUiState newState) {
    isActiveGuidance = false;

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
      buttonsContainer.setPadding(0, 0, 0, 0);
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
    isActiveGuidance = true;
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

    // Add an example custom button to the buttons container (Because we're adding it first, this
    // will be the top-most button displayed in the container)
    if (exampleCustomButton == null) {
      exampleCustomButton = createExampleCustomButton(context);
    }
    removeFromParentView(exampleCustomButton);
    LayoutParams customButtonLayoutParams = createExampleCustomButtonLayoutParams(context);
    buttonsContainer.addView(exampleCustomButton, customButtonLayoutParams);

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

    // Add the container for start side UI controls (like Speed Widget and Trip Progress Bar)
    if (startUiControlsContainer == null) {
      // Using an AutoHidingVerticalLayout makes it easy to auto-hide UI
      // elements if there isn't enough vertical space to display them all.
      startUiControlsContainer = new AutoHidingVerticalLayout(context);
      startUiControlsContainer.setId(startUiControlsContainerId);
      startUiControlsContainer.setPadding(0, 0, 0, 0);
      startUiControlsContainer.setChildSpacing(dpToPx(8, context));
      startUiControlsContainer.setGravity(Gravity.BOTTOM | Gravity.START);
    }
    removeFromParentView(startUiControlsContainer);
    LayoutParams startUiControlsContainerLayoutParams =
        new LayoutParams(WRAP_CONTENT, MATCH_CONSTRAINT);
    layout.addView(startUiControlsContainer, startUiControlsContainerLayoutParams);

    // Add the Trip Progress Bar and speed widget to the container
    LayoutParams startControlLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);

    removeFromParentView(newState.getTripProgressBar());
    startUiControlsContainer.addView(newState.getTripProgressBar(), startControlLayoutParams);

    removeFromParentView(newState.getSpeedWidget());
    startUiControlsContainer.addView(newState.getSpeedWidget(), startControlLayoutParams);

    // Build constraint sets for Active Guidance state
    if (activeGuidanceConstraintSet == null) {
      activeGuidanceConstraintSet = buildActiveGuidanceConstraintSet(context, newState);
    }
    if (activeGuidanceWideModeConstraintSet == null) {
      activeGuidanceWideModeConstraintSet =
          buildActiveGuidanceWideModeConstraintSet(context, newState);
    }

    // Apply the constraints
    if (activeGuidanceUiState.isWideMode()) {
      activeGuidanceWideModeConstraintSet.applyTo(layout);
    } else {
      activeGuidanceConstraintSet.applyTo(layout);
    }
  }

  private View createExampleCustomButton(Context context) {
    FloatingActionButton button = new FloatingActionButton(context);
    button.setId(exampleCustomButtonId);
    button.setBackgroundTintList(ColorStateList.valueOf(CUSTOM_UI_ELEMENT_COLOR));
    button.setCompatElevation(0f);
    return button;
  }

  private LayoutParams createExampleCustomButtonLayoutParams(Context context) {
    int sizePx = dpToPx(CUSTOM_BUTTON_SIZE_DP, context);
    AutoHidingVerticalLayout.LayoutParams layoutParams =
        new AutoHidingVerticalLayout.LayoutParams(sizePx, sizePx);

    // The StyleValues API provides margins that can be used to align custom elements with Nav SDK
    // UI elements
    int horizontalMarginPx = dpToPx(StyleValues.buttonHorizontalPaddingDp(), context);
    int verticalMarginPx = dpToPx(StyleValues.buttonVerticalPaddingDp(), context);
    layoutParams.setMargins(horizontalMarginPx, verticalMarginPx, horizontalMarginPx, 0);

    return layoutParams;
  }

  @Override
  public void onLeaveActiveGuidance(
      NavigationUiParent navigationUiParent,
      ActiveGuidanceUiState oldState,
      NavigationReadyUiState newState) {
    isActiveGuidance = false;

    // Remove Active Guidance UI elements
    removeFromParentView(oldState.getSpeedWidget());
    removeFromParentView(oldState.getTripProgressBar());
    removeFromParentView(oldState.getEtaCard());
    removeFromParentView(oldState.getTurnCard());
    removeFromParentView(startUiControlsContainer);
    buttonsContainer.removeAllViews();
    startUiControlsContainer.removeAllViews();

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

    constrainButtonsToTopOfEtaCard(uiState, constraintSet);

    constrainStartUiControlsContainerAboveGoogleLogo(uiState, constraintSet);

    return constraintSet;
  }

  private ConstraintSet buildActiveGuidanceWideModeConstraintSet(
      Context context, ActiveGuidanceUiState uiState) {
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(layout);

    // Split the screen vertically. Since the turn card and ETA card become half-width in wide mode,
    // we position the viewport in the end half to make better use of the screen space.
    int halfGuidelineId = View.generateViewId();
    createHalfwayVerticalGuideline(constraintSet, halfGuidelineId);

    constraintSet.clear(uiState.getViewport().getId());
    constrainViewportToEndHalf(uiState, constraintSet, halfGuidelineId);

    // Constrain turn card to top start
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

    constraintSet.clear(uiState.getGoogleLogo().getId(), ConstraintSet.BOTTOM);
    constrainLogoToTopOfEtaCard(uiState, constraintSet);

    constrainEtaCardToBottomStart(uiState, constraintSet);

    constrainButtonsToBottomEnd(constraintSet);

    constrainStartUiControlsContainerAboveGoogleLogo(uiState, constraintSet);

    return constraintSet;
  }

  private static void createHalfwayVerticalGuideline(
      ConstraintSet constraintSet, int halfGuidelineId) {
    constraintSet.create(halfGuidelineId, ConstraintSet.VERTICAL_GUIDELINE);
    constraintSet.setGuidelinePercent(halfGuidelineId, 0.5f);
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
      ActiveGuidanceUiState uiState, ConstraintSet constraintSet) {
    constraintSet.clear(buttonsContainerId, ConstraintSet.BOTTOM);
    constraintSet.clear(buttonsContainerId, ConstraintSet.TOP);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.TOP, uiState.getTurnCard().getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.BOTTOM, uiState.getEtaCard().getId(), ConstraintSet.TOP);
    constraintSet.connect(
        buttonsContainerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
  }

  private void constrainStartUiControlsContainerAboveGoogleLogo(
      ActiveGuidanceUiState uiState, ConstraintSet constraintSet) {
    constraintSet.connect(
        startUiControlsContainerId,
        ConstraintSet.TOP,
        uiState.getTurnCard().getId(),
        ConstraintSet.BOTTOM);
    constraintSet.connect(
        startUiControlsContainerId,
        ConstraintSet.BOTTOM,
        uiState.getGoogleLogo().getId(),
        ConstraintSet.TOP);
    constraintSet.connect(
        startUiControlsContainerId,
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);
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
        activeGuidanceUiState.isWideMode()
            ? buildActiveGuidanceWithPromptWideModeConstraintSet(newPrompt)
            : buildActiveGuidanceWithPromptConstraintSet(context, newPrompt);
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
        activeGuidanceUiState.isWideMode()
            ? buildActiveGuidanceWithPromptWideModeConstraintSet(newPrompt)
            : buildActiveGuidanceWithPromptConstraintSet(context, newPrompt);
    activeGuidanceWithPromptConstraintSet.applyTo(layout);
  }

  @Override
  public void onHidePrompt(NavigationUiParent navigationUiParent, View oldPrompt) {
    activeGuidanceWithPromptConstraintSet.clear(oldPrompt.getId());

    layout.removeView(oldPrompt);

    if (activeGuidanceUiState.isWideMode()) {
      activeGuidanceWideModeConstraintSet.applyTo(layout);
    } else {
      activeGuidanceConstraintSet.applyTo(layout);
    }
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

    // Constrain logo to top of prompt
    constraintSet.clear(activeGuidanceUiState.getGoogleLogo().getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(
        activeGuidanceUiState.getGoogleLogo().getId(),
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

  private ConstraintSet buildActiveGuidanceWithPromptWideModeConstraintSet(View prompt) {
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(layout);

    // Split the screen vertically. Since the turn card and ETA card become half-width in wide mode,
    // we position the viewport in the end half to make better use of the screen space.
    int halfGuidelineId = View.generateViewId();
    createHalfwayVerticalGuideline(constraintSet, halfGuidelineId);

    // Constrain viewport to end half of parent
    constraintSet.clear(activeGuidanceUiState.getViewport().getId());
    constraintSet.connect(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.START,
        halfGuidelineId,
        ConstraintSet.START);
    constraintSet.connect(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP);
    constraintSet.connect(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END);
    constraintSet.connect(
        activeGuidanceUiState.getViewport().getId(),
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM);

    // Constrain prompt to bottom start corner
    constraintSet.connect(
        prompt.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
    constraintSet.connect(
        prompt.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

    return constraintSet;
  }

  private static void constrainViewportToEndHalf(
      UiState uiState, ConstraintSet constraintSet, int halfGuidelineId) {
    constraintSet.connect(
        uiState.getViewport().getId(), ConstraintSet.START, halfGuidelineId, ConstraintSet.START);
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
  }

  @Override
  public void onSizeChanged(NavigationUiParent navigationUiParent, UiState state) {
    if (isActiveGuidance) {
      if (state.isWideMode()) {
        activeGuidanceWideModeConstraintSet.applyTo(layout);
      } else {
        activeGuidanceConstraintSet.applyTo(layout);
      }
    } else {
      navigationReadyConstraintSet.applyTo(layout);
    }
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
