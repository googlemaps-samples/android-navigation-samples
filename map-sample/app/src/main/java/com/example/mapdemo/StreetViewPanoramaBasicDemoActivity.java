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

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.ImmutableList;

/** This shows how to create a simple activity with streetview */
public class StreetViewPanoramaBasicDemoActivity extends AppCompatActivity {

  // Pitt St, Sydney
  private static final LatLng SYDNEY = new LatLng(-33.8682624, 151.2083773);

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.street_view_panorama_basic_demo);

    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
        ImmutableList.of(
            EdgeToEdgeMarginConfig.builder().setView(findViewById(R.id.layout_container)).build()));

    SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
        (SupportStreetViewPanoramaFragment)
            getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama);
    streetViewPanoramaFragment.setRetainInstance(true);
    streetViewPanoramaFragment.getStreetViewPanoramaAsync(
        panorama -> {
          // Only set the panorama to SYDNEY on startup (when no panoramas have been
          // loaded which is when the savedInstanceState is null).
          if (savedInstanceState == null) {
            panorama.setPosition(SYDNEY);
          }
        });
  }
}
