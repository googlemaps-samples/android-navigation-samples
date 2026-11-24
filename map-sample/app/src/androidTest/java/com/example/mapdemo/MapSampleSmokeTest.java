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

package com.example.mapdemo;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.ListView;
import android.widget.ToggleButton;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Connected-device instrumentation smoke tests for {@code map-sample} using <b>Google Truth</b>.
 *
 * <p>Sits near the top of the testing pyramid to verify that:
 *
 * <ol>
 *   <li>Every demo activity listed in {@link DemoDetailsList#DEMOS} is registered in {@code
 *       AndroidManifest.xml} on the target device.
 *   <li>{@link MainActivity} launches cleanly, populates the full catalog adapter, and toggles the
 *       Maps vs. Navigation flavor switch without error.
 * </ol>
 */
@RunWith(AndroidJUnit4.class)
public class MapSampleSmokeTest {

  @Test
  public void manifest_registersAll46DemoActivitiesFromDemoDetailsList() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    PackageInfo packageInfo =
        context
            .getPackageManager()
            .getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);

    Set<String> registeredActivities = new HashSet<>();
    if (packageInfo.activities != null) {
      for (ActivityInfo info : packageInfo.activities) {
        registeredActivities.add(info.name);
      }
    }

    assertThat(DemoDetailsList.DEMOS).hasLength(47);
    for (DemoDetails demo : DemoDetailsList.DEMOS) {
      assertThat(registeredActivities).contains(demo.activityClass.getName());
    }
  }

  @Test
  public void mainActivity_launchesAndTogglesNavigationFlavorSwitch() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            ListView demoListView = activity.findViewById(R.id.demoListView);
            assertThat(demoListView).isNotNull();
            assertThat(demoListView.getAdapter().getCount()).isEqualTo(DemoDetailsList.DEMOS.length);
            ToggleButton flavorToggle = activity.findViewById(R.id.view_source_toggle_btn);
            assertThat(flavorToggle).isNotNull();
            assertThat(flavorToggle.isChecked()).isFalse();

            flavorToggle.performClick();
            assertThat(flavorToggle.isChecked()).isTrue();
          });
    }
  }
}
