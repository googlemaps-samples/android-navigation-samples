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
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** This shows how to create a simple activity with streetview and a map */
public class SplitStreetViewPanoramaAndMapDemoActivity extends AppCompatActivity
    implements OnMarkerDragListener, OnStreetViewPanoramaChangeListener {

  private static final String MARKER_POSITION_KEY = "MarkerPosition";

  // Pitt St, Sydney
  private static final LatLng SYDNEY = new LatLng(-33.8682624, 151.2083773);

  private StreetViewPanorama streetViewPanorama;
  private Marker marker;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final LatLng markerPosition;
    if (savedInstanceState == null) {
      markerPosition = SYDNEY;
    } else {
      markerPosition = savedInstanceState.getParcelable(MARKER_POSITION_KEY);
    }

    OnMapReadyCallback callback = map -> {
      map.setOnMarkerDragListener(SplitStreetViewPanoramaAndMapDemoActivity.this);
      // Creates a draggable marker. Long press to drag.
      marker =
          map.addMarker(
              new MarkerOptions()
                  .position(markerPosition)
                  .icon(BitmapDescriptorFactory.fromResource(R.drawable.pegman))
                  .draggable(true));
    };
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.split_street_view_panorama_and_map_demo_nav_flavor);

      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(callback);
    } else {
      setContentView(R.layout.split_street_view_panorama_and_map_demo_maps_flavor);

      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(callback);
    }

    SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
        (SupportStreetViewPanoramaFragment)
            getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama);
    streetViewPanoramaFragment.getStreetViewPanoramaAsync(
        panorama -> {
          streetViewPanorama = panorama;
          streetViewPanorama.setOnStreetViewPanoramaChangeListener(
              SplitStreetViewPanoramaAndMapDemoActivity.this);
          // Only need to set the position once as the streetview fragment will maintain
          // its state.
          if (savedInstanceState == null) {
            streetViewPanorama.setPosition(SYDNEY);
          }
        });
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(MARKER_POSITION_KEY, marker.getPosition());
  }

  @Override
  public void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) {
    if (location != null) {
      marker.setPosition(location.position);
    }
  }

  @Override
  public void onMarkerDragStart(Marker marker) {}

  @Override
  public void onMarkerDragEnd(Marker marker) {
    streetViewPanorama.setPosition(marker.getPosition(), 150);
  }

  @Override
  public void onMarkerDrag(Marker marker) {}
}
