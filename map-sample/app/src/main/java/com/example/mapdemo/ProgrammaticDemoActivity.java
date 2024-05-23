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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/**
 * Demonstrates how to instantiate a SupportNavigationFragment programmatically and add a marker to
 * it.
 */
public class ProgrammaticDemoActivity extends AppCompatActivity implements OnMapReadyCallback {

  private static final String MAP_FRAGMENT_TAG = "map";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // It isn't possible to set a fragment's id programmatically so we set a tag instead and
    // search for it using that.
    Fragment fragment = getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);

    // We only create a fragment if it doesn't already exist.
    if (fragment == null) {
      if (getIntent()
          .getBooleanExtra(
              ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
              /* defaultValue= */ false)) {
        // To programmatically add the map, we first create a SupportNavigationFragment.
        SupportNavigationFragment navFragment = new SupportNavigationFragment();
        // Then we add it using a FragmentTransaction.
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(android.R.id.content, navFragment, MAP_FRAGMENT_TAG);
        fragmentTransaction.commit();
        navFragment.getMapAsync(this);
      } else {
        // To programmatically add the map, we first create a SupportMapFragment.
        SupportMapFragment mapFragment = new SupportMapFragment();
        // Then we add it using a FragmentTransaction.
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(android.R.id.content, mapFragment, MAP_FRAGMENT_TAG);
        fragmentTransaction.commit();
        mapFragment.getMapAsync(this);
      }
    } else if (fragment instanceof SupportMapFragment) {
      // The fragment already exists, so we proceed with fetching the map and drawing the marker.
      ((SupportMapFragment) fragment).getMapAsync(this);
    } else {
      // The fragment already exists, so we proceed with fetching the map and drawing the marker.
      ((SupportNavigationFragment) fragment).getMapAsync(this);
    }
  }

  @Override
  public void onMapReady(GoogleMap map) {
    map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
  }
}
