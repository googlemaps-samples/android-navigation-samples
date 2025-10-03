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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;

/** This shows how UI settings can be toggled. */
public class UiSettingsDemoActivity extends AppCompatActivity implements OnMapReadyCallback {
  private GoogleMap map;
  private UiSettings uiSettings;
  private CheckBox myLocationButtonCheckbox;
  private CheckBox myLocationLayerCheckbox;

  private boolean isPermissionBeingRequested = false;

  private static final int MY_LOCATION_PERMISSION_REQUEST_CODE = 1;
  private static final int LOCATION_LAYER_PERMISSION_REQUEST_CODE = 2;

  /**
   * Flag indicating whether a requested permission has been denied after returning in {@link
   * #onRequestPermissionsResult(int, String[], int[])}.
   */
  private boolean locationPermissionDenied = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.ui_settings_demo_nav_flavor);
      setMarginForEdgeToEdgeSupport();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.ui_settings_demo_maps_flavor);
      setMarginForEdgeToEdgeSupport();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }

    myLocationButtonCheckbox = (CheckBox) findViewById(R.id.mylocationbutton_toggle);
    myLocationLayerCheckbox = (CheckBox) findViewById(R.id.mylocationlayer_toggle);
  }

  /** Returns whether the checkbox with the given id is checked. */
  private boolean isChecked(int id) {
    return ((CheckBox) findViewById(id)).isChecked();
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;
    map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

    uiSettings = this.map.getUiSettings();

    // Keep the UI Settings state in sync with the checkboxes.
    uiSettings.setZoomControlsEnabled(isChecked(R.id.zoom_buttons_toggle));
    uiSettings.setCompassEnabled(isChecked(R.id.compass_toggle));
    uiSettings.setScrollGesturesEnabled(isChecked(R.id.scroll_toggle));
    uiSettings.setZoomGesturesEnabled(isChecked(R.id.zoom_gestures_toggle));
    uiSettings.setTiltGesturesEnabled(isChecked(R.id.tilt_toggle));
    uiSettings.setRotateGesturesEnabled(isChecked(R.id.rotate_toggle));
    uiSettings.setScrollGesturesEnabledDuringRotateOrZoom(
        isChecked(R.id.scroll_on_rotate_or_zoom_toggle));

    updateMyLocationButton();
    updateMyLocationLayer();
  }

  /**
   * Checks if the map is ready (which depends on whether the Google Play services APK is available.
   * This should be called prior to calling any methods on GoogleMap.
   */
  private boolean checkReady() {
    if (map == null) {
      Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  public void setZoomButtonsEnabled(View v) {
    if (!checkReady()) {
      return;
    }
    // Enables/disables the zoom controls (+/- buttons in the bottom-right of the map for LTR
    // locale or bottom-left for RTL locale).
    uiSettings.setZoomControlsEnabled(((CheckBox) v).isChecked());
  }

  public void setCompassEnabled(View v) {
    if (!checkReady()) {
      return;
    }
    // Enables/disables the compass (icon in the top-left for LTR locale or top-right for RTL
    // locale that indicates the orientation of the map).
    uiSettings.setCompassEnabled(((CheckBox) v).isChecked());
  }

  public void setMyLocationButtonEnabled(View v) {
    updateMyLocationButton();
  }

  public void setMyLocationLayerEnabled(View v) {
    updateMyLocationLayer();
  }

  public void setScrollGesturesEnabled(View v) {
    if (!checkReady()) {
      return;
    }
    uiSettings.setScrollGesturesEnabled(((CheckBox) v).isChecked());
  }

  public void setZoomGesturesEnabled(View v) {
    if (!checkReady()) {
      return;
    }
    uiSettings.setZoomGesturesEnabled(((CheckBox) v).isChecked());
  }

  public void setTiltGesturesEnabled(View v) {
    if (!checkReady()) {
      return;
    }
    uiSettings.setTiltGesturesEnabled(((CheckBox) v).isChecked());
  }

  public void setRotateGesturesEnabled(View v) {
    if (!checkReady()) {
      return;
    }
    uiSettings.setRotateGesturesEnabled(((CheckBox) v).isChecked());
  }

  public void setScrollGesturesEnabledDuringRotateOrZoom(View v) {
    if (!checkReady()) {
      return;
    }
    uiSettings.setScrollGesturesEnabledDuringRotateOrZoom(((CheckBox) v).isChecked());
  }

  /**
   * Requests the fine location permission. If a rationale with an additional explanation should be
   * shown to the user, displays a dialog that triggers the request.
   */
  public void requestLocationPermission(int requestCode) {
    if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.ACCESS_FINE_LOCATION)) {
      // Display a dialog with rationale.
      PermissionUtils.RationaleDialog.newInstance(requestCode, false)
          .show(getSupportFragmentManager(), "dialog");
    } else {
      // Location permission has not been granted yet, request it.
      PermissionUtils.requestPermission(
          this, requestCode, Manifest.permission.ACCESS_FINE_LOCATION, false);
    }
  }

  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();
    if (locationPermissionDenied) {
      PermissionUtils.PermissionDeniedDialog.newInstance(false)
          .show(getSupportFragmentManager(), "dialog");
      locationPermissionDenied = false;
    }
  }

  private void updateMyLocationButton() {
    if (!checkReady()) {
      return;
    }

    // Enables/disables the my location button (this DOES NOT enable/disable the my location
    // dot/chevron on the map). The my location button will never appear if the my location
    // layer is not enabled.
    // First verify that the location permission has been granted.
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      updateMapLocationFeatures();
    } else {
      // Uncheck the box and request missing location permission.
      myLocationButtonCheckbox.setChecked(false);
      if (!isPermissionBeingRequested) {
        requestLocationPermission(MY_LOCATION_PERMISSION_REQUEST_CODE);
      }
    }
  }

  @SuppressWarnings("MissingPermission") // We retrieve location permissions earlier in the flow.
  private void updateMyLocationLayer() {
    if (!checkReady()) {
      return;
    }

    if (!myLocationLayerCheckbox.isChecked()) {
      map.setMyLocationEnabled(false);
      return;
    }

    // Enables/disables the my location layer (i.e., the dot/chevron on the map). If enabled, it
    // will also cause the my location button to show (if it is enabled); if disabled, the my
    // location button will never show.
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      updateMapLocationFeatures();
    } else {
      // Uncheck the box and request missing location permission.
      myLocationLayerCheckbox.setChecked(false);
      if (!isPermissionBeingRequested) {
        requestLocationPermission(UiSettingsDemoActivity.LOCATION_LAYER_PERMISSION_REQUEST_CODE);
      }
    }
  }

  @SuppressWarnings("MissingPermission") // We retrieve location permissions earlier in the flow.
  private void updateMapLocationFeatures() {
    map.setMyLocationEnabled(myLocationLayerCheckbox.isChecked());
    uiSettings.setMyLocationButtonEnabled(myLocationButtonCheckbox.isChecked());
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
