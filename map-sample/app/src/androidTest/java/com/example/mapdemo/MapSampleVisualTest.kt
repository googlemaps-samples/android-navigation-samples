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

package com.example.mapdemo

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.maps.android.testing.visual.BaseVisualTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Top-of-pyramid multimodal AI visual and hybrid golden test for `map-sample`.
 *
 * Extends [BaseVisualTest] from `com.google.maps.android:maps-testing-visual` to verify the visual
 * presentation of the `MainActivity` demo catalog and flavor toggle switch via
 * `verifyScreenshotWithGoldenFallback`.
 */
@RunWith(AndroidJUnit4::class)
class MapSampleVisualTest : BaseVisualTest() {

  @Test
  fun mainActivity_rendersMapDemoCatalogWithFlavorToggle() = runBlocking {
    ActivityScenario.launch(MainActivity::class.java).use {
      val screenshotBitmap = captureScreenshot("map_sample_main_catalog.png")
      verifyScreenshotWithGoldenFallback(
        testName = "map_sample_main_catalog",
        screenshotBitmap = screenshotBitmap,
        prompt =
          "Verify that the Map Demo catalog list is displayed with a flavor switch at the top " +
            "and entries such as Basic Map and Camera, and reply PASSED if all criteria are met.",
        tolerancePercent = 2.0,
      )
    }
  }
}
