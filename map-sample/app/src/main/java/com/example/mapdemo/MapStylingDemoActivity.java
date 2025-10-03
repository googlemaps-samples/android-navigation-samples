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

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;

/**
 * This demo shows styling of a mode map, using a static JSON styling string. See use of {@link
 * GoogleMap#setMapStyle} in {@link #onMapReady(GoogleMap)}.
 */
public class MapStylingDemoActivity extends AppCompatActivity implements OnMapReadyCallback {

  private static final LatLng ADELAIDE = new LatLng(-34.92873, 138.59995);
  private static final LatLng CENTRAL_AUSTRALIA = new LatLng(-26.0, 134.0);
  private static final LatLng SOUTH_CHINA_SEA = new LatLng(13.870237, 111.470760);
  private static final CharSequence STYLING_FAILED_MESSAGE = "Style parsing failed: check logs";

  private GoogleMap map;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Get the map and register for the ready callback.
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.map_styling_demo_nav_flavor);
      setMarginForEdgeToEdgeSupport();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.map_styling_demo_maps_flavor);
      setMarginForEdgeToEdgeSupport();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  /** Called when the map is ready to add all markers and objects to the map. */
  @Override
  public void onMapReady(GoogleMap googleMap) {
    map = googleMap;
    setMapStyleRawResource(R.raw.desaturated_style);
  }

  /** Move the camera to center on South China Sea. */
  public void showSouthChinaSea(View v) {
    // Wait until map is ready
    if (map == null) {
      return;
    }

    // Center camera on South China Sea
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(SOUTH_CHINA_SEA, 4f));
  }

  /** Move the camera to center on Adelaide. */
  public void showAdelaide(View v) {
    // Wait until map is ready
    if (map == null) {
      return;
    }

    // Center camera on Adelaide marker
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(ADELAIDE, 10f));
  }

  /** Move the camera to show all of Australia. */
  public void showAustralia(View v) {
    // Wait until map is ready
    if (map == null) {
      return;
    }

    // Center camera on central Australia.
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(CENTRAL_AUSTRALIA, 4f));
  }

  public void setMapStyleDesaturated(View v) {
    setMapStyleRawResource(R.raw.desaturated_style);
  }

  public void setMapStyleTextOutline(View v) {
    setMapStyleRawResource(R.raw.outline_style);
  }

  public void setMapStyleNoStyle(View v) {
    handleStylingError(map.setMapStyle(null));
  }

  public void setMapStyleInvalidStyle(View v) {
    handleStylingError(
        map.setMapStyle(
            new MapStyleOptions(getResources().getString(R.string.style_json_invalid))));
  }

  private void setMapStyleRawResource(int resourceId) {
    // Customise the styling of the base map using a JSON object defined in a raw resource file.
    // First create a MapStyleOptions object, load the raw resource style into this
    // MapStyleOptions object, then pass the MapStyleOptions object to the setMapStyle method of
    // the GoogleMap object.
    try {
      map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, resourceId));
    } catch (Resources.NotFoundException e) {
      handleStylingError(false);
    }
  }

  private void handleStylingError(boolean success) {
    if (!success) {
      Toast.makeText(this, STYLING_FAILED_MESSAGE, Toast.LENGTH_SHORT).show();
    }
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
