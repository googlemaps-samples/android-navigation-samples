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

@file:Suppress("PackageName")
@file:SuppressLint("PackageName")

package com.example.navigationapidemo.layoutdelegate

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.toColorInt
import androidx.transition.TransitionManager
import com.google.android.libraries.navigation.layoutcustomization.ActiveGuidanceUiState
import com.google.android.libraries.navigation.layoutcustomization.AutoHidingVerticalLayout
import com.google.android.libraries.navigation.layoutcustomization.NavigationLayoutDelegate
import com.google.android.libraries.navigation.layoutcustomization.NavigationReadyUiState
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiParent
import com.google.android.libraries.navigation.layoutcustomization.StyleValues
import com.google.android.libraries.navigation.layoutcustomization.UiState
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * A sample implementation of [NavigationLayoutDelegate] in Kotlin that features an expandable
 * bottom sheet to which custom content can be added. This sample also demonstrates placing a prompt
 * above the bottom sheet rather than at the bottom of the screen, including adding rounded corners
 * at the bottom to match those of the top.
 */
class BottomSheetLandscapeLayoutDelegateKt : NavigationLayoutDelegate() {

  private val layoutId = View.generateViewId()
  private val exampleCustomButtonId = View.generateViewId()
  private val bottomSheetViewId = View.generateViewId()
  private val endControlsContainerId = View.generateViewId()
  private val halfGuidelineId = View.generateViewId()

  private var layout: ConstraintLayout? = null
  private var exampleCustomButton: View? = null
  private var bottomSheetView: View? = null
  private var endControlsContainer: AutoHidingVerticalLayout? = null

  private var isBottomSheetExpanded = false
  private var isActiveGuidance = false
  private var activePrompt: View? = null

  // We cache our ConstraintSet definitions to avoid cloning or rebuilding
  // constraint configurations programmatically on every transition. This optimization
  // keeps UI state switches (such as entering active guidance or popping up prompts) highly
  // performant.
  private var navigationReadyConstraintSet: ConstraintSet? = null
  private var activeGuidanceConstraintSet: ConstraintSet? = null
  private var activeGuidanceWideModeConstraintSet: ConstraintSet? = null
  private var activeGuidanceWithPromptConstraintSet: ConstraintSet? = null

  private var activeGuidanceUiState: ActiveGuidanceUiState? = null

  override fun onEnterNavigationReady(
    navigationUiParent: NavigationUiParent,
    newState: NavigationReadyUiState?,
  ) {
    val context = navigationUiParent.viewContext
    val layout =
      this.layout ?: ConstraintLayout(context).apply { id = layoutId }.also { this.layout = it }
    layout.removeAllViews()

    // Implementation Tip: For simplicity, this sample instantiates views and constraints
    // programmatically. In a production application, you can safely inflate standard XML
    // layout templates to build your layout hierarchies and define base UI constraints.

    if (newState != null) {
      ensureRootLayoutCreated(layout, newState.viewport, newState.googleLogo)
    }

    // We use AutoHidingVerticalLayout to create an adaptive vertical button container that
    // automatically hides or shows child views based on available screen height.
    if (endControlsContainer == null) {
      endControlsContainer =
        AutoHidingVerticalLayout(context).apply {
          id = endControlsContainerId
          childSpacing = dpToPx(StyleValues.buttonVerticalPaddingDp(), context)
          setPadding(0, 0, 0, 0)
        }
    }

    endControlsContainer?.removeAllViews()

    // Add the Navigation Ready UI buttons
    newState?.navigationReadyButtons?.forEach { button ->
      removeFromParentView(button.view)
      val buttonLayoutParams = AutoHidingVerticalLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
      // Mark critical buttons (such as the compass) as high priority so they are the last to be
      // hidden by AutoHidingVerticalLayout when layout space is limited.
      if (button.type == NavigationUiButton.ButtonKnownType.COMPASS) {
        buttonLayoutParams.isHighPriority = true
      }
      endControlsContainer?.addView(button.view, buttonLayoutParams)
    }

    if (endControlsContainer?.parent == null) {
      val endControlsLayoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
      layout.addView(endControlsContainer, endControlsLayoutParams)
    }

    // Build constraint set for Navigation Ready state
    if (navigationReadyConstraintSet == null && newState != null) {
      navigationReadyConstraintSet = buildNavigationReadyConstraintSet(newState)
    }

    // Apply the constraints
    navigationReadyConstraintSet?.applyTo(layout)

    navigationUiParent.removeNavigationLayout(layout)
    navigationUiParent.setNavigationLayout(layout)
  }

  private fun ensureRootLayoutCreated(layout: ConstraintLayout, viewport: View, googleLogo: View) {
    // Add the Viewport (REQUIRED):
    // The viewport is an invisible bounding box used by Nav SDK to frame the vehicle
    // chevron and the upcoming route line. We want to position this view such that it avoids
    // being obscured by fully-opaque UI elements (like the turn card or the ETA card).
    removeFromParentView(viewport)
    val viewportLayoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    layout.addView(viewport, viewportLayoutParams)

    // Add the Google Logo / Re-center Button (REQUIRED):
    // This view displays the Google Maps logo during guidance and may transition into a
    // "Re-center" button if the user scrolls away from the vehicle chevron.
    removeFromParentView(googleLogo)
    val logoLayoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    layout.addView(googleLogo, logoLayoutParams)
  }

  private fun createExampleCustomButton(context: Context): View {
    return FloatingActionButton(context).apply {
      id = exampleCustomButtonId
      backgroundTintList = ColorStateList.valueOf(CUSTOM_UI_ELEMENT_COLOR)
      compatElevation = 0f
    }
  }

  private fun createExampleCustomButtonLayoutParams(
    context: Context
  ): AutoHidingVerticalLayout.LayoutParams {
    val sizePx = dpToPx(CUSTOM_BUTTON_SIZE_DP, context)
    val layoutParams = AutoHidingVerticalLayout.LayoutParams(sizePx, sizePx)

    // We use the StyleValues API margins to align custom elements with standard
    // Navigation SDK UI elements.
    val horizontalMarginPx = dpToPx(StyleValues.buttonHorizontalPaddingDp(), context)
    val verticalMarginPx = dpToPx(StyleValues.buttonVerticalPaddingDp(), context)
    layoutParams.setMargins(
      horizontalMarginPx,
      verticalMarginPx,
      horizontalMarginPx,
      verticalMarginPx,
    )
    return layoutParams
  }

  private fun createBottomSheetView(context: Context): View {
    val sheetView =
      LinearLayout(context).apply {
        id = bottomSheetViewId
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
      }

    // We use the StyleValues API margins and corner radii to visually match
    // or align custom elements (like this bottom sheet) with standard Navigation SDK UI elements
    // (such as the turn card or ETA card).
    val sheetBackground =
      GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(CUSTOM_UI_ELEMENT_COLOR)
        val cornerRadiusPx = dpToPx(StyleValues.defaultCornerRadiusDp(), context).toFloat()
        cornerRadius = cornerRadiusPx
      }
    sheetView.background = sheetBackground
    sheetView.setOnClickListener { toggleBottomSheetState(context) }

    // Create the handle icon
    val handle = View(context)
    val handleParams =
      LinearLayout.LayoutParams(dpToPx(36, context), dpToPx(4, context)).apply {
        topMargin = dpToPx(10, context)
        bottomMargin = dpToPx(6, context)
      }
    handle.layoutParams = handleParams
    val handleDrawable =
      GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(Color.WHITE)
        cornerRadius = dpToPx(2, context).toFloat()
      }
    handle.background = handleDrawable
    sheetView.addView(handle)

    // Add a text view to demonstrate adding custom content
    val textView =
      TextView(context).apply {
        text = "Application-specific UI here"
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
      }
    val textParams =
      LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dpToPx(8, context) }
    sheetView.addView(textView, textParams)

    return sheetView
  }

  private fun getBottomSheetTargetHeight(context: Context): Int {
    return if (isBottomSheetExpanded) {
      val screenHeight = context.resources.displayMetrics.heightPixels
      val isLandscape =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
      if (isLandscape) {
        screenHeight
      } else {
        (screenHeight * 0.50f).toInt()
      }
    } else {
      dpToPx(COLLAPSED_BOTTOM_SHEET_HEIGHT_DP, context)
    }
  }

  private fun createBottomSheetLayoutParams(context: Context): ConstraintLayout.LayoutParams {
    val targetHeight = getBottomSheetTargetHeight(context)
    return ConstraintLayout.LayoutParams(0, targetHeight)
  }

  private fun toggleBottomSheetState(context: Context) {
    val view = bottomSheetView ?: return
    val layout = this.layout ?: return
    isBottomSheetExpanded = !isBottomSheetExpanded

    val isLandscape =
      context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activeGuidance = activeGuidanceUiState
    val prompt = activePrompt
    if (isLandscape && activeGuidance != null) {
      val constraintSet =
        if (prompt != null) {
          buildActiveGuidanceWithPromptWideModeConstraintSet(context, prompt)
        } else {
          buildActiveGuidanceWideModeConstraintSet(context, activeGuidance)
        }
      TransitionManager.beginDelayedTransition(layout)
      constraintSet.applyTo(layout)
    } else {
      val targetHeight = getBottomSheetTargetHeight(context)
      val params = view.layoutParams
      if (params != null) {
        params.height = targetHeight
        view.layoutParams = params
      }
    }
  }

  private fun buildNavigationReadyConstraintSet(uiState: NavigationReadyUiState): ConstraintSet {
    val constraintSet = ConstraintSet()
    constraintSet.clone(layout)

    // Constrain viewport to the edges of its parent
    val viewportId = uiState.viewport.id
    constraintSet.connect(
      viewportId,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.connect(viewportId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    constraintSet.connect(viewportId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    constraintSet.connect(
      viewportId,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )

    // Constrain the logo to the bottom start corner
    val logoId = uiState.googleLogo.id
    constraintSet.connect(
      logoId,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )
    constraintSet.connect(logoId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

    // Constrain end controls container to the bottom end corner
    constraintSet.connect(
      endControlsContainerId,
      ConstraintSet.TOP,
      ConstraintSet.PARENT_ID,
      ConstraintSet.TOP,
    )
    constraintSet.connect(
      endControlsContainerId,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )
    constraintSet.connect(
      endControlsContainerId,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
    )
    constraintSet.constrainHeight(endControlsContainerId, ConstraintSet.MATCH_CONSTRAINT)
    constraintSet.constrainDefaultHeight(
      endControlsContainerId,
      ConstraintSet.MATCH_CONSTRAINT_WRAP,
    )
    constraintSet.setVerticalBias(endControlsContainerId, 1.0f)

    return constraintSet
  }

  override fun onLeaveNavigationReady(
    navigationUiParent: NavigationUiParent,
    oldState: NavigationReadyUiState?,
  ) {
    endControlsContainer?.removeAllViews()
    oldState?.navigationReadyButtons?.forEach { button -> removeFromParentView(button.view) }
    layout?.let {
      it.removeAllViews()
      navigationUiParent.removeNavigationLayout(it)
    }
  }

  override fun onEnterActiveGuidance(
    navigationUiParent: NavigationUiParent,
    oldState: NavigationReadyUiState?,
    newState: ActiveGuidanceUiState?,
  ) {
    activeGuidanceUiState = newState
    isActiveGuidance = true
    val context = navigationUiParent.viewContext
    val layout =
      this.layout ?: ConstraintLayout(context).apply { id = layoutId }.also { this.layout = it }
    layout.removeAllViews()

    if (newState != null) {
      ensureRootLayoutCreated(layout, newState.viewport, newState.googleLogo)
    }

    if (endControlsContainer == null) {
      endControlsContainer =
        AutoHidingVerticalLayout(context).apply {
          id = endControlsContainerId
          childSpacing = dpToPx(StyleValues.buttonVerticalPaddingDp(), context)
          setPadding(0, 0, 0, 0)
        }
    }
    endControlsContainer?.removeAllViews()

    // Add the turn card
    newState?.turnCard?.let { turnCard ->
      removeFromParentView(turnCard)
      val turnCardLayoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
      layout.addView(turnCard, turnCardLayoutParams)
    }

    // Add the custom bottom sheet
    if (bottomSheetView == null) {
      bottomSheetView = createBottomSheetView(context)
    }
    if (bottomSheetView?.parent == null) {
      val bottomSheetLayoutParams = createBottomSheetLayoutParams(context)
      layout.addView(bottomSheetView, bottomSheetLayoutParams)
    }

    // Remove the Navigation Ready UI buttons
    oldState?.navigationReadyButtons?.forEach { button -> removeFromParentView(button.view) }

    // Add the example button to its container
    if (exampleCustomButton == null) {
      exampleCustomButton = createExampleCustomButton(context)
    }
    removeFromParentView(exampleCustomButton)
    val customButtonLayoutParams = createExampleCustomButtonLayoutParams(context)
    endControlsContainer?.addView(exampleCustomButton, customButtonLayoutParams)

    // By adding all buttons to the AutoHidingVerticalLayout, we can easily incorporate the latest
    // set of buttons when upgrading without any code changes required
    newState?.activeGuidanceButtons?.forEach { button ->
      removeFromParentView(button.view)
      val buttonLayoutParams = AutoHidingVerticalLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
      // Mark critical buttons (such as the compass) as high priority so they are the last to be
      // hidden by AutoHidingVerticalLayout when layout space is limited.
      if (button.type == NavigationUiButton.ButtonKnownType.COMPASS) {
        buttonLayoutParams.isHighPriority = true
      }
      endControlsContainer?.addView(button.view, buttonLayoutParams)
    }

    if (endControlsContainer?.parent == null) {
      val endControlsLayoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
      layout.addView(endControlsContainer, endControlsLayoutParams)
    }

    // Build constraint set for Active Guidance state
    if (activeGuidanceConstraintSet == null && newState != null) {
      activeGuidanceConstraintSet = buildActiveGuidanceConstraintSet(context, newState)
    }
    if (activeGuidanceWideModeConstraintSet == null && newState != null) {
      activeGuidanceWideModeConstraintSet =
        buildActiveGuidanceWideModeConstraintSet(context, newState)
    }

    // Apply the constraints
    if (newState != null && newState.isWideMode) {
      activeGuidanceWideModeConstraintSet?.applyTo(layout)
    } else {
      activeGuidanceConstraintSet?.applyTo(layout)
    }

    navigationUiParent.removeNavigationLayout(layout)
    navigationUiParent.setNavigationLayout(layout)
  }

  override fun onLeaveActiveGuidance(
    navigationUiParent: NavigationUiParent,
    oldState: ActiveGuidanceUiState?,
    newState: NavigationReadyUiState?,
  ) {
    isActiveGuidance = false
    // Remove Active Guidance UI elements
    oldState?.turnCard?.let { removeFromParentView(it) }
    removeFromParentView(bottomSheetView)
    removeFromParentView(exampleCustomButton)
    oldState?.activeGuidanceButtons?.forEach { removeFromParentView(it.view) }

    // Add Navigation Ready UI buttons
    newState?.navigationReadyButtons?.forEach { button ->
      if (button.view.parent == null) {
        val buttonLayoutParams = AutoHidingVerticalLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        // Mark critical buttons (such as the compass) as high priority so they are the last to be
        // hidden by AutoHidingVerticalLayout when layout space is limited.
        if (button.type == NavigationUiButton.ButtonKnownType.COMPASS) {
          buttonLayoutParams.isHighPriority = true
        }
        endControlsContainer?.addView(button.view, buttonLayoutParams)
      }
    }

    if (navigationReadyConstraintSet != null && layout != null) {
      navigationReadyConstraintSet?.applyTo(layout)
    }
  }

  private fun buildActiveGuidanceConstraintSet(
    context: Context,
    uiState: ActiveGuidanceUiState,
  ): ConstraintSet {
    val constraintSet = ConstraintSet()
    constraintSet.clone(layout)

    // Constrain turn card to top start corner
    val turnCardId = uiState.turnCard.id
    constraintSet.connect(turnCardId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    constraintSet.connect(
      turnCardId,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )

    // Constrain viewport to top of bottom sheet
    val viewportId = uiState.viewport.id
    constraintSet.clear(viewportId)
    constraintSet.connect(
      viewportId,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.connect(viewportId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

    // Instead of constraining the viewport's top directly to the bottom of the turn card
    // (which varies in height and would trigger jumpy camera framing updates), we position
    // it relative to the top of the parent layout with a fixed margin representing the
    // estimated turn card height.
    constraintSet.setMargin(
      viewportId,
      ConstraintSet.TOP,
      dpToPx(DEFAULT_TURN_CARD_HEIGHT_DP, context),
    )
    constraintSet.connect(viewportId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    constraintSet.connect(viewportId, ConstraintSet.BOTTOM, bottomSheetViewId, ConstraintSet.TOP)

    // Constrain the logo to the top of the bottom sheet
    val logoId = uiState.googleLogo.id
    constraintSet.clear(logoId, ConstraintSet.BOTTOM)
    constraintSet.connect(logoId, ConstraintSet.BOTTOM, bottomSheetViewId, ConstraintSet.TOP)
    constraintSet.connect(logoId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

    constrainBottomSheet(constraintSet, context)

    // Constrain end controls container to top of bottom sheet
    constrainEndControlsContainer(
      constraintSet,
      context,
      uiState,
      bottomSheetViewId,
      isWideMode = false,
    )

    return constraintSet
  }

  private fun constrainBottomSheet(constraintSet: ConstraintSet, context: Context) {
    constraintSet.connect(
      bottomSheetViewId,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.connect(
      bottomSheetViewId,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
    )
    constraintSet.connect(
      bottomSheetViewId,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )
  }

  private fun constrainEndControlsContainer(
    constraintSet: ConstraintSet,
    context: Context,
    uiState: ActiveGuidanceUiState,
    bottomAnchorId: Int,
    isWideMode: Boolean,
  ) {
    constraintSet.clear(endControlsContainerId)
    if (isWideMode) {
      constraintSet.connect(
        endControlsContainerId,
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP,
        dpToPx(8, context),
      )
    } else {
      constraintSet.connect(
        endControlsContainerId,
        ConstraintSet.TOP,
        uiState.turnCard.id,
        ConstraintSet.BOTTOM,
        dpToPx(8, context),
      )
    }
    val marginPx: Int
    val bottomPaddingPx: Int
    if (bottomAnchorId == ConstraintSet.PARENT_ID) {
      constraintSet.connect(
        endControlsContainerId,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
      )
      marginPx = 0
      bottomPaddingPx = dpToPx(20, context)
    } else {
      constraintSet.connect(
        endControlsContainerId,
        ConstraintSet.BOTTOM,
        bottomAnchorId,
        ConstraintSet.TOP,
      )
      marginPx = dpToPx(BUTTONS_CONTAINER_BOTTOM_MARGIN_DP, context)
      bottomPaddingPx = 0
    }
    constraintSet.setMargin(endControlsContainerId, ConstraintSet.BOTTOM, marginPx)

    endControlsContainer?.setPadding(
      endControlsContainer!!.paddingLeft,
      endControlsContainer!!.paddingTop,
      endControlsContainer!!.paddingRight,
      bottomPaddingPx,
    )
    constraintSet.connect(
      endControlsContainerId,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
    )
    constraintSet.constrainHeight(endControlsContainerId, ConstraintSet.MATCH_CONSTRAINT)
    constraintSet.constrainDefaultHeight(
      endControlsContainerId,
      ConstraintSet.MATCH_CONSTRAINT_WRAP,
    )
    constraintSet.setVerticalBias(endControlsContainerId, 1.0f)
  }

  override fun onShowPrompt(navigationUiParent: NavigationUiParent, newPrompt: View) {
    activePrompt = newPrompt
    val context = navigationUiParent.viewContext
    // By default, prompts in non-wide mode are positioned at the bottom of the screen with
    // square bottom corners. When positioning the prompt above a custom bottom sheet, applying
    // rounded bottom corners gives the prompt a clean, floating appearance that matches its top
    // corners.
    applyPromptRoundedBottomCorners(context, newPrompt)
    layout?.addView(newPrompt)

    // When a prompt is displayed above the bottom sheet, we update our active constraints so
    // that the invisible Viewport sits entirely above the prompt. This automatically forces the
    // Nav SDK camera to adjust its zoom and framing so that the route chevron is always visible to
    // the driver.
    activeGuidanceWithPromptConstraintSet =
      if (activeGuidanceUiState?.isWideMode == true) {
        buildActiveGuidanceWithPromptWideModeConstraintSet(context, newPrompt)
      } else {
        buildActiveGuidanceWithPromptConstraintSet(context, newPrompt)
      }
    activeGuidanceWithPromptConstraintSet?.applyTo(layout)
  }

  override fun onChangePrompt(
    navigationUiParent: NavigationUiParent,
    oldPrompt: View,
    newPrompt: View,
  ) {
    activePrompt = newPrompt
    val context = navigationUiParent.viewContext
    applyPromptRoundedBottomCorners(context, newPrompt)

    activeGuidanceWithPromptConstraintSet?.clear(oldPrompt.id)
    layout?.removeView(oldPrompt)
    layout?.addView(newPrompt)

    // When a prompt is displayed above the bottom sheet, we update our active constraints so
    // that the invisible Viewport sits entirely above the prompt. This automatically forces the
    // Nav SDK camera to adjust its zoom and framing so that the route chevron is always visible to
    // the driver.
    activeGuidanceWithPromptConstraintSet =
      if (activeGuidanceUiState?.isWideMode == true) {
        buildActiveGuidanceWithPromptWideModeConstraintSet(context, newPrompt)
      } else {
        buildActiveGuidanceWithPromptConstraintSet(context, newPrompt)
      }
    activeGuidanceWithPromptConstraintSet?.applyTo(layout)
  }

  override fun onHidePrompt(navigationUiParent: NavigationUiParent, oldPrompt: View) {
    activePrompt = null
    activeGuidanceWithPromptConstraintSet?.clear(oldPrompt.id)
    layout?.removeView(oldPrompt)
    if (activeGuidanceUiState?.isWideMode == true) {
      activeGuidanceWideModeConstraintSet?.applyTo(layout)
    } else {
      activeGuidanceConstraintSet?.applyTo(layout)
    }
  }

  private fun buildActiveGuidanceWithPromptConstraintSet(
    context: Context,
    prompt: View,
  ): ConstraintSet {
    val constraintSet = ConstraintSet()
    constraintSet.clone(layout)

    val activeGuidance = activeGuidanceUiState ?: return constraintSet
    val viewportId = activeGuidance.viewport.id
    val logoId = activeGuidance.googleLogo.id
    val promptId = prompt.id

    // Constrain viewport to top of prompt
    constraintSet.clear(viewportId)
    constraintSet.connect(
      viewportId,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.connect(viewportId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    constraintSet.setMargin(
      viewportId,
      ConstraintSet.TOP,
      dpToPx(DEFAULT_TURN_CARD_HEIGHT_DP, context),
    )
    constraintSet.connect(viewportId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    constraintSet.connect(viewportId, ConstraintSet.BOTTOM, promptId, ConstraintSet.TOP)

    // Constrain logo to top of prompt
    constraintSet.clear(logoId, ConstraintSet.BOTTOM)
    constraintSet.connect(logoId, ConstraintSet.BOTTOM, promptId, ConstraintSet.TOP)
    constraintSet.connect(logoId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

    constrainBottomSheet(constraintSet, context)

    // Constrain prompt above bottom sheet
    constraintSet.connect(
      promptId,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.connect(promptId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    constraintSet.connect(promptId, ConstraintSet.BOTTOM, bottomSheetViewId, ConstraintSet.TOP)

    // When the prompt is positioned above the bottom sheet rather than at the bottom of the screen,
    // horizontal padding is added in non-wide mode to visually match standard Navigation SDK
    // elements
    // such as the turn card. In wide mode, the SDK automatically applies horizontal padding to the
    // prompt, so this margin is only applied when in non-wide mode.
    // We can use StyleValues.headerFooterHorizontalPaddingDp() to visually align with the turn
    // card.
    if (!activeGuidance.isWideMode) {
      val promptSideMarginPx = dpToPx(StyleValues.headerFooterHorizontalPaddingDp(), context)
      constraintSet.setMargin(promptId, ConstraintSet.START, promptSideMarginPx)
      constraintSet.setMargin(promptId, ConstraintSet.END, promptSideMarginPx)
    }

    // Reposition end controls container above prompt
    constrainEndControlsContainer(
      constraintSet,
      context,
      activeGuidance,
      promptId,
      isWideMode = false,
    )

    return constraintSet
  }

  private fun removeFromParentView(view: View?) {
    (view?.parent as? ViewGroup)?.removeView(view)
  }

  private fun buildActiveGuidanceWideModeConstraintSet(
    context: Context,
    uiState: ActiveGuidanceUiState,
  ): ConstraintSet {
    val constraintSet = ConstraintSet()
    constraintSet.clone(layout)

    constraintSet.create(halfGuidelineId, ConstraintSet.VERTICAL_GUIDELINE)
    constraintSet.setGuidelinePercent(halfGuidelineId, 0.5f)

    constraintSet.clear(uiState.viewport.id)
    constraintSet.connect(
      uiState.viewport.id,
      ConstraintSet.START,
      halfGuidelineId,
      ConstraintSet.START,
    )
    constraintSet.connect(
      uiState.viewport.id,
      ConstraintSet.TOP,
      ConstraintSet.PARENT_ID,
      ConstraintSet.TOP,
    )
    constraintSet.connect(
      uiState.viewport.id,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
    )
    constraintSet.connect(
      uiState.viewport.id,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )

    val sideMarginPx = dpToPx(StyleValues.headerFooterHorizontalPaddingDp(), context)

    constraintSet.connect(
      uiState.turnCard.id,
      ConstraintSet.TOP,
      ConstraintSet.PARENT_ID,
      ConstraintSet.TOP,
    )
    constraintSet.connect(
      uiState.turnCard.id,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.setMargin(uiState.turnCard.id, ConstraintSet.START, sideMarginPx)
    constraintSet.clear(uiState.turnCard.id, ConstraintSet.END)
    constraintSet.constrainWidth(uiState.turnCard.id, ConstraintSet.WRAP_CONTENT)

    val turnCardId = uiState.turnCard.id
    constraintSet.connect(bottomSheetViewId, ConstraintSet.START, turnCardId, ConstraintSet.START)
    constraintSet.connect(bottomSheetViewId, ConstraintSet.END, turnCardId, ConstraintSet.END)
    constraintSet.connect(
      bottomSheetViewId,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )

    if (isBottomSheetExpanded) {
      // Exactly overlap the turn card
      constraintSet.connect(bottomSheetViewId, ConstraintSet.TOP, turnCardId, ConstraintSet.TOP)
      constraintSet.constrainHeight(bottomSheetViewId, ConstraintSet.MATCH_CONSTRAINT)
    } else {
      constraintSet.clear(bottomSheetViewId, ConstraintSet.TOP)
      constraintSet.constrainHeight(
        bottomSheetViewId,
        dpToPx(COLLAPSED_BOTTOM_SHEET_HEIGHT_DP, context),
      )
    }

    constraintSet.clear(uiState.googleLogo.id, ConstraintSet.BOTTOM)
    constraintSet.connect(
      uiState.googleLogo.id,
      ConstraintSet.BOTTOM,
      bottomSheetViewId,
      ConstraintSet.TOP,
    )
    constraintSet.connect(
      uiState.googleLogo.id,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.connect(
      uiState.googleLogo.id,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
    )
    constraintSet.constrainWidth(uiState.googleLogo.id, ConstraintSet.WRAP_CONTENT)
    constraintSet.setHorizontalBias(uiState.googleLogo.id, 0f)

    constrainEndControlsContainer(
      constraintSet,
      context,
      uiState,
      ConstraintSet.PARENT_ID,
      isWideMode = true,
    )

    return constraintSet
  }

  private fun buildActiveGuidanceWithPromptWideModeConstraintSet(
    context: Context,
    prompt: View,
  ): ConstraintSet {
    val constraintSet = ConstraintSet()
    constraintSet.clone(layout)

    constraintSet.create(halfGuidelineId, ConstraintSet.VERTICAL_GUIDELINE)
    constraintSet.setGuidelinePercent(halfGuidelineId, 0.5f)

    val activeGuidance = activeGuidanceUiState ?: return constraintSet

    constraintSet.clear(activeGuidance.viewport.id)
    constraintSet.connect(
      activeGuidance.viewport.id,
      ConstraintSet.START,
      halfGuidelineId,
      ConstraintSet.START,
    )
    constraintSet.connect(
      activeGuidance.viewport.id,
      ConstraintSet.TOP,
      ConstraintSet.PARENT_ID,
      ConstraintSet.TOP,
    )
    constraintSet.connect(
      activeGuidance.viewport.id,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
    )
    constraintSet.connect(
      activeGuidance.viewport.id,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )

    val sideMarginPx = dpToPx(StyleValues.headerFooterHorizontalPaddingDp(), context)

    constraintSet.connect(
      activeGuidance.turnCard.id,
      ConstraintSet.TOP,
      ConstraintSet.PARENT_ID,
      ConstraintSet.TOP,
    )
    constraintSet.connect(
      activeGuidance.turnCard.id,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.setMargin(activeGuidance.turnCard.id, ConstraintSet.START, sideMarginPx)
    constraintSet.clear(activeGuidance.turnCard.id, ConstraintSet.END)
    constraintSet.constrainWidth(activeGuidance.turnCard.id, ConstraintSet.WRAP_CONTENT)

    val turnCardId = activeGuidance.turnCard.id
    constraintSet.connect(bottomSheetViewId, ConstraintSet.START, turnCardId, ConstraintSet.START)
    constraintSet.connect(bottomSheetViewId, ConstraintSet.END, turnCardId, ConstraintSet.END)
    constraintSet.connect(
      bottomSheetViewId,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )

    if (isBottomSheetExpanded) {
      constraintSet.connect(bottomSheetViewId, ConstraintSet.TOP, turnCardId, ConstraintSet.TOP)
      constraintSet.constrainHeight(bottomSheetViewId, ConstraintSet.MATCH_CONSTRAINT)
    } else {
      constraintSet.clear(bottomSheetViewId, ConstraintSet.TOP)
      constraintSet.constrainHeight(
        bottomSheetViewId,
        dpToPx(COLLAPSED_BOTTOM_SHEET_HEIGHT_DP, context),
      )
    }

    constraintSet.connect(prompt.id, ConstraintSet.START, turnCardId, ConstraintSet.START)
    constraintSet.connect(prompt.id, ConstraintSet.END, turnCardId, ConstraintSet.END)
    constraintSet.constrainWidth(prompt.id, ConstraintSet.MATCH_CONSTRAINT)
    if (isBottomSheetExpanded) {
      constraintSet.connect(
        prompt.id,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
      )
    } else {
      constraintSet.connect(prompt.id, ConstraintSet.BOTTOM, bottomSheetViewId, ConstraintSet.TOP)
    }

    constraintSet.clear(activeGuidance.googleLogo.id, ConstraintSet.BOTTOM)
    constraintSet.connect(
      activeGuidance.googleLogo.id,
      ConstraintSet.BOTTOM,
      bottomSheetViewId,
      ConstraintSet.TOP,
    )
    constraintSet.connect(
      activeGuidance.googleLogo.id,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    constraintSet.connect(
      activeGuidance.googleLogo.id,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
    )
    constraintSet.constrainWidth(activeGuidance.googleLogo.id, ConstraintSet.WRAP_CONTENT)
    constraintSet.setHorizontalBias(activeGuidance.googleLogo.id, 0f)

    constrainEndControlsContainer(
      constraintSet,
      context,
      activeGuidance,
      ConstraintSet.PARENT_ID,
      isWideMode = true,
    )

    return constraintSet
  }

  override fun onSizeChanged(parent: NavigationUiParent, state: UiState) {
    val layout = layout ?: return
    if (state is ActiveGuidanceUiState) {
      activeGuidanceUiState = state
    }
    if (isActiveGuidance) {
      val prompt = activePrompt
      if (prompt != null) {
        activeGuidanceWithPromptConstraintSet =
          if (state.isWideMode) {
            buildActiveGuidanceWithPromptWideModeConstraintSet(parent.viewContext, prompt)
          } else {
            buildActiveGuidanceWithPromptConstraintSet(parent.viewContext, prompt)
          }
        activeGuidanceWithPromptConstraintSet?.applyTo(layout)
      } else {
        if (state.isWideMode) {
          val activeGuidance = activeGuidanceUiState
          if (activeGuidance != null) {
            val constraintSet =
              buildActiveGuidanceWideModeConstraintSet(parent.viewContext, activeGuidance)
            constraintSet.applyTo(layout)
          }
        } else {
          activeGuidanceConstraintSet?.applyTo(layout)
        }
      }
    } else {
      navigationReadyConstraintSet?.applyTo(layout)
    }
  }

  companion object {
    private const val DEFAULT_TURN_CARD_HEIGHT_DP = 150
    private const val CUSTOM_BUTTON_SIZE_DP = 56
    private const val BUTTONS_CONTAINER_BOTTOM_MARGIN_DP = 8
    private const val COLLAPSED_BOTTOM_SHEET_HEIGHT_DP = 110
    private val CUSTOM_UI_ELEMENT_COLOR = "#FFA500".toColorInt()

    private fun dpToPx(dp: Int, context: Context): Int {
      return TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP,
          dp.toFloat(),
          context.resources.displayMetrics,
        )
        .toInt()
    }

    private fun applyPromptRoundedBottomCorners(context: Context, prompt: View?) {
      if (prompt == null) return

      // We apply bottom rounded corners to the prompt view so that it matches the top rounded
      // corners
      // when positioned above the bottom sheet. We use the StyleValues.defaultCornerRadiusDp() API
      // to retrieve the standard default corner radius. To avoid clipping the top shadow or top
      // corners, we apply a custom ViewOutlineProvider that offsets the outline bound at the top.
      val cornerRadius = dpToPx(StyleValues.defaultCornerRadiusDp(), context).toFloat()
      val topOffsetPx = 0
      val sidePaddingPx = 0

      val shape =
        GradientDrawable().apply {
          this.shape = GradientDrawable.RECTANGLE
          setColor(Color.WHITE)
          this.cornerRadius = cornerRadius
        }
      prompt.background = shape

      prompt.outlineProvider =
        object : ViewOutlineProvider() {
          override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(
              sidePaddingPx,
              topOffsetPx,
              view.width - sidePaddingPx,
              view.height,
              cornerRadius,
            )
          }
        }
      prompt.clipToOutline = true
    }
  }
}
