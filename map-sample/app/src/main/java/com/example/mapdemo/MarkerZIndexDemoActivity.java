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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnGroundOverlayClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** This shows how to create markes and set their z-index values. */
public class MarkerZIndexDemoActivity extends AppCompatActivity
    implements OnMapReadyCallback, OnGroundOverlayClickListener {
  private static final float CENTER_LAT = 10.0f;
  private static final float CENTER_LNG = 0.0f;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.basic_demo_nav_flavor);
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.basic_demo_maps_flavor);
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  /** Add multiple markers close enough to each other to demonstrate z-index based rendering. */
  @Override
  public void onMapReady(GoogleMap map) {
    map.addMarker(
        new MarkerOptions()
            .position(new LatLng(CENTER_LAT, CENTER_LNG))
            .title("Marker z1")
            .zIndex(1.0f));
    map.addMarker(
        new MarkerOptions()
            .position(new LatLng(CENTER_LAT + 0.1f, CENTER_LNG))
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
            .title("Marker z2")
            .zIndex(2.0f));
    map.addMarker(
        new MarkerOptions()
            .position(new LatLng(CENTER_LAT + 0.2f, 0))
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            .title("Marker z4")
            .zIndex(4.0f));
    map.addMarker(
        new MarkerOptions()
            .position(new LatLng(CENTER_LAT + 0.3f, 0))
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            .title("Marker z3")
            .zIndex(3.0f));

    BitmapDescriptor image = BitmapDescriptorFactory.fromResource(R.drawable.newark_nj_1922);

    map.addGroundOverlay(
        new GroundOverlayOptions()
            .image(image)
            .anchor(0, 1)
            .position(new LatLng(CENTER_LAT - 0.5f, CENTER_LNG - 0.5f), 100000f, 100000f)
            .zIndex(5)
            .clickable(true));

    map.setOnGroundOverlayClickListener(this);
  }

  @Override
  public void onGroundOverlayClick(GroundOverlay groundOverlay) {
    // Toggle transparency value between 0.0f and 0.5f. Initial default value is 0.0f.
    groundOverlay.setTransparency(0.5f - groundOverlay.getTransparency());
  }
}
