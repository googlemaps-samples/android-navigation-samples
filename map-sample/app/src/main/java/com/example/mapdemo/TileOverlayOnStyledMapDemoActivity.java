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
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;

/** This demonstrates how to add a tile overlay to a map. */
public class TileOverlayOnStyledMapDemoActivity extends AppCompatActivity
    implements OnSeekBarChangeListener, OnMapReadyCallback {

  private static final MoonTileProvider MOON_TILE_PROVIDER = new MoonTileProvider();
  private static final int TRANSPARENCY_MAX = 100;

  private TileOverlay moonTiles;
  private SeekBar transparencyBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.tile_overlay_demo_nav_flavor);
      setMarginForEdgeToEdgeSupport();
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.tile_overlay_demo_maps_flavor);
      setMarginForEdgeToEdgeSupport();
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
    map.setMapStyle(
        new MapStyleOptions(
            getResources().getString(R.string.multiple_maps_style_midnight_command)));
    moonTiles = map.addTileOverlay(new TileOverlayOptions().tileProvider(MOON_TILE_PROVIDER));
    transparencyBar.setOnSeekBarChangeListener(this);
  }

  public void setFadeIn(View v) {
    if (moonTiles == null) {
      return;
    }
    moonTiles.setFadeIn(((CheckBox) v).isChecked());
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (moonTiles != null) {
      moonTiles.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
    }
  }

  public void clearCache(View view) {
    if (moonTiles != null) {
      moonTiles.clearTileCache();
    }
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
