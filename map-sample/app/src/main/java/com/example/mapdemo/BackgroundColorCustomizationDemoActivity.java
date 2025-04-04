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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/**
 * This shows how to create a simple activity with a custom background color appiled to the map, and
 * add a marker on the map.
 */
public class BackgroundColorCustomizationDemoActivity extends AppCompatActivity
    implements OnMapReadyCallback {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setupNavFragment();
    } else {
      setupMapFragment();
    }
  }

  private void setupNavFragment() {
    setContentView(R.layout.background_color_customization_demo_nav_flavor);
    SupportNavigationFragment navFragment =
        (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    navFragment.getMapAsync(this);
  }

  private void setupMapFragment() {
    setContentView(R.layout.background_color_customization_demo_maps_flavor);
    SupportMapFragment mapFragment =
        (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
  }

  /**
   * This is where we can add markers or lines, add listeners or move the camera. In this case, we
   * just add a marker near Africa.
   */
  @Override
  public void onMapReady(GoogleMap map) {
    map.setMapType(GoogleMap.MAP_TYPE_NONE);

    CheckBox mapTypeToggleCheckbox = (CheckBox) findViewById(R.id.map_type_toggle);
    mapTypeToggleCheckbox.setChecked(false);
    mapTypeToggleCheckbox.setOnCheckedChangeListener(
        new CheckBox.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            map.setMapType(isChecked ? GoogleMap.MAP_TYPE_NORMAL : GoogleMap.MAP_TYPE_NONE);
          }
        });

    map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
  }
}
