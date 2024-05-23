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
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnGroundOverlayClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import java.util.ArrayList;
import java.util.List;

/** This shows how to add a ground overlay to a map. */
public class GroundOverlayDemoActivity extends AppCompatActivity
    implements OnSeekBarChangeListener, OnMapReadyCallback, OnGroundOverlayClickListener {

  private static final int POSITION_MAX = 100;
  private static final double POSITION_LATLNG_DELTA_MAX = 0.05;
  private static final int DIMENSION_MAX = 3000;
  private static final int BEARING_MIN = -450;
  private static final int BEARING_MAX = 450;
  private static final int TRANSPARENCY_MAX = 100;

  private static final LatLng NEWARK = new LatLng(40.714086, -74.228697);
  private static final LatLng NEAR_NEWARK =
      new LatLng(NEWARK.latitude - 0.001, NEWARK.longitude - 0.01);
  private static final float WIDTH_METERS = 4300f;
  private static final float HEIGHT_METERS = 3025f;
  private static final int INITIAL_BEARING_DEG = 30;

  private final List<BitmapDescriptor> images = new ArrayList<BitmapDescriptor>();

  private GroundOverlay groundOverlay1;
  private GroundOverlay groundOverlay2;
  private SeekBar positionBar;
  private SeekBar dimensionBar;
  private SeekBar bearingBar;
  private SeekBar transparencyBar;

  private int currentEntry = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.ground_overlay_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.ground_overlay_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    // SeekBar values [0, POSITION_MAX] represent delta [LatLng(0.0, 0.0),
    // LatLng(POSITION_LATLNG_DELTA_MAX, POSITION_LATLNG_DELTA_MAX)] with respect to NEWARK.
    positionBar = (SeekBar) findViewById(R.id.positionSeekBar);
    positionBar.setMax(POSITION_MAX);
    positionBar.setProgress(0); // position = NEWARK

    // SeekBar values [0, DIMENSION_MAX] represent dimensions
    // [(width = WIDTH_METERS, height = HEIGHT_METERS),
    //  (width = WIDTH_METERS + DIMENSION_MAX, height = HEIGHT_METERS + DIMENSION_MAX].
    dimensionBar = (SeekBar) findViewById(R.id.dimensionSeekBar);
    dimensionBar.setMax(DIMENSION_MAX);
    dimensionBar.setProgress(0); // width = WIDTH_METERS, height = HEIGHT_METERS

    // SeekBar values [0, BEARING_MAX - BEARING_MIN] represent bearings [BEARING_MIN, BEARING_MAX].
    bearingBar = (SeekBar) findViewById(R.id.bearingSeekBar);
    bearingBar.setMax(BEARING_MAX - BEARING_MIN);
    bearingBar.setProgress(INITIAL_BEARING_DEG - BEARING_MIN); // bearing = INITIAL_BEARING_DEG

    // SeekBar values [0, TRANSPARENCY_MAX] represent transparencies [0.0, 1.0].
    transparencyBar = (SeekBar) findViewById(R.id.transparencySeekBar);
    transparencyBar.setMax(TRANSPARENCY_MAX);
    transparencyBar.setProgress(0); // transparency = 0.0
  }

  @Override
  public void onMapReady(GoogleMap map) {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(NEWARK, 11));

    images.clear();
    images.add(BitmapDescriptorFactory.fromResource(R.drawable.newark_nj_1922));
    images.add(BitmapDescriptorFactory.fromResource(R.drawable.newark_prudential_sunny));

    groundOverlay1 =
        map.addGroundOverlay(
            new GroundOverlayOptions()
                .image(images.get(1))
                .anchor(0.25f, 0.5f)
                .position(NEAR_NEWARK, WIDTH_METERS, HEIGHT_METERS)
                .bearing(INITIAL_BEARING_DEG)
                .clickable(((CheckBox) findViewById(R.id.toggleClickability)).isChecked()));

    currentEntry = 0;
    groundOverlay2 =
        map.addGroundOverlay(
            new GroundOverlayOptions()
                .image(images.get(currentEntry))
                .anchor(0.0f, 1.0f)
                .position(NEWARK, 8600f, 6500f));

    map.setOnGroundOverlayClickListener(this);
    positionBar.setOnSeekBarChangeListener(this);
    dimensionBar.setOnSeekBarChangeListener(this);
    bearingBar.setOnSeekBarChangeListener(this);
    transparencyBar.setOnSeekBarChangeListener(this);

    // Override the default content description on the view, for accessibility mode.
    // Ideally this string would be localised.
    map.setContentDescription("Google Map with ground overlay.");
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    int seekBarId = seekBar.getId();
    if (seekBarId == R.id.positionSeekBar) {
      if (groundOverlay2 != null) {
        double delta = (((double) progress) * POSITION_LATLNG_DELTA_MAX) / POSITION_MAX;
        groundOverlay2.setPosition(new LatLng(NEWARK.latitude + delta, NEWARK.longitude + delta));
      }
    } else if (seekBarId == R.id.dimensionSeekBar) {
      if (groundOverlay1 != null) {
        groundOverlay1.setDimensions(WIDTH_METERS + progress, HEIGHT_METERS + progress);
      }
    } else if (seekBarId == R.id.bearingSeekBar) {
      if (groundOverlay1 != null) {
        groundOverlay1.setBearing(progress + BEARING_MIN);
      }
    } else if (seekBarId == R.id.transparencySeekBar) {
      if (groundOverlay2 != null) {
        groundOverlay2.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
      }
    }
  }

  public void switchImage(View view) {
    if (groundOverlay2 != null) {
      currentEntry = (currentEntry + 1) % images.size();
      groundOverlay2.setImage(images.get(currentEntry));
    }
  }

  @Override
  public void onGroundOverlayClick(GroundOverlay groundOverlay2) {
    if (groundOverlay1 != null) {
      // Toggle transparency value between 0.0f and 0.5f. Initial default value is 0.0f.
      groundOverlay1.setTransparency(0.5f - groundOverlay1.getTransparency());
    }
  }

  public void toggleClickability(View view) {
    if (groundOverlay1 != null) {
      groundOverlay1.setClickable(((CheckBox) view).isChecked());
    }
  }

  public void toggleVisibility(View view) {
    if (groundOverlay2 != null) {
      groundOverlay2.setVisible(((CheckBox) view).isChecked());
    }
  }
}
