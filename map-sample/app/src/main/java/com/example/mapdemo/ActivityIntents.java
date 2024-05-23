/*
 * Copyright 2024 Google LLC
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

/** Utilities related to the Intents that start demo Activities. */
final class ActivityIntents {

  /**
   * Identifier for extra that indicates whether to inflate the NavigationView flavor or the MapView
   * flavor of the corresponding demo.
   */
  public static final String EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO =
      "should_use_navigation_flavor_for_demo";

  private ActivityIntents() {}
}
