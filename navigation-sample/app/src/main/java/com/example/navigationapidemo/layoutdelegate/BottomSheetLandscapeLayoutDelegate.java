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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.TransitionManager;
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
 * A sample implementation of {@link NavigationLayoutDelegate} that features an expandable bottom
 * sheet to which custom content can be added. In this sample, we demonstrate placing a prompt above
 * the bottom sheet rather than at the bottom of the screen, including adding rounded corners at the
 * bottom to match those of the top.
 */
public class BottomSheetLandscapeLayoutDelegate extends NavigationLayoutDelegate {
  // The turn card's height varies dynamically based on the current maneuver.
  // Constraining viewport heights to dynamic-height views causes the map camera
  // to constantly adjust its zoom and focus, creating a jumpy user experience.
  // We define a fixed estimate of the turn card's height to use as a stable top margin.
  private static final int DEFAULT_TURN_CARD_HEIGHT_DP = 150;

  private static final int CUSTOM_BUTTON_SIZE_DP = 56;
  private static final int BUTTONS_CONTAINER_BOTTOM_MARGIN_DP = 8;
  private static final int COLLAPSED_BOTTOM_SHEET_HEIGHT_DP = 110;

  @ColorInt private static final int CUSTOM_UI_ELEMENT_COLOR = Color.parseColor("#FFA500");

  private final int layoutId;
  private final int exampleCustomButtonId;
  private final int bottomSheetViewId;
  private final int endControlsContainerId;
  private final int halfGuidelineId;

  private ConstraintLayout layout;
  private View exampleCustomButton;
  private View bottomSheetView;
  private AutoHidingVerticalLayout endControlsContainer;

  private boolean isBottomSheetExpanded = false;
  private boolean isActiveGuidance = false;
  @Nullable private View activePrompt;

  // We cache our ConstraintSet definitions to avoid cloning or rebuilding
  // constraint configurations programmatically on every transition. This optimization
  // keeps UI state switches (such as entering active guidance or popping up prompts) highly
  // performant.
  private ConstraintSet navigationReadyConstraintSet;
  private ConstraintSet activeGuidanceConstraintSet;
  private ConstraintSet activeGuidanceWideModeConstraintSet;
  private ConstraintSet activeGuidanceWithPromptConstraintSet;

  private ActiveGuidanceUiState activeGuidanceUiState;

  public BottomSheetLandscapeLayoutDelegate() {
    layoutId = View.generateViewId();
    exampleCustomButtonId = View.generateViewId();
    bottomSheetViewId = View.generateViewId();
    endControlsContainerId = View.generateViewId();
    halfGuidelineId = View.generateViewId();
  }

  @Override
  public void onEnterNavigationReady(
      NavigationUiParent navigationUiParent, NavigationReadyUiState newState) {
    Context context = navigationUiParent.getViewContext();

    // Implementation Tip: For simplicity, this sample instantiates views and constraints
    // programmatically. In a production application, you can safely inflate standard XML
    // layout templates to build your layout hierarchies and define base UI constraints.

    if (layout == null) {
      layout = new ConstraintLayout(context);
      layout.setId(layoutId);
    }
    layout.removeAllViews();
    if (newState != null) {
      createRootLayout(newState.getViewport(), newState.getGoogleLogo());
    }

    // We use AutoHidingVerticalLayout to create an adaptive vertical button container that
    // automatically hides or shows child views based on available screen height.
    if (endControlsContainer == null) {
      endControlsContainer = new AutoHidingVerticalLayout(context);
      endControlsContainer.setId(endControlsContainerId);
      endControlsContainer.setChildSpacing(dpToPx(StyleValues.buttonVerticalPaddingDp(), context));
      endControlsContainer.setPadding(0, 0, 0, 0);
    }

    endControlsContainer.removeAllViews();

    // Add the Navigation Ready UI buttons
    if (newState != null && newState.getNavigationReadyButtons() != null) {
      for (NavigationUiButton button : newState.getNavigationReadyButtons()) {
        removeFromParentView(button.getView());
        AutoHidingVerticalLayout.LayoutParams buttonLayoutParams =
            new AutoHidingVerticalLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        // Mark critical buttons (such as the compass) as high priority so they are the last to be
        // hidden by AutoHidingVerticalLayout when layout space is limited.
        if (button.getType() == NavigationUiButton.ButtonKnownType.COMPASS) {
          buttonLayoutParams.isHighPriority = true;
        }
        endControlsContainer.addView(button.getView(), buttonLayoutParams);
      }
    }

    if (endControlsContainer.getParent() == null) {
      LayoutParams endControlsLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
      layout.addView(endControlsContainer, endControlsLayoutParams);
    }

    // Build constraint set for Navigation Ready state
    if (navigationReadyConstraintSet == null) {
      navigationReadyConstraintSet = buildNavigationReadyConstraintSet(newState);
    }

    // Apply the constraints
    navigationReadyConstraintSet.applyTo(layout);

    navigationUiParent.removeNavigationLayout(layout);
    navigationUiParent.setNavigationLayout(layout);
  }

  private void createRootLayout(View viewport, View googleLogo) {
    // Add the Viewport (REQUIRED):
    // The viewport is an invisible bounding box used by Nav SDK to frame the vehicle
    // chevron and the upcoming route line. We want to position this view such that it avoids
    // being obscured by fully-opaque UI elements (like the turn card or the ETA card).
    removeFromParentView(viewport);
    LayoutParams viewportLayoutParams = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
    layout.addView(viewport, viewportLayoutParams);

    // Add the Google Logo / Re-center Button (REQUIRED):
    // This view displays the Google Maps logo during guidance and may transition into a
    // "Re-center" button if the user scrolls away from the vehicle chevron.
    removeFromParentView(googleLogo);
    LayoutParams logoLayoutParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    layout.addView(googleLogo, logoLayoutParams);
  }

  private View createExampleCustomButton(Context context) {
    FloatingActionButton button = new FloatingActionButton(context);
    button.setId(exampleCustomButtonId);
    button.setBackgroundTintList(ColorStateList.valueOf(CUSTOM_UI_ELEMENT_COLOR));
    button.setCompatElevation(0f);
    return button;
  }

  private AutoHidingVerticalLayout.LayoutParams createExampleCustomButtonLayoutParams(
      Context context) {
    int sizePx = dpToPx(CUSTOM_BUTTON_SIZE_DP, context);
    AutoHidingVerticalLayout.LayoutParams layoutParams =
        new AutoHidingVerticalLayout.LayoutParams(sizePx, sizePx);

    // We use the StyleValues API margins to align custom elements with standard
    // Navigation SDK UI elements.
    int horizontalMarginPx = dpToPx(StyleValues.buttonHorizontalPaddingDp(), context);
    int verticalMarginPx = dpToPx(StyleValues.buttonVerticalPaddingDp(), context);
    layoutParams.setMargins(
        horizontalMarginPx, verticalMarginPx, horizontalMarginPx, verticalMarginPx);

    return layoutParams;
  }

  @SuppressWarnings("SetTextI18n") // Sample layout delegate placeholder text is not translated.
  private View createBottomSheetView(Context context) {
    LinearLayout sheetView = new LinearLayout(context);
    sheetView.setId(bottomSheetViewId);
    sheetView.setOrientation(LinearLayout.VERTICAL);
    sheetView.setGravity(Gravity.CENTER_HORIZONTAL);

    // We use the StyleValues API margins and corner radii to visually match
    // or align custom elements (like this bottom sheet) with standard Navigation SDK UI elements
    // (such as the turn card or ETA card).
    GradientDrawable sheetBackground = new GradientDrawable();
    sheetBackground.setShape(GradientDrawable.RECTANGLE);
    sheetBackground.setColor(CUSTOM_UI_ELEMENT_COLOR);
    float cornerRadiusPx = dpToPx(StyleValues.defaultCornerRadiusDp(), context);
    sheetBackground.setCornerRadius(cornerRadiusPx);
    sheetView.setBackground(sheetBackground);

    sheetView.setOnClickListener(v -> toggleBottomSheetState(context));

    // Create the handle icon
    View handle = new View(context);
    LinearLayout.LayoutParams handleParams =
        new LinearLayout.LayoutParams(dpToPx(36, context), dpToPx(4, context));
    handleParams.topMargin = dpToPx(10, context);
    handleParams.bottomMargin = dpToPx(6, context);
    handle.setLayoutParams(handleParams);
    GradientDrawable handleDrawable = new GradientDrawable();
    handleDrawable.setShape(GradientDrawable.RECTANGLE);
    handleDrawable.setColor(Color.WHITE);
    handleDrawable.setCornerRadius(dpToPx(2, context));
    handle.setBackground(handleDrawable);
    sheetView.addView(handle);

    // Add a text view to demonstrate adding custom content
    TextView textView = new TextView(context);
    textView.setText("Application-specific UI here");
    textView.setGravity(Gravity.CENTER);
    textView.setTextColor(Color.WHITE);
    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    LinearLayout.LayoutParams textParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    textParams.topMargin = dpToPx(8, context);
    sheetView.addView(textView, textParams);

    return sheetView;
  }

  private int getBottomSheetTargetHeight(Context context) {
    if (isBottomSheetExpanded) {
      int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
      boolean isLandscape =
          context.getResources().getConfiguration().orientation
              == Configuration.ORIENTATION_LANDSCAPE;
      if (isLandscape) {
        return screenHeight;
      }
      return (int) (screenHeight * 0.50f);
    } else {
      return dpToPx(COLLAPSED_BOTTOM_SHEET_HEIGHT_DP, context);
    }
  }

  private LayoutParams createBottomSheetLayoutParams(Context context) {
    int targetHeight = getBottomSheetTargetHeight(context);
    return new LayoutParams(0, targetHeight);
  }

  private void toggleBottomSheetState(Context context) {
    if (bottomSheetView == null) {
      return;
    }
    isBottomSheetExpanded = !isBottomSheetExpanded;

    boolean isLandscape =
        context.getResources().getConfiguration().orientation
            == Configuration.ORIENTATION_LANDSCAPE;
    if (isLandscape && activeGuidanceUiState != null) {
      // Rebuild and apply the wide mode constraint set with a transition animation
      ConstraintSet constraintSet;
      if (activePrompt != null) {
        constraintSet = buildActiveGuidanceWithPromptWideModeConstraintSet(context, activePrompt);
        activeGuidanceWithPromptConstraintSet = constraintSet;
        activeGuidanceWideModeConstraintSet =
            buildActiveGuidanceWideModeConstraintSet(context, activeGuidanceUiState);
      } else {
        constraintSet = buildActiveGuidanceWideModeConstraintSet(context, activeGuidanceUiState);
        activeGuidanceWideModeConstraintSet = constraintSet;
      }
      TransitionManager.beginDelayedTransition(layout);
      constraintSet.applyTo(layout);
    } else {
      // In portrait, continue using the old height-based expansion
      int targetHeight = getBottomSheetTargetHeight(context);
      LayoutParams params = bottomSheetView.getLayoutParams();
      if (params != null) {
        params.height = targetHeight;
        bottomSheetView.setLayoutParams(params);
      }
    }
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

    // Constrain end controls container to the bottom end corner
    constraintSet.connect(
        endControlsContainerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
    constraintSet.connect(
        endControlsContainerId,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM);
    constraintSet.connect(
        endControlsContainerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
    constraintSet.constrainHeight(endControlsContainerId, ConstraintSet.MATCH_CONSTRAINT);
    constraintSet.constrainDefaultHeight(
        endControlsContainerId, ConstraintSet.MATCH_CONSTRAINT_WRAP);
    constraintSet.setVerticalBias(endControlsContainerId, 1.0f);

    return constraintSet;
  }

  @Override
  public void onLeaveNavigationReady(
      NavigationUiParent navigationUiParent, NavigationReadyUiState oldState) {
    if (endControlsContainer != null) {
      endControlsContainer.removeAllViews();
    }
    if (oldState != null && oldState.getNavigationReadyButtons() != null) {
      for (NavigationUiButton button : oldState.getNavigationReadyButtons()) {
        removeFromParentView(button.getView());
      }
    }
    if (layout != null) {
      layout.removeAllViews();
      navigationUiParent.removeNavigationLayout(layout);
    }
  }

  @Override
  public void onEnterActiveGuidance(
      NavigationUiParent navigationUiParent,
      NavigationReadyUiState oldState,
      ActiveGuidanceUiState newState) {
    activeGuidanceUiState = newState;
    isActiveGuidance = true;

    Context context = navigationUiParent.getViewContext();
    if (layout == null) {
      layout = new ConstraintLayout(context);
      layout.setId(layoutId);
    }
    layout.removeAllViews();
    if (newState != null) {
      createRootLayout(newState.getViewport(), newState.getGoogleLogo());
    }

    if (endControlsContainer == null) {
      endControlsContainer = new AutoHidingVerticalLayout(context);
      endControlsContainer.setId(endControlsContainerId);
      endControlsContainer.setGravity(Gravity.BOTTOM | Gravity.END);
      endControlsContainer.setChildSpacing(dpToPx(StyleValues.buttonVerticalPaddingDp(), context));
      endControlsContainer.setPadding(0, 0, 0, 0);
    }

    endControlsContainer.removeAllViews();

    // Add the turn card
    if (newState != null && newState.getTurnCard() != null) {
      removeFromParentView(newState.getTurnCard());
      LayoutParams turnCardLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
      layout.addView(newState.getTurnCard(), turnCardLayoutParams);
    }

    // Add the custom bottom sheet
    if (bottomSheetView == null) {
      bottomSheetView = createBottomSheetView(context);
    }
    if (bottomSheetView.getParent() == null) {
      LayoutParams bottomSheetLayoutParams = createBottomSheetLayoutParams(context);
      layout.addView(bottomSheetView, bottomSheetLayoutParams);
    }

    // Remove the Navigation Ready UI buttons
    if (oldState != null && oldState.getNavigationReadyButtons() != null) {
      for (NavigationUiButton button : oldState.getNavigationReadyButtons()) {
        removeFromParentView(button.getView());
      }
    }

    // Add the example button to its container
    if (exampleCustomButton == null) {
      exampleCustomButton = createExampleCustomButton(context);
    }
    removeFromParentView(exampleCustomButton);
    AutoHidingVerticalLayout.LayoutParams customButtonLayoutParams =
        createExampleCustomButtonLayoutParams(context);
    endControlsContainer.addView(exampleCustomButton, customButtonLayoutParams);

    // By adding all buttons to the AutoHidingVerticalLayout, we can easily incorporate the latest
    // set of buttons when upgrading without any code changes required
    if (newState != null && newState.getActiveGuidanceButtons() != null) {
      for (NavigationUiButton button : newState.getActiveGuidanceButtons()) {
        removeFromParentView(button.getView());
        AutoHidingVerticalLayout.LayoutParams buttonLayoutParams =
            new AutoHidingVerticalLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        // Mark critical buttons (such as the compass) as high priority so they are the last to be
        // hidden by AutoHidingVerticalLayout when layout space is limited.
        if (button.getType() == NavigationUiButton.ButtonKnownType.COMPASS) {
          buttonLayoutParams.isHighPriority = true;
        }
        endControlsContainer.addView(button.getView(), buttonLayoutParams);
      }
    }

    if (endControlsContainer.getParent() == null) {
      LayoutParams endControlsLayoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
      layout.addView(endControlsContainer, endControlsLayoutParams);
    }

    // Build constraint set for Active Guidance state
    if (activeGuidanceConstraintSet == null) {
      activeGuidanceConstraintSet = buildActiveGuidanceConstraintSet(context, newState);
    }
    if (activeGuidanceWideModeConstraintSet == null) {
      activeGuidanceWideModeConstraintSet =
          buildActiveGuidanceWideModeConstraintSet(context, newState);
    }

    // Apply the constraints
    if (newState != null && newState.isWideMode()) {
      activeGuidanceWideModeConstraintSet.applyTo(layout);
    } else {
      activeGuidanceConstraintSet.applyTo(layout);
    }

    navigationUiParent.removeNavigationLayout(layout);
    navigationUiParent.setNavigationLayout(layout);
  }

  @Override
  public void onLeaveActiveGuidance(
      NavigationUiParent navigationUiParent,
      ActiveGuidanceUiState oldState,
      NavigationReadyUiState newState) {
    isActiveGuidance = false;

    // Remove Active Guidance UI elements
    if (oldState != null && oldState.getTurnCard() != null) {
      removeFromParentView(oldState.getTurnCard());
    }
    removeFromParentView(bottomSheetView);
    removeFromParentView(exampleCustomButton);
    if (oldState != null && oldState.getActiveGuidanceButtons() != null) {
      for (NavigationUiButton button : oldState.getActiveGuidanceButtons()) {
        removeFromParentView(button.getView());
      }
    }

    // Add Navigation Ready UI buttons
    if (newState != null && newState.getNavigationReadyButtons() != null) {
      for (NavigationUiButton button : newState.getNavigationReadyButtons()) {
        if (button.getView().getParent() == null) {
          AutoHidingVerticalLayout.LayoutParams buttonLayoutParams =
              new AutoHidingVerticalLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
          // Mark critical buttons (such as the compass) as high priority so they are the last to be
          // hidden by AutoHidingVerticalLayout when layout space is limited.
          if (button.getType() == NavigationUiButton.ButtonKnownType.COMPASS) {
            buttonLayoutParams.isHighPriority = true;
          }
          endControlsContainer.addView(button.getView(), buttonLayoutParams);
        }
      }
    }

    if (navigationReadyConstraintSet != null && layout != null) {
      navigationReadyConstraintSet.applyTo(layout);
    }
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

    // Constrain viewport to top of bottom sheet
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
    // (which varies in height and would trigger jumpy camera framing updates), we position
    // it relative to the top of the parent layout with a fixed margin representing the
    // estimated turn card height.
    constraintSet.setMargin(
        uiState.getViewport().getId(),
        ConstraintSet.TOP,
        dpToPx(DEFAULT_TURN_CARD_HEIGHT_DP, context));
    constraintSet.connect(
        uiState.getViewport().getId(),
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END);
    constraintSet.connect(
        uiState.getViewport().getId(), ConstraintSet.BOTTOM, bottomSheetViewId, ConstraintSet.TOP);

    // Constrain the logo to the top of the bottom sheet
    constraintSet.clear(uiState.getGoogleLogo().getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(
        uiState.getGoogleLogo().getId(),
        ConstraintSet.BOTTOM,
        bottomSheetViewId,
        ConstraintSet.TOP);
    constraintSet.connect(
        uiState.getGoogleLogo().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);

    constrainBottomSheet(constraintSet, context);

    // Constrain end controls container to top of bottom sheet
    constrainEndControlsContainer(
        constraintSet, context, uiState, bottomSheetViewId, /* isWideMode= */ false);

    return constraintSet;
  }

  private void constrainBottomSheet(ConstraintSet constraintSet, Context context) {
    constraintSet.connect(
        bottomSheetViewId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
    constraintSet.connect(
        bottomSheetViewId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
    constraintSet.connect(
        bottomSheetViewId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
  }

  private void constrainEndControlsContainer(
      ConstraintSet constraintSet,
      Context context,
      ActiveGuidanceUiState uiState,
      int bottomAnchorId,
      boolean isWideMode) {
    constraintSet.clear(endControlsContainerId);
    if (isWideMode) {
      // In landscape (wide) mode, the turn card is on the left half and doesn't overlap the
      // buttons container on the right. Connect to parent top to maximize available height.
      constraintSet.connect(
          endControlsContainerId,
          ConstraintSet.TOP,
          ConstraintSet.PARENT_ID,
          ConstraintSet.TOP,
          dpToPx(8, context));
    } else {
      constraintSet.connect(
          endControlsContainerId,
          ConstraintSet.TOP,
          uiState.getTurnCard().getId(),
          ConstraintSet.BOTTOM,
          dpToPx(8, context));
    }
    int marginPx;
    int bottomPaddingPx;
    if (bottomAnchorId == ConstraintSet.PARENT_ID) {
      constraintSet.connect(
          endControlsContainerId,
          ConstraintSet.BOTTOM,
          ConstraintSet.PARENT_ID,
          ConstraintSet.BOTTOM);
      marginPx = 0;
      bottomPaddingPx = dpToPx(20, context);
    } else {
      constraintSet.connect(
          endControlsContainerId, ConstraintSet.BOTTOM, bottomAnchorId, ConstraintSet.TOP);
      marginPx = dpToPx(BUTTONS_CONTAINER_BOTTOM_MARGIN_DP, context);
      bottomPaddingPx = 0;
    }
    constraintSet.setMargin(endControlsContainerId, ConstraintSet.BOTTOM, marginPx);

    if (endControlsContainer != null) {
      endControlsContainer.setPadding(
          endControlsContainer.getPaddingLeft(),
          endControlsContainer.getPaddingTop(),
          endControlsContainer.getPaddingRight(),
          bottomPaddingPx);
    }
    constraintSet.connect(
        endControlsContainerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
    constraintSet.constrainHeight(endControlsContainerId, ConstraintSet.MATCH_CONSTRAINT);
    constraintSet.constrainDefaultHeight(
        endControlsContainerId, ConstraintSet.MATCH_CONSTRAINT_WRAP);
    constraintSet.setVerticalBias(endControlsContainerId, 1.0f);
  }

  @Override
  public void onShowPrompt(NavigationUiParent navigationUiParent, View newPrompt) {
    activePrompt = newPrompt;
    Context context = navigationUiParent.getViewContext();
    // By default, prompts in non-wide mode are positioned at the bottom of the screen with
    // square bottom corners. When positioning the prompt above a custom bottom sheet, applying
    // rounded bottom corners gives the prompt a clean, floating appearance that matches its top
    // corners.
    applyPromptRoundedBottomCorners(context, newPrompt);
    layout.addView(newPrompt);

    // When a prompt is displayed above the bottom sheet, we update our active constraints so
    // that the invisible Viewport sits entirely above the prompt. This automatically forces the
    // Nav SDK camera to adjust its zoom and framing so that the route chevron is always visible to
    // the driver.
    activeGuidanceWithPromptConstraintSet =
        activeGuidanceUiState != null && activeGuidanceUiState.isWideMode()
            ? buildActiveGuidanceWithPromptWideModeConstraintSet(context, newPrompt)
            : buildActiveGuidanceWithPromptConstraintSet(context, newPrompt);
    activeGuidanceWithPromptConstraintSet.applyTo(layout);
  }

  @Override
  public void onChangePrompt(
      NavigationUiParent navigationUiParent, View oldPrompt, View newPrompt) {
    activePrompt = newPrompt;
    Context context = navigationUiParent.getViewContext();
    applyPromptRoundedBottomCorners(context, newPrompt);

    activeGuidanceWithPromptConstraintSet.clear(oldPrompt.getId());
    layout.removeView(oldPrompt);
    layout.addView(newPrompt);

    // When a prompt is displayed above the bottom sheet, we update our active constraints so
    // that the invisible Viewport sits entirely above the prompt. This automatically forces the
    // Nav SDK camera to adjust its zoom and framing so that the route chevron is always visible to
    // the driver.
    activeGuidanceWithPromptConstraintSet =
        activeGuidanceUiState != null && activeGuidanceUiState.isWideMode()
            ? buildActiveGuidanceWithPromptWideModeConstraintSet(context, newPrompt)
            : buildActiveGuidanceWithPromptConstraintSet(context, newPrompt);
    activeGuidanceWithPromptConstraintSet.applyTo(layout);
  }

  @Override
  public void onHidePrompt(NavigationUiParent navigationUiParent, View oldPrompt) {
    activePrompt = null;
    activeGuidanceWithPromptConstraintSet.clear(oldPrompt.getId());
    layout.removeView(oldPrompt);
    if (activeGuidanceUiState != null && activeGuidanceUiState.isWideMode()) {
      activeGuidanceWideModeConstraintSet.applyTo(layout);
    } else {
      activeGuidanceConstraintSet.applyTo(layout);
    }
  }

  private static void applyPromptRoundedBottomCorners(Context context, View prompt) {
    if (prompt == null) {
      return;
    }

    // We apply bottom rounded corners to the prompt view so that it matches the top rounded corners
    // when positioned above the bottom sheet. We use the StyleValues.defaultCornerRadiusDp() API
    // to retrieve the standard default corner radius. To avoid clipping the top shadow or top
    // corners, we apply a custom ViewOutlineProvider that offsets the outline bound at the top.
    float cornerRadius = dpToPx(StyleValues.defaultCornerRadiusDp(), context);
    int topOffsetPx = 0;
    int sidePaddingPx = 0;

    GradientDrawable shape = new GradientDrawable();
    shape.setShape(GradientDrawable.RECTANGLE);
    shape.setColor(Color.WHITE);
    shape.setCornerRadius(cornerRadius);
    prompt.setBackground(shape);

    prompt.setOutlineProvider(
        new ViewOutlineProvider() {
          @Override
          public void getOutline(View view, Outline outline) {
            outline.setRoundRect(
                sidePaddingPx,
                topOffsetPx,
                view.getWidth() - sidePaddingPx,
                view.getHeight(),
                cornerRadius);
          }
        });
    prompt.setClipToOutline(true);
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
        dpToPx(DEFAULT_TURN_CARD_HEIGHT_DP, context));
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
    constraintSet.connect(
        activeGuidanceUiState.getGoogleLogo().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);

    constrainBottomSheet(constraintSet, context);

    // Constrain prompt above bottom sheet
    constraintSet.connect(
        prompt.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
    constraintSet.connect(
        prompt.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
    constraintSet.connect(
        prompt.getId(), ConstraintSet.BOTTOM, bottomSheetViewId, ConstraintSet.TOP);

    // When the prompt is positioned above the bottom sheet rather than at the bottom of the screen,
    // horizontal padding is added in non-wide mode to visually match standard Navigation SDK
    // elements
    // such as the turn card. In wide mode, the SDK automatically applies horizontal padding to the
    // prompt, so this margin is only applied when in non-wide mode.
    // We can use StyleValues.headerFooterSidePaddingDp() to visually align with the turn card.
    if (!activeGuidanceUiState.isWideMode()) {
      int promptSideMarginPx = dpToPx(StyleValues.headerFooterHorizontalPaddingDp(), context);
      constraintSet.setMargin(prompt.getId(), ConstraintSet.START, promptSideMarginPx);
      constraintSet.setMargin(prompt.getId(), ConstraintSet.END, promptSideMarginPx);
    }

    // Reposition end controls container above prompt
    constrainEndControlsContainer(
        constraintSet, context, activeGuidanceUiState, prompt.getId(), /* isWideMode= */ false);

    return constraintSet;
  }

  private ConstraintSet buildActiveGuidanceWideModeConstraintSet(
      Context context, ActiveGuidanceUiState uiState) {
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(layout);

    constraintSet.create(halfGuidelineId, ConstraintSet.VERTICAL_GUIDELINE);
    constraintSet.setGuidelinePercent(halfGuidelineId, 0.5f);

    constraintSet.clear(uiState.getViewport().getId());
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

    int sideMarginPx = dpToPx(StyleValues.headerFooterHorizontalPaddingDp(), context);

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
    constraintSet.setMargin(uiState.getTurnCard().getId(), ConstraintSet.START, sideMarginPx);
    constraintSet.clear(uiState.getTurnCard().getId(), ConstraintSet.END);
    constraintSet.constrainWidth(uiState.getTurnCard().getId(), ConstraintSet.WRAP_CONTENT);

    int turnCardId = uiState.getTurnCard().getId();
    constraintSet.connect(bottomSheetViewId, ConstraintSet.START, turnCardId, ConstraintSet.START);
    constraintSet.connect(bottomSheetViewId, ConstraintSet.END, turnCardId, ConstraintSet.END);
    constraintSet.connect(
        bottomSheetViewId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

    if (isBottomSheetExpanded) {
      // Exactly overlap the turn card
      constraintSet.connect(bottomSheetViewId, ConstraintSet.TOP, turnCardId, ConstraintSet.TOP);
      constraintSet.constrainHeight(bottomSheetViewId, ConstraintSet.MATCH_CONSTRAINT);
    } else {
      constraintSet.clear(bottomSheetViewId, ConstraintSet.TOP);
      constraintSet.constrainHeight(
          bottomSheetViewId, dpToPx(COLLAPSED_BOTTOM_SHEET_HEIGHT_DP, context));
    }

    constraintSet.clear(uiState.getGoogleLogo().getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(
        uiState.getGoogleLogo().getId(),
        ConstraintSet.BOTTOM,
        bottomSheetViewId,
        ConstraintSet.TOP);
    constraintSet.connect(
        uiState.getGoogleLogo().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);
    constraintSet.connect(
        uiState.getGoogleLogo().getId(),
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END);
    constraintSet.constrainWidth(uiState.getGoogleLogo().getId(), ConstraintSet.WRAP_CONTENT);
    constraintSet.setHorizontalBias(uiState.getGoogleLogo().getId(), 0f);

    constrainEndControlsContainer(
        constraintSet, context, uiState, ConstraintSet.PARENT_ID, /* isWideMode= */ true);

    return constraintSet;
  }

  private ConstraintSet buildActiveGuidanceWithPromptWideModeConstraintSet(
      Context context, View prompt) {
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(layout);

    constraintSet.create(halfGuidelineId, ConstraintSet.VERTICAL_GUIDELINE);
    constraintSet.setGuidelinePercent(halfGuidelineId, 0.5f);

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

    int sideMarginPx = dpToPx(StyleValues.headerFooterHorizontalPaddingDp(), context);

    constraintSet.connect(
        activeGuidanceUiState.getTurnCard().getId(),
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP);
    constraintSet.connect(
        activeGuidanceUiState.getTurnCard().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);
    constraintSet.setMargin(
        activeGuidanceUiState.getTurnCard().getId(), ConstraintSet.START, sideMarginPx);
    constraintSet.clear(activeGuidanceUiState.getTurnCard().getId(), ConstraintSet.END);
    constraintSet.constrainWidth(
        activeGuidanceUiState.getTurnCard().getId(), ConstraintSet.WRAP_CONTENT);

    int turnCardId = activeGuidanceUiState.getTurnCard().getId();
    constraintSet.connect(bottomSheetViewId, ConstraintSet.START, turnCardId, ConstraintSet.START);
    constraintSet.connect(bottomSheetViewId, ConstraintSet.END, turnCardId, ConstraintSet.END);
    constraintSet.connect(
        bottomSheetViewId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

    if (isBottomSheetExpanded) {
      constraintSet.connect(bottomSheetViewId, ConstraintSet.TOP, turnCardId, ConstraintSet.TOP);
      constraintSet.constrainHeight(bottomSheetViewId, ConstraintSet.MATCH_CONSTRAINT);
    } else {
      constraintSet.clear(bottomSheetViewId, ConstraintSet.TOP);
      constraintSet.constrainHeight(
          bottomSheetViewId, dpToPx(COLLAPSED_BOTTOM_SHEET_HEIGHT_DP, context));
    }

    constraintSet.connect(prompt.getId(), ConstraintSet.START, turnCardId, ConstraintSet.START);
    constraintSet.connect(prompt.getId(), ConstraintSet.END, turnCardId, ConstraintSet.END);
    constraintSet.constrainWidth(prompt.getId(), ConstraintSet.MATCH_CONSTRAINT);
    if (isBottomSheetExpanded) {
      constraintSet.connect(
          prompt.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
    } else {
      constraintSet.connect(
          prompt.getId(), ConstraintSet.BOTTOM, bottomSheetViewId, ConstraintSet.TOP);
    }

    constraintSet.clear(activeGuidanceUiState.getGoogleLogo().getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(
        activeGuidanceUiState.getGoogleLogo().getId(),
        ConstraintSet.BOTTOM,
        bottomSheetViewId,
        ConstraintSet.TOP);
    constraintSet.connect(
        activeGuidanceUiState.getGoogleLogo().getId(),
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START);
    constraintSet.connect(
        activeGuidanceUiState.getGoogleLogo().getId(),
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END);
    constraintSet.constrainWidth(
        activeGuidanceUiState.getGoogleLogo().getId(), ConstraintSet.WRAP_CONTENT);
    constraintSet.setHorizontalBias(activeGuidanceUiState.getGoogleLogo().getId(), 0f);

    constrainEndControlsContainer(
        constraintSet,
        context,
        activeGuidanceUiState,
        ConstraintSet.PARENT_ID,
        /* isWideMode= */ true);

    return constraintSet;
  }

  private void updateBottomSheetHeightForPortrait(Context context) {
    if (bottomSheetView != null) {
      int targetHeight = getBottomSheetTargetHeight(context);
      LayoutParams params = bottomSheetView.getLayoutParams();
      if (params != null) {
        params.height = targetHeight;
        bottomSheetView.setLayoutParams(params);
      }
    }
  }

  @Override
  public void onSizeChanged(NavigationUiParent parent, UiState state) {
    if (state instanceof ActiveGuidanceUiState) {
      activeGuidanceUiState = (ActiveGuidanceUiState) state;
    }
    if (isActiveGuidance) {
      View localActivePrompt = activePrompt;
      if (localActivePrompt != null) {
        activeGuidanceWithPromptConstraintSet =
            state.isWideMode()
                ? buildActiveGuidanceWithPromptWideModeConstraintSet(
                    parent.getViewContext(), localActivePrompt)
                : buildActiveGuidanceWithPromptConstraintSet(
                    parent.getViewContext(), localActivePrompt);
        activeGuidanceWithPromptConstraintSet.applyTo(layout);
        if (!state.isWideMode()) {
          updateBottomSheetHeightForPortrait(parent.getViewContext());
        }
      } else {
        if (state.isWideMode()) {
          if (activeGuidanceUiState != null) {
            activeGuidanceWideModeConstraintSet =
                buildActiveGuidanceWideModeConstraintSet(
                    parent.getViewContext(), activeGuidanceUiState);
            activeGuidanceWideModeConstraintSet.applyTo(layout);
          }
        } else {
          activeGuidanceConstraintSet.applyTo(layout);
          updateBottomSheetHeightForPortrait(parent.getViewContext());
        }
      }
    } else {
      if (navigationReadyConstraintSet != null) {
        navigationReadyConstraintSet.applyTo(layout);
      }
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
