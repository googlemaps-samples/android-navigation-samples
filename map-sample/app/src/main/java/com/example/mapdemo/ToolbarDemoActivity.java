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

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** This shows how to control the Map Toolbar. */
public class ToolbarDemoActivity extends AppCompatActivity
    implements OnMarkerClickListener,
        OnMarkerDragListener,
        OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener,
        OnMapLongClickListener {

  private static final LatLng ADELAIDE = new LatLng(-34.92873, 138.59995);
  private static final LatLng BRISBANE = new LatLng(-27.47093, 153.0235);
  private static final LatLng PERTH = new LatLng(-31.952854, 115.857342);

  private GoogleMap map;

  private CheckBox infoWindowCheckbox;

  private CheckBox enableGlobalToolbarCheckbox;

  private CheckBox focusMarkerOnClickCheckbox;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.toolbar_demo_nav_flavor);
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);

      OnMapAndViewReadyListener unused = new OnMapAndViewReadyListener(navFragment, this);
    } else {
      setContentView(R.layout.toolbar_demo_maps_flavor);
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

      OnMapAndViewReadyListener unused = new OnMapAndViewReadyListener(mapFragment, this);
    }

    // Get checkbox references
    infoWindowCheckbox = (CheckBox) findViewById(R.id.add_info_window_to_new_marker);

    enableGlobalToolbarCheckbox = ((CheckBox) findViewById(R.id.enable_toolbar_globally));

    focusMarkerOnClickCheckbox = (CheckBox) findViewById(R.id.focus_marker_on_click);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;

    // Allow Map Toolbar to be disabled/enabled globally via UiSettings
    enableGlobalToolbarCheckbox.setOnClickListener(
        view -> map.getUiSettings().setMapToolbarEnabled(((CheckBox) view).isChecked()));

    // Hide the zoom controls.
    this.map.getUiSettings().setZoomControlsEnabled(false);

    // Add markers, polygons and polylines to the map.
    addObjectsToMap();

    // Set listener for marker click event.  See the bottom of this class for its behavior.
    this.map.setOnMarkerClickListener(this);

    // Set listener for marker drag event.  See the bottom of this class for its behavior.
    this.map.setOnMarkerDragListener(this);

    // Set listener for map long click event.  See the bottom of this class for its behavior.
    this.map.setOnMapLongClickListener(this);

    // Override the default content description on the view, for accessibility mode.
    // Ideally this string would be localised.
    map.setContentDescription("Demo showing how to control the Map Toolbar.");

    LatLngBounds bounds =
        new LatLngBounds.Builder().include(ADELAIDE).include(BRISBANE).include(PERTH).build();
    this.map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
  }

  // Add some other objects to click on, to verify that the toolbar closes when they are clicked.
  private void addObjectsToMap() {
    // A polygon centered at Adelaide.
    map.addPolygon(
        new PolygonOptions()
            .add(
                new LatLng(ADELAIDE.latitude + 3, ADELAIDE.longitude - 3),
                new LatLng(ADELAIDE.latitude + 3, ADELAIDE.longitude + 3),
                new LatLng(ADELAIDE.latitude - 3, ADELAIDE.longitude + 3),
                new LatLng(ADELAIDE.latitude - 3, ADELAIDE.longitude - 3))
            .fillColor(Color.argb(150, 34, 173, 24))
            .strokeColor(Color.rgb(34, 173, 24))
            .clickable(true));

    // A polyline from Perth to Brisbane.
    map.addPolyline(
        new PolylineOptions()
            .add(PERTH, BRISBANE)
            .color(Color.rgb(103, 24, 173))
            .width(30)
            .clickable(true));
  }

  @Override
  public void onMapLongClick(final LatLng point) {
    addMarker(point);
  }

  private void addMarker(final LatLng point) {
    MarkerOptions markerOptions = new MarkerOptions().position(point).draggable(true);

    if (infoWindowCheckbox.isChecked()) {
      markerOptions.title("Marker");
    }

    // Add the marker.
    map.addMarker(markerOptions);
  }

  @Override
  public boolean onMarkerClick(final Marker marker) {
    // If the checkbox is checked then return false to indicate that we have not consumed the
    // event and that we wish for the default behavior to occur (which is for the camera to move
    // such that the marker is centered and for the marker's info window to open, if it has
    // one). Otherwise, return true.
    return !focusMarkerOnClickCheckbox.isChecked();
  }

  @Override
  public void onMarkerDragStart(final Marker marker) {
    marker.remove();
  }

  @Override
  public void onMarkerDrag(final Marker marker) {
    // Do nothing.
  }

  @Override
  public void onMarkerDragEnd(final Marker marker) {
    // Do nothing.
  }

  public void clearMarkers(View v /* unused */) {
    map.clear();
    addObjectsToMap();
  }

  public void addTenMarkers(View v /* unused */) {
    int numMarkers = 10;
    for (int i = 0; i < numMarkers; i++) {
      addMarker(
          new LatLng(
              -30 + 10 * Math.sin(i * Math.PI / (numMarkers - 1)),
              135 - 10 * Math.cos(i * Math.PI / (numMarkers - 1))));
    }
  }
}
