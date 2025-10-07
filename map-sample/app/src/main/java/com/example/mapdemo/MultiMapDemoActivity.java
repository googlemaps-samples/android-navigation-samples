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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;

/** This shows how to create a simple activity with multiple maps on screen. */
public class MultiMapDemoActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.multimap_demo_nav_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetupForNavFlavor();
    } else {
      setContentView(R.layout.multimap_demo_maps_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetupForMapsFlavor();
    }
  }

  private void performAdditionalSetupForNavFlavor() {
    SupportNavigationFragment navFragment1 =
        (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map1);
    navFragment1.getMapAsync(
        map ->
            map.setMapStyle(
                new MapStyleOptions(
                    getResources().getString(R.string.multiple_maps_style_midnight_command))));

    SupportNavigationFragment navFragment2 =
        (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map2);
    navFragment2.getMapAsync(
        map ->
            map.setMapStyle(
                new MapStyleOptions(getResources().getString(R.string.style_json_text_outline))));

    SupportNavigationFragment navFragment3 =
        (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map3);
    navFragment3.getMapAsync(
        map ->
            map.setMapStyle(
                new MapStyleOptions(getResources().getString(R.string.desaturated_style))));
  }

  private void performAdditionalSetupForMapsFlavor() {
    SupportMapFragment mapFragment1 =
        (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map1);
    mapFragment1.getMapAsync(
        map ->
            map.setMapStyle(
                new MapStyleOptions(
                    getResources().getString(R.string.multiple_maps_style_midnight_command))));

    SupportMapFragment mapFragment2 =
        (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map2);
    mapFragment2.getMapAsync(
        map ->
            map.setMapStyle(
                new MapStyleOptions(getResources().getString(R.string.style_json_text_outline))));

    SupportMapFragment mapFragment3 =
        (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map3);
    mapFragment3.getMapAsync(
        map ->
            map.setMapStyle(
                new MapStyleOptions(getResources().getString(R.string.desaturated_style))));
  }

  private void setMarginForEdgeToEdgeSupport() {
    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
        ImmutableList.of(
            EdgeToEdgeMarginConfig.builder().setView(findViewById(R.id.map_container)).build()));
  }
}
