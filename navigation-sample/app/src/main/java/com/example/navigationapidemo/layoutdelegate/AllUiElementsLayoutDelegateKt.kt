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

package com.example.navigationapidemo.layoutdelegate

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.toColorInt
import com.google.android.libraries.navigation.layoutcustomization.ActiveGuidanceUiState
import com.google.android.libraries.navigation.layoutcustomization.AutoHidingVerticalLayout
import com.google.android.libraries.navigation.layoutcustomization.NavigationLayoutDelegate
import com.google.android.libraries.navigation.layoutcustomization.NavigationReadyUiState
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton.ButtonKnownType.COMPASS
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiParent
import com.google.android.libraries.navigation.layoutcustomization.StyleValues
import com.google.android.libraries.navigation.layoutcustomization.UiState
import com.google.android.material.floatingactionbutton.FloatingActionButton

/** Kotlin equivalent of AllUiElementsLayoutDelegate. */
class AllUiElementsLayoutDelegateKt : NavigationLayoutDelegate() {

  private val layoutId = View.generateViewId()
  private val exampleCustomButtonId = View.generateViewId()
  private val buttonsContainerId = View.generateViewId()
  private val startUiControlsContainerId = View.generateViewId()

  private var layout: ConstraintLayout? = null
  private var exampleCustomButton: View? = null
  private var buttonsContainer: AutoHidingVerticalLayout? = null
  private var startUiControlsContainer: AutoHidingVerticalLayout? = null

  private var navigationReadyConstraintSet: ConstraintSet? = null
  private var activeGuidanceConstraintSet: ConstraintSet? = null
  private var activeGuidanceWideModeConstraintSet: ConstraintSet? = null
  private var activeGuidanceWithPromptConstraintSet: ConstraintSet? = null

  private var isActiveGuidance = false
  private var activeGuidanceUiState: ActiveGuidanceUiState? = null

  override fun onEnterNavigationReady(
    navigationUiParent: NavigationUiParent,
    newState: NavigationReadyUiState,
  ) {
    isActiveGuidance = false
    val context = navigationUiParent.viewContext

    var currentLayout = layout
    if (currentLayout == null) {
      currentLayout =
        ConstraintLayout(context).apply {
          layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
          id = layoutId
        }
      layout = currentLayout
    }

    removeFromParentView(newState.viewport)
    currentLayout.addView(
      newState.viewport,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      ),
    )

    removeFromParentView(newState.googleLogo)
    currentLayout.addView(
      newState.googleLogo,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      ),
    )

    var currentButtonsContainer = buttonsContainer
    if (currentButtonsContainer == null) {
      currentButtonsContainer =
        AutoHidingVerticalLayout(context).apply {
          id = buttonsContainerId
          setPadding(0, 0, 0, 0)
        }
      buttonsContainer = currentButtonsContainer
    }

    removeFromParentView(currentButtonsContainer)
    currentLayout.addView(
      currentButtonsContainer,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
      ),
    )

    for (button in newState.navigationReadyButtons) {
      removeFromParentView(button.view)
      currentButtonsContainer.addView(button.view)
    }

    if (navigationReadyConstraintSet == null) {
      navigationReadyConstraintSet = buildNavigationReadyConstraintSet(newState)
    }
    navigationReadyConstraintSet?.applyTo(currentLayout)

    navigationUiParent.removeNavigationLayout(currentLayout)
    navigationUiParent.setNavigationLayout(currentLayout)
  }

  private fun buildNavigationReadyConstraintSet(uiState: NavigationReadyUiState): ConstraintSet {
    return ConstraintSet().apply {
      clone(layout)

      connect(
        uiState.viewport.id,
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START,
      )
      connect(uiState.viewport.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
      connect(uiState.viewport.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
      connect(
        uiState.viewport.id,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
      )

      connect(
        uiState.googleLogo.id,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
      )
      connect(
        uiState.googleLogo.id,
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START,
      )

      constrainButtonsToBottomEnd()
    }
  }

  override fun onLeaveNavigationReady(
    navigationUiParent: NavigationUiParent,
    oldState: NavigationReadyUiState,
  ) {
    buttonsContainer?.removeAllViews()
    layout?.removeAllViews()
    layout?.let { navigationUiParent.removeNavigationLayout(it) }
  }

  override fun onEnterActiveGuidance(
    navigationUiParent: NavigationUiParent,
    oldState: NavigationReadyUiState,
    newState: ActiveGuidanceUiState,
  ) {
    isActiveGuidance = true
    activeGuidanceUiState = newState
    val context = navigationUiParent.viewContext
    val currentLayout = checkNotNull(layout) { "layout must be initialized" }
    val currentButtonsContainer =
      checkNotNull(buttonsContainer) { "buttonsContainer must be initialized" }

    removeFromParentView(newState.turnCard)
    currentLayout.addView(
      newState.turnCard,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      ),
    )

    removeFromParentView(newState.etaCard)
    currentLayout.addView(
      newState.etaCard,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      ),
    )

    for (button in oldState.navigationReadyButtons) {
      removeFromParentView(button.view)
    }

    var currentCustomButton = exampleCustomButton
    if (currentCustomButton == null) {
      currentCustomButton = createExampleCustomButton(context)
      exampleCustomButton = currentCustomButton
    }
    removeFromParentView(currentCustomButton)
    currentButtonsContainer.addView(
      currentCustomButton,
      createExampleCustomButtonLayoutParams(context),
    )

    for (button in newState.activeGuidanceButtons) {
      val buttonLayoutParams =
        AutoHidingVerticalLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT,
        )
      if (button.type == COMPASS) {
        buttonLayoutParams.isHighPriority = true
      }
      removeFromParentView(button.view)
      currentButtonsContainer.addView(button.view, buttonLayoutParams)
    }

    var currentStartContainer = startUiControlsContainer
    if (currentStartContainer == null) {
      currentStartContainer =
        AutoHidingVerticalLayout(context).apply {
          id = startUiControlsContainerId
          setPadding(0, 0, 0, 0)
          setChildSpacing(dpToPx(8, context))
          gravity = Gravity.BOTTOM or Gravity.START
        }
      startUiControlsContainer = currentStartContainer
    }
    removeFromParentView(currentStartContainer)
    currentLayout.addView(
      currentStartContainer,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
      ),
    )

    removeFromParentView(newState.tripProgressBar)
    currentStartContainer.addView(
      newState.tripProgressBar,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      ),
    )

    removeFromParentView(newState.speedWidget)
    currentStartContainer.addView(
      newState.speedWidget,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      ),
    )

    if (activeGuidanceConstraintSet == null) {
      activeGuidanceConstraintSet = buildActiveGuidanceConstraintSet(context, newState)
    }
    if (activeGuidanceWideModeConstraintSet == null) {
      activeGuidanceWideModeConstraintSet =
        buildActiveGuidanceWideModeConstraintSet(context, newState)
    }

    if (newState.isWideMode) {
      activeGuidanceWideModeConstraintSet?.applyTo(currentLayout)
    } else {
      activeGuidanceConstraintSet?.applyTo(currentLayout)
    }
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
    val horizontalMarginPx = dpToPx(StyleValues.buttonHorizontalPaddingDp(), context)
    val verticalMarginPx = dpToPx(StyleValues.buttonVerticalPaddingDp(), context)
    layoutParams.setMargins(horizontalMarginPx, verticalMarginPx, horizontalMarginPx, 0)
    return layoutParams
  }

  override fun onLeaveActiveGuidance(
    navigationUiParent: NavigationUiParent,
    oldState: ActiveGuidanceUiState,
    newState: NavigationReadyUiState,
  ) {
    isActiveGuidance = false

    removeFromParentView(oldState.speedWidget)
    removeFromParentView(oldState.tripProgressBar)
    removeFromParentView(oldState.etaCard)
    removeFromParentView(oldState.turnCard)
    removeFromParentView(startUiControlsContainer)
    buttonsContainer?.removeAllViews()
    startUiControlsContainer?.removeAllViews()

    val currentButtonsContainer =
      checkNotNull(buttonsContainer) { "buttonsContainer must be initialized" }
    for (button in newState.navigationReadyButtons) {
      currentButtonsContainer.addView(
        button.view,
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT,
        ),
      )
    }

    navigationReadyConstraintSet?.applyTo(layout)
  }

  private fun buildActiveGuidanceConstraintSet(
    context: Context,
    uiState: ActiveGuidanceUiState,
  ): ConstraintSet {
    return ConstraintSet().apply {
      clone(layout)

      connect(uiState.turnCard.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
      connect(
        uiState.turnCard.id,
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START,
      )

      clear(uiState.viewport.id)
      connect(
        uiState.viewport.id,
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START,
      )
      connect(uiState.viewport.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
      setMargin(
        uiState.viewport.id,
        ConstraintSet.TOP,
        dpToPx(DEFAULT_TURN_CARD_HEIGHT_DP, context),
      )
      connect(uiState.viewport.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
      connect(uiState.viewport.id, ConstraintSet.BOTTOM, uiState.etaCard.id, ConstraintSet.TOP)

      clear(uiState.googleLogo.id, ConstraintSet.BOTTOM)
      constrainLogoToTopOfEtaCard(uiState)
      constrainEtaCardToBottomStart(uiState)
      constrainButtonsToTopOfEtaCard(context, uiState)
      constrainStartUiControlsContainerAboveGoogleLogo(context, uiState)
    }
  }

  private fun buildActiveGuidanceWideModeConstraintSet(
    context: Context,
    uiState: ActiveGuidanceUiState,
  ): ConstraintSet {
    return ConstraintSet().apply {
      clone(layout)

      val halfGuidelineId = View.generateViewId()
      createHalfwayVerticalGuideline(halfGuidelineId)

      clear(uiState.viewport.id)
      constrainViewportToEndHalf(uiState, halfGuidelineId)

      connect(uiState.turnCard.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
      connect(
        uiState.turnCard.id,
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START,
      )

      clear(uiState.googleLogo.id, ConstraintSet.BOTTOM)
      constrainLogoToTopOfEtaCard(uiState)
      constrainEtaCardToBottomStart(uiState)
      constrainButtonsToBottomEnd()
      constrainStartUiControlsContainerAboveGoogleLogo(context, uiState)
    }
  }

  private fun ConstraintSet.createHalfwayVerticalGuideline(halfGuidelineId: Int) {
    create(halfGuidelineId, ConstraintSet.VERTICAL_GUIDELINE)
    setGuidelinePercent(halfGuidelineId, 0.5f)
  }

  private fun ConstraintSet.constrainEtaCardToBottomStart(uiState: ActiveGuidanceUiState) {
    connect(uiState.etaCard.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
    connect(uiState.etaCard.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
  }

  private fun ConstraintSet.constrainLogoToTopOfEtaCard(uiState: ActiveGuidanceUiState) {
    connect(uiState.googleLogo.id, ConstraintSet.BOTTOM, uiState.etaCard.id, ConstraintSet.TOP)
  }

  private fun ConstraintSet.constrainButtonsToBottomEnd() {
    clear(buttonsContainerId, ConstraintSet.BOTTOM)
    clear(buttonsContainerId, ConstraintSet.TOP)
    connect(buttonsContainerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    connect(buttonsContainerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
    connect(buttonsContainerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
  }

  private fun ConstraintSet.constrainButtonsToTopOfEtaCard(
    context: Context,
    uiState: ActiveGuidanceUiState,
  ) {
    clear(buttonsContainerId, ConstraintSet.BOTTOM)
    clear(buttonsContainerId, ConstraintSet.TOP)
    connect(buttonsContainerId, ConstraintSet.BOTTOM, uiState.etaCard.id, ConstraintSet.TOP)
    connect(buttonsContainerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    connect(buttonsContainerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    setMargin(buttonsContainerId, ConstraintSet.TOP, dpToPx(DEFAULT_TURN_CARD_HEIGHT_DP, context))
  }

  private fun ConstraintSet.constrainStartUiControlsContainerAboveGoogleLogo(
    context: Context,
    uiState: ActiveGuidanceUiState,
  ) {
    connect(
      startUiControlsContainerId,
      ConstraintSet.BOTTOM,
      uiState.googleLogo.id,
      ConstraintSet.TOP,
    )
    connect(
      startUiControlsContainerId,
      ConstraintSet.TOP,
      ConstraintSet.PARENT_ID,
      ConstraintSet.TOP,
    )
    connect(
      startUiControlsContainerId,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
    )
    setMargin(
      startUiControlsContainerId,
      ConstraintSet.TOP,
      dpToPx(DEFAULT_TURN_CARD_HEIGHT_DP, context),
    )
  }

  override fun onShowPrompt(navigationUiParent: NavigationUiParent, newPrompt: View) {
    val context = navigationUiParent.viewContext
    layout?.addView(newPrompt)

    val state = activeGuidanceUiState
    if (state != null) {
      activeGuidanceWithPromptConstraintSet =
        if (state.isWideMode) {
          buildActiveGuidanceWithPromptWideModeConstraintSet(newPrompt)
        } else {
          buildActiveGuidanceWithPromptConstraintSet(context, newPrompt)
        }
      activeGuidanceWithPromptConstraintSet?.applyTo(layout)
    }
  }

  override fun onChangePrompt(
    navigationUiParent: NavigationUiParent,
    oldPrompt: View,
    newPrompt: View,
  ) {
    val context = navigationUiParent.viewContext
    activeGuidanceWithPromptConstraintSet?.clear(oldPrompt.id)

    val currentLayout = checkNotNull(layout) { "layout must be initialized" }
    currentLayout.removeView(oldPrompt)
    currentLayout.addView(newPrompt)

    val state = activeGuidanceUiState
    if (state != null) {
      activeGuidanceWithPromptConstraintSet =
        if (state.isWideMode) {
          buildActiveGuidanceWithPromptWideModeConstraintSet(newPrompt)
        } else {
          buildActiveGuidanceWithPromptConstraintSet(context, newPrompt)
        }
      activeGuidanceWithPromptConstraintSet?.applyTo(currentLayout)
    }
  }

  override fun onHidePrompt(navigationUiParent: NavigationUiParent, oldPrompt: View) {
    activeGuidanceWithPromptConstraintSet?.clear(oldPrompt.id)
    layout?.removeView(oldPrompt)

    val state = activeGuidanceUiState
    if (state != null && state.isWideMode) {
      activeGuidanceWideModeConstraintSet?.applyTo(layout)
    } else {
      activeGuidanceConstraintSet?.applyTo(layout)
    }
  }

  private fun buildActiveGuidanceWithPromptConstraintSet(
    context: Context,
    prompt: View,
  ): ConstraintSet {
    return ConstraintSet().apply {
      clone(layout)

      val state =
        checkNotNull(activeGuidanceUiState) { "activeGuidanceUiState must be initialized" }

      clear(state.viewport.id)
      connect(state.viewport.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
      connect(state.viewport.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
      setMargin(state.viewport.id, ConstraintSet.TOP, dpToPx(DEFAULT_TURN_CARD_HEIGHT_DP, context))
      connect(state.viewport.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
      connect(state.viewport.id, ConstraintSet.BOTTOM, prompt.id, ConstraintSet.TOP)

      clear(state.googleLogo.id, ConstraintSet.BOTTOM)
      connect(state.googleLogo.id, ConstraintSet.BOTTOM, prompt.id, ConstraintSet.TOP)

      connect(prompt.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
      connect(prompt.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
    }
  }

  private fun buildActiveGuidanceWithPromptWideModeConstraintSet(prompt: View): ConstraintSet {
    return ConstraintSet().apply {
      clone(layout)

      val halfGuidelineId = View.generateViewId()
      createHalfwayVerticalGuideline(halfGuidelineId)

      val state =
        checkNotNull(activeGuidanceUiState) { "activeGuidanceUiState must be initialized" }

      clear(state.viewport.id)
      connect(state.viewport.id, ConstraintSet.START, halfGuidelineId, ConstraintSet.START)
      connect(state.viewport.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
      connect(state.viewport.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
      connect(
        state.viewport.id,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
      )

      connect(prompt.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
      connect(prompt.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
    }
  }

  private fun ConstraintSet.constrainViewportToEndHalf(uiState: UiState, halfGuidelineId: Int) {
    connect(uiState.viewport.id, ConstraintSet.START, halfGuidelineId, ConstraintSet.START)
    connect(uiState.viewport.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    connect(uiState.viewport.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    connect(
      uiState.viewport.id,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
    )
  }

  override fun onSizeChanged(navigationUiParent: NavigationUiParent, state: UiState) {
    if (isActiveGuidance) {
      if (state.isWideMode) {
        activeGuidanceWideModeConstraintSet?.applyTo(layout)
      } else {
        activeGuidanceConstraintSet?.applyTo(layout)
      }
    } else {
      navigationReadyConstraintSet?.applyTo(layout)
    }
  }

  private fun removeFromParentView(view: View?) {
    if (view?.parent != null) {
      (view.parent as ViewGroup).removeView(view)
    }
  }

  private fun dpToPx(dp: Int, context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        context.resources.displayMetrics,
      )
      .toInt()
  }

  companion object {
    private const val DEFAULT_TURN_CARD_HEIGHT_DP = 150
    private const val CUSTOM_BUTTON_SIZE_DP = 56
    private val CUSTOM_UI_ELEMENT_COLOR = "#FFA500".toColorInt()
  }
}
