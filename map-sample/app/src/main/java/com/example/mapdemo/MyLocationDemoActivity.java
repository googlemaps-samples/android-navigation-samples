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
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/**
 * This demo shows how to use the My Location Layer to display the current location of the device.
 * The My Location Layer shows a blue dot/chevron representing the current location and an optional
 * My Location button that the user can click to center the camera on the current location.
 * Listeners are available for clicks on both the My Location dot and the My Location button. When
 * the My Location dot is first clicked, a Marker is added at the current location. Permission for
 * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} is requested at run time. If the
 * permission has not been granted, the Activity is finished with an error message.
 */
public class MyLocationDemoActivity extends AppCompatActivity
    implements OnMyLocationButtonClickListener,
        OnInfoWindowClickListener,
        OnMarkerClickListener,
        OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

  /**
   * Request code for location permission request.
   *
   * @see #onRequestPermissionsResult(int, String[], int[])
   */
  private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

  /**
   * Flag indicating whether a requested permission has been denied after returning in {@link
   * #onRequestPermissionsResult(int, String[], int[])}.
   */
  private boolean permissionDenied = false;

  private GoogleMap map;
  private Marker marker;

  private CheckBox myLocationCheckbox;
  private CheckBox myLocationClickListenerCheckbox;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.my_location_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.my_location_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    myLocationCheckbox = (CheckBox) findViewById(R.id.my_location);
    myLocationClickListenerCheckbox = (CheckBox) findViewById(R.id.my_location_click_listener);
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;

    this.map.setOnInfoWindowClickListener(this);
    this.map.setOnMyLocationButtonClickListener(this);
    this.map.setOnMarkerClickListener(this);

    updateMyLocation();
  }

  private boolean checkReady() {
    if (map == null) {
      Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  /** Called when the MyLocation checkbox is clicked. */
  public void onMyLocationToggled(View view) {
    updateMyLocation();
  }

  private void updateMyLocation() {
    if (!checkReady()) {
      return;
    }

    if (!myLocationCheckbox.isChecked()) {
      enableMyLocation(false);
      return;
    }

    // Enable the location layer. Request the location permission if needed.
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      enableMyLocation(true);
    } else {
      // Disable the my location layer until the permission has been granted and request missing
      // permission.
      enableMyLocation(false);
      PermissionUtils.requestPermission(
          this, LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION, false);
      // Check again after permissions have been granted.
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
          == PackageManager.PERMISSION_GRANTED) {
        enableMyLocation(true);
      }
    }
  }

  @SuppressWarnings("MissingPermission") // We retrieve location permissions earlier in the flow.
  private void enableMyLocation(boolean enable) {
    map.setMyLocationEnabled(enable);
    myLocationCheckbox.setChecked(enable);
    // MyLocationClickListener checkbox is dependent on the My Location Layer's enabled status.
    myLocationClickListenerCheckbox.setEnabled(enable);
    myLocationClickListenerCheckbox.setChecked(enable);
    map.setOnMyLocationClickListener(enable ? this : null);
  }

  /** Called when the MyLocationClickListener checkbox is clicked. */
  public void onMyLocationClickListenerToggled(View view) {
    if (!checkReady()) {
      return;
    }

    map.setOnMyLocationClickListener(myLocationClickListenerCheckbox.isChecked() ? this : null);
  }

  @Override
  public void onMyLocationClick(@NonNull Location location) {
    Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
    if (marker == null) {
      marker =
          map.addMarker(new MarkerOptions().position(position).draggable(true).title("Marker"));
    } else {
      marker.setPosition(position);
    }
  }

  @Override
  public boolean onMyLocationButtonClick() {
    Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
    // Return false so that we don't consume the event and the default behavior still occurs
    // (the camera animates to the user's current position).
    return false;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
      return;
    }

    if (PermissionUtils.isPermissionGranted(
        permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
      // Enable the my location layer if the permission has been granted.
      enableMyLocation(true);
    } else {
      // Display the missing permission error dialog when the fragments resume.
      permissionDenied = true;
    }
  }

  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();
    if (permissionDenied) {
      // Permission was not granted, display error dialog.
      showMissingPermissionError();
      permissionDenied = false;
    }
  }

  /** Displays a dialog with error message explaining that the location permission is missing. */
  private void showMissingPermissionError() {
    PermissionUtils.PermissionDeniedDialog.newInstance(true)
        .show(getSupportFragmentManager(), "dialog");
  }

  @Override
  public void onInfoWindowClick(final Marker marker) {
    Toast.makeText(this, "InfoWindow clicked", Toast.LENGTH_SHORT).show();
  }

  @Override
  public boolean onMarkerClick(final Marker marker) {
    Toast.makeText(this, "Marker clicked", Toast.LENGTH_SHORT).show();
    // Return false to indicate that we have not consumed the event and that we wish
    // for the default behavior to occur (which is for the camera to move such that the
    // marker is centered and for the marker's info window to open, if it has one).
    return false;
  }
}
