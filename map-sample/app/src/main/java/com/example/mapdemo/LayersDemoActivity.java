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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** Demonstrates the different base layers of a map. */
public class LayersDemoActivity extends AppCompatActivity implements OnMapReadyCallback {

  private static final String KEY_TAFFIC = "trafic";
  private static final String KEY_BUILDING = "building";
  private static final String KEY_STYLE_MAP = "style_map";

  private GoogleMap map;

  private CheckBox trafficCheckbox;
  private CheckBox buildingsCheckbox;
  private CheckBox styleMapCheckbox;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.layers_demo_nav_flavor);
      performAdditionalSetup(savedInstanceState);
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.layers_demo_maps_flavor);
      performAdditionalSetup(savedInstanceState);
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup(Bundle savedInstanceState) {
    trafficCheckbox = findViewById(R.id.traffic);
    buildingsCheckbox = findViewById(R.id.buildings);
    styleMapCheckbox = findViewById(R.id.styled_map);
    if (savedInstanceState != null) {
      trafficCheckbox.setChecked(savedInstanceState.getBoolean(KEY_TAFFIC));
      buildingsCheckbox.setChecked(savedInstanceState.getBoolean(KEY_BUILDING));
      styleMapCheckbox.setChecked(savedInstanceState.getBoolean(KEY_STYLE_MAP));
    }
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;
    updateTraffic();
    updateBuildings();
    updateStyledMap();
  }

  private boolean checkReady() {
    if (map == null) {
      Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  /** Called when the Traffic checkbox is clicked. */
  public void onTrafficToggled(View view) {
    updateTraffic();
  }

  private void updateTraffic() {
    if (!checkReady()) {
      return;
    }
    map.setTrafficEnabled(trafficCheckbox.isChecked());
  }

  /** Called when the styled checkbox is clicked. */
  public void onStyleMapToggled(View view) {
    updateStyledMap();
  }

  private void updateStyledMap() {
    if (!checkReady()) {
      return;
    }

    if (!styleMapCheckbox.isChecked()) {
      map.setMapStyle(null);
    } else {
      map.setMapStyle(new MapStyleOptions(getResources().getString(R.string.desaturated_style)));
    }
  }

  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();
  }

  @Override
  protected void onSaveInstanceState(Bundle bundle) {
    bundle.putBoolean(KEY_TAFFIC, trafficCheckbox.isChecked());
    bundle.putBoolean(KEY_BUILDING, buildingsCheckbox.isChecked());
    bundle.putBoolean(KEY_STYLE_MAP, styleMapCheckbox.isChecked());
    super.onSaveInstanceState(bundle);
  }

  /** Called when the Buildings checkbox is clicked. */
  public void onBuildingsToggled(View view) {
    updateBuildings();
  }

  private void updateBuildings() {
    if (!checkReady()) {
      return;
    }
    map.setBuildingsEnabled(buildingsCheckbox.isChecked());
  }
}
