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
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** This shows how to take a snapshot of the map. */
public class SnapshotDemoActivity extends FragmentActivity implements OnMapReadyCallback {

  private GoogleMap map;

  private CheckBox waitForMapLoadCheckBox;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get the map and register for the ready callback.
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.snapshot_demo_nav_flavor);
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.snapshot_demo_maps_flavor);
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }

    waitForMapLoadCheckBox = (CheckBox) findViewById(R.id.wait_for_map_load);
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    this.map = googleMap;
  }

  /** Called when the snapshot button is clicked. */
  public void onScreenshot(View view) {
    takeSnapshot();
  }

  private void takeSnapshot() {
    if (map == null) {
      return;
    }

    final ImageView snapshotHolder = (ImageView) findViewById(R.id.snapshot_holder);

    final SnapshotReadyCallback callback =
        snapshot -> {
          // Callback is ensured to be called from the main thread, so we can modify the ImageView
          // safely.
          snapshotHolder.setImageBitmap(snapshot);
        };

    if (waitForMapLoadCheckBox.isChecked()) {
      map.setOnMapLoadedCallback(() -> map.snapshot(callback));
    } else {
      map.snapshot(callback);
    }
  }

  /** Called when the clear screenshot button is clicked. */
  public void onClearScreenshot(View view) {
    ImageView snapshotHolder = (ImageView) findViewById(R.id.snapshot_holder);
    snapshotHolder.setImageDrawable(null);
  }
}
