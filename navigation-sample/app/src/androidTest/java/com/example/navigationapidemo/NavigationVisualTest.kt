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

package com.example.navigationapidemo

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.maps.android.testing.visual.BaseVisualTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Top-of-pyramid multimodal AI visual and hybrid golden test for `navigation-sample`.
 *
 * Extends [BaseVisualTest] from `com.google.maps.android:maps-testing-visual` to verify the visual
 * presentation of the launcher chooser screen using `verifyScreenshotWithGoldenFallback`:
 * - When a committed golden exists under `src/androidTest/assets/goldens/`, compares against it
 *   offline with zero API quota usage.
 * - When visual drift exceeds tolerance or when a golden is not yet recorded and `GEMINI_API_KEY`
 *   is configured, falls back to Gemini Vision semantic verification.
 */
@RunWith(AndroidJUnit4::class)
class NavigationVisualTest : BaseVisualTest() {

  @Test
  fun mainActivity_rendersNavigationSampleChooserScreen() = runBlocking {
    ActivityScenario.launch(MainActivity::class.java).use {
      val screenshotBitmap = captureScreenshot("navigation_sample_main_chooser.png")
      verifyScreenshotWithGoldenFallback(
        testName = "navigation_sample_main_chooser",
        screenshotBitmap = screenshotBitmap,
        prompt =
          "Verify that the Google Maps Navigation API Demo launcher screen is visible with " +
            "entries for NavViewActivity, NavFragmentActivity, and SwappingMapAndNavActivity, " +
            "and reply PASSED if all criteria are met.",
        tolerancePercent = 2.0,
      )
    }
  }
}
