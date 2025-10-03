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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;

/** This shows how marker and info window anchors work. */
public class MarkerAnchorsDemoActivity extends AppCompatActivity
    implements OnMarkerClickListener,
        OnSeekBarChangeListener,
        OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener {
  private static final LatLng ADELAIDE = new LatLng(-34.92873, 138.59995);
  private static final LatLng SYDNEY = new LatLng(-33.87365, 151.20689);

  private GoogleMap map = null;

  private Marker adelaide;
  private Marker sydney;

  private SeekBar rotationBar;
  private SeekBar markerAnchorUBar;
  private SeekBar markerAnchorVBar;
  private SeekBar infoWindowAnchorUBar;
  private SeekBar infoWindowAnchorVBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.marker_anchors_demo_nav_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      new OnMapAndViewReadyListener(navFragment, this);
    } else {
      setContentView(R.layout.marker_anchors_demo_maps_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      new OnMapAndViewReadyListener(mapFragment, this);
    }
  }

  private void performAdditionalSetup() {
    rotationBar = (SeekBar) findViewById(R.id.rotationSeekBar);
    rotationBar.setMax(360);
    rotationBar.setOnSeekBarChangeListener(this);

    markerAnchorUBar = (SeekBar) findViewById(R.id.markerAnchorUBar);
    markerAnchorUBar.setMax(100);
    markerAnchorUBar.setProgress(50);
    markerAnchorUBar.setOnSeekBarChangeListener(this);

    markerAnchorVBar = (SeekBar) findViewById(R.id.markerAnchorVBar);
    markerAnchorVBar.setMax(100);
    markerAnchorVBar.setProgress(100);
    markerAnchorVBar.setOnSeekBarChangeListener(this);

    infoWindowAnchorUBar = (SeekBar) findViewById(R.id.infoWindowAnchorUBar);
    infoWindowAnchorUBar.setMax(100);
    infoWindowAnchorUBar.setProgress(50);
    infoWindowAnchorUBar.setOnSeekBarChangeListener(this);

    infoWindowAnchorVBar = (SeekBar) findViewById(R.id.infoWindowAnchorVBar);
    infoWindowAnchorVBar.setMax(100);
    infoWindowAnchorVBar.setProgress(0);
    infoWindowAnchorVBar.setOnSeekBarChangeListener(this);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;

    // Hide the zoom controls.
    this.map.getUiSettings().setZoomControlsEnabled(false);

    // Add markers to the map.
    addMarkersToMap();

    // Set listeners for marker events.  See the bottom of this class for their behavior.
    this.map.setOnMarkerClickListener(this);

    // Override the default content description on the view, for accessibility mode.
    // Ideally this string would be localised.
    map.setContentDescription("Map with markers.");

    LatLngBounds bounds = new LatLngBounds.Builder().include(ADELAIDE).include(SYDNEY).build();
    this.map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
    CameraPosition currentCameraPosition = this.map.getCameraPosition();
    float slightZoomOut = Math.max(0, currentCameraPosition.zoom - 0.5f);
    CameraPosition cameraPosition =
        new CameraPosition.Builder(currentCameraPosition).tilt(90).zoom(slightZoomOut).build();
    this.map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  }

  private void addMarkersToMap() {
    adelaide =
        map.addMarker(
            new MarkerOptions()
                .position(ADELAIDE)
                .title("Adelaide")
                .snippet("Population: 1,213,000")
                .draggable(true));

    sydney =
        map.addMarker(
            new MarkerOptions()
                .position(SYDNEY)
                .title("Sydney")
                .snippet("Population: 4,627,300")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(true)
                .flat(true));
  }

  private boolean checkReady() {
    if (map == null) {
      Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  private void setMarginForEdgeToEdgeSupport() {
    // Margins are only set if the edge-to-edge mode is enabled, it's enabled by default for Android
    // V+ devices.
    // No margins are set for pre-Android V devices.
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(
        ImmutableList.of(
            EdgeToEdgeMarginConfig.builder().setView(findViewById(R.id.layout_container)).build()));
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (!checkReady()) {
      return;
    }

    if (seekBar == rotationBar) {
      float rotation = seekBar.getProgress();
      adelaide.setRotation(rotation);
      sydney.setRotation(rotation);
      return;
    }

    if (seekBar == markerAnchorUBar || seekBar == markerAnchorVBar) {
      float markerAnchorU = markerAnchorUBar.getProgress() / 100.0f;
      float markerAnchorV = markerAnchorVBar.getProgress() / 100.0f;
      adelaide.setAnchor(markerAnchorU, markerAnchorV);
      sydney.setAnchor(markerAnchorU, markerAnchorV);
      return;
    }

    if (seekBar == infoWindowAnchorUBar || seekBar == infoWindowAnchorVBar) {
      float infoWindowAnchorU = infoWindowAnchorUBar.getProgress() / 100.0f;
      float infoWindowAnchorV = infoWindowAnchorVBar.getProgress() / 100.0f;
      adelaide.setInfoWindowAnchor(infoWindowAnchorU, infoWindowAnchorV);
      sydney.setInfoWindowAnchor(infoWindowAnchorU, infoWindowAnchorV);
      return;
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    // Do nothing.
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    // Do nothing.
  }

  //
  // Marker related listeners.
  //

  @Override
  public boolean onMarkerClick(final Marker marker) {
    // Show the info window manually.
    marker.showInfoWindow();
    // Return true to indicate we have consumed the event and that we do not want the
    // the default behavior to occur (which is for the camera to move such that the
    // marker is centered and for the marker's info window to open, if it has one).
    return true;
  }
}
