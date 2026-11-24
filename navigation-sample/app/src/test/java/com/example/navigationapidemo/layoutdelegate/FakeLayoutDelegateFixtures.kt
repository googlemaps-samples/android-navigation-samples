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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.libraries.navigation.layoutcustomization.ActiveGuidanceUiState
import com.google.android.libraries.navigation.layoutcustomization.NavigationReadyUiState
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton.ButtonKnownType.COMPASS
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiParent

/** Shared fake [NavigationUiParent] used by Kotlin and Java layout delegate unit tests. */
class FakeNavigationUiParent(private val context: Context) : NavigationUiParent {
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

/** Shared fake [NavigationUiButton] with a unique view ID and known button type. */
class FakeUiButton(private val buttonType: Int, context: Context) : NavigationUiButton {
  private val buttonView = View(context).apply { id = View.generateViewId() }

  override fun getView(): View = buttonView

  override fun getType(): Int = buttonType
}

/** Shared fake [NavigationReadyUiState] for layout delegate state-machine tests. */
class FakeNavigationReadyUiState(
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

/** Shared fake [ActiveGuidanceUiState] for layout delegate state-machine tests. */
class FakeActiveGuidanceUiState(
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
