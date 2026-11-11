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

package com.example.navigationapidemo.layoutdelegate

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.libraries.navigation.layoutcustomization.ActiveGuidanceUiState
import com.google.android.libraries.navigation.layoutcustomization.AutoHidingVerticalLayout
import com.google.android.libraries.navigation.layoutcustomization.NavigationLayoutDelegate
import com.google.android.libraries.navigation.layoutcustomization.NavigationReadyUiState
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton.ButtonKnownType.COMPASS
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiParent
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.powerassert.assertGeo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Headless JVM Robolectric unit tests for the Kotlin [NavigationLayoutDelegate] implementations
 * ([StandardUiElementsLayoutDelegateKt], [AllUiElementsLayoutDelegateKt],
 * [BottomSheetLayoutDelegateKt], and [BottomSheetLandscapeLayoutDelegateKt]) and their structural
 * parity with the corresponding Java layout delegates.
 *
 * Uses **Kotlin Power-Assert** (`assertGeo`) to verify view hierarchy attachments, high-priority
 * compass button configuration, bottom sheet expand/collapse transitions, and prompt lifecycle.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LayoutDelegatesTest {

  private lateinit var activity: Activity

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
  }

  internal class FakeNavigationUiParent(private val context: Context) : NavigationUiParent {
    val hostContainer = FrameLayout(context)
    var currentLayout: View? = null
      private set

    override fun setNavigationLayout(view: View) {
      if (view.parent != null) {
        (view.parent as ViewGroup).removeView(view)
      }
      hostContainer.addView(view)
      currentLayout = view
    }

    override fun removeNavigationLayout(view: View) {
      hostContainer.removeView(view)
      if (currentLayout === view) {
        currentLayout = null
      }
    }

    override fun getViewContext(): Context = context
  }

  internal class FakeUiButton(private val buttonType: Int, context: Context) : NavigationUiButton {
    private val buttonView = View(context).apply { id = View.generateViewId() }
    override fun getView(): View = buttonView
    override fun getType(): Int = buttonType
  }

  internal class FakeNavigationReadyUiState(
    context: Context,
    private val wideMode: Boolean = false,
  ) : NavigationReadyUiState {
    private val viewportView = View(context).apply { id = View.generateViewId() }
    private val logoView = View(context).apply { id = View.generateViewId() }
    private val buttons =
      listOf(FakeUiButton(COMPASS, context), FakeUiButton(COMPASS + 1, context))

    override fun isWideMode(): Boolean = wideMode
    override fun getGoogleLogo(): View = logoView
    override fun getViewport(): View = viewportView
    override fun getNavigationReadyButtons(): List<NavigationUiButton> = buttons
  }

  internal class FakeActiveGuidanceUiState(
    context: Context,
    readyState: FakeNavigationReadyUiState,
    private val wideMode: Boolean = false,
  ) : ActiveGuidanceUiState {
    private val viewportView = readyState.viewport
    private val logoView = readyState.googleLogo
    private val turnCardView = View(context).apply { id = View.generateViewId() }
    private val etaCardView = View(context).apply { id = View.generateViewId() }
    private val progressBarView = View(context).apply { id = View.generateViewId() }
    private val speedWidgetView = View(context).apply { id = View.generateViewId() }
    private val buttons =
      listOf(FakeUiButton(COMPASS, context), FakeUiButton(COMPASS + 1, context))

    override fun isWideMode(): Boolean = wideMode
    override fun getGoogleLogo(): View = logoView
    override fun getViewport(): View = viewportView
    override fun getTurnCard(): View = turnCardView
    override fun getEtaCard(): View = etaCardView
    override fun getTripProgressBar(): View = progressBarView
    override fun getSpeedWidget(): View = speedWidgetView
    override fun getActiveGuidanceButtons(): List<NavigationUiButton> = buttons
  }

  private fun executeFullStateMachine(
    delegate: NavigationLayoutDelegate,
    wideMode: Boolean = false,
  ): FakeNavigationUiParent {
    val parent = FakeNavigationUiParent(activity)
    val readyState = FakeNavigationReadyUiState(activity, wideMode = wideMode)
    val activeState = FakeActiveGuidanceUiState(activity, readyState, wideMode = wideMode)
    val prompt1 = View(activity).apply { id = View.generateViewId() }
    val prompt2 = View(activity).apply { id = View.generateViewId() }

    // 1. Enter Navigation Ready
    delegate.onEnterNavigationReady(parent, readyState)
    val rootLayout = parent.currentLayout as? ConstraintLayout
    assertGeo(
      rootLayout != null &&
        readyState.viewport.parent === rootLayout &&
        readyState.googleLogo.parent === rootLayout &&
        readyState.navigationReadyButtons.all { it.view.parent != null }
    )

    // 2. Enter Active Guidance
    delegate.onEnterActiveGuidance(parent, readyState, activeState)
    val compassButton = activeState.activeGuidanceButtons.first { it.type == COMPASS }.view
    val compassParams = compassButton.layoutParams as? AutoHidingVerticalLayout.LayoutParams
    assertGeo(
      activeState.turnCard.parent === rootLayout &&
        readyState.navigationReadyButtons.all { it.view.parent == null } &&
        activeState.activeGuidanceButtons.all { it.view.parent != null } &&
        compassParams?.isHighPriority == true
    )

    // 3. Prompt lifecycle: Show -> Change -> Hide
    delegate.onShowPrompt(parent, prompt1)
    assertGeo(prompt1.parent === rootLayout)

    delegate.onChangePrompt(parent, prompt1, prompt2)
    assertGeo(prompt1.parent == null && prompt2.parent === rootLayout)

    delegate.onHidePrompt(parent, prompt2)
    assertGeo(prompt2.parent == null)

    // 4. Size change callback in Active Guidance
    delegate.onSizeChanged(parent, activeState)

    // 5. Leave Active Guidance -> returns to Navigation Ready buttons
    delegate.onLeaveActiveGuidance(parent, activeState, readyState)
    assertGeo(
      activeState.turnCard.parent == null &&
        readyState.navigationReadyButtons.all { it.view.parent != null }
    )

    // 6. Leave Navigation Ready -> detaches layout from NavigationUiParent
    delegate.onLeaveNavigationReady(parent, readyState)
    assertGeo(parent.currentLayout == null && rootLayout?.childCount == 0)

    return parent
  }

  @Test
  fun standardUiElementsLayoutDelegateKt_completesFullLifecycleStateMachine() {
    executeFullStateMachine(StandardUiElementsLayoutDelegateKt(), wideMode = false)
  }

  @Test
  fun allUiElementsLayoutDelegateKt_supportsPortraitAndWideModeAndAttachesSpeedAndProgressWidgets() {
    val delegate = AllUiElementsLayoutDelegateKt()
    val parent = FakeNavigationUiParent(activity)
    val readyState = FakeNavigationReadyUiState(activity, wideMode = false)
    val activePortrait = FakeActiveGuidanceUiState(activity, readyState, wideMode = false)

    delegate.onEnterNavigationReady(parent, readyState)
    delegate.onEnterActiveGuidance(parent, readyState, activePortrait)

    assertGeo(
      activePortrait.etaCard.parent != null &&
        activePortrait.tripProgressBar.parent != null &&
        activePortrait.speedWidget.parent != null
    )

    // Verify wide mode (landscape) transition
    val activeWide = FakeActiveGuidanceUiState(activity, readyState, wideMode = true)
    delegate.onSizeChanged(parent, activeWide)

    delegate.onLeaveActiveGuidance(parent, activePortrait, readyState)
    assertGeo(
      activePortrait.etaCard.parent == null &&
        activePortrait.tripProgressBar.parent == null &&
        activePortrait.speedWidget.parent == null
    )

    // Also run the full state machine starting directly in wideMode = true
    executeFullStateMachine(AllUiElementsLayoutDelegateKt(), wideMode = true)
  }

  @Test
  fun bottomSheetLayoutDelegateKt_togglesBottomSheetHeightAndAppliesRoundedCornersToPrompt() {
    val delegate = BottomSheetLayoutDelegateKt()
    val parent = FakeNavigationUiParent(activity)
    val readyState = FakeNavigationReadyUiState(activity)
    val activeState = FakeActiveGuidanceUiState(activity, readyState)

    delegate.onEnterNavigationReady(parent, readyState)
    delegate.onEnterActiveGuidance(parent, readyState, activeState)

    val rootLayout = checkNotNull(parent.currentLayout as? ConstraintLayout)
    // Locate the custom bottom sheet LinearLayout inside rootLayout
    val bottomSheet =
      (0 until rootLayout.childCount)
        .map { rootLayout.getChildAt(it) }
        .first { it.isClickable && it is ViewGroup && it !is AutoHidingVerticalLayout }

    val collapsedHeight = bottomSheet.layoutParams.height
    val screenHeight = activity.resources.displayMetrics.heightPixels

    // Clicking the bottom sheet expands it to 50% of the screen height
    bottomSheet.performClick()
    val expandedHeight = bottomSheet.layoutParams.height
    assertGeo(
      collapsedHeight > 0 &&
        expandedHeight == (screenHeight * 0.50f).toInt() &&
        expandedHeight > collapsedHeight
    )

    // Clicking again collapses it back to its original 110dp height
    bottomSheet.performClick()
    assertGeo(bottomSheet.layoutParams.height == collapsedHeight)

    // Showing a prompt enables clipToOutline and assigns a custom ViewOutlineProvider
    val prompt = View(activity).apply { id = View.generateViewId() }
    delegate.onShowPrompt(parent, prompt)
    assertGeo(prompt.clipToOutline && prompt.outlineProvider != null && prompt.background != null)
  }

  @Test
  fun bottomSheetLandscapeLayoutDelegateKt_adaptsBottomSheetExpansionAcrossPortraitAndLandscape() {
    // 1. Portrait full state machine
    executeFullStateMachine(BottomSheetLandscapeLayoutDelegateKt(), wideMode = false)

    // 2. Landscape (wide mode) bottom sheet expansion with active prompt
    activity.resources.configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
    val delegate = BottomSheetLandscapeLayoutDelegateKt()
    val parent = FakeNavigationUiParent(activity)
    val readyState = FakeNavigationReadyUiState(activity, wideMode = true)
    val activeState = FakeActiveGuidanceUiState(activity, readyState, wideMode = true)

    delegate.onEnterNavigationReady(parent, readyState)
    delegate.onEnterActiveGuidance(parent, readyState, activeState)

    val rootLayout = checkNotNull(parent.currentLayout as? ConstraintLayout)
    val bottomSheet =
      (0 until rootLayout.childCount)
        .map { rootLayout.getChildAt(it) }
        .first { it.isClickable && it is ViewGroup && it !is AutoHidingVerticalLayout }

    val prompt = View(activity).apply { id = View.generateViewId() }
    delegate.onShowPrompt(parent, prompt)

    // Toggling bottom sheet in landscape with an active prompt rebuilds wide-mode constraints
    bottomSheet.performClick()
    assertGeo(prompt.parent === rootLayout && bottomSheet.parent === rootLayout)

    delegate.onHidePrompt(parent, prompt)
    bottomSheet.performClick()
    assertGeo(prompt.parent == null && bottomSheet.parent === rootLayout)
  }

  @Test
  fun javaAndKotlinLayoutDelegates_produceMatchingChildHierarchyCountsAcrossStates() {
    val pairs =
      listOf(
        StandardUiElementsLayoutDelegate() to StandardUiElementsLayoutDelegateKt(),
        AllUiElementsLayoutDelegate() to AllUiElementsLayoutDelegateKt(),
        BottomSheetLayoutDelegate() to BottomSheetLayoutDelegateKt(),
        BottomSheetLandscapeLayoutDelegate() to BottomSheetLandscapeLayoutDelegateKt(),
      )

    for ((javaDelegate, kotlinDelegate) in pairs) {
      val javaParent = FakeNavigationUiParent(activity)
      val ktParent = FakeNavigationUiParent(activity)
      val javaReady = FakeNavigationReadyUiState(activity)
      val ktReady = FakeNavigationReadyUiState(activity)
      val javaActive = FakeActiveGuidanceUiState(activity, javaReady)
      val ktActive = FakeActiveGuidanceUiState(activity, ktReady)

      javaDelegate.onEnterNavigationReady(javaParent, javaReady)
      kotlinDelegate.onEnterNavigationReady(ktParent, ktReady)

      val javaRoot = checkNotNull(javaParent.currentLayout as? ConstraintLayout)
      val ktRoot = checkNotNull(ktParent.currentLayout as? ConstraintLayout)
      assertThat(ktRoot.childCount).isEqualTo(javaRoot.childCount)

      javaDelegate.onEnterActiveGuidance(javaParent, javaReady, javaActive)
      kotlinDelegate.onEnterActiveGuidance(ktParent, ktReady, ktActive)
      assertGeo(ktRoot.childCount == javaRoot.childCount)
    }
  }
}
