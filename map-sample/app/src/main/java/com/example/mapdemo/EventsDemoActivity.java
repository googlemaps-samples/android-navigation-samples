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
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;

/** This shows how to listen to some map events. */
public class EventsDemoActivity extends AppCompatActivity
    implements OnMapClickListener,
        OnMapLongClickListener,
        OnCameraMoveStartedListener,
        OnMapReadyCallback {

  private TextView tapTextView;
  private TextView cameraTextView;
  private GoogleMap map;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.events_demo_nav_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.events_demo_maps_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    tapTextView = (TextView) findViewById(R.id.tap_text);
    cameraTextView = (TextView) findViewById(R.id.camera_text);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;
    map.setOnMapClickListener(this);
    map.setOnMapLongClickListener(this);
    map.setOnCameraMoveStartedListener(this);
  }

  @Override
  public void onMapClick(LatLng point) {
    Resources res = getResources();
    String text = String.format(res.getString(R.string.events_demo_on_map_click), point);
    tapTextView.setText(text);
  }

  @Override
  public void onMapLongClick(LatLng point) {
    Resources res = getResources();
    String text = String.format(res.getString(R.string.events_demo_on_map_long_click), point);
    tapTextView.setText(text);
  }

  @Override
  public void onCameraMoveStarted(int reason) {
    Resources res = getResources();
    String text =
        res.getString(
            R.string.demo_on_camera_move,
            ConversionUtils.toAnimationReasonString(reason),
            map.getCameraPosition().toString());
    cameraTextView.setText(text);
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
