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
import android.view.ViewGroup.LayoutParams;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.StreetViewPanoramaView;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.ImmutableList;

/** This shows how to create a simple activity with streetview */
public class StreetViewPanoramaViewDemoActivity extends AppCompatActivity {

  // Pitt St, Sydney
  private static final LatLng SYDNEY = new LatLng(-33.8682624, 151.2083773);

  private StreetViewPanoramaView streetViewPanoramaView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    StreetViewPanoramaOptions options = new StreetViewPanoramaOptions();
    if (savedInstanceState == null) {
      options.position(SYDNEY);
    }

    streetViewPanoramaView = new StreetViewPanoramaView(this, options);
    addContentView(
        streetViewPanoramaView,
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default forAndroid
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
        ImmutableList.of(EdgeToEdgeMarginConfig.builder().setView(streetViewPanoramaView).build()));

    streetViewPanoramaView.onCreate(savedInstanceState);
  }

  @Override
  protected void onStart() {
    streetViewPanoramaView.onStart();
    super.onStart();
  }

  @Override
  protected void onResume() {
    streetViewPanoramaView.onResume();
    super.onResume();
  }

  @Override
  protected void onPause() {
    streetViewPanoramaView.onPause();
    super.onPause();
  }

  @Override
  protected void onStop() {
    streetViewPanoramaView.onStop();
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    streetViewPanoramaView.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    streetViewPanoramaView.onSaveInstanceState(outState);
  }
}
