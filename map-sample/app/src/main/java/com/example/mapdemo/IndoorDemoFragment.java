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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnIndoorStateChangeListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.IndoorLevel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import java.util.Arrays;
import java.util.List;

/** Fragment to assist indoor maps demo. */
public class IndoorDemoFragment extends Fragment
    implements OnClickListener, OnMapReadyCallback, OnIndoorStateChangeListener {

  private static final String TAG = IndoorDemoFragment.class.getSimpleName();

  private static class Location {
    public final String name;
    public final LatLng latLng;
    public final float zoom;

    public Location(String name, LatLng latLng, float zoom) {
      this.name = name;
      this.latLng = latLng;
      this.zoom = zoom;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private static final List<Location> TEST_LOCATIONS =
      Arrays.asList(
          // SFO airport terminal (San Francisco, US): Building with average labels density
          new Location("SFO", new LatLng(37.614631, -122.385153), 18),
          // Queen Victoria Building (Sydney, AU): Area with average buildings density
          new Location("QVB", new LatLng(-33.872090, 151.206766), 19),
          // Place du Carousel - Louvre Museum (Paris, FR): Fully underground building
          new Location("Louvre", new LatLng(48.861562, 2.333531), 18),
          // EPFL Faculty of Computer Science (Lausanne, CH): Building with sparse labels
          new Location("EPFL", new LatLng(46.518714, 6.563146), 20),
          // Tokyo railway station (Tokyo, JP): Area with dense buildings
          new Location("Tokyo", new LatLng(35.679789, 139.766589), 15));

  private GoogleMap map;
  private CheckBox enableIndoorCheckBox;
  private CheckBox enableLevelPickerCheckBox;
  private CheckBox enableListenerCheckBox;
  private Spinner testLocationsSpinner;
  private EditText levelIndexEditText;
  private TextView textView;
  private IndoorBuilding lastRetrievedFocusedBuilding;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view;
    Intent intent = getActivity().getIntent();
    if (intent.getBooleanExtra(
        ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO, /* defaultValue= */ false)) {
      view = inflater.inflate(R.layout.indoor_demo_fragment_nav_flavor, container, false);
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getChildFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      view = inflater.inflate(R.layout.indoor_demo_fragment_maps_flavor, container, false);
      SupportMapFragment mapFragment =
          (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }

    enableIndoorCheckBox = (CheckBox) view.findViewById(R.id.enable_indoor_checkbox);
    enableIndoorCheckBox.setOnClickListener(this);

    enableLevelPickerCheckBox = (CheckBox) view.findViewById(R.id.enable_level_picker_checkbox);
    enableLevelPickerCheckBox.setOnClickListener(this);

    enableListenerCheckBox =
        (CheckBox) view.findViewById(R.id.enable_state_change_listener_checkbox);
    enableListenerCheckBox.setOnClickListener(this);

    levelIndexEditText = (EditText) view.findViewById(R.id.level_index_edittext);
    textView = (TextView) view.findViewById(R.id.top_text);

    lastRetrievedFocusedBuilding = null;
    refreshRetrievedFocusedBuildingInfo();

    view.findViewById(R.id.go_button).setOnClickListener(this);
    view.findViewById(R.id.retrieve_focused_building_button).setOnClickListener(this);
    view.findViewById(R.id.refresh_focused_building_info_button).setOnClickListener(this);
    view.findViewById(R.id.activate_level_at_index_button).setOnClickListener(this);

    testLocationsSpinner = (Spinner) view.findViewById(R.id.test_loc_spinner);
    ArrayAdapter<Location> adapter =
        new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, TEST_LOCATIONS);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    testLocationsSpinner.setAdapter(adapter);

    return view;
  }

  @Override
  public void onMapReady(@NonNull GoogleMap map) {
    this.map = map;
    onGoButtonClick();

    map.setOnIndoorStateChangeListener(this);
    enableListenerCheckBox.setChecked(true);

    enableIndoorCheckBox.setChecked(map.isIndoorEnabled());
    enableLevelPickerCheckBox.setChecked(map.getUiSettings().isIndoorLevelPickerEnabled());
    refreshRetrievedFocusedBuildingInfo();
  }

  @Override
  public void onClick(View view) {
    int id = view.getId();

    if (id == R.id.enable_indoor_checkbox) {
      onEnableIndoorCheckboxClick();
    } else if (id == R.id.enable_level_picker_checkbox) {
      onEnableLevelPickerCheckboxClick();
    } else if (id == R.id.enable_state_change_listener_checkbox) {
      onEnableStateChangeListenerCheckboxClick();
    } else if (id == R.id.go_button) {
      onGoButtonClick();
    } else if (id == R.id.retrieve_focused_building_button) {
      onRetrieveFocusedBuildingButtonClick();
    } else if (id == R.id.refresh_focused_building_info_button) {
      onRefreshFocusedBuildingInfoButtonClick();
    } else if (id == R.id.activate_level_at_index_button) {
      onActivateLevelAtIndexButtonClick();
    }
  }

  private void onEnableLevelPickerCheckboxClick() {
    if (map == null) {
      return;
    }

    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setIndoorLevelPickerEnabled(enableLevelPickerCheckBox.isChecked());
    enableLevelPickerCheckBox.setChecked(uiSettings.isIndoorLevelPickerEnabled());
  }

  private void onGoButtonClick() {
    if (map == null) {
      return;
    }

    Location location = (Location) testLocationsSpinner.getSelectedItem();
    if (location == null) {
      return;
    }

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(location.latLng, location.zoom));
  }

  private void onRetrieveFocusedBuildingButtonClick() {
    if (map == null) {
      return;
    }

    lastRetrievedFocusedBuilding = map.getFocusedBuilding();
    refreshRetrievedFocusedBuildingInfo();
  }

  private void onActivateLevelAtIndexButtonClick() {
    if (lastRetrievedFocusedBuilding == null) {
      logAndToast("'Focused building' not yet retrieved, or really no 'focused building'.");
      return;
    }

    List<IndoorLevel> levels = lastRetrievedFocusedBuilding.getLevels();
    if (levels.isEmpty()) {
      logAndToast("No levels in retrieved 'focused building'.");
      return;
    }

    int levelIndex;
    try {
      levelIndex = Integer.parseInt(String.valueOf(levelIndexEditText.getText()));
    } catch (NumberFormatException e) {
      logAndToast("Unparseable level index");
      return;
    }

    if ((levelIndex < 0) || (levelIndex >= levels.size())) {
      logAndToast(String.format("Level index outside valid range [0, %s]", levels.size() - 1));
      return;
    }

    levels.get(levelIndex).activate();
    refreshRetrievedFocusedBuildingInfo();
  }

  private void onRefreshFocusedBuildingInfoButtonClick() {
    refreshRetrievedFocusedBuildingInfo();
  }

  private void onEnableStateChangeListenerCheckboxClick() {
    if (map == null) {
      return;
    }

    map.setOnIndoorStateChangeListener(enableListenerCheckBox.isChecked() ? this : null);
  }

  private void onEnableIndoorCheckboxClick() {
    if (map == null) {
      return;
    }

    map.setIndoorEnabled(enableIndoorCheckBox.isChecked());
    enableIndoorCheckBox.setChecked(map.isIndoorEnabled());
  }

  @Override
  public void onIndoorBuildingFocused() {
    logAndToast("onIndoorBuildingFocused");
  }

  @Override
  public void onIndoorLevelActivated(IndoorBuilding building) {
    logAndToast(String.format("onIndoorLevelActivated for building:\n\n%s", toString(building)));
  }

  private static String toString(IndoorBuilding building) {
    if (building == null) {
      return "(no building)";
    }

    StringBuilder levelInfoStrBuilder = new StringBuilder();
    List<IndoorLevel> levels = building.getLevels();
    for (int i = 0; i < levels.size(); i++) {
      levelInfoStrBuilder.append(String.format("\t[index=%s %s]\n", i, toString(levels.get(i))));
    }

    return String.format(
        "[isUnderground=%s] [defaultLevelIdx=%s] [activeLevelIdx=%s]\n\nlevels={\n%s}",
        building.isUnderground(),
        building.getDefaultLevelIndex(),
        building.getActiveLevelIndex(),
        levelInfoStrBuilder);
  }

  private static String toString(IndoorLevel level) {
    return (level == null)
        ? "(no level)"
        : String.format("name=\"%s\" shortName=\"%s\"", level.getName(), level.getShortName());
  }

  private void refreshRetrievedFocusedBuildingInfo() {
    String message =
        "Last retrieved 'FOCUSED BUILDING':\n\n" + toString(lastRetrievedFocusedBuilding);
    textView.setText(message);
    Log.i(TAG, message);
  }

  private void logAndToast(String message) {
    String timestampedMessage =
        String.format("<timestamp: %s>\n\n%s", System.currentTimeMillis(), message);
    Log.i(TAG, timestampedMessage);
    Toast toast = Toast.makeText(getActivity(), timestampedMessage, Toast.LENGTH_SHORT);
    toast.setGravity(Gravity.CENTER, 0, 0);
    toast.show();
  }
}
