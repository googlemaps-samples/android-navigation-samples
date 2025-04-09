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
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** This demonstrates tile overlay coordinates. */
public class TileCoordinateDemoActivity extends AppCompatActivity
    implements OnSeekBarChangeListener, OnMapReadyCallback {

  private static final int TRANSPARENCY_MAX = 100;

  private TileOverlay tileOverlay;
  private SeekBar transparencyBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.tile_coordinate_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.tile_coordinate_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    transparencyBar = (SeekBar) findViewById(R.id.transparencySeekBar);
    transparencyBar.setMax(TRANSPARENCY_MAX);
    transparencyBar.setProgress(0);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    TileProvider coordTileProvider =
        new TileCoordsTileProvider(
            getApplicationContext().getResources().getDisplayMetrics().density);
    tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(coordTileProvider));
    transparencyBar.setOnSeekBarChangeListener(this);
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (tileOverlay != null) {
      tileOverlay.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
    }
  }
}
