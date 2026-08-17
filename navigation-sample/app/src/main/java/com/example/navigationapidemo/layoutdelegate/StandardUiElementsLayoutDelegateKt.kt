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
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.libraries.navigation.layoutcustomization.ActiveGuidanceUiState
import com.google.android.libraries.navigation.layoutcustomization.AutoHidingVerticalLayout
import com.google.android.libraries.navigation.layoutcustomization.NavigationLayoutDelegate
import com.google.android.libraries.navigation.layoutcustomization.NavigationReadyUiState
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton.ButtonKnownType.COMPASS
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiParent
import com.google.android.libraries.navigation.layoutcustomization.StyleValues.headerNominalHeightDp

/** Kotlin equivalent of StandardUiElementsLayoutDelegate. */
class StandardUiElementsLayoutDelegateKt : NavigationLayoutDelegate() {

  private val layoutId = View.generateViewId()
  private val buttonsContainerId = View.generateViewId()

  private var layout: ConstraintLayout? = null
  private var buttonsContainer: AutoHidingVerticalLayout? = null

  private var navigationReadyConstraintSet: ConstraintSet? = null
  private var activeGuidanceConstraintSet: ConstraintSet? = null
  private var activeGuidanceWithPromptConstraintSet: ConstraintSet? = null

  private var activeGuidanceUiState: ActiveGuidanceUiState? = null

  override fun onEnterNavigationReady(
    navigationUiParent: NavigationUiParent,
    newState: NavigationReadyUiState,
  ) {
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
      currentButtonsContainer = AutoHidingVerticalLayout(context).apply { id = buttonsContainerId }
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

    if (activeGuidanceConstraintSet == null) {
      activeGuidanceConstraintSet = buildActiveGuidanceConstraintSet(context, newState)
    }

    activeGuidanceConstraintSet?.applyTo(currentLayout)
  }

  override fun onLeaveActiveGuidance(
    navigationUiParent: NavigationUiParent,
    oldState: ActiveGuidanceUiState,
    newState: NavigationReadyUiState,
  ) {
    removeFromParentView(oldState.etaCard)
    removeFromParentView(oldState.turnCard)
    buttonsContainer?.removeAllViews()

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
      setMargin(uiState.viewport.id, ConstraintSet.TOP, dpToPx(headerNominalHeightDp(), context))
      connect(uiState.viewport.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
      connect(uiState.viewport.id, ConstraintSet.BOTTOM, uiState.etaCard.id, ConstraintSet.TOP)

      clear(uiState.googleLogo.id, ConstraintSet.BOTTOM)
      constrainLogoToTopOfEtaCard(uiState)
      constrainEtaCardToBottomStart(uiState)
      constrainButtonsToTopOfEtaCard(context, uiState)
    }
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
    setMargin(buttonsContainerId, ConstraintSet.TOP, dpToPx(headerNominalHeightDp(), context))
  }

  override fun onShowPrompt(navigationUiParent: NavigationUiParent, newPrompt: View) {
    val context = navigationUiParent.viewContext
    layout?.addView(newPrompt)

    if (activeGuidanceUiState != null) {
      activeGuidanceWithPromptConstraintSet =
        buildActiveGuidanceWithPromptConstraintSet(context, newPrompt)
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

    if (activeGuidanceUiState != null) {
      activeGuidanceWithPromptConstraintSet =
        buildActiveGuidanceWithPromptConstraintSet(context, newPrompt)
      activeGuidanceWithPromptConstraintSet?.applyTo(currentLayout)
    }
  }

  override fun onHidePrompt(navigationUiParent: NavigationUiParent, oldPrompt: View) {
    activeGuidanceWithPromptConstraintSet?.clear(oldPrompt.id)
    layout?.removeView(oldPrompt)

    activeGuidanceConstraintSet?.applyTo(layout)
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
      setMargin(state.viewport.id, ConstraintSet.TOP, dpToPx(headerNominalHeightDp(), context))
      connect(state.viewport.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
      connect(state.viewport.id, ConstraintSet.BOTTOM, prompt.id, ConstraintSet.TOP)

      clear(state.googleLogo.id, ConstraintSet.BOTTOM)
      connect(state.googleLogo.id, ConstraintSet.BOTTOM, prompt.id, ConstraintSet.TOP)

      connect(prompt.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
      connect(prompt.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
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
}
