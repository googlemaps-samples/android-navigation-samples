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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import java.util.Locale;

/** Demonstrates top custom styles from Snazzy Maps on a map. */
public class TopStylesDemoActivity extends AppCompatActivity
    implements OnItemSelectedListener, OnMapReadyCallback {

  private GoogleMap map;

  private CheckBox trafficCheckbox;
  private CheckBox buildingsCheckbox;

  private Spinner spinner;

  /**
   * Flag indicating whether a requested permission has been denied after returning in {@link
   * #onRequestPermissionsResult(int, String[], int[])}.
   */
  private boolean showPermissionDeniedDialog = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.top_styles_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.top_styles_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    spinner = (Spinner) findViewById(R.id.styles_spinner);
    ArrayAdapter<CharSequence> adapter =
        ArrayAdapter.createFromResource(
            this, R.array.top_styles_array, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setOnItemSelectedListener(this);

    trafficCheckbox = (CheckBox) findViewById(R.id.traffic);
    buildingsCheckbox = (CheckBox) findViewById(R.id.buildings);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;
    updateStyle(getResources().getString(R.string.no_style));
    updateTraffic();
    updateBuildings();
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

  private void updateStyle(String styleName) {
    if (!checkReady()) {
      return;
    }

    styleName = styleName.toLowerCase(Locale.getDefault());

    if (styleName.equals(
        getResources().getString(R.string.no_style).toLowerCase(Locale.getDefault()))) {
      map.setMapStyle(null);
    } else {
      map.setMapStyle(
          MapStyleOptions.loadRawResourceStyle(
              this,
              getResources()
                  .getIdentifier(styleName, "raw", getApplicationContext().getPackageName())));
    }
  }

  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();
    if (showPermissionDeniedDialog) {
      PermissionUtils.PermissionDeniedDialog.newInstance(false)
          .show(getSupportFragmentManager(), "dialog");
      showPermissionDeniedDialog = false;
    }
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

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    String selectedItem = (String) spinner.getSelectedItem();
    String styleName = selectedItem.replace(" ", "_");
    updateStyle(styleName);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    updateStyle(getResources().getString(R.string.no_style));
  }
}
