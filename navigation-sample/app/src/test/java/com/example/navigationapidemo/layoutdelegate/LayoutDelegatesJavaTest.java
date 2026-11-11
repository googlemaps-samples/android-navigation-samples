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

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.navigationapidemo.layoutdelegate.LayoutDelegatesTest.FakeActiveGuidanceUiState;
import com.example.navigationapidemo.layoutdelegate.LayoutDelegatesTest.FakeNavigationReadyUiState;
import com.example.navigationapidemo.layoutdelegate.LayoutDelegatesTest.FakeNavigationUiParent;
import com.google.android.libraries.navigation.layoutcustomization.AutoHidingVerticalLayout;
import com.google.android.libraries.navigation.layoutcustomization.NavigationLayoutDelegate;
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton;
import com.google.android.libraries.navigation.layoutcustomization.NavigationUiButton.ButtonKnownType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * Headless JVM Robolectric unit tests for the Java {@link NavigationLayoutDelegate} implementations
 * ({@link StandardUiElementsLayoutDelegate}, {@link AllUiElementsLayoutDelegate}, {@link
 * BottomSheetLayoutDelegate}, and {@link BottomSheetLandscapeLayoutDelegate}).
 *
 * <p>Uses <b>Google Truth</b> ({@code assertThat}) to verify Java layout delegate contracts,
 * view hierarchy transitions, and button prioritization.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = {34})
public class LayoutDelegatesJavaTest {

  private Activity activity;

  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(Activity.class).create().get();
  }

  private void assertDelegateStateMachine(NavigationLayoutDelegate delegate, boolean wideMode) {
    FakeNavigationUiParent parent = new FakeNavigationUiParent(activity);
    FakeNavigationReadyUiState readyState = new FakeNavigationReadyUiState(activity, wideMode);
    FakeActiveGuidanceUiState activeState =
        new FakeActiveGuidanceUiState(activity, readyState, wideMode);

    // 1. Navigation Ready
    delegate.onEnterNavigationReady(parent, readyState);
    assertThat(parent.getCurrentLayout()).isInstanceOf(ConstraintLayout.class);
    ConstraintLayout root = (ConstraintLayout) parent.getCurrentLayout();
    assertThat(readyState.getViewport().getParent()).isSameInstanceAs(root);
    assertThat(readyState.getGoogleLogo().getParent()).isSameInstanceAs(root);

    for (NavigationUiButton button : readyState.getNavigationReadyButtons()) {
      assertThat(button.getView().getParent()).isNotNull();
    }

    // 2. Active Guidance
    delegate.onEnterActiveGuidance(parent, readyState, activeState);
    assertThat(activeState.getTurnCard().getParent()).isSameInstanceAs(root);
    for (NavigationUiButton button : readyState.getNavigationReadyButtons()) {
      assertThat(button.getView().getParent()).isNull();
    }
    for (NavigationUiButton button : activeState.getActiveGuidanceButtons()) {
      assertThat(button.getView().getParent()).isNotNull();
      if (button.getType() == ButtonKnownType.COMPASS) {
        ViewGroup.LayoutParams lp = button.getView().getLayoutParams();
        assertThat(lp).isInstanceOf(AutoHidingVerticalLayout.LayoutParams.class);
        assertThat(((AutoHidingVerticalLayout.LayoutParams) lp).isHighPriority).isTrue();
      }
    }

    // 3. Prompt lifecycle
    View prompt1 = new View(activity);
    prompt1.setId(View.generateViewId());
    View prompt2 = new View(activity);
    prompt2.setId(View.generateViewId());

    delegate.onShowPrompt(parent, prompt1);
    assertThat(prompt1.getParent()).isSameInstanceAs(root);

    delegate.onChangePrompt(parent, prompt1, prompt2);
    assertThat(prompt1.getParent()).isNull();
    assertThat(prompt2.getParent()).isSameInstanceAs(root);

    delegate.onHidePrompt(parent, prompt2);
    assertThat(prompt2.getParent()).isNull();

    // 4. Size change & exit Active Guidance
    delegate.onSizeChanged(parent, activeState);
    delegate.onLeaveActiveGuidance(parent, activeState, readyState);
    assertThat(activeState.getTurnCard().getParent()).isNull();

    // 5. Exit Navigation Ready
    delegate.onLeaveNavigationReady(parent, readyState);
    assertThat(parent.getCurrentLayout()).isNull();
  }

  @Test
  public void standardUiElementsLayoutDelegate_transitionsCleanlyAcrossAllStates() {
    assertDelegateStateMachine(new StandardUiElementsLayoutDelegate(), /* wideMode= */ false);
  }

  @Test
  public void allUiElementsLayoutDelegate_transitionsCleanlyInPortraitAndWideMode() {
    assertDelegateStateMachine(new AllUiElementsLayoutDelegate(), /* wideMode= */ false);
    assertDelegateStateMachine(new AllUiElementsLayoutDelegate(), /* wideMode= */ true);
  }

  @Test
  public void bottomSheetLayoutDelegate_transitionsAndExpandsBottomSheetOnClick() {
    BottomSheetLayoutDelegate delegate = new BottomSheetLayoutDelegate();
    FakeNavigationUiParent parent = new FakeNavigationUiParent(activity);
    FakeNavigationReadyUiState readyState = new FakeNavigationReadyUiState(activity, false);
    FakeActiveGuidanceUiState activeState =
        new FakeActiveGuidanceUiState(activity, readyState, false);

    delegate.onEnterNavigationReady(parent, readyState);
    delegate.onEnterActiveGuidance(parent, readyState, activeState);

    ConstraintLayout root = (ConstraintLayout) parent.getCurrentLayout();
    assertThat(root).isNotNull();

    View bottomSheet = null;
    for (int i = 0; i < root.getChildCount(); i++) {
      View child = root.getChildAt(i);
      if (child.isClickable()
          && child instanceof ViewGroup
          && !(child instanceof AutoHidingVerticalLayout)) {
        bottomSheet = child;
        break;
      }
    }
    assertThat(bottomSheet).isNotNull();
    int collapsedHeight = bottomSheet.getLayoutParams().height;
    bottomSheet.performClick();
    assertThat(bottomSheet.getLayoutParams().height).isGreaterThan(collapsedHeight);

    assertDelegateStateMachine(new BottomSheetLayoutDelegate(), /* wideMode= */ false);
  }

  @Test
  public void bottomSheetLandscapeLayoutDelegate_transitionsCleanlyInPortraitAndWideMode() {
    assertDelegateStateMachine(new BottomSheetLandscapeLayoutDelegate(), /* wideMode= */ false);
    assertDelegateStateMachine(new BottomSheetLandscapeLayoutDelegate(), /* wideMode= */ true);
  }
}
