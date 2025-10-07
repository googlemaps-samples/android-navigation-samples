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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;

/**
 * This shows how to retain a map across activity restarts (e.g., from screen rotations), which can
 * be faster than relying on state serialization.
 */
public class RetainMapDemoActivity extends AppCompatActivity implements OnMapReadyCallback {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      // TODO: This behavior may not be supported presently. Legacy issue with NavigationView
      // results in a crash on rotation in this demo.
      setContentView(R.layout.basic_demo_nav_flavor);
      setMarginForEdgeToEdgeSupport();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      if (savedInstanceState == null) {
        // First incarnation of this activity.
        navFragment.setRetainInstance(true);
      }
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.basic_demo_maps_flavor);
      setMarginForEdgeToEdgeSupport();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      if (savedInstanceState == null) {
        // First incarnation of this activity.
        mapFragment.setRetainInstance(true);
      }
      mapFragment.getMapAsync(this);
    }
  }

  @Override
  public void onMapReady(GoogleMap map) {
    map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
  }

  private void setMarginForEdgeToEdgeSupport() {
    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
        ImmutableList.of(
            EdgeToEdgeMarginConfig.builder().setView(findViewById(R.id.layout_container)).build()));
  }
}
