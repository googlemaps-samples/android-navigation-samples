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

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveCanceledListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;

/** This shows how to change the camera position for the map. */
public class CameraDemoActivity extends AppCompatActivity
    implements OnCameraMoveStartedListener,
        OnCameraMoveListener,
        OnCameraMoveCanceledListener,
        OnCameraIdleListener,
        OnMapReadyCallback {
  private static final String TAG = CameraDemoActivity.class.getName();
  /**
   * The amount by which to scroll the camera. Note that this amount is in raw pixels, not dp
   * (density-independent pixels).
   */
  private static final int SCROLL_BY_PX = 100;

  public static final CameraPosition BONDI =
      new CameraPosition.Builder()
          .target(new LatLng(-33.891614, 151.276417))
          .zoom(15.5f)
          .bearing(300)
          .tilt(50)
          .build();

  public static final CameraPosition SYDNEY =
      new CameraPosition.Builder()
          .target(new LatLng(-33.87365, 151.20689))
          .zoom(15.5f)
          .bearing(0)
          .tilt(25)
          .build();

  private GoogleMap map;

  private CompoundButton animateToggle;
  private CompoundButton customDurationToggle;
  private SeekBar customDurationBar;
  private PolylineOptions currPolylineOptions;
  private boolean isCanceled = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent()
        .getBooleanExtra(
            ActivityIntents.EXTRA_SHOULD_USE_NAVIGATION_FLAVOR_FOR_DEMO,
            /* defaultValue= */ false)) {
      setContentView(R.layout.camera_demo_nav_flavor);
      performAdditionalSetup();
      SupportNavigationFragment navFragment =
          (SupportNavigationFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      navFragment.getMapAsync(this);
    } else {
      setContentView(R.layout.camera_demo_maps_flavor);
      performAdditionalSetup();
      SupportMapFragment mapFragment =
          (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
      mapFragment.getMapAsync(this);
    }
  }

  private void performAdditionalSetup() {
    animateToggle = (CompoundButton) findViewById(R.id.animate);
    customDurationToggle = (CompoundButton) findViewById(R.id.duration_toggle);
    customDurationBar = (SeekBar) findViewById(R.id.duration_bar);

    updateEnabledState();
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateEnabledState();
  }

  @Override
  public void onMapReady(GoogleMap map) {
    this.map = map;

    this.map.setOnCameraIdleListener(this);
    this.map.setOnCameraMoveStartedListener(this);
    this.map.setOnCameraMoveListener(this);
    this.map.setOnCameraMoveCanceledListener(this);

    // We will provide our own zoom controls.
    this.map.getUiSettings().setZoomControlsEnabled(false);

    // Show Sydney
    this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-33.87365, 151.20689), 10));
  }

  /**
   * When the map is not ready the CameraUpdateFactory cannot be used. This should be called on all
   * entry points that call methods on the Maps SDK for Android.
   */
  private boolean checkReady() {
    if (map == null) {
      Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  /** Called when the Go To Bondi button is clicked. */
  public void onGoToBondi(View view) {
    if (!checkReady()) {
      return;
    }

    changeCamera(CameraUpdateFactory.newCameraPosition(BONDI));
  }

  /** Called when the Animate To Sydney button is clicked. */
  public void onGoToSydney(View view) {
    if (!checkReady()) {
      return;
    }

    changeCamera(
        CameraUpdateFactory.newCameraPosition(SYDNEY),
        new CancelableCallback() {
          @Override
          public void onFinish() {
            Toast.makeText(getBaseContext(), "Animation to Sydney complete", Toast.LENGTH_SHORT)
                .show();
          }

          @Override
          public void onCancel() {
            Toast.makeText(getBaseContext(), "Animation to Sydney canceled", Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  /** Called when the stop button is clicked. */
  public void onStopAnimation(View view) {
    if (!checkReady()) {
      return;
    }

    map.stopAnimation();
  }

  /** Called when the zoom in button (the one with the +) is clicked. */
  public void onZoomIn(View view) {
    if (!checkReady()) {
      return;
    }

    changeCamera(CameraUpdateFactory.zoomIn());
  }

  /** Called when the zoom out button (the one with the -) is clicked. */
  public void onZoomOut(View view) {
    if (!checkReady()) {
      return;
    }

    changeCamera(CameraUpdateFactory.zoomOut());
  }

  /** Called when the tilt more button (the one with the /) is clicked. */
  public void onTiltMore(View view) {
    if (!checkReady()) {
      return;
    }

    CameraPosition currentCameraPosition = map.getCameraPosition();
    float currentTilt = currentCameraPosition.tilt;
    float newTilt = currentTilt + 10;

    newTilt = (newTilt > 90) ? 90 : newTilt;

    CameraPosition cameraPosition =
        new CameraPosition.Builder(currentCameraPosition).tilt(newTilt).build();

    changeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  }

  /** Called when the tilt less button (the one with the \) is clicked. */
  public void onTiltLess(View view) {
    if (!checkReady()) {
      return;
    }

    CameraPosition currentCameraPosition = map.getCameraPosition();

    float currentTilt = currentCameraPosition.tilt;

    float newTilt = currentTilt - 10;
    newTilt = (newTilt > 0) ? newTilt : 0;

    CameraPosition cameraPosition =
        new CameraPosition.Builder(currentCameraPosition).tilt(newTilt).build();

    changeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  }

  /** Called when the left arrow button is clicked. This causes the camera to move to the left */
  public void onScrollLeft(View view) {
    if (!checkReady()) {
      return;
    }

    changeCamera(CameraUpdateFactory.scrollBy(-SCROLL_BY_PX, 0));
  }

  /** Called when the right arrow button is clicked. This causes the camera to move to the right. */
  public void onScrollRight(View view) {
    if (!checkReady()) {
      return;
    }

    changeCamera(CameraUpdateFactory.scrollBy(SCROLL_BY_PX, 0));
  }

  /** Called when the up arrow button is clicked. The causes the camera to move up. */
  public void onScrollUp(View view) {
    if (!checkReady()) {
      return;
    }

    changeCamera(CameraUpdateFactory.scrollBy(0, -SCROLL_BY_PX));
  }

  /** Called when the down arrow button is clicked. This causes the camera to move down. */
  public void onScrollDown(View view) {
    if (!checkReady()) {
      return;
    }

    changeCamera(CameraUpdateFactory.scrollBy(0, SCROLL_BY_PX));
  }

  /** Called when the animate button is toggled */
  public void onToggleAnimate(View view) {
    updateEnabledState();
  }

  /** Called when the custom duration checkbox is toggled */
  public void onToggleCustomDuration(View view) {
    updateEnabledState();
  }

  /** Update the enabled state of the custom duration controls. */
  private void updateEnabledState() {
    customDurationToggle.setEnabled(animateToggle.isChecked());
    customDurationBar.setEnabled(animateToggle.isChecked() && customDurationToggle.isChecked());
  }

  private void changeCamera(CameraUpdate update) {
    changeCamera(update, null);
  }

  /**
   * Change the camera position by moving or animating the camera depending on the state of the
   * animate toggle button.
   */
  private void changeCamera(CameraUpdate update, CancelableCallback callback) {
    if (animateToggle.isChecked()) {
      if (customDurationToggle.isChecked()) {
        int duration = customDurationBar.getProgress();
        // The duration must be strictly positive so we make it at least 1.
        map.animateCamera(update, Math.max(duration, 1), callback);
      } else {
        map.animateCamera(update, callback);
      }
    } else {
      map.moveCamera(update);
    }
  }

  @Override
  public void onCameraMoveStarted(int reason) {
    if (!isCanceled) {
      map.clear();
    }

    String reasonStr = "UNKNOWN_REASON";
    PolylineOptions newPolylineOptions = new PolylineOptions().width(5);
    if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
      currPolylineOptions = newPolylineOptions.color(Color.BLUE);
      reasonStr = "GESTURE";
    } else if (reason == OnCameraMoveStartedListener.REASON_API_ANIMATION) {
      currPolylineOptions = newPolylineOptions.color(Color.RED);
      reasonStr = "API_ANIMATION";
    } else if (reason == OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
      currPolylineOptions = newPolylineOptions.color(Color.GREEN);
      reasonStr = "DEVELOPER_ANIMATION";
    }
    Log.i(TAG, "onCameraMoveStarted(" + reasonStr + ")");
    addCameraTargetToPath();
  }

  @Override
  public void onCameraMove() {
    if (currPolylineOptions != null) {
      addCameraTargetToPath();
    }
    Log.i(TAG, "onCameraMove");
  }

  @Override
  public void onCameraMoveCanceled() {
    if (currPolylineOptions != null) {
      addCameraTargetToPath();
      map.addPolyline(currPolylineOptions);
    }
    isCanceled = true;
    currPolylineOptions = null;
    Log.i(TAG, "onCameraMoveCancelled");
  }

  @Override
  public void onCameraIdle() {
    if (currPolylineOptions != null) {
      addCameraTargetToPath();
      map.addPolyline(currPolylineOptions);
    }
    currPolylineOptions = null;
    isCanceled = false;
    Log.i(TAG, "onCameraIdle");
  }

  private void addCameraTargetToPath() {
    LatLng target = map.getCameraPosition().target;
    currPolylineOptions.add(target);
  }
}
